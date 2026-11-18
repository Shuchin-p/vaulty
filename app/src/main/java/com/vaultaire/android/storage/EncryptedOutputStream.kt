package com.vaultaire.android.storage

import java.io.OutputStream
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedOutputStream(
    private val outputStream: OutputStream,
    private val key: SecretKey,
    val chunkSize: Int = 64 * 1024 // 64 KB default chunking
) : OutputStream() {

    private val cipher: Cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private val buffer: ByteBuffer = ByteBuffer.allocate(chunkSize)
    private val masterIv = ByteArray(12).apply { java.security.SecureRandom().nextBytes(this) }
    private var chunkIndex: Long = 0L
    private var totalBytesWritten: Long = 0L
    private var isHeaderWritten = false

    private fun writeHeader() {
        outputStream.write("VAULT1".toByteArray(Charsets.US_ASCII)) // 6 Bytes
        outputStream.write(0x01) // Version 1 (1 Byte)
        outputStream.write(0x00) // Reserved (1 Byte)
        
        // Salt placeholder (32 Bytes)
        val salt = ByteArray(32)
        outputStream.write(salt)
        
        // Master IV (12 Bytes)
        outputStream.write(masterIv)
        
        // Chunk Size (4 Bytes, Big Endian)
        outputStream.write(ByteBuffer.allocate(4).putInt(chunkSize).array())
        
        // Placeholder for File Size (8 Bytes)
        outputStream.write(ByteBuffer.allocate(8).putLong(0L).array())
        
        // Header Auth Tag Placeholder (16 Bytes)
        outputStream.write(ByteArray(16))
        
        isHeaderWritten = true
    }

    override fun write(b: Int) {
        write(byteArrayOf(b.toByte()), 0, 1)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (!isHeaderWritten) writeHeader()

        var bytesRemaining = len
        var currentOffset = off

        while (bytesRemaining > 0) {
            val spaceAvailable = buffer.remaining()
            val bytesToCopy = minOf(bytesRemaining, spaceAvailable)
            
            buffer.put(b, currentOffset, bytesToCopy)
            totalBytesWritten += bytesToCopy
            currentOffset += bytesToCopy
            bytesRemaining -= bytesToCopy

            if (!buffer.hasRemaining()) {
                flushChunk(isFinal = false)
            }
        }
    }

    private fun flushChunk(isFinal: Boolean) {
        buffer.flip()
        if (buffer.remaining() == 0 && !isFinal) return

        val chunkIv = deriveChunkIv(masterIv, chunkIndex)
        val gcmSpec = GCMParameterSpec(128, chunkIv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val plaintextBytes = ByteArray(buffer.remaining())
        buffer.get(plaintextBytes)
        
        val encryptedChunk = cipher.doFinal(plaintextBytes)
        outputStream.write(encryptedChunk) // Payload + 16-byte Tag appended by Cipher
        
        buffer.clear()
        chunkIndex++
    }

    private fun deriveChunkIv(baseIv: ByteArray, index: Long): ByteArray {
        val iv = baseIv.clone()
        val indexBuffer = ByteBuffer.allocate(8).putLong(index).array()
        for (i in 0 until 8) {
            iv[12 - 8 + i] = (iv[12 - 8 + i].toInt() xor indexBuffer[i].toInt()).toByte()
        }
        return iv
    }

    override fun flush() {
        outputStream.flush()
    }

    override fun close() {
        if (!isHeaderWritten) writeHeader()
        flushChunk(isFinal = true)
        outputStream.flush()
        outputStream.close()
    }
}
