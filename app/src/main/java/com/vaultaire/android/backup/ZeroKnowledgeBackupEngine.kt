package com.vaultaire.android.backup

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class BackupHeader(
    val magic: ByteArray = "VLTBK01".toByteArray(StandardCharsets.UTF_8), // 7 bytes
    val salt: ByteArray, // 16 bytes
    val iv: ByteArray, // 12 bytes
    val originalSize: Long,
    val paddedSize: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BackupHeader

        if (!magic.contentEquals(other.magic)) return false
        if (!salt.contentEquals(other.salt)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (originalSize != other.originalSize) return false
        if (paddedSize != other.paddedSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = magic.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + originalSize.hashCode()
        result = 31 * result + paddedSize.hashCode()
        return result
    }

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE_BYTES)
        buffer.put(magic)
        buffer.put(salt)
        buffer.put(iv)
        buffer.putLong(originalSize)
        buffer.putLong(paddedSize)
        return buffer.array()
    }

    companion object {
        const val HEADER_SIZE_BYTES = 7 + 16 + 12 + 8 + 8 // 51 bytes

        fun fromInputStream(stream: InputStream): BackupHeader {
            val bytes = ByteArray(HEADER_SIZE_BYTES)
            val read = stream.read(bytes)
            if (read < HEADER_SIZE_BYTES) {
                throw IllegalArgumentException("Invalid backup file: header truncated.")
            }
            val buffer = ByteBuffer.wrap(bytes)
            val magic = ByteArray(7)
            buffer.get(magic)
            if (!magic.contentEquals("VLTBK01".toByteArray(StandardCharsets.UTF_8))) {
                throw IllegalArgumentException("Invalid backup file magic header.")
            }
            val salt = ByteArray(16)
            buffer.get(salt)
            val iv = ByteArray(12)
            buffer.get(iv)
            val originalSize = buffer.long
            val paddedSize = buffer.long

            return BackupHeader(magic, salt, iv, originalSize, paddedSize)
        }
    }
}

class ZeroKnowledgeBackupEngine {

    companion object {
        private const val CIPHER_ALGO = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val IV_SIZE_BYTES = 12
        private const val SALT_SIZE_BYTES = 16
        private const val CHUNK_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB exactly
    }

    /**
     * Calculates the padded size for a given original size.
     * Always rounds up to the next multiple of CHUNK_SIZE_BYTES (10 MB).
     * 
     * Examples:
     * - 100 KB -> 10 MB
     * - 9.9 MB -> 10 MB
     * - 10.1 MB -> 20 MB
     * - 55 MB -> 60 MB
     */
    fun calculatePaddedSize(originalSize: Long): Long {
        if (originalSize <= 0) return CHUNK_SIZE_BYTES
        return ((originalSize + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES) * CHUNK_SIZE_BYTES
    }

    /**
     * Encrypts a backup stream using the provided backup key (not the master vault key).
     * Uses fixed-size 10 MB padding with zero bytes.
     * Header is authenticated via AAD.
     */
    fun encryptBackup(
        inputStream: InputStream,
        outputStream: OutputStream,
        originalSize: Long,
        backupKey: SecretKey
    ) {
        val salt = ByteArray(SALT_SIZE_BYTES)
        val iv = ByteArray(IV_SIZE_BYTES)
        SecureRandom().nextBytes(salt)
        SecureRandom().nextBytes(iv)

        val paddedSize = calculatePaddedSize(originalSize)
        val paddingLength = (paddedSize - originalSize).toInt()

        val header = BackupHeader(
            salt = salt,
            iv = iv,
            originalSize = originalSize,
            paddedSize = paddedSize
        )
        val headerBytes = header.toByteArray()
        outputStream.write(headerBytes)

        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, backupKey, gcmSpec)
        
        cipher.updateAAD(headerBytes)

        val cipherOutputStream = CipherOutputStream(outputStream, cipher)
        val buffer = ByteArray(8192)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            cipherOutputStream.write(buffer, 0, bytesRead)
        }

        if (paddingLength > 0) {
            val paddingBuffer = ByteArray(minOf(paddingLength, 8192))
            var remainingPadding = paddingLength
            while (remainingPadding > 0) {
                val toWrite = minOf(remainingPadding, paddingBuffer.size)
                cipherOutputStream.write(paddingBuffer, 0, toWrite)
                remainingPadding -= toWrite
            }
        }

        cipherOutputStream.flush()
        cipherOutputStream.close()
    }

    /**
     * Decrypts a backup stream using the provided backup key.
     * Verifies header integrity via AAD and decrypts only the original data.
     */
    fun decryptBackup(
        inputStream: InputStream,
        outputStream: OutputStream,
        backupKey: SecretKey
    ) {
        val header = BackupHeader.fromInputStream(inputStream)
        val headerBytes = header.toByteArray()

        val cipher = Cipher.getInstance(CIPHER_ALGO)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, header.iv)
        cipher.init(Cipher.DECRYPT_MODE, backupKey, gcmSpec)
        
        cipher.updateAAD(headerBytes)

        val cipherInputStream = CipherInputStream(inputStream, cipher)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var remainingBytesToRead = header.originalSize

        while (remainingBytesToRead > 0) {
            val maxToRead = minOf(buffer.size.toLong(), remainingBytesToRead).toInt()
            bytesRead = cipherInputStream.read(buffer, 0, maxToRead)
            if (bytesRead == -1) break
            outputStream.write(buffer, 0, bytesRead)
            remainingBytesToRead -= bytesRead
        }

        outputStream.flush()
    }
}