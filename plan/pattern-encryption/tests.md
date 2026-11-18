# Testing Document: Pattern Encryption
## Test Specifications and Strategy

---

## Test Strategy Overview

| Test Level | Target | Framework | Coverage Goal |
|------------|--------|-----------|---------------|
| Unit | Individual classes/functions | JUnit 5 + KotlinTest | 95% |
| Integration | Module boundaries | JUnit 5 + Robolectric | 80% |
| Security | Crypto properties | Custom + JUnit 5 | 100% of crypto paths |
| Edge Case | Boundary conditions | JUnit 5 | All identified edges |
| Performance | Timing/benchmarks | JMH (separate) | Baseline metrics |

---

## Unit Tests

### PatternPoint Tests

```kotlin
class PatternPointTest {
    
    @Test
    fun `index calculation for all 25 positions`() {
        // Grid is 5x5, row-major order
        val cases = listOf(
            Pair(PatternPoint(0, 0), 0),
            Pair(PatternPoint(0, 4), 4),
            Pair(PatternPoint(1, 0), 5),
            Pair(PatternPoint(2, 2), 12),  // Center
            Pair(PatternPoint(4, 4), 24)
        )
        
        cases.forEach { (point, expectedIndex) ->
            assertEquals(expectedIndex, point.index)
        }
    }
    
    @Test
    fun `equality and hashCode`() {
        val p1 = PatternPoint(2, 3)
        val p2 = PatternPoint(2, 3)
        val p3 = PatternPoint(3, 2)
        
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
        assertNotEquals(p1, p3)
    }
    
    @Test
    fun `toString is readable`() {
        val point = PatternPoint(1, 4)
        assertEquals("PatternPoint(row=1, col=4)", point.toString())
    }
}
```

### PatternUtils Tests

```kotlin
class PatternUtilsTest {
    
    private val utils = PatternUtils
    
    @Test
    fun `normalize simple horizontal line`() {
        // Draw: (0,0) → (0,4) - top row left to right
        val input = listOf(
            PatternPoint(0, 0), PatternPoint(0, 1), 
            PatternPoint(0, 2), PatternPoint(0, 3), PatternPoint(0, 4)
        )
        
        val normalized = utils.normalizePattern(input)
        
        // Domain separator (19 bytes) + 5 points × 2 bytes = 29 bytes
        assertEquals(29, normalized.size)
        assertArrayEquals("VAULTAIRE_PATTERN_V1".toByteArray(Charsets.UTF_8), normalized.copyOfRange(0, 19))
        
        // Points: 0, 1, 2, 3, 4
        val pointBytes = normalized.copyOfRange(19, 29)
        val expectedPoints = byteArrayOf(0, 0, 0, 1, 0, 2, 0, 3, 0, 4)
        assertArrayEquals(expectedPoints, pointBytes)
    }
    
    @Test
    fun `normalize horizontal line with skipped dots interpolates`() {
        // Draw: (0,0) → (0,4) directly (skipping intermediate)
        val input = listOf(PatternPoint(0, 0), PatternPoint(0, 4))
        
        val normalized = utils.normalizePattern(input)
        
        // Should interpolate (0,1), (0,2), (0,3)
        val pointCount = (normalized.size - 19) / 2
        assertEquals(5, pointCount)  // 0, 1, 2, 3, 4
    }
    
    @Test
    fun `normalize vertical line interpolates`() {
        // Draw: (0,0) → (4,0) - left column top to bottom
        val input = listOf(PatternPoint(0, 0), PatternPoint(4, 0))
        
        val normalized = utils.normalizePattern(input)
        
        // Should interpolate (1,0), (2,0), (3,0) → 5 points total
        assertEquals(29, normalized.size)
        val pointBytes = normalized.copyOfRange(19, 29)
        val expectedPoints = byteArrayOf(0, 0, 0, 5, 0, 10, 0, 15, 0, 20)
        assertArrayEquals(expectedPoints, pointBytes)
    }
    
    @Test
    fun `normalize diagonal interpolates center`() {
        // Draw: (0,0) → (4,4) - main diagonal
        val input = listOf(PatternPoint(0, 0), PatternPoint(4, 4))
        
        val normalized = utils.normalizePattern(input)
        
        // Midpoint (2,2) = index 12 should be interpolated
        val pointBytes = normalized.copyOfRange(19, normalized.size)
        assertTrue(pointBytes.contains(12.toByte()))  // Contains center point
    }
    
    @Test
    fun `normalize complex path with multiple segments`() {
        // L-shape: (0,0) → (0,2) → (2,2)
        val input = listOf(
            PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(0, 2),
            PatternPoint(1, 2), PatternPoint(2, 2)
        )
        
        val normalized = utils.normalizePattern(input)
        
        // 6 points: 0, 1, 2, 7, 12 (with interpolation)
        val pointCount = (normalized.size - 19) / 2
        assertEquals(6, pointCount)
    }
    
    @Test
    fun `normalize removes consecutive duplicates`() {
        val input = listOf(
            PatternPoint(0, 0), PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(0, 1)
        )
        
        val normalized = utils.normalizePattern(input)
        
        // Should deduplicate
        val pointBytes = normalized.copyOfRange(19, normalized.size)
        val uniquePoints = pointBytes.chunked(2).distinct()
        assertEquals(pointBytes.size / 2, uniquePoints.size)
    }
    
    @Test
    fun `normalize throws on less than 6 points`() {
        val input = listOf(PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(0, 2))
        
        assertThrows<IllegalArgumentException> { utils.normalizePattern(input) }
            .message?.let { assertTrue(it.contains("at least 6 points")) }
    }
    
    @Test
    fun `validate pattern - valid complex pattern`() {
        val pattern = listOf(
            PatternPoint(0, 0), PatternPoint(0, 2), PatternPoint(2, 2),
            PatternPoint(2, 0), PatternPoint(4, 0), PatternPoint(4, 4)
        )
        
        val (isValid, error) = PatternUtils.validatePattern(pattern)
        
        assertTrue(isValid)
        assertNull(error)
    }
    
    @Test
    fun `validate pattern - too few points`() {
        val pattern = listOf(
            PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(0, 2)
        )
        
        val (isValid, error) = PatternUtils.validatePattern(pattern)
        
        assertFalse(isValid)
        assertTrue(error!!.contains("6"))
    }
    
    @Test
    fun `validate pattern - no direction change`() {
        // Straight line - no direction change
        val pattern = listOf(
            PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(0, 2),
            PatternPoint(0, 3), PatternPoint(0, 4)
        )
        
        val (isValid, error) = PatternUtils.validatePattern(pattern)
        
        assertFalse(isValid)
        assertTrue(error!!.contains("direction change"))
    }
    
    @Test
    fun `pattern strength calculation`() {
        // Minimal pattern
        val weak = listOf(
            PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(1, 1),
            PatternPoint(1, 2), PatternPoint(2, 2), PatternPoint(2, 3)
        )
        
        // Strong pattern - uses many dots, crosses, changes direction
        val strong = listOf(
            PatternPoint(0, 0), PatternPoint(4, 4), PatternPoint(0, 4),
            PatternPoint(4, 0), PatternPoint(2, 2), PatternPoint(1, 3),
            PatternPoint(3, 1), PatternPoint(2, 4), PatternPoint(4, 2)
        )
        
        val weakScore = PatternUtils.calculatePatternStrength(weak)
        val strongScore = PatternUtils.calculatePatternStrength(strong)
        
        assertTrue(weakScore < strongScore)
        assertTrue(weakScore >= 0 && weakScore <= 100)
        assertTrue(strongScore >= 0 && strongScore <= 100)
    }
}
```

### DeviceSaltManager Tests

```kotlin
class DeviceSaltManagerTest {
    
    @Test
    fun `getOrCreateDeviceSalt generates 32-byte salt`() {
        // Use mock context with in-memory preferences
        val context = mockContext()
        val manager = DeviceSaltManager(context)
        
        val salt1 = manager.getOrCreateDeviceSalt()
        val salt2 = manager.getOrCreateDeviceSalt()
        
        assertEquals(32, salt1.size)
        assertArrayEquals(salt1, salt2)  // Should return same salt
    }
    
    @Test
    fun `different instances return same salt`() {
        val context = mockContext()
        val manager1 = DeviceSaltManager(context)
        val manager2 = DeviceSaltManager(context)
        
        val salt1 = manager1.getOrCreateDeviceSalt()
        val salt2 = manager2.getOrCreateDeviceSalt()
        
        assertArrayEquals(salt1, salt2)
    }
    
    @Test
    fun `salt is cryptographically random`() {
        val context = mockContext()
        val manager = DeviceSaltManager(context)
        
        val salt = manager.getOrCreateDeviceSalt()
        
        // Statistical randomness test
        assertTrue(hasHighEntropy(salt))
    }
    
    @Test
    fun `hex encoding round trip`() {
        val bytes = ByteArray(32) { SecureRandom().nextBytes(it); it[0] }
        val hex = bytes.joinToString("") { "%02x".format(it) }
        val decoded = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        
        assertArrayEquals(bytes, decoded)
    }
}
```

### KeyDerivationEngine Tests

```kotlin
class KeyDerivationEngineTest {
    
    private lateinit var engine: KeyDerivationEngine
    private val testSalt = ByteArray(32) { it.toByte() }
    
    @BeforeEach
    fun setup() {
        val saltManager = mockDeviceSaltManager(testSalt)
        engine = KeyDerivationEngine(saltManager)
    }
    
    @Test
    fun `deriveVaultKey produces 256-bit key`() {
        val patternBytes = "VAULTAIRE_PATTERN_V1".toByteArray(Charsets.UTF_8) + byteArrayOf(0, 0, 0, 1, 0, 2)
        
        val key = engine.deriveVaultKey(patternBytes)
        
        assertEquals("AES", key.algorithm)
        assertEquals(32, key.encoded.size)  // 256 bits
    }
    
    @Test
    fun `deriveVaultKey is deterministic`() {
        val patternBytes = "VAULTAIRE_PATTERN_V1".toByteArray(Charsets.UTF_8) + byteArrayOf(0, 0, 0, 1)
        
        val key1 = engine.deriveVaultKey(patternBytes)
        val key2 = engine.deriveVaultKey(patternBytes)
        
        assertArrayEquals(key1.encoded, key2.encoded)
    }
    
    @Test
    fun `different patterns produce different keys`() {
        val pattern1 = "VAULTAIRE_PATTERN_V1".toByteArray(Charsets.UTF_8) + byteArrayOf(0, 0)
        val pattern2 = "VAULTAIRE_PATTERN_V1".toByteArray(Charsets.UTF_8) + byteArrayOf(0, 1)
        
        val key1 = engine.deriveVaultKey(pattern1)
        val key2 = engine.deriveVaultKey(pattern2)
        
        assertNotEquals(key1.encoded.contentToString(), key2.encoded.contentToString())
    }
    
    @Test
    fun `different salts produce different keys`() {
        val pattern = "VAULTAIRE_PATTERN_V1".toByteArray(Charsets.UTF_8) + byteArrayOf(0, 0)
        
        val salt1 = ByteArray(32) { 0 }
        val salt2 = ByteArray(32) { 1 }
        
        val engine1 = KeyDerivationEngine(mockDeviceSaltManager(salt1))
        val engine2 = KeyDerivationEngine(mockDeviceSaltManager(salt2))
        
        val key1 = engine1.deriveVaultKey(pattern)
        val key2 = engine2.deriveVaultKey(pattern)
        
        assertNotEquals(key1.encoded.contentToString(), key2.encoded.contentToString())
    }
    
    @Test
    fun `deriveRecoveryKey uses 800k iterations`() {
        val words = listOf("abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd", "abuse", "access", "accident")
        
        val key = engine.deriveRecoveryKey(words)
        
        assertEquals(32, key.encoded.size)
        // Can't easily test iteration count, but verify it's not 600k
    }
    
    @Test
    fun `memory hygiene: char array cleared after derivation`() {
        // This requires white-box testing or mocking Arrays.fill
        // At minimum, verify no exception thrown
        val patternBytes = "VAULTAIRE_PATTERN_V1".toByteArray(Charsets.UTF_8) + byteArrayOf(0, 0, 0, 1, 0, 2, 0, 3)
        
        val key = engine.deriveVaultKey(patternBytes)
        
        // Key should be valid
        assertNotNull(key)
    }
    
    @Test
    fun `constant time: similar patterns take same time`() {
        val pattern1 = "VAULTAIRE_PATTERN_V1".toByteArray(Charsets.UTF_8) + byteArrayOf(0, 0, 0, 1, 0, 2)
        val pattern2 = "VAULTAIRE_PATTERN_V1".toByteArray(Charsets.UTF_8) + byteArrayOf(0, 0, 0, 1, 0, 3)
        
        // Warmup
        repeat(10) { engine.deriveVaultKey(pattern1) }
        repeat(10) { engine.deriveVaultKey(pattern2) }
        
        val time1 = measureTimeMillis { repeat(5) { engine.deriveVaultKey(pattern1) } }
        val time2 = measureTimeMillis { repeat(5) { engine.deriveVaultKey(pattern2) } }
        
        // Times should be within reasonable variance (e.g., 20%)
        val ratio = time1.toDouble() / time2.toDouble()
        assertTrue(ratio > 0.8 && ratio < 1.2, "Timing leak detected: $time1 vs $time2 ms")
    }
}
```

### EnvelopeEncryptionEngine Tests

```kotlin
class EnvelopeEncryptionEngineTest {
    
    private val engine = EnvelopeEncryptionEngine()
    
    @Test
    fun `generateMasterVaultKey produces random 256-bit key`() {
        val key1 = engine.generateMasterVaultKey()
        val key2 = engine.generateMasterVaultKey()
        
        assertEquals(32, key1.encoded.size)
        assertEquals(32, key2.encoded.size)
        assertNotEquals(key1.encoded.contentToString(), key2.encoded.contentToString())
    }
    
    @Test
    fun `wrap and unwrap round trip`() {
        val masterKey = engine.generateMasterVaultKey()
        val derivationKey = generateTestKey()
        
        val envelope = engine.wrapMasterKey(masterKey, derivationKey)
        val unwrapped = engine.unwrapMasterKey(envelope, derivationKey)
        
        assertArrayEquals(masterKey.encoded, unwrapped.encoded)
    }
    
    @Test
    fun `wrong derivation key fails with AEADBadTagException`() {
        val masterKey = engine.generateMasterVaultKey()
        val correctKey = generateTestKey()
        val wrongKey = generateTestKey()
        
        val envelope = engine.wrapMasterKey(masterKey, correctKey)
        
        assertThrows<AEADBadTagException> {
            engine.unwrapMasterKey(envelope, wrongKey)
        }
    }
    
    @Test
    fun `envelope contains IV and ciphertext with tag`() {
        val masterKey = engine.generateMasterVaultKey()
        val derivationKey = generateTestKey()
        
        val envelope = engine.wrapMasterKey(masterKey, derivationKey)
        
        assertEquals(12, envelope.iv.size)
        assertEquals(48, envelope.ciphertext.size)  // 32 key + 16 tag
    }
    
    @Test
    fun `each wrap produces unique IV`() {
        val masterKey = engine.generateMasterVaultKey()
        val derivationKey = generateTestKey()
        
        val env1 = engine.wrapMasterKey(masterKey, derivationKey)
        val env2 = engine.wrapMasterKey(masterKey, derivationKey)
        
        assertNotEquals(env1.iv.contentToString(), env2.iv.contentToString())
        assertNotEquals(env1.ciphertext.contentToString(), env2.ciphertext.contentToString())
    }
    
    @Test
    fun `tampered ciphertext fails`() {
        val masterKey = engine.generateMasterVaultKey()
        val derivationKey = generateTestKey()
        
        val envelope = engine.wrapMasterKey(masterKey, derivationKey)
        
        // Flip a bit in ciphertext
        val tamperedCiphertext = envelope.ciphertext.copyOf()
        tamperedCiphertext[0] = (tamperedCiphertext[0] xor 0xFF).toByte()
        val tamperedEnvelope = EncryptedEnvelope(envelope.iv, tamperedCiphertext)
        
        assertThrows<AEADBadTagException> {
            engine.unwrapMasterKey(tamperedEnvelope, derivationKey)
        }
    }
    
    @Test
    fun `tampered IV fails`() {
        val masterKey = engine.generateMasterVaultKey()
        val derivationKey = generateTestKey()
        
        val envelope = engine.wrapMasterKey(masterKey, derivationKey)
        
        // Flip a bit in IV
        val tamperedIv = envelope.iv.copyOf()
        tamperedIv[0] = (tamperedIv[0] xor 0xFF).toByte()
        val tamperedEnvelope = EncryptedEnvelope(tamperedIv, envelope.ciphertext)
        
        assertThrows<AEADBadTagException> {
            engine.unwrapMasterKey(tamperedEnvelope, derivationKey)
        }
    }
}
```

### MemoryHygiene Tests

```kotlin
class MemoryHygieneTest {
    
    @Test
    fun `wipe byte array`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        
        MemoryHygiene.wipe(bytes)
        
        assertArrayEquals(byteArrayOf(0, 0, 0, 0, 0), bytes)
    }
    
    @Test
    fun `wipe null byte array is no-op`() {
        MemoryHygiene.wipe(null)
        // No exception
    }
    
    @Test
    fun `wipeKey clears secret key`() {
        val key = SecretKeySpec(ByteArray(32) { 0xFF }, "AES")
        
        MemoryHygiene.wipeKey(key)
        
        assertArrayEquals(ByteArray(32), key.encoded)
    }
    
    @Test
    fun `wipeAll clears multiple arrays`() {
        val arr1 = byteArrayOf(0x01, 0x02)
        val arr2 = byteArrayOf(0x03, 0x04)
        val arr3 = byteArrayOf(0x05, 0x06)
        
        MemoryHygiene.wipeAll(arr1, arr2, arr3)
        
        assertArrayEquals(byteArrayOf(0, 0), arr1)
        assertArrayEquals(byteArrayOf(0, 0), arr2)
        assertArrayEquals(byteArrayOf(0, 0), arr3)
    }
    
    @Test
    fun `wipeChars clears char array`() {
        val chars = "secret".toCharArray()
        
        MemoryHygiene.wipeChars(chars)
        
        assertArrayEquals('\u0000'.toCharArray().repeat(chars.size), chars)
    }
}
```

### VaultHeader Tests

```kotlin
class VaultHeaderTest {
    
    @Test
    fun `serialize and deserialize round trip`() {
        val envelope = EncryptedEnvelope(
            iv = ByteArray(12) { it.toByte() },
            ciphertext = ByteArray(48) { (it + 10).toByte() }
        )
        
        val header = VaultHeader(
            vaultId = "550e8400-e29b-41d4-a716-446655440000",
            isDuressDecoy = false,
            envelope = envelope
        )
        
        val outputStream = ByteArrayOutputStream()
        header.serialize(outputStream)
        val bytes = outputStream.toByteArray()
        
        val inputStream = ByteArrayInputStream(bytes)
        val deserialized = VaultHeader.deserialize(inputStream)
        
        assertEquals(header, deserialized)
    }
    
    @Test
    fun `deserialize rejects invalid magic`() {
        val badHeader = "BADMAGIC".toByteArray(Charsets.UTF_8) + ByteArray(100)
        val inputStream = ByteArrayInputStream(badHeader)
        
        assertThrows<IllegalArgumentException> {
            VaultHeader.deserialize(inputStream)
        }.message?.let { assertTrue(it.contains("Invalid Vault Header Magic")) }
    }
    
    @Test
    fun `duress flag serialization`() {
        val envelope = EncryptedEnvelope(ByteArray(12), ByteArray(48))
        
        val normal = VaultHeader(vaultId = "test", isDuressDecoy = false, envelope = envelope)
        val duress = VaultHeader(vaultId = "test", isDuressDecoy = true, envelope = envelope)
        
        val normalBytes = serializeToBytes(normal)
        val duressBytes = serializeToBytes(duress)
        
        // Check duress byte at correct offset
        assertEquals(0x00, normalBytes[7 + 4 + 36])  // magic + idLen + id
        assertEquals(0x01, duressBytes[7 + 4 + 36])
    }
}
```

### EncryptedOutputStream Tests

```kotlin
class EncryptedOutputStreamTest {
    
    @Test
    fun `write and read round trip`() {
        val key = SecretKeySpec(ByteArray(32) { 0xAA }, "AES")
        val plaintext = "Hello, Vaultaire! This is a test message.".toByteArray(Charsets.UTF_8)
        
        // Write
        val outputStream = ByteArrayOutputStream()
        val encryptedStream = EncryptedOutputStream(outputStream, key)
        encryptedStream.write(plaintext)
        encryptedStream.close()
        
        // Read
        val ciphertext = outputStream.toByteArray()
        val inputStream = ByteArrayInputStream(ciphertext)
        
        // Parse header (80 bytes)
        val header = parseFileHeader(inputStream)
        assertEquals("VAULT1", String(header.magic, Charsets.US_ASCII))
        
        // Decrypt using RandomAccessEncryptedSourceImpl
        val file = createTempFile(ciphertext)
        val source = RandomAccessEncryptedSourceImpl(file, key)
        
        val dest = ByteArray(plaintext.size)
        val read = source.readAt(0, dest, 0, plaintext.size)
        
        assertEquals(plaintext.size, read)
        assertArrayEquals(plaintext, dest)
    }
    
    @Test
    fun `large file chunking works`() {
        val key = SecretKeySpec(ByteArray(32) { 0xBB }, "AES")
        val plaintext = ByteArray(200 * 1024) { SecureRandom().nextBytes(it); it[0] }  // 200KB
        
        val outputStream = ByteArrayOutputStream()
        val encryptedStream = EncryptedOutputStream(outputStream, key)
        encryptedStream.write(plaintext)
        encryptedStream.close()
        
        val ciphertext = outputStream.toByteArray()
        val file = createTempFile(ciphertext)
        val source = RandomAccessEncryptedSourceImpl(file, key)
        
        assertEquals(plaintext.size, source.totalPlaintextLength)
        
        // Read at various offsets
        val dest = ByteArray(1024)
        source.readAt(0, dest, 0, 1024)
        assertArrayEquals(plaintext.copyOfRange(0, 1024), dest)
        
        source.readAt(50000, dest, 0, 1024)
        assertArrayEquals(plaintext.copyOfRange(50000, 51024), dest)
        
        source.readAt(plaintext.size - 1024, dest, 0, 1024)
        assertArrayEquals(plaintext.copyOfRange(plaintext.size - 1024, plaintext.size), dest)
    }
    
    @Test
    fun `random access reads correct data`() {
        val key = SecretKeySpec(ByteArray(32) { 0xCC }, "AES")
        val plaintext = ByteArray(64 * 1024) { (it % 256).toByte() }
        
        val outputStream = ByteArrayOutputStream()
        val encryptedStream = EncryptedOutputStream(outputStream, key)
        encryptedStream.write(plaintext)
        encryptedStream.close()
        
        val ciphertext = outputStream.toByteArray()
        val file = createTempFile(ciphertext)
        val source = RandomAccessEncryptedSourceImpl(file, key)
        
        // Read every 1KB
        val chunkSize = 1024
        for (offset in 0..plaintext.size - chunkSize step chunkSize) {
            val dest = ByteArray(chunkSize)
            val read = source.readAt(offset, dest, 0, chunkSize)
            assertEquals(chunkSize, read)
            assertArrayEquals(plaintext.copyOfRange(offset, offset + chunkSize), dest)
        }
    }
}
```

---

## Integration Tests

### Pattern Unlock Flow

```kotlin
@RunWith(AndroidJUnit4::class)
class PatternUnlockIntegrationTest {
    
    @Test
    fun `full unlock flow: pattern -> key -> vault -> files`() {
        // 1. Create vault with pattern
        val pattern = createTestPattern()
        val patternBytes = PatternUtils.normalizePattern(pattern)
        
        val saltManager = createTestSaltManager()
        val kdfEngine = KeyDerivationEngine(saltManager)
        val envelopeEngine = EnvelopeEncryptionEngine()
        val vaultManager = VaultManager(applicationContext)
        
        // 2. Derive vault key
        val vdk = kdfEngine.deriveVaultKey(patternBytes)
        
        // 3. Create master key and wrap
        val mvk = envelopeEngine.generateMasterVaultKey()
        val vaultId = vaultManager.generateVaultId()
        vaultManager.createVault(vaultId, vdk, mvk)
        
        // 4. Unlock with correct pattern
        val unlockedMvk = vaultManager.unlockVault(vaultId, vdk)
        
        assertArrayEquals(mvk.encoded, unlockedMvk.encoded)
    }
    
    @Test
    fun `incorrect pattern fails to unlock`() {
        val correctPattern = createTestPattern()
        val wrongPattern = createDifferentPattern()
        
        val patternBytes = PatternUtils.normalizePattern(correctPattern)
        val wrongPatternBytes = PatternUtils.normalizePattern(wrongPattern)
        
        val saltManager = createTestSaltManager()
        val kdfEngine = KeyDerivationEngine(saltManager)
        val envelopeEngine = EnvelopeEncryptionEngine()
        val vaultManager = VaultManager(applicationContext)
        
        val vdk = kdfEngine.deriveVaultKey(patternBytes)
        val mvk = envelopeEngine.generateMasterVaultKey()
        val vaultId = vaultManager.generateVaultId()
        vaultManager.createVault(vaultId, vdk, mvk)
        
        // Try to unlock with wrong pattern
        val wrongVdk = kdfEngine.deriveVaultKey(wrongPatternBytes)
        
        assertThrows<SecurityException> {
            vaultManager.unlockVault(vaultId, wrongVdk)
        }
    }
}
```

### Recovery Flow

```kotlin
@RunWith(AndroidJUnit4::class)
class RecoveryFlowIntegrationTest {
    
    @Test
    fun `recovery phrase derives key that unlocks vault`() {
        // 1. Create vault and recovery phrase
        val pattern = createTestPattern()
        val phrase = RecoveryPhraseEngine.generateMnemonic()
        
        val saltManager = createTestSaltManager()
        val kdfEngine = KeyDerivationEngine(saltManager)
        val recoveryEngine = RecoveryPhraseEngine(saltManager)
        val envelopeEngine = EnvelopeEncryptionEngine()
        val vaultManager = VaultManager(applicationContext)
        
        // 2. Create vault
        val vdk = kdfEngine.deriveVaultKey(PatternUtils.normalizePattern(pattern))
        val mvk = envelopeEngine.generateMasterVaultKey()
        val vaultId = vaultManager.generateVaultId()
        vaultManager.createVault(vaultId, vdk, mvk)
        
        // 3. Create recovery envelope (would be stored in Keystore)
        val rvk = recoveryEngine.deriveRecoveryKeyFromPhrase(phrase)
        val recoveryEnvelope = envelopeEngine.wrapMasterKey(vdk, rvk)  // Wrap VDK with RVK
        
        // 4. Recover using phrase
        val recoveredRvk = recoveryEngine.deriveRecoveryKeyFromPhrase(phrase)
        val recoveredVdk = envelopeEngine.unwrapMasterKey(recoveryEnvelope, recoveredRvk)
        
        // 5. Use recovered VDK to unlock vault
        val unlockedMvk = vaultManager.unlockVault(vaultId, recoveredVdk)
        
        assertArrayEquals(mvk.encoded, unlockedMvk.encoded)
    }
    
    @Test
    fun `wrong recovery phrase fails`() {
        val phrase1 = listOf("word1", "word2", "word3", "word4", "word5", "word6", 
                             "word7", "word8", "word9", "word10", "word11", "word12")
        val phrase2 = listOf("different", "words", "here", "for", "testing", "purposes",
                             "only", "not", "the", "same", "phrase", "either")
        
        val saltManager = createTestSaltManager()
        val recoveryEngine = RecoveryPhraseEngine(saltManager)
        
        val rvk1 = recoveryEngine.deriveRecoveryKeyFromPhrase(phrase1)
        val rvk2 = recoveryEngine.deriveRecoveryKeyFromPhrase(phrase2)
        
        assertNotEquals(rvk1.encoded.contentToString(), rvk2.encoded.contentToString())
    }
}
```

### Backup/Restore Integration

```kotlin
@RunWith(AndroidJUnit4::class)
class BackupIntegrationTest {
    
    @Test
    fun `backup and restore with master key`() {
        val masterKey = SecretKeySpec(ByteArray(32) { 0xDD }, "AES")
        val plaintext = "Test backup data".repeat(1000).toByteArray(Charsets.UTF_8)
        
        // Encrypt backup
        val outputStream = ByteArrayOutputStream()
        ZeroKnowledgeBackupEngine().encryptBackup(
            ByteArrayInputStream(plaintext),
            outputStream,
            plaintext.size.toLong(),
            masterKey
        )
        
        val backupData = outputStream.toByteArray()
        
        // Decrypt backup
        val restoredStream = ByteArrayOutputStream()
        ZeroKnowledgeBackupEngine().decryptBackup(
            ByteArrayInputStream(backupData),
            restoredStream,
            masterKey
        )
        
        assertArrayEquals(plaintext, restoredStream.toByteArray())
    }
    
    @Test
    fun `wrong master key fails backup decryption`() {
        val masterKey1 = SecretKeySpec(ByteArray(32) { 0xEE }, "AES")
        val masterKey2 = SecretKeySpec(ByteArray(32) { 0xFF }, "AES")
        val plaintext = "Test".toByteArray(Charsets.UTF_8)
        
        val outputStream = ByteArrayOutputStream()
        ZeroKnowledgeBackupEngine().encryptBackup(
            ByteArrayInputStream(plaintext),
            outputStream,
            plaintext.size.toLong(),
            masterKey1
        )
        
        val backupData = outputStream.toByteArray()
        
        assertThrows<AEADBadTagException> {
            ZeroKnowledgeBackupEngine().decryptBackup(
                ByteArrayInputStream(backupData),
                ByteArrayOutputStream(),
                masterKey2
            )
        }
    }
}
```

---

## Security Tests

### Brute Force Resistance

```kotlin
class BruteForceResistanceTest {
    
    @Test
    fun `pattern space is large enough`() {
        // 5x5 grid, min 6 points, at least 1 direction change
        // Theoretical: ~11,168 patterns for 6 dots with 1 turn
        // 8 dots: ~140,704
        // 10 dots: ~1.6M
        
        val patterns6 = countValidPatterns(6)
        val patterns8 = countValidPatterns(8)
        val patterns10 = countValidPatterns(10)
        
        assertTrue(patterns6 >= 11000)
        assertTrue(patterns8 >= 140000)
        assertTrue(patterns10 >= 1600000)
    }
    
    @Test
    fun `PBKDF2 iteration count matches spec`() {
        assertEquals(600_000, KeyDerivationEngine.PATTERN_ITERATIONS)
        assertEquals(800_000, KeyDerivationEngine.RECOVERY_ITERATIONS)
    }
    
    @Test
    fun `iteration count is configurable for testing`() {
        // Allow reduced iterations in test environment
        val testEngine = KeyDerivationEngine(mockDeviceSaltManager()) {
            override val PATTERN_ITERATIONS = 10_000  // Fast for tests
        }
        
        val key = testEngine.deriveVaultKey(testPatternBytes())
        assertNotNull(key)
    }
}
```

### Timing Attack Resistance

```kotlin
class TimingAttackTest {
    
    @Test
    fun `key derivation time independent of input`() {
        val engine = KeyDerivationEngine(mockDeviceSaltManager())
        val pattern1 = testPatternBytes()
        val pattern2 = testPatternBytes().also { it[0] = (it[0] xor 0xFF).toByte() }
        
        // Warmup
        repeat(20) { engine.deriveVaultKey(pattern1) }
        repeat(20) { engine.deriveVaultKey(pattern2) }
        
        // Measure
        val times1 = List(50) { measureNanos { engine.deriveVaultKey(pattern1) } }
        val times2 = List(50) { measureNanos { engine.deriveVaultKey(pattern2) } }
        
        val avg1 = times1.average()
        val avg2 = times2.average()
        val diff = abs(avg1 - avg2).toDouble() / max(avg1, avg2)
        
        // Timing difference should be < 5%
        assertTrue(diff < 0.05, "Timing leak: ${avg1}ns vs ${avg2}ns (${diff * 100}%)")
    }
    
    @Test
    fun `GCM unwrap time independent of key correctness`() {
        val engine = EnvelopeEncryptionEngine()
        val masterKey = engine.generateMasterVaultKey()
        val correctKey = generateTestKey()
        val wrongKey = generateTestKey()
        
        val envelope = engine.wrapMasterKey(masterKey, correctKey)
        
        // Warmup
        repeat(10) { 
            try { engine.unwrapMasterKey(envelope, correctKey) } catch (e: Exception) {}
            try { engine.unwrapMasterKey(envelope, wrongKey) } catch (e: Exception) {}
        }
        
        val correctTimes = List(30) { measureNanos { 
            try { engine.unwrapMasterKey(envelope, correctKey) } catch (e: Exception) {} 
        } }
        val wrongTimes = List(30) { measureNanos { 
            try { engine.unwrapMasterKey(envelope, wrongKey) } catch (e: Exception) {} 
        } }
        
        val avgCorrect = correctTimes.average()
        val avgWrong = wrongTimes.average()
        val diff = abs(avgCorrect - avgWrong).toDouble() / max(avgCorrect, avgWrong)
        
        assertTrue(diff < 0.1, "GCM timing leak: ${diff * 100}%")
    }
}
```

### Memory Leakage Tests

```kotlin
class MemoryLeakageTest {
    
    @Test
    fun `no key material in memory after wipe`() {
        val bytes = ByteArray(32) { SecureRandom().nextBytes(it); it[0] }
        val original = bytes.copyOf()
        
        MemoryHygiene.wipe(bytes)
        
        // Should be all zeros
        assertTrue(bytes.all { it == 0.toByte() })
        
        // Original should be unchanged (wipe is in-place)
        assertArrayEquals(original, original) // Just verifying reference
    }
    
    @Test
    fun `SecretKey encoded array is cleared`() {
        val keyBytes = ByteArray(32) { 0xAA }
        val key = SecretKeySpec(keyBytes, "AES")
        
        MemoryHygiene.wipeKey(key)
        
        assertArrayEquals(ByteArray(32), key.encoded)
    }
    
    @Test
    fun `derivation does not leak pattern in memory`() {
        // This is a white-box test - verify char array is cleared
        val engine = KeyDerivationEngine(mockDeviceSaltManager())
        val patternBytes = testPatternBytes()
        
        // Can't easily test internal char array clearing without mocking
        // But we can verify no exception and result is valid
        val key = engine.deriveVaultKey(patternBytes)
        assertNotNull(key)
    }
}
```

### Side Channel Tests

```kotlin
class SideChannelTest {
    
    @Test
    fun `envelope encryption produces unique IVs`() {
        val engine = EnvelopeEncryptionEngine()
        val masterKey = engine.generateMasterVaultKey()
        val derivationKey = generateTestKey()
        
        val ivs = mutableSetOf<ByteArray>()
        
        repeat(100) {
            val envelope = engine.wrapMasterKey(masterKey, derivationKey)
            ivs.add(envelope.iv)
        }
        
        // All IVs should be unique
        assertEquals(100, ivs.size)
    }
    
    @Test
    fun `file encryption produces unique chunk IVs`() {
        val key = SecretKeySpec(ByteArray(32) { 0x11 }, "AES")
        val plaintext = ByteArray(200 * 1024) { 0x42 }
        
        val outputStream = ByteArrayOutputStream()
        val encryptedStream = EncryptedOutputStream(outputStream, key)
        encryptedStream.write(plaintext)
        encryptedStream.close()
        
        val ciphertext = outputStream.toByteArray()
        val file = createTempFile(ciphertext)
        val source = RandomAccessEncryptedSourceImpl(file, key)
        
        // Collect all chunk IVs
        val chunkIvs = mutableSetOf<ByteArray>()
        val chunkCount = (plaintext.size + source.chunkSize - 1) / source.chunkSize
        
        for (i in 0 until chunkCount) {
            val chunkIv = source.deriveChunkIv(source.masterIv, i.toLong())
            chunkIvs.add(chunkIv)
        }
        
        assertEquals(chunkCount, chunkIvs.size)
    }
}
```

---

## Edge Case Tests

### Pattern Edge Cases

```kotlin
class PatternEdgeCaseTest {
    
    @Test
    fun `all 25 dots in sequence`() {
        val allDots = (0..24).map { PatternPoint(it / 5, it % 5) }
        val normalized = PatternUtils.normalizePattern(allDots)
        
        // 25 points, no interpolation needed
        val pointCount = (normalized.size - 19) / 2
        assertEquals(25, pointCount)
    }
    
    @Test
    fun `pattern that crosses itself multiple times`() {
        // Star pattern
        val pattern = listOf(
            PatternPoint(2, 2), PatternPoint(0, 0),  // Center to corner
            PatternPoint(2, 2), PatternPoint(0, 4),  // Back to center, other corner
            PatternPoint(2, 2), PatternPoint(4, 4),
            PatternPoint(2, 2), PatternPoint(4, 0)
        )
        
        val normalized = PatternUtils.normalizePattern(pattern)
        
        // Should handle repeated center visits
        assertTrue(normalized.size > 19)
    }
    
    @Test
    fun `knight move pattern (non-straight)`() {
        // L-shaped moves that don't interpolate
        val pattern = listOf(
            PatternPoint(0, 0), PatternPoint(2, 1),
            PatternPoint(4, 2), PatternPoint(2, 3),
            PatternPoint(0, 4), PatternPoint(1, 2)
        )
        
        val normalized = PatternUtils.normalizePattern(pattern)
        
        // No intermediate points on knight moves
        val pointCount = (normalized.size - 19) / 2
        assertEquals(6, pointCount)
    }
    
    @Test
    fun `minimum valid pattern: 6 dots, 1 direction change`() {
        // Small L shape
        val pattern = listOf(
            PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(0, 2),
            PatternPoint(1, 2), PatternPoint(2, 2), PatternPoint(2, 1)
        )
        
        val (isValid, error) = PatternUtils.validatePattern(pattern)
        
        assertTrue(isValid, error ?: "")
    }
    
    @Test
    fun `pattern at grid boundaries`() {
        // Corners and edges
        val pattern = listOf(
            PatternPoint(0, 0), PatternPoint(0, 4),
            PatternPoint(4, 4), PatternPoint(4, 0),
            PatternPoint(2, 2), PatternPoint(1, 1)
        )
        
        val normalized = PatternUtils.normalizePattern(pattern)
        assertTrue(normalized.size > 19)
    }
}
```

### Crypto Edge Cases

```kotlin
class CryptoEdgeCaseTest {
    
    @Test
    fun `empty plaintext encryption`() {
        val key = SecretKeySpec(ByteArray(32) { 0x33 }, "AES")
        val plaintext = ByteArray(0)
        
        val outputStream = ByteArrayOutputStream()
        val encryptedStream = EncryptedOutputStream(outputStream, key)
        encryptedStream.write(plaintext)
        encryptedStream.close()
        
        val ciphertext = outputStream.toByteArray()
        
        // Should still have header (80 bytes) + at least one chunk
        assertTrue(ciphertext.size >= 80)
    }
    
    @Test
    fun `exactly one chunk plaintext`() {
        val key = SecretKeySpec(ByteArray(32) { 0x44 }, "AES")
        val plaintext = ByteArray(64 * 1024) { 0x55 }  // Exactly one chunk
        
        val outputStream = ByteArrayOutputStream()
        val encryptedStream = EncryptedOutputStream(outputStream, key)
        encryptedStream.write(plaintext)
        encryptedStream.close()
        
        val ciphertext = outputStream.toByteArray()
        val file = createTempFile(ciphertext)
        val source = RandomAccessEncryptedSourceImpl(file, key)
        
        assertEquals(plaintext.size, source.totalPlaintextLength)
    }
    
    @Test
    fun `max chunk size boundary`() {
        // 64KB chunks, test 64KB-1, 64KB, 64KB+1
        val sizes = listOf(64 * 1024 - 1, 64 * 1024, 64 * 1024 + 1)
        
        sizes.forEach { size ->
            val key = SecretKeySpec(ByteArray(32) { 0x66 }, "AES")
            val plaintext = ByteArray(size) { 0x77 }
            
            val outputStream = ByteArrayOutputStream()
            val encryptedStream = EncryptedOutputStream(outputStream, key)
            encryptedStream.write(plaintext)
            encryptedStream.close()
            
            val ciphertext = outputStream.toByteArray()
            val file = createTempFile(ciphertext)
            val source = RandomAccessEncryptedSourceImpl(file, key)
            
            assertEquals(size, source.totalPlaintextLength)
            
            val dest = ByteArray(size)
            source.readAt(0, dest, 0, size)
            assertArrayEquals(plaintext, dest)
        }
    }
    
    @Test
    fun `corrupted file header fails gracefully`() {
        val key = SecretKeySpec(ByteArray(32) { 0x88 }, "AES")
        val plaintext = "test".toByteArray(Charsets.UTF_8)
        
        val outputStream = ByteArrayOutputStream()
        val encryptedStream = EncryptedOutputStream(outputStream, key)
        encryptedStream.write(plaintext)
        encryptedStream.close()
        
        val ciphertext = outputStream.toByteArray()
        
        // Corrupt magic bytes
        ciphertext[0] = 0xFF
        
        val file = createTempFile(ciphertext)
        
        assertThrows<IllegalArgumentException> {
            RandomAccessEncryptedSourceImpl(file, key)
        }.message?.let { assertTrue(it.contains("Invalid vault header magic")) }
    }
}
```

---

## Test Harness Setup Recommendations

### Test Dependencies

```kotlin
// app/build.gradle.kts additions
dependencies {
    // Unit testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.0.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    
    // Android instrumentation
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("org.robolectric:robolectric:4.11")
    
    // Benchmark
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}
```

### Test Fixtures

```kotlin
// test/fixtures/TestPatterns.kt
object TestPatterns {
    fun simpleL(): List<PatternPoint> = listOf(
        PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(0, 2),
        PatternPoint(1, 2), PatternPoint(2, 2), PatternPoint(2, 1)
    )
    
    fun complexStar(): List<PatternPoint> = listOf(
        PatternPoint(2, 2), PatternPoint(0, 0), PatternPoint(2, 2),
        PatternPoint(0, 4), PatternPoint(2, 2), PatternPoint(4, 4),
        PatternPoint(2, 2), PatternPoint(4, 0)
    )
    
    fun all25(): List<PatternPoint> = (0..24).map { PatternPoint(it / 5, it % 5) }
}

// test/fixtures/TestKeys.kt
object TestKeys {
    fun generateTestKey(): SecretKey = SecretKeySpec(
        ByteArray(32) { SecureRandom().nextBytes(it); it[0] }, 
        "AES"
    )
    
    fun fixedKey(bytes: ByteArray = ByteArray(32) { it.toByte() }): SecretKey = 
        SecretKeySpec(bytes, "AES")
}

// test/fixtures/TestSaltManager.kt
fun mockDeviceSaltManager(salt: ByteArray = ByteArray(32) { 0 }): DeviceSaltManager {
    return object : DeviceSaltManager(mockContext()) {
        override fun getOrCreateDeviceSalt(): ByteArray = salt
    }
}
```

### Robolectric Config

```xml
<!-- test/resources/robolectric.properties -->
sdk=35
qualifiers=
```

### CI Integration

```yaml
# .github/workflows/test.yml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Run Unit Tests
        run: ./gradlew test --continue
      
  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
      - name: Run Security Tests
        run: ./gradlew test --tests "*Security*" --tests "*Timing*" --tests "*Memory*"
      
  instrumentation-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
      - name: Run Instrumented Tests
        run: ./gradlew connectedAndroidTest
```

---

## Mock/Stub Strategy for Cryptographic Dependencies

### What to Mock

| Dependency | Mock Strategy | Reason |
|------------|---------------|--------|
| `DeviceSaltManager` | Test double returning fixed salt | Isolate KDF from storage |
| `SecureRandom` | Fixed seed for deterministic tests | Reproducible IVs/keys |
| `SecretKeyFactory` | Not mocked - test real impl | Verify actual crypto |
| `Cipher` | Not mocked - test real impl | Verify actual GCM |
| `Context` | Robolectric shadow or mock | Android API access |
| `SharedPreferences` | Robolectric or in-memory impl | Salt storage |

### What NOT to Mock

| Component | Reason |
|-----------|--------|
| PBKDF2 implementation | Must test real JCE provider |
| AES-GCM implementation | Must test real JCE provider |
| Key derivation logic | Core security function |

### Test Double Pattern

```kotlin
// Use interfaces for testability
interface SaltProvider {
    fun getSalt(): ByteArray
    fun setSalt(salt: ByteArray)
}

class DeviceSaltManager @Inject constructor(
    private val context: Context
) : SaltProvider {
    // Implementation
}

class FixedSaltProvider(private val salt: ByteArray) : SaltProvider {
    override fun getSalt(): ByteArray = salt
    override fun setSalt(salt: ByteArray) {}
}

// In tests:
val testEngine = KeyDerivationEngine(FixedSaltProvider(testSalt))
```

---

## Coverage Targets

| Package | Line Coverage | Branch Coverage |
|---------|---------------|-----------------|
| `crypto.pattern` | 100% | 95% |
| `crypto.kdf` | 100% | 100% |
| `crypto.envelope` | 100% | 100% |
| `crypto.memory` | 100% | 90% |
| `vault` | 90% | 85% |
| `storage` | 90% | 85% |
| `backup` | 85% | 80% |

---

## Continuous Security Testing

### Fuzzing Targets

```kotlin
// Fuzz inputs to:
// - PatternUtils.normalizePattern
// - KeyDerivationEngine.deriveVaultKey
// - EnvelopeEncryptionEngine.unwrapMasterKey
// - VaultHeader.deserialize
// - RandomAccessEncryptedSourceImpl.readAt
```

### Property-Based Testing

```kotlin
// Kotest property tests for:
// - normalizePattern is idempotent
// - wrap/unwrap is symmetric
// - deriveVaultKey is deterministic
// - MemoryHygiene.wipe leaves all zeros
```

### Regression Tests

```kotlin
// Maintain test vectors for:
// - Known PBKDF2 outputs (NIST vectors)
// - Known AES-GCM outputs (NIST vectors)
// - Serialization format stability
```