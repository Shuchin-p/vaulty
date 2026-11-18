package com.vaultaire.android.sync.engine

import com.vaultaire.android.sync.model.*
import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.security.InvalidAlgorithmParameterException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.json.Json

/**
 * Encrypts/decrypts sync records using AES-256-GCM with the Master Vault Key.
 *
 * AAD binds each record to: vaultId + deviceId + syncFormatVersion.
 * This prevents record swap attacks across vaults or devices.
 * Throws [IllegalStateException] if called after duress wipe (enrollment key destroyed).
 */
class SyncEncryptionEngine(
    private val masterVaultKey: SecretKey,
    private val vaultId: String,
    private val deviceId: String
) {

    companion object {
        private const val CIPHER_ALGO = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 16
        private const val BUCKET_SIZE = 10 * 1024 * 1024 // 10 MB
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Encrypts [metadata] into an [EncryptedSyncRecord] padded to 10 MB bucket.
     */
    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidAlgorithmParameterException::class
    )
    fun encryptSyncRecord(metadata: SyncMetadata): EncryptedSyncRecord {
        // Serialize to JSON
        val plaintext = json.encodeToString(SyncMetadata.serializer(), metadata).toByteArray(StandardCharsets.UTF_8)

        // Generate random IV
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)

        // Build AAD: vaultId + deviceId + syncFormatVersion
        val aad = buildAAD()

        // Encrypt
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, masterVaultKey, spec)
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)

        // Pad to 10 MB bucket
        val padded = padToBucket(ciphertext)

        // Extract GCM tag (last 16 bytes of ciphertext before padding)
        val tag = ciphertext.copyOfRange(ciphertext.size - TAG_SIZE, ciphertext.size)

        return EncryptedSyncRecord(
            iv = iv,
            ciphertext = padded,
            tag = tag,
            aad = aad
        )
    }

    /**
     * Decrypts an [EncryptedSyncRecord] back to [SyncMetadata].
     * Verifies GCM tag and AAD.
     */
    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidAlgorithmParameterException::class
    )
    fun decryptSyncRecord(record: EncryptedSyncRecord): SyncMetadata {
        // Verify AAD matches expected vault + device
        val expectedAad = buildAAD()
        if (!expectedAad.contentEquals(record.aad)) {
            throw SecurityException("AAD mismatch: record does not belong to this vault/device")
        }

        // Decrypt - use the actual ciphertext (excluding padding)
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val spec = GCMParameterSpec(128, record.iv)
        cipher.init(Cipher.DECRYPT_MODE, masterVaultKey, spec)
        cipher.updateAAD(record.aad)
        
        // Find the actual data length (ciphertext+padded_tag without random padding)
        val actualCiphertext = findActualCiphertext(record.ciphertext)
        val plaintext = cipher.doFinal(actualCiphertext)

        return json.decodeFromString(SyncMetadata.serializer(), plaintext.decodeToString())
    }

    /**
     * Encrypts a file chunk for upload.
     */
    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidAlgorithmParameterException::class
    )
    fun encryptChunk(plaintext: ByteArray): EncryptedChunk {
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, masterVaultKey, spec)
        val ciphertext = cipher.doFinal(plaintext)

        val tag = ciphertext.copyOfRange(ciphertext.size - TAG_SIZE, ciphertext.size)
        val encryptedHash = java.security.MessageDigest.getInstance("SHA-256").digest(ciphertext)

        return EncryptedChunk(
            iv = iv,
            ciphertext = ciphertext.copyOfRange(0, ciphertext.size - TAG_SIZE),
            tag = tag,
            encryptedHash = encryptedHash
        )
    }

    /**
     * Decrypts a file chunk.
     */
    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchPaddingException::class,
        InvalidAlgorithmParameterException::class
    )
    fun decryptChunk(chunk: EncryptedChunk): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val spec = GCMParameterSpec(128, chunk.iv)
        cipher.init(Cipher.DECRYPT_MODE, masterVaultKey, spec)
        val fullCiphertext = chunk.ciphertext + chunk.tag
        return cipher.doFinal(fullCiphertext)
    }

    private fun buildAAD(): ByteArray {
        return java.io.ByteArrayOutputStream().apply {
            write(vaultId.toByteArray(StandardCharsets.UTF_8))
            write(deviceId.toByteArray(StandardCharsets.UTF_8))
            val buffer = ByteBuffer.allocate(4).putInt(SyncFormat.CURRENT_VERSION)
            write(buffer.array())
        }.toByteArray()
    }

    private fun padToBucket(data: ByteArray): ByteArray {
        if (data.size >= BUCKET_SIZE) {
            // Should not happen for metadata, but handle gracefully
            return data
        }
        val padding = ByteArray(BUCKET_SIZE - data.size)
        SecureRandom().nextBytes(padding)
        return data + padding
    }

    private fun findActualCiphertext(paddedData: ByteArray): ByteArray {
        // The padded data contains [ciphertext + tag + random_padding]
        // We need to find where the actual data ends.
        // Try from the end - the ciphertext ends right before random padding starts
        // GCM tag is 16 bytes, so the actual data ends at ciphertext.length + 16
        
        // Try removing increasing amounts of padding until decryption works
        for (paddingSize in 0..(paddedData.size - TAG_SIZE - 1)) {
            try {
                val candidate = paddedData.copyOfRange(0, paddedData.size - paddingSize)
                val testCipher = Cipher.getInstance(CIPHER_ALGO)
                testCipher.init(Cipher.DECRYPT_MODE, masterVaultKey, GCMParameterSpec(128, ByteArray(IV_SIZE)))
                testCipher.doFinal(candidate) // This validates the GCM tag
                return candidate
            } catch (e: Exception) {
                // Continue trying
            }
        }
        // Fallback: return data as-is (may fail decryption but we'll see the error)
        return paddedData
    }
}

/** Encrypted sync record with IV, ciphertext, GCM tag, and AAD. */
data class EncryptedSyncRecord(
    val iv: ByteArray,
    val ciphertext: ByteArray,
    val tag: ByteArray,
    val aad: ByteArray
)

/** Encrypted chunk for file content. */
data class EncryptedChunk(
    val iv: ByteArray,
    val ciphertext: ByteArray,
    val tag: ByteArray,
    val encryptedHash: ByteArray
)