package com.vaultaire.android.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class ZeroKnowledgeBackupEngineTest {

    private val engine = ZeroKnowledgeBackupEngine()
    private val keyGenerator = KeyGenerator.getInstance("AES").apply { init(256) }

    private fun generateKey(): SecretKey = keyGenerator.generateKey()

    @Test
    fun `calculatePaddedSize rounds up to 10 MB chunks`() {
        val testCases = listOf(
            100_000L to 10 * 1024 * 1024L,        // 100 KB -> 10 MB
            9_900_000L to 10 * 1024 * 1024L,      // 9.9 MB -> 10 MB
            10_100_000L to 20 * 1024 * 1024L,     // 10.1 MB -> 20 MB
            10 * 1024 * 1024L to 10 * 1024 * 1024L, // 10 MB -> 10 MB
            20 * 1024 * 1024L to 20 * 1024 * 1024L, // 20 MB -> 20 MB
            55 * 1024 * 1024L to 60 * 1024 * 1024L, // 55 MB -> 60 MB
            0L to 10 * 1024 * 1024L,               // 0 -> 10 MB
            1L to 10 * 1024 * 1024L                // 1 byte -> 10 MB
        )

        for ((input, expected) in testCases) {
            val result = engine.calculatePaddedSize(input)
            assertEquals("Failed for input $input: expected $expected, got $result", expected, result)
        }
    }

    @Test
    fun `encryptBackup produces valid header with magic bytes`() {
        val originalData = "Test backup data".toByteArray()
        val key = generateKey()

        val output = ByteArrayOutputStream()
        val input = ByteArrayInputStream(originalData)
        
        engine.encryptBackup(input, output, originalData.size.toLong(), key)
        
        val encrypted = output.toByteArray()
        
        // Check header magic
        val header = BackupHeader.fromInputStream(ByteArrayInputStream(encrypted))
        assertArrayEquals("VLTBK01".toByteArray(StandardCharsets.UTF_8), header.magic)
        
        // Check sizes
        assertEquals(originalData.size.toLong(), header.originalSize)
        assertEquals(engine.calculatePaddedSize(originalData.size.toLong()), header.paddedSize)
        assertEquals(16, header.salt.size)
        assertEquals(12, header.iv.size)
    }

    @Test
    fun `encryptDecrypt round trip preserves data`() {
        val originalData = "Test backup data that should be preserved exactly".toByteArray()
        val key = generateKey()

        // Encrypt
        val encryptedOutput = ByteArrayOutputStream()
        val encryptedInput = ByteArrayInputStream(originalData)
        engine.encryptBackup(encryptedInput, encryptedOutput, originalData.size.toLong(), key)
        val encryptedData = encryptedOutput.toByteArray()

        // Decrypt
        val decryptedOutput = ByteArrayOutputStream()
        val decryptedInput = ByteArrayInputStream(encryptedData)
        engine.decryptBackup(decryptedInput, decryptedOutput, key)
        val decryptedData = decryptedOutput.toByteArray()

        assertArrayEquals(originalData, decryptedData)
    }

    @Test
    fun `header AAD tampering causes AEADBadTagException`() {
        val originalData = "Test data".toByteArray()
        val key = generateKey()

        // Encrypt
        val encryptedOutput = ByteArrayOutputStream()
        val encryptedInput = ByteArrayInputStream(originalData)
        engine.encryptBackup(encryptedInput, encryptedOutput, originalData.size.toLong(), key)
        val encryptedData = encryptedOutput.toByteArray()

        // Tamper with header (change originalSize)
        val tamperedData = encryptedData.copyOf()
        tamperedData[BackupHeader.HEADER_SIZE_BYTES - 16 + 7] = (tamperedData[BackupHeader.HEADER_SIZE_BYTES - 16 + 7] + 1).toByte()

        // Decrypt should fail
        val decryptedOutput = ByteArrayOutputStream()
        val decryptedInput = ByteArrayInputStream(tamperedData)
        assertThrows(AEADBadTagException::class.java) {
            engine.decryptBackup(decryptedInput, decryptedOutput, key)
        }
    }

    @Test
    fun `ciphertext tampering causes AEADBadTagException`() {
        val originalData = "Test data for tampering test".toByteArray()
        val key = generateKey()

        // Encrypt
        val encryptedOutput = ByteArrayOutputStream()
        val encryptedInput = ByteArrayInputStream(originalData)
        engine.encryptBackup(encryptedInput, encryptedOutput, originalData.size.toLong(), key)
        val encryptedData = encryptedOutput.toByteArray()

        // Tamper with ciphertext (after header)
        val tamperedData = encryptedData.copyOf()
        val headerEnd = BackupHeader.HEADER_SIZE_BYTES
        if (tamperedData.size > headerEnd) {
            tamperedData[headerEnd] = (tamperedData[headerEnd] + 1).toByte()
        }

        // Decrypt should fail
        val decryptedOutput = ByteArrayOutputStream()
        val decryptedInput = ByteArrayInputStream(tamperedData)
        assertThrows(AEADBadTagException::class.java) {
            engine.decryptBackup(decryptedInput, decryptedOutput, key)
        }
    }

    @Test
    fun `zero padding used for padding bytes`() {
        val originalData = "Short".toByteArray() // 5 bytes
        val key = generateKey()

        val encryptedOutput = ByteArrayOutputStream()
        val encryptedInput = ByteArrayInputStream(originalData)
        engine.encryptBackup(encryptedInput, encryptedOutput, originalData.size.toLong(), key)
        val encryptedData = encryptedOutput.toByteArray()

        // The encrypted data should be exactly one chunk (10 MB)
        val header = BackupHeader.fromInputStream(ByteArrayInputStream(encryptedData))
        assertEquals(10 * 1024 * 1024L, header.paddedSize)
        assertEquals(originalData.size.toLong(), header.originalSize)
    }

    @Test
    fun `large file 50 MB works without OOM`() {
        val key = generateKey()
        val size = 50 * 1024 * 1024 // 50 MB
        val originalData = ByteArray(size) { (it % 256).toByte() }

        // Encrypt
        val encryptedOutput = ByteArrayOutputStream()
        val encryptedInput = ByteArrayInputStream(originalData)
        engine.encryptBackup(encryptedInput, encryptedOutput, originalData.size.toLong(), key)
        val encryptedData = encryptedOutput.toByteArray()

        // Should be 6 chunks of 10 MB = 60 MB padded
        val header = BackupHeader.fromInputStream(ByteArrayInputStream(encryptedData))
        assertEquals(60 * 1024 * 1024L, header.paddedSize)
        assertEquals(size.toLong(), header.originalSize)

        // Decrypt
        val decryptedOutput = ByteArrayOutputStream()
        val decryptedInput = ByteArrayInputStream(encryptedData)
        engine.decryptBackup(decryptedInput, decryptedOutput, key)
        val decryptedData = decryptedOutput.toByteArray()

        assertArrayEquals(originalData, decryptedData)
    }

    @Test
    fun `different keys produce different ciphertext`() {
        val originalData = "Test data".toByteArray()
        val key1 = generateKey()
        val key2 = generateKey()

        val encryptedOutput1 = ByteArrayOutputStream()
        engine.encryptBackup(ByteArrayInputStream(originalData), encryptedOutput1, originalData.size.toLong(), key1)
        val encrypted1 = encryptedOutput1.toByteArray()

        val encryptedOutput2 = ByteArrayOutputStream()
        engine.encryptBackup(ByteArrayInputStream(originalData), encryptedOutput2, originalData.size.toLong(), key2)
        val encrypted2 = encryptedOutput2.toByteArray()

        // Ciphertext should be different (different IVs and keys)
        assertNotNull(encrypted1)
        assertNotNull(encrypted2)
        assertTrue("Ciphertext should differ with different keys", !encrypted1.contentEquals(encrypted2))
    }

    @Test
    fun `same key and data produces different ciphertext each time (random IV/salt)`() {
        val originalData = "Test data".toByteArray()
        val key = generateKey()

        val encryptedOutput1 = ByteArrayOutputStream()
        engine.encryptBackup(ByteArrayInputStream(originalData), encryptedOutput1, originalData.size.toLong(), key)
        val encrypted1 = encryptedOutput1.toByteArray()

        val encryptedOutput2 = ByteArrayOutputStream()
        engine.encryptBackup(ByteArrayInputStream(originalData), encryptedOutput2, originalData.size.toLong(), key)
        val encrypted2 = encryptedOutput2.toByteArray()

        // Should have different IVs and salts, so ciphertext differs
        assertTrue("Ciphertext should differ due to random IV/salt", !encrypted1.contentEquals(encrypted2))
        
        // But both should decrypt correctly
        val decrypted1 = ByteArrayOutputStream()
        engine.decryptBackup(ByteArrayInputStream(encrypted1), decrypted1, key)
        assertArrayEquals(originalData, decrypted1.toByteArray())

        val decrypted2 = ByteArrayOutputStream()
        engine.decryptBackup(ByteArrayInputStream(encrypted2), decrypted2, key)
        assertArrayEquals(originalData, decrypted2.toByteArray())
    }
}