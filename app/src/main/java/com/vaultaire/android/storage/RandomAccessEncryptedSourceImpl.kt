package com.vaultaire.android.storage

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class RandomAccessEncryptedSourceImpl(
    private val file: File,
    private val key: SecretKey
) : RandomAccessEncryptedSource {

    private val raf = RandomAccessFile(file, "r")
    override val chunkSize: Int
    override val totalPlaintextLength: Long
    private val masterIv = ByteArray(12)
    private val headerOffset = 80L

    init {
        val header = ByteArray(80)
        raf.readFully(header)
        
        val magic = String(header, 0, 6, Charsets.US_ASCII)
        require(magic == "VAULT1") { "Invalid vault header magic" }

        System.arraycopy(header, 40, masterIv, 0, 12)
        chunkSize = ByteBuffer.wrap(header, 52, 4).int
        totalPlaintextLength = file.length() - headerOffset
    }

    override fun readAt(plaintextOffset: Long, dest: ByteArray, destOffset: Int, length: Int): Int {
        if (plaintextOffset >= totalPlaintextLength) return -1

        val startChunk = plaintextOffset / chunkSize
        val offsetInStartChunk = (plaintextOffset % chunkSize).toInt()
        val endChunk = minOf((plaintextOffset + length - 1) / chunkSize, (totalPlaintextLength - 1) / chunkSize)

        var totalBytesRead = 0
        var currentDestOffset = destOffset
        var remainingToRead = length

        for (c in startChunk..endChunk) {
            val chunkCiphertextLen = chunkSize + 16 // 16 bytes auth tag
            val fileOffsetForChunk = headerOffset + (c * chunkCiphertextLen)
            
            val actualCipherLen = minOf(chunkCiphertextLen.toLong(), raf.length() - fileOffsetForChunk).toInt()
            if (actualCipherLen <= 16) break

            val chunkBuffer = ByteArray(actualCipherLen)
            synchronized(raf) {
                raf.seek(fileOffsetForChunk)
                raf.readFully(chunkBuffer)
            }

            val decryptedChunk = decryptChunk(c, chunkBuffer)
            
            val readStart = if (c == startChunk) offsetInStartChunk else 0
            val bytesAvailableInChunk = decryptedChunk.size - readStart
            val bytesToCopy = minOf(remainingToRead, bytesAvailableInChunk)

            System.arraycopy(decryptedChunk, readStart, dest, currentDestOffset, bytesToCopy)

            totalBytesRead += bytesToCopy
            currentDestOffset += bytesToCopy
            remainingToRead -= bytesToCopy

            if (remainingToRead <= 0) break
        }

        return if (totalBytesRead > 0) totalBytesRead else -1
    }

    private fun decryptChunk(index: Long, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val chunkIv = deriveChunkIv(masterIv, index)
        val gcmSpec = GCMParameterSpec(128, chunkIv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        return cipher.doFinal(ciphertext)
    }

    private fun deriveChunkIv(baseIv: ByteArray, index: Long): ByteArray {
        val iv = baseIv.clone()
        val indexBuffer = ByteBuffer.allocate(8).putLong(index).array()
        for (i in 0 until 8) {
            iv[12 - 8 + i] = (iv[12 - 8 + i].toInt() xor indexBuffer[i].toInt()).toByte()
        }
        return iv
    }

    override fun close() {
        raf.close()
    }
}
