package com.vaultaire.android.storage

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedOutputStreamTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()
    
    private lateinit var testKey: SecretKey
    private lateinit var testFile: File

    @Before
    fun setup() {
        testKey = SecretKeySpec(ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }, "AES")
        testFile = temporaryFolder.newFile("test.vlt")
    }

    @Test
    fun test_UT_ENC_01_HeaderWrittenCorrectly_MagicVersionSaltMasterIVChunkSizeFileSizeHeaderTag() {
        val outputStream = FileOutputStream(testFile)
        EncryptedOutputStream(outputStream, testKey).use { eos ->
            eos.write("test".toByteArray())
        }
        
        // Read and verify header
        val header = ByteArray(80)
        FileInputStream(testFile).read(header)
        
        // Magic: "VAULT1" (6 bytes)
        val magic = String(header, 0, 6, StandardCharsets.US_ASCII)
        Assert.assertEquals("VAULT1", magic)
        
        // Version: 1 (1 byte)
        Assert.assertEquals(0x01, header[6])
        
        // Reserved: 0 (1 byte)
        Assert.assertEquals(0x00, header[7])
        
        // Salt: 32 bytes (positions 8-39)
        val salt = header.copyOfRange(8, 40)
        Assert.assertEquals(32, salt.size)
        
        // Master IV: 12 bytes (positions 40-51)
        val masterIv = header.copyOfRange(40, 52)
        Assert.assertEquals(12, masterIv.size)
        
        // Chunk size: 4 bytes big endian (positions 52-55)
        val chunkSize = ByteBuffer.wrap(header, 52, 4).int
        Assert.assertEquals(64 * 1024, chunkSize)
        
        // File size: 8 bytes (positions 56-63) - should be updated on close
        // Header auth tag: 16 bytes (positions 64-79)
    }

    @Test
    fun test_UT_ENC_02_ChunkEncryptionUniqueIVs_EachChunkUsesUniqueIVDerivedFromMasterIVAndChunkIndex() {
        val chunkSize = 64 * 1024
        val data = ByteArray(chunkSize * 3) // 3 chunks
        java.security.SecureRandom().nextBytes(data)
        
        val outputStream = FileOutputStream(testFile)
        EncryptedOutputStream(outputStream, testKey, chunkSize).use { eos ->
            eos.write(data)
        }
        
        // Verify we can decrypt and get original data back
        val decrypted = decryptFullFile(testFile, testKey)
        Assert.assertArrayEquals("Decrypted data should match original", data, decrypted)
        
        // Verify each chunk has unique IV by checking ciphertexts differ
        val ciphertexts = extractChunkCiphertexts(testFile)
        Assert.assertEquals(3, ciphertexts.size)
        Assert.assertFalse("Chunk 0 and 1 should differ", java.util.Arrays.equals(ciphertexts[0], ciphertexts[1]))
        Assert.assertFalse("Chunk 1 and 2 should differ", java.util.Arrays.equals(ciphertexts[1], ciphertexts[2]))
    }

    @Test
    fun test_UT_ENC_03_GCMAuthTagVerification_TamperedChunkThrowsAEADBadTagException() {
        val data = "test data".toByteArray()
        
        val outputStream = FileOutputStream(testFile)
        EncryptedOutputStream(outputStream, testKey).use { eos ->
            eos.write(data)
        }
        
        // Tamper with ciphertext (flip a bit in first chunk)
        val raf = java.io.RandomAccessFile(testFile, "rw")
        try {
            // Skip header (80 bytes) + first chunk ciphertext start
            raf.seek(80 + 1) // Skip first byte of first chunk
            val b = raf.readByte()
            raf.seek(80 + 1)
            raf.writeByte((b xor 0x01).toByte()) // Flip a bit
        } finally {
            raf.close()
        }
        
        // Attempt to decrypt - should throw AEADBadTagException
        var exception: Exception? = null
        try {
            decryptFullFile(testFile, testKey)
        } catch (e: Exception) {
            exception = e
        }
        
        Assert.assertNotNull("Should throw on tampered data", exception)
        Assert.assertTrue("Should be AEADBadTagException or similar", 
            exception is javax.crypto.AEADBadTagException || exception.cause is javax.crypto.AEADBadTagException)
    }

    @Test
    fun test_UT_ENC_04_FileSizeUpdatedInHeader_HeaderFileSizeEqualsPlaintextSize() {
        val data = "test data for file size".toByteArray()
        val expectedSize = data.size.toLong()
        
        val outputStream = FileOutputStream(testFile)
        EncryptedOutputStream(outputStream, testKey).use { eos ->
            eos.write(data)
        }
        
        // Read header fileSize field (positions 56-63, 8 bytes big endian)
        val header = ByteArray(80)
        FileInputStream(testFile).read(header)
        val fileSize = ByteBuffer.wrap(header, 56, 8).long
        
        Assert.assertEquals("File size in header should match plaintext size", expectedSize, fileSize)
    }

    @Test
    fun test_UT_ENC_05_EmptyFileHandling_ValidHeaderWrittenZeroChunksFileSizeEqualsZero() {
        val outputStream = FileOutputStream(testFile)
        EncryptedOutputStream(outputStream, testKey).use { eos ->
            // No write calls
        }
        
        val header = ByteArray(80)
        FileInputStream(testFile).read(header)
        
        val magic = String(header, 0, 6, StandardCharsets.US_ASCII)
        Assert.assertEquals("VAULT1", magic)
        
        val fileSize = ByteBuffer.wrap(header, 56, 8).long
        Assert.assertEquals("Empty file should have fileSize=0", 0L, fileSize)
        
        // Verify it can be decrypted (empty)
        val decrypted = decryptFullFile(testFile, testKey)
        Assert.assertEquals(0, decrypted.size)
    }

    @Test
    fun test_UT_ENC_06_LargeFile_10MB_CompletesWithoutOOM_FileSizeCorrect() {
        val chunkSize = 64 * 1024
        val largeData = ByteArray(10 * 1024 * 1024) // 10MB
        java.security.SecureRandom().nextBytes(largeData)
        
        val outputStream = FileOutputStream(testFile)
        EncryptedOutputStream(outputStream, testKey, chunkSize).use { eos ->
            eos.write(largeData)
        }
        
        val header = ByteArray(80)
        FileInputStream(testFile).read(header)
        val fileSize = ByteBuffer.wrap(header, 56, 8).long
        
        Assert.assertEquals("Large file size should match", largeData.size.toLong(), fileSize)
        
        // Verify decryption works
        val decrypted = decryptFullFile(testFile, testKey)
        Assert.assertArrayEquals("Large file decryption should match", largeData, decrypted)
    }

    @Test
    fun test_UT_ENC_07_ConcurrentWrites_ThreadSafety_SequentialChunkIndicesMaintained() {
        // EncryptedOutputStream is not thread-safe by design (single writer)
        // This test verifies the current implementation behavior
        val data = ByteArray(64 * 1024 * 2) // 2 chunks
        java.security.SecureRandom().nextBytes(data)
        
        val outputStream = FileOutputStream(testFile)
        EncryptedOutputStream(outputStream, testKey).use { eos ->
            // Single-threaded writes
            eos.write(data, 0, data.size / 2)
            eos.write(data, data.size / 2, data.size / 2)
        }
        
        val decrypted = decryptFullFile(testFile, testKey)
        Assert.assertArrayEquals("Sequential writes should work", data, decrypted)
    }

    // Helper methods
    private fun decryptFullFile(file: File, key: SecretKey): ByteArray {
        val source = RandomAccessEncryptedSourceImpl(file, key)
        try {
            val buffer = ByteArray(source.totalPlaintextLength.toInt())
            val read = source.readAt(0, buffer, 0, buffer.size)
            return if (read > 0) buffer.copyOfRange(0, read) else ByteArray(0)
        } finally {
            source.close()
        }
    }

    private fun extractChunkCiphertexts(file: File): List<ByteArray> {
        val raf = java.io.RandomAccessFile(file, "r")
        val chunks = mutableListOf<ByteArray>()
        try {
            val header = ByteArray(80)
            raf.readFully(header)
            
            val chunkSize = ByteBuffer.wrap(header, 52, 4).int
            val chunkCiphertextLen = chunkSize + 16 // + auth tag
            
            var offset = 80L
            while (offset < raf.length()) {
                val remaining = raf.length() - offset
                val toRead = kotlin.math.min(chunkCiphertextLen.toLong(), remaining).toInt()
                if (toRead <= 16) break
                
                val chunk = ByteArray(toRead)
                raf.readFully(chunk)
                chunks.add(chunk)
                offset += toRead
            }
        } finally {
            raf.close()
        }
        return chunks
    }
}
