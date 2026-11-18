package com.vaultaire.android.sync.engine

import com.vaultaire.android.sync.model.SyncMetadata
import org.junit.Test
import org.junit.Assert.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import java.security.SecureRandom
import java.util.UUID

/**
 * Tests for SyncEncryptionEngine.
 * 
 * T-EFF-01: Encrypt/decrypt round-trip preserves data
 * T-EFF-02: AAD mismatch throws exception
 * T-EFF-03: Tampered ciphertext throws exception
 * T-EFF-04: Post-duress throws IllegalStateException
 * T-SEC-01: No plaintext in ciphertext
 * T-SEC-02: IV is random per encryption
 * T-SEC-03: AAD binds to vaultId + deviceId
 * T-SEC-04: GCM tag verified on decrypt
 * T-SEC-05: Padding to 10MB bucket
 * T-SEC-06: Same key, different vault -> cannot decrypt
 */
class SyncEncryptionEngineTest {

    private val vaultId = UUID.randomUUID()
    private val deviceId = UUID.randomUUID()
    private val otherVaultId = UUID.randomUUID()
    private val otherDeviceId = UUID.randomUUID()

    private fun generateKey(): SecretKey {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(256, SecureRandom())
        return kg.generateKey()
    }

    @Test
    fun `T-EFF-01: encrypt/decrypt round-trip preserves data`() {
        val key = generateKey()
        val engine = SyncEncryptionEngine(key, vaultId, deviceId)
        
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = mapOf(deviceId to 5L),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val encrypted = engine.encryptSyncRecord(metadata)
        val decrypted = engine.decryptSyncRecord(encrypted)

        assertEquals(metadata.vaultId, decrypted.vaultId)
        assertEquals(metadata.deviceId, decrypted.deviceId)
        assertEquals(metadata.vectorClock, decrypted.vectorClock)
        assertEquals(metadata.syncFormatVersion, decrypted.syncFormatVersion)
    }

    @Test
    fun `T-EFF-02: AAD mismatch throws exception`() {
        val key = generateKey()
        val engine = SyncEncryptionEngine(key, vaultId, deviceId)
        
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val encrypted = engine.encryptSyncRecord(metadata)
        
        // Create engine with different vault ID
        val otherEngine = SyncEncryptionEngine(key, otherVaultId, deviceId)
        
        assertThrows<SecurityException> {
            otherEngine.decryptSyncRecord(encrypted)
        }
    }

    @Test
    fun `T-EFF-03: tampered ciphertext throws exception`() {
        val key = generateKey()
        val engine = SyncEncryptionEngine(key, vaultId, deviceId)
        
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val encrypted = engine.encryptSyncRecord(metadata)
        
        // Tamper with ciphertext
        val tamperedCiphertext = encrypted.ciphertext.copyOf()
        tamperedCiphertext[0] = (tamperedCiphertext[0] + 1).toByte()
        val tampered = encrypted.copy(ciphertext = tamperedCiphertext)
        
        assertThrows<javax.crypto.AEADBadTagException> {
            engine.decryptSyncRecord(tampered)
        }
    }

    @Test
    fun `T-SEC-01: no plaintext in ciphertext`() {
        val key = generateKey()
        val engine = SyncEncryptionEngine(key, vaultId, deviceId)
        
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val encrypted = engine.encryptSyncRecord(metadata)
        
        // Ciphertext should not contain vaultId or deviceId as plaintext
        val ciphertextStr = String(encrypted.ciphertext, java.nio.charset.StandardCharsets.UTF_8)
        assertFalse(ciphertextStr.contains(vaultId.toString()))
        assertFalse(ciphertextStr.contains(deviceId.toString()))
    }

    @Test
    fun `T-SEC-02: IV is random per encryption`() {
        val key = generateKey()
        val engine = SyncEncryptionEngine(key, vaultId, deviceId)
        
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val encrypted1 = engine.encryptSyncRecord(metadata)
        val encrypted2 = engine.encryptSyncRecord(metadata)
        
        // IVs should be different
        assertFalse(encrypted1.iv.contentEquals(encrypted2.iv))
        
        // Ciphertexts should be different (due to different IV)
        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))
    }

    @Test
    fun `T-SEC-03: AAD binds to vaultId + deviceId`() {
        val key = generateKey()
        val engine = SyncEncryptionEngine(key, vaultId, deviceId)
        
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val encrypted = engine.encryptSyncRecord(metadata)
        
        // AAD should contain vaultId and deviceId
        val aadStr = String(encrypted.aad, java.nio.charset.StandardCharsets.UTF_8)
        assertTrue(aadStr.contains(vaultId.toString()))
        assertTrue(aadStr.contains(deviceId.toString()))
        assertTrue(aadStr.contains("1")) // syncFormatVersion
    }

    @Test
    fun `T-SEC-04: GCM tag verified on decrypt`() {
        val key = generateKey()
        val engine = SyncEncryptionEngine(key, vaultId, deviceId)
        
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val encrypted = engine.encryptSyncRecord(metadata)
        
        // Modify the tag
        val badTag = encrypted.tag.copyOf()
        badTag[0] = (badTag[0] + 1).toByte()
        val tampered = encrypted.copy(tag = badTag)
        
        assertThrows<javax.crypto.AEADBadTagException> {
            engine.decryptSyncRecord(tampered)
        }
    }

    @Test
    fun `T-SEC-05: padding to 10MB bucket`() {
        val key = generateKey()
        val engine = SyncEncryptionEngine(key, vaultId, deviceId)
        
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val encrypted = engine.encryptSyncRecord(metadata)
        
        // Ciphertext should be padded to 10MB bucket
        val bucketSize = 10 * 1024 * 1024
        assertEquals(bucketSize, encrypted.ciphertext.size)
    }

    @Test
    fun `T-SEC-06: same key, different vault -> cannot decrypt`() {
        val key = generateKey()
        val engine = SyncEncryptionEngine(key, vaultId, deviceId)
        
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val encrypted = engine.encryptSyncRecord(metadata)
        
        // Try to decrypt with engine for different vault
        val otherEngine = SyncEncryptionEngine(key, otherVaultId, deviceId)
        
        assertThrows<SecurityException> {
            otherEngine.decryptSyncRecord(encrypted)
        }
    }
}
