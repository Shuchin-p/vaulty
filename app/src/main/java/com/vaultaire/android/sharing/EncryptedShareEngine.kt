package com.vaultaire.android.sharing

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable

@Serializable
data class SharePolicy(
    val expirationTimestampMs: Long? = null,
    val maxOpenCount: Int? = null,
    val disableExport: Boolean = false,
    val createdTimestampMs: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        return expirationTimestampMs != null && System.currentTimeMillis() > expirationTimestampMs
    }

    fun isCountExceeded(currentOpenCount: Int): Boolean {
        return maxOpenCount != null && currentOpenCount >= maxOpenCount
    }
}

data class EncryptedSharePackage(
    val version: Byte = 0x01,
    val salt: ByteArray,
    val iv: ByteArray,
    val policyHeader: ByteArray, // Authenticated Additional Data (AAD)
    val ciphertext: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedSharePackage

        if (version != other.version) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!policyHeader.contentEquals(other.policyHeader)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.toInt()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + policyHeader.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }
}

class EncryptedShareEngine {

    companion object {
        private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA512"
        private const val PBKDF2_ITERATIONS = 250_000
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val IV_SIZE_BYTES = 12
        private const val SALT_SIZE_BYTES = 16
        private const val POLICY_VERSION = 1
    }

    /**
     * Generates a cryptographically secure 12-word BIP39 sharing phrase with checksum.
     * Delegates to [SharingPhraseGenerator] which uses the full 2048-word BIP39 wordlist.
     */
    fun generateShareSecretPhrase(): String {
        return SharingPhraseGenerator.generateShareSecretPhrase()
    }

    /**
     * Validates a sharing phrase using BIP39 checksum verification.
     * Checks: exactly 12 words, all words in BIP39 wordlist, valid checksum.
     */
    fun validatePhrase(phrase: String): Boolean {
        return SharingPhraseGenerator.validatePhrase(phrase)
    }

    /**
     * Creates an encrypted share package from a [SharePayload] and sharing phrase.
     * 
     * The payload is serialized using [SharePayloadSerializer], then encrypted with
     * AES-256-GCM using a key derived from the phrase via PBKDF2 (250k iterations).
     * The policy is bound as AAD (Additional Authenticated Data) so any tampering
     * causes decryption to fail with AEADBadTagException.
     */
    fun createSharePackage(
        payload: SharePayload,
        secretPhrase: String,
        policy: SharePolicy
    ): EncryptedSharePackage {
        val salt = ByteArray(SALT_SIZE_BYTES)
        val iv = ByteArray(IV_SIZE_BYTES)
        SecureRandom().nextBytes(salt)
        SecureRandom().nextBytes(iv)

        val shareKey = deriveShareKey(secretPhrase, salt)
        val payloadBytes = SharePayloadSerializer.serialize(payload)
        val policyJson = serializePolicy(policy)
        val policyHeaderBytes = policyJson.toByteArray(StandardCharsets.UTF_8)

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, shareKey, gcmSpec)
        
        cipher.updateAAD(policyHeaderBytes)
        val ciphertext = cipher.doFinal(payloadBytes)

        return EncryptedSharePackage(
            version = 0x01,
            salt = salt,
            iv = iv,
            policyHeader = policyHeaderBytes,
            ciphertext = ciphertext
        )
    }

    /**
     * Unpacks and decrypts a share package, returning the [SharePayload] and [SharePolicy].
     * 
     * Verifies:
     * - Policy expiration (throws SecurityException if expired)
     * - Max opens not exceeded (throws SecurityException if exceeded)
     * - GCM authentication tag (throws AEADBadTagException if tampered)
     * 
     * @param pkg The encrypted share package
     * @param secretPhrase The 12-word sharing phrase
     * @param currentOpenCount Number of times this share has been opened (for maxOpen enforcement)
     * @return Pair of (policy, payload)
     */
    fun unpackSharePackage(
        pkg: EncryptedSharePackage,
        secretPhrase: String,
        currentOpenCount: Int = 0
    ): Pair<SharePolicy, SharePayload> {
        val policyJson = String(pkg.policyHeader, StandardCharsets.UTF_8)
        val policy = deserializePolicy(policyJson)

        if (policy.isExpired()) {
            throw SecurityException("Shared vault payload has expired.")
        }
        if (policy.isCountExceeded(currentOpenCount)) {
            throw SecurityException("Shared vault access limit exceeded (Max opens: ${policy.maxOpenCount}).")
        }

        val shareKey = deriveShareKey(secretPhrase, pkg.salt)
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, pkg.iv)
        cipher.init(Cipher.DECRYPT_MODE, shareKey, gcmSpec)
        
        cipher.updateAAD(pkg.policyHeader)
        val decryptedBytes = cipher.doFinal(pkg.ciphertext)

        val payload = SharePayloadSerializer.deserialize(decryptedBytes)
        return Pair(policy, payload)
    }

    private fun deriveShareKey(secretPhrase: String, salt: ByteArray): SecretKey {
        val cleanPhrase = SharingPhraseGenerator.normalizePhrase(secretPhrase)
        val spec = PBEKeySpec(cleanPhrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun serializePolicy(policy: SharePolicy): String {
        // Canonical JSON: sorted keys, no whitespace, null fields omitted
        val builder = StringBuilder()
        builder.append("{\"v\":").append(POLICY_VERSION)
        policy.expirationTimestampMs?.let { builder.append(",\"exp\":").append(it) }
        policy.maxOpenCount?.let { builder.append(",\"maxOpen\":").append(it) }
        if (policy.disableExport) builder.append(",\"noExport\":true")
        builder.append(",\"created\":").append(policy.createdTimestampMs).append("}")
        return builder.toString()
    }

    private fun deserializePolicy(json: String): SharePolicy {
        // Parse canonical JSON - keys are sorted alphabetically
        val expMatch = Regex("\"exp\":(\\d+)").find(json)?.groupValues?.get(1)
        val maxOpenMatch = Regex("\"maxOpen\":(\\d+)").find(json)?.groupValues?.get(1)
        val noExportMatch = Regex("\"noExport\":(true|false)").find(json)?.groupValues?.get(1)
        val createdMatch = Regex("\"created\":(\\d+)").find(json)?.groupValues?.get(1)

        return SharePolicy(
            expirationTimestampMs = expMatch?.toLongOrNull(),
            maxOpenCount = maxOpenMatch?.toIntOrNull(),
            disableExport = noExportMatch?.toBoolean() ?: false,
            createdTimestampMs = createdMatch?.toLongOrNull() ?: System.currentTimeMillis()
        )
    }
}