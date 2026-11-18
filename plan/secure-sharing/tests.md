# Secure Sharing — Test Specifications

## 1. Test Organization

```
app/src/test/java/com/vaultaire/android/sharing/
├── EncryptedShareEngineTest.kt           # Unit: crypto core
├── SharePolicyTest.kt                    # Unit: policy logic
├── SharePayloadSerializerTest.kt         # Unit: payload (de)serialization
├── SharingPhraseGeneratorTest.kt         # Unit: phrase gen/validation
│
app/src/androidTest/java/com/vaultaire/android/sharing/
├── ShareCreationIntegrationTest.kt       # Integration: create → upload → claim
├── RecipientOpenFlowTest.kt              # Integration: open → view → sync
├── RevocationFlowTest.kt                 # Integration: revoke → sync → lock
├── UpdateSyncFlowTest.kt                 # Integration: publish → sync → view
│
app/src/androidTest/java/com/vaultaire/android/sharing/security/
├── AADTamperingTest.kt                   # Security: policy modification
├── ExportBlockingTest.kt                 # Security: no-export enforcement
├── PhraseReuseTest.kt                    # Security: one-time claim
├── KeySeparationTest.kt                  # Security: sharing vs pattern vs recovery
└── ReplayAttackTest.kt                   # Security: version replay
```

---

## 2. Unit Tests

### 2.1 `EncryptedShareEngineTest`

#### `createSharePackage_ValidInputs_ReturnsPackage`
```kotlin
@Test
fun createSharePackage_ValidInputs_ReturnsPackage() {
    val engine = EncryptedShareEngine()
    val payload = "test vault data".toByteArray(Charsets.UTF_8)
    val phrase = "abandon ability able about above absent absorb abstract absurd abuse access accident"
    val policy = SharePolicy(
        expirationTimestampMs = System.currentTimeMillis() + 86400000, // 1 day
        maxOpenCount = 5,
        disableExport = true
    )

    val pkg = engine.createSharePackage(payload, phrase, policy)

    assertEquals(0x01, pkg.version)
    assertEquals(16, pkg.salt.size)
    assertEquals(12, pkg.iv.size)
    assertTrue(pkg.policyHeader.size > 0)
    assertTrue(pkg.ciphertext.size > payload.size) // Includes GCM tag
}
```

#### `createSharePackage_UniqueSaltAndIVPerCall`
```kotlin
@Test
fun createSharePackage_UniqueSaltAndIVPerCall() {
    val engine = EncryptedShareEngine()
    val payload = "data".toByteArray(Charsets.UTF_8)
    val phrase = "abandon ability able about above absent absorb abstract absurd abuse access accident"
    val policy = SharePolicy()

    val pkg1 = engine.createSharePackage(payload, phrase, policy)
    val pkg2 = engine.createSharePackage(payload, phrase, policy)

    assertFalse(pkg1.salt.contentEquals(pkg2.salt))
    assertFalse(pkg1.iv.contentEquals(pkg2.iv))
    assertFalse(pkg1.ciphertext.contentEquals(pkg2.ciphertext)) // Different IV → different ciphertext
}
```

#### `unpackSharePackage_ValidPackage_ReturnsPayloadAndPolicy`
```kotlin
@Test
fun unpackSharePackage_ValidPackage_ReturnsPayloadAndPolicy() {
    val engine = EncryptedShareEngine()
    val originalPayload = "secret vault content".toByteArray(Charsets.UTF_8)
    val phrase = "abandon ability able about above absent absorb abstract absurd abuse access accident"
    val policy = SharePolicy(maxOpenCount = 3, disableExport = true)

    val pkg = engine.createSharePackage(originalPayload, phrase, policy)
    val (unpackedPolicy, decryptedPayload) = engine.unpackSharePackage(pkg, phrase, currentOpenCount = 0)

    assertEquals(originalPayload, decryptedPayload)
    assertEquals(policy.maxOpenCount, unpackedPolicy.maxOpenCount)
    assertEquals(policy.disableExport, unpackedPolicy.disableExport)
}
```

#### `unpackSharePackage_WrongPhrase_ThrowsAEADBadTagException`
```kotlin
@Test
fun unpackSharePackage_WrongPhrase_ThrowsAEADBadTagException() {
    val engine = EncryptedShareEngine()
    val payload = "data".toByteArray(Charsets.UTF_8)
    val pkg = engine.createSharePackage(payload, "correct phrase here twelve words", SharePolicy())

    assertThrows<javax.crypto.AEADBadTagException> {
        engine.unpackSharePackage(pkg, "wrong phrase here twelve words", 0)
    }
}
```

#### `unpackSharePackage_TamperedCiphertext_ThrowsAEADBadTagException`
```kotlin
@Test
fun unpackSharePackage_TamperedCiphertext_ThrowsAEADBadTagException() {
    val engine = EncryptedShareEngine()
    val pkg = engine.createSharePackage("data".toByteArray(Charsets.UTF_8), "phrase twelve words here now", SharePolicy())

    // Flip a bit in ciphertext
    val tamperedCiphertext = pkg.ciphertext.copyOf()
    tamperedCiphertext[0] = (tamperedCiphertext[0] ^ 0xFF).toByte()
    val tamperedPkg = pkg.copy(ciphertext = tamperedCiphertext)

    assertThrows<javax.crypto.AEADBadTagException> {
        engine.unpackSharePackage(tamperedPkg, "phrase twelve words here now", 0)
    }
}
```

#### `unpackSharePackage_TamperedPolicyAAD_ThrowsAEADBadTagException`
```kotlin
@Test
fun unpackSharePackage_TamperedPolicyAAD_ThrowsAEADBadTagException() {
    val engine = EncryptedShareEngine()
    val pkg = engine.createSharePackage("data".toByteArray(Charsets.UTF_8), "phrase twelve words here now", SharePolicy(maxOpenCount = 5))

    // Modify policy header (change maxOpen from 5 to 10)
    val tamperedPolicy = pkg.policyHeader.copyOf()
    // Find and modify the maxOpen value in JSON
    val json = String(tamperedPolicy, Charsets.UTF_8).replace("\"maxOpen\":5", "\"maxOpen\":10")
    val tamperedPkg = pkg.copy(policyHeader = json.toByteArray(Charsets.UTF_8))

    assertThrows<javax.crypto.AEADBadTagException> {
        engine.unpackSharePackage(tamperedPkg, "phrase twelve words here now", 0)
    }
}
```

#### `unpackSharePackage_ExpiredPolicy_ThrowsSecurityException`
```kotlin
@Test
fun unpackSharePackage_ExpiredPolicy_ThrowsSecurityException() {
    val engine = EncryptedShareEngine()
    val policy = SharePolicy(expirationTimestampMs = System.currentTimeMillis() - 1000) // Expired 1s ago
    val pkg = engine.createSharePackage("data".toByteArray(Charsets.UTF_8), "phrase twelve words here now", policy)

    val exception = assertThrows<SecurityException> {
        engine.unpackSharePackage(pkg, "phrase twelve words here now", 0)
    }
    assertTrue(exception.message?.contains("expired") == true)
}
```

#### `unpackSharePackage_MaxOpensExceeded_ThrowsSecurityException`
```kotlin
@Test
fun unpackSharePackage_MaxOpensExceeded_ThrowsSecurityException() {
    val engine = EncryptedShareEngine()
    val policy = SharePolicy(maxOpenCount = 2)
    val pkg = engine.createSharePackage("data".toByteArray(Charsets.UTF_8), "phrase twelve words here now", policy)

    // First two opens succeed
    engine.unpackSharePackage(pkg, "phrase twelve words here now", 0)
    engine.unpackSharePackage(pkg, "phrase twelve words here now", 1)

    // Third open fails
    val exception = assertThrows<SecurityException> {
        engine.unpackSharePackage(pkg, "phrase twelve words here now", 2)
    }
    assertTrue(exception.message?.contains("exceeded") == true)
}
```

#### `unpackSharePackage_PhraseNormalization_Works`
```kotlin
@Test
fun unpackSharePackage_PhraseNormalization_Works() {
    val engine = EncryptedShareEngine()
    val payload = "data".toByteArray(Charsets.UTF_8)
    val phrase = "AbAnDoN AbIlItY AbLe AbOuT AbOvE AbSeNt AbSoRb AbStRaCt AbSuRd AbUsE AcCeSs AcCiDeNt"
    val pkg = engine.createSharePackage(payload, phrase, SharePolicy())

    // Should work with normalized phrase
    val (_, decrypted) = engine.unpackSharePackage(pkg, "abandon ability able about above absent absorb abstract absurd abuse access accident", 0)
    assertEquals(payload, decrypted)
}
```

#### `deriveShareKey_DeterministicForSameInputs`
```kotlin
@Test
fun deriveShareKey_DeterministicForSameInputs() {
    val engine = EncryptedShareEngine()
    val phrase = "phrase twelve words here now"
    val salt = "saltsaltsaltsalt".toByteArray(Charsets.UTF_8) // 16 bytes

    // Use reflection to test private method
    val method = EncryptedShareEngine::class.java.getDeclaredFunction("deriveShareKey")
    method.isAccessible = true

    val key1 = method.call(engine, phrase, salt) as SecretKey
    val key2 = method.call(engine, phrase, salt) as SecretKey

    assertEquals(key1.encoded, key2.encoded)
}
```

---

### 2.2 `SharePolicyTest`

#### `isExpired_ReturnsTrueWhenPastExpiration`
```kotlin
@Test
fun isExpired_ReturnsTrueWhenPastExpiration() {
    val policy = SharePolicy(expirationTimestampMs = System.currentTimeMillis() - 1000)
    assertTrue(policy.isExpired())
}
```

#### `isExpired_ReturnsFalseWhenFutureExpiration`
```kotlin
@Test
fun isExpired_ReturnsFalseWhenFutureExpiration() {
    val policy = SharePolicy(expirationTimestampMs = System.currentTimeMillis() + 86400000)
    assertFalse(policy.isExpired())
}
```

#### `isExpired_ReturnsFalseWhenNullExpiration`
```kotlin
@Test
fun isExpired_ReturnsFalseWhenNullExpiration() {
    val policy = SharePolicy(expirationTimestampMs = null)
    assertFalse(policy.isExpired())
}
```

#### `isCountExceeded_ReturnsTrueWhenAtLimit`
```kotlin
@Test
fun isCountExceeded_ReturnsTrueWhenAtLimit() {
    val policy = SharePolicy(maxOpenCount = 3)
    assertTrue(policy.isCountExceeded(3))
    assertTrue(policy.isCountExceeded(4))
}
```

#### `isCountExceeded_ReturnsFalseWhenUnderLimit`
```kotlin
@Test
fun isCountExceeded_ReturnsFalseWhenUnderLimit() {
    val policy = SharePolicy(maxOpenCount = 3)
    assertFalse(policy.isCountExceeded(0))
    assertFalse(policy.isCountExceeded(2))
}
```

#### `isCountExceeded_ReturnsFalseWhenNullLimit`
```kotlin
@Test
fun isCountExceeded_ReturnsFalseWhenNullLimit() {
    val policy = SharePolicy(maxOpenCount = null)
    assertFalse(policy.isCountExceeded(100))
}
```

#### `serializeDeserialize_RoundTrip`
```kotlin
@Test
fun serializeDeserialize_RoundTrip() {
    val original = SharePolicy(
        expirationTimestampMs = 1767225600000L,
        maxOpenCount = 5,
        disableExport = true,
        createdTimestampMs = 1767139200000L
    )

    val engine = EncryptedShareEngine()
    val method = EncryptedShareEngine::class.java.getDeclaredFunction("serializePolicy")
    method.isAccessible = true
    val json = method.call(engine, original) as String

    val deserializeMethod = EncryptedShareEngine::class.java.getDeclaredFunction("deserializePolicy")
    deserializeMethod.isAccessible = true
    val restored = deserializeMethod.call(engine, json) as SharePolicy

    assertEquals(original.expirationTimestampMs, restored.expirationTimestampMs)
    assertEquals(original.maxOpenCount, restored.maxOpenCount)
    assertEquals(original.disableExport, restored.disableExport)
    assertEquals(original.createdTimestampMs, restored.createdTimestampMs)
}
```

#### `deserialize_MissingFields_Defaults`
```kotlin
@Test
fun deserialize_MissingFields_Defaults() {
    val engine = EncryptedShareEngine()
    val json = "{\"v\":1,\"created\":1767139200000}" // Missing exp, maxOpen, noExport

    val method = EncryptedShareEngine::class.java.getDeclaredFunction("deserializePolicy")
    method.isAccessible = true
    val policy = method.call(engine, json) as SharePolicy

    assertNull(policy.expirationTimestampMs)
    assertNull(policy.maxOpenCount)
    assertFalse(policy.disableExport)
    assertEquals(1767139200000L, policy.createdTimestampMs)
}
```

---

### 2.3 `SharePayloadSerializerTest`

#### `serialize_ValidPayload_RoundTrip`
```kotlin
@Test
fun serialize_ValidPayload_RoundTrip() {
    val payload = SharePayload(
        vaultName = "Personal Docs",
        ownerId = "owner-123",
        files = listOf(
            SharedFile("f1", "tax.pdf", "application/pdf", 1024, 1000, ByteArray(100))
        ),
        masterKeyWrapped = ByteArray(48), // 32 + 16 tag
        fileIndex = ByteArray(512)
    )

    val serialized = SharePayloadSerializer.serialize(payload)
    val deserialized = SharePayloadSerializer.deserialize(serialized)

    assertEquals(payload.vaultName, deserialized.vaultName)
    assertEquals(payload.ownerId, deserialized.ownerId)
    assertEquals(payload.files, deserialized.files)
    assertEquals(payload.masterKeyWrapped, deserialized.masterKeyWrapped)
    assertEquals(payload.fileIndex, deserialized.fileIndex)
}
```

#### `deserialize_CorruptedData_Throws`
```kotlin
@Test
fun deserialize_CorruptedData_Throws() {
    val badData = "not valid data".toByteArray(Charsets.UTF_8)
    assertThrows<IllegalArgumentException> {
        SharePayloadSerializer.deserialize(badData)
    }
}
```

#### `serialize_LargePayload_HandlesSize`
```kotlin
@Test
fun serialize_LargePayload_HandlesSize() {
    val largeFiles = (1..1000).map { i ->
        SharedFile("f$i", "file$i.txt", "text/plain", 1024L, System.currentTimeMillis(), null)
    }
    val payload = SharePayload(
        vaultName = "Large Vault",
        ownerId = "owner",
        files = largeFiles,
        masterKeyWrapped = ByteArray(48),
        fileIndex = ByteArray(10000)
    )

    val serialized = SharePayloadSerializer.serialize(payload)
    assertTrue(serialized.size > 0)
    // Should not OOME
    val deserialized = SharePayloadSerializer.deserialize(serialized)
    assertEquals(1000, deserialized.files.size)
}
```

---

### 2.4 `SharingPhraseGeneratorTest`

#### `generateShareSecretPhrase_Returns12Words`
```kotlin
@Test
fun generateShareSecretPhrase_Returns12Words() {
    val engine = EncryptedShareEngine()
    val phrase = engine.generateShareSecretPhrase()
    val words = phrase.split(" ")
    assertEquals(12, words.size)
}
```

#### `generateShareSecretPhrase_AllWordsFromWordlist`
```kotlin
@Test
fun generateShareSecretPhrase_AllWordsFromWordlist() {
    val engine = EncryptedShareEngine()
    val phrase = engine.generateShareSecretPhrase()
    val words = phrase.split(" ")
    val wordlist = EncryptedShareEngine.WORDLIST
    words.forEach { word ->
        assertTrue(wordlist.contains(word), "Word '$word' not in wordlist")
    }
}
```

#### `generateShareSecretPhrase_UniquePerCall`
```kotlin
@Test
fun generateShareSecretPhrase_UniquePerCall() {
    val engine = EncryptedShareEngine()
    val phrases = (1..100).map { engine.generateShareSecretPhrase() }.toSet()
    // With 80^12 combinations, collision probability is negligible
    assertEquals(100, phrases.size)
}
```

#### `validatePhrase_ValidPhrase_ReturnsTrue` (New API)
```kotlin
@Test
fun validatePhrase_ValidPhrase_ReturnsTrue() {
    // After adding validatePhrase method to engine
    val engine = EncryptedShareEngine()
    val phrase = engine.generateShareSecretPhrase()
    assertTrue(engine.validatePhrase(phrase))
}
```

#### `validatePhrase_InvalidWordCount_ReturnsFalse`
```kotlin
@Test
fun validatePhrase_InvalidWordCount_ReturnsFalse() {
    val engine = EncryptedShareEngine()
    assertFalse(engine.validatePhrase("one two three"))
    assertFalse(engine.validatePhrase(""))
}
```

#### `validatePhrase_UnknownWord_ReturnsFalse`
```kotlin
@Test
fun validatePhrase_UnknownWord_ReturnsFalse() {
    val engine = EncryptedShareEngine()
    assertFalse(engine.validatePhrase("invalidword " + "abandon ".repeat(11).trim()))
}
```

---

## 3. Integration Tests

### 3.1 `ShareCreationIntegrationTest`

#### `createShare_UploadsToDrive_ReturnsShareId`
```kotlin
@Test
fun createShare_UploadsToDrive_ReturnsShareId() {
    // Requires: Test Google account, Drive API enabled
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = 3)

    val shareId = scenario.owner.createShare(
        vaultId = vault.id,
        policy = SharePolicy(maxOpenCount = 3, disableExport = true)
    )

    assertNotNull(shareId)
    // Verify Drive has package.v1.bin + metadata.json
    val metadata = scenario.drive.downloadMetadata(shareId)
    assertEquals(1, metadata.version)
    assertFalse(metadata.revoked)
    assertEquals(3, metadata.policy.maxOpen)
    assertTrue(metadata.policy.noExport)
}
```

#### `createShare_LargeVault_StreamsUpload`
```kotlin
@Test
fun createShare_LargeVault_StreamsUpload() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = 50, totalSizeMB = 50)

    val shareId = scenario.owner.createShare(vault.id, SharePolicy())

    assertNotNull(shareId)
    // Verify package size matches
    val pkgSize = scenario.drive.getFileSize("${shareId}/package.v1.bin")
    assertTrue(pkgSize > 50 * 1024 * 1024) // At least original size
}
```

#### `createShare_Offline_QueuesAndRetries`
```kotlin
@Test
fun createShare_Offline_QueuesAndRetries() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = 1)

    // Disable network
    scenario.network.disable()
    val shareId = scenario.owner.createShare(vault.id, SharePolicy())
    assertNotNull(shareId) // Returns immediately with local shareId

    // Share should be in pending state
    val localShare = scenario.owner.getLocalShare(shareId)
    assertEquals(ShareStatus.PENDING_UPLOAD, localShare.status)

    // Re-enable network
    scenario.network.enable()
    scenario.workManager.runPendingWork()

    // Should now be uploaded
    val metadata = scenario.drive.downloadMetadata(shareId)
    assertEquals(1, metadata.version)
}
```

---

### 3.2 `RecipientOpenFlowTest`

#### `openShare_ValidPhrase_DownloadsAndDecrypts`
```kotlin
@Test
fun openShare_ValidPhrase_DownloadsAndDecrypts() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = 3, fileNames = ["a.txt", "b.pdf", "c.jpg"])
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())

    // Recipient opens
    val sharedVault = scenario.recipient.openSharedVault(phrase)

    assertEquals("a.txt", sharedVault.files[0].name)
    assertEquals("b.pdf", sharedVault.files[1].name)
    assertEquals("c.jpg", sharedVault.files[2].name)
    assertTrue(sharedVault.isReadOnly)
}
```

#### `openShare_AlreadyClaimed_Throws`
```kotlin
@Test
fun openShare_AlreadyClaimed_Throws() {
    val scenario = ShareTestScenario.setup()
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())

    // First recipient claims
    scenario.recipient1.openSharedVault(phrase)

    // Second recipient tries
    assertThrows<ShareClaimedException> {
        scenario.recipient2.openSharedVault(phrase)
    }
}
```

#### `openShare_ExpiredShare_Throws`
```kotlin
@Test
fun openShare_ExpiredShare_Throws() {
    val scenario = ShareTestScenario.setup()
    val policy = SharePolicy(expirationTimestampMs = System.currentTimeMillis() - 1000)
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, policy)

    assertThrows<ShareExpiredException> {
        scenario.recipient.openSharedVault(phrase)
    }
}
```

#### `openShare_MaxOpensExceeded_Throws`
```kotlin
@Test
fun openShare_MaxOpensExceeded_Throws() {
    val scenario = ShareTestScenario.setup()
    val policy = SharePolicy(maxOpenCount = 1)
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, policy)

    scenario.recipient.openSharedVault(phrase) // 1st open - OK

    assertThrows<ShareMaxOpensExceededException> {
        scenario.recipient.openSharedVault(phrase) // 2nd open - fails
    }
}
```

#### `openShare_NoExportPolicy_BlocksExport`
```kotlin
@Test
fun openShare_NoExportPolicy_BlocksExport() {
    val scenario = ShareTestScenario.setup()
    val policy = SharePolicy(disableExport = true)
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, policy)

    val sharedVault = scenario.recipient.openSharedVault(phrase)

    // Try to export a file
    assertThrows<ExportDisabledException> {
        sharedVault.exportFile(sharedVault.files[0].id, destinationDir)
    }

    // Verify FLAG_SECURE is set on Activity
    assertTrue(sharedVault.activity.flags.contains(WindowManager.LayoutParams.FLAG_SECURE))
}
```

---

### 3.3 `RevocationFlowTest`

#### `revokeShare_OwnerOnline_RecipientLocksOnNextSync`
```kotlin
@Test
fun revokeShare_OwnerOnline_RecipientLocksOnNextSync() {
    val scenario = ShareTestScenario.setup()
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    val sharedVault = scenario.recipient.openSharedVault(phrase)
    assertTrue(sharedVault.isAccessible)

    // Owner revokes
    scenario.owner.revokeShare(shareId)

    // Recipient syncs
    scenario.recipient.syncShares()

    // Vault should be locked
    assertFalse(sharedVault.isAccessible)
    assertEquals(SharedVaultState.REVOKED, sharedVault.state)
}
```

#### `revokeShare_OwnerOffline_QueuedAndSentWhenOnline`
```kotlin
@Test
fun revokeShare_OwnerOffline_QueuedAndSentWhenOnline() {
    val scenario = ShareTestScenario.setup()
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    scenario.recipient.openSharedVault(phrase)

    // Owner offline
    scenario.network.disable()
    scenario.owner.revokeShare(shareId)
    val localShare = scenario.owner.getLocalShare(shareId)
    assertEquals(ShareStatus.REVOKING, localShare.status)

    // Owner online
    scenario.network.enable()
    scenario.workManager.runPendingWork()

    // Recipient syncs
    scenario.recipient.syncShares()
    assertEquals(SharedVaultState.REVOKED, scenario.recipient.getSharedVault(shareId).state)
}
```

#### `revokeShare_RecipientOffline_LocksOnNextOnline`
```kotlin
@Test
fun revokeShare_RecipientOffline_LocksOnNextOnline() {
    val scenario = ShareTestScenario.setup()
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    scenario.recipient.openSharedVault(phrase)

    // Recipient offline
    scenario.network.disable(recipient = true)
    scenario.owner.revokeShare(shareId)

    // Recipient online
    scenario.network.enable(recipient = true)
    scenario.recipient.syncShares()

    assertEquals(SharedVaultState.REVOKED, scenario.recipient.getSharedVault(shareId).state)
}
```

---

### 3.4 `UpdateSyncFlowTest`

#### `publishUpdate_OwnerModifiesVault_RecipientSeesChanges`
```kotlin
@Test
fun publishUpdate_OwnerModifiesVault_RecipientSeesChanges() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = ["original.txt"])
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    val sharedVault = scenario.recipient.openSharedVault(phrase)

    assertEquals(1, sharedVault.files.size)
    assertEquals("original.txt", sharedVault.files[0].name)

    // Owner adds file
    scenario.owner.addFileToVault(vault.id, "new.txt", "content".toByteArray())
    scenario.owner.publishUpdatesToShares()

    // Recipient syncs
    scenario.recipient.syncShares()

    val updatedVault = scenario.recipient.getSharedVault(shareId)
    assertEquals(2, updatedVault.files.size)
    assertTrue(updatedVault.files.any { it.name == "new.txt" })
}
```

#### `publishUpdate_OwnerDeletesFile_RecipientNoLongerSeesIt`
```kotlin
@Test
fun publishUpdate_OwnerDeletesFile_RecipientNoLongerSeesIt() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = ["keep.txt", "delete.txt"])
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    scenario.recipient.openSharedVault(phrase)

    scenario.owner.deleteFileFromVault(vault.id, "delete.txt")
    scenario.owner.publishUpdatesToShares()
    scenario.recipient.syncShares()

    val updatedVault = scenario.recipient.getSharedVault(shareId)
    assertEquals(1, updatedVault.files.size)
    assertEquals("keep.txt", updatedVault.files[0].name)
}
```

#### `publishUpdate_MultipleVersions_RecipientGetsLatest`
```kotlin
@Test
fun publishUpdate_MultipleVersions_RecipientGetsLatest() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = ["v1.txt"])
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    scenario.recipient.openSharedVault(phrase)

    // Owner publishes v2
    scenario.owner.addFileToVault(vault.id, "v2.txt", "".toByteArray())
    scenario.owner.publishUpdatesToShares()

    // Owner publishes v3 (before recipient syncs)
    scenario.owner.addFileToVault(vault.id, "v3.txt", "".toByteArray())
    scenario.owner.publishUpdatesToShares()

    // Recipient syncs once
    scenario.recipient.syncShares()

    val vault = scenario.recipient.getSharedVault(shareId)
    assertEquals(3, vault.files.size)
    assertTrue(vault.files.map { it.name }.containsAll(["v1.txt", "v2.txt", "v3.txt"]))
}
```

---

## 4. Security Tests

### 4.1 `AADTamperingTest`

#### `tamperExpirationDate_DecryptionFails`
```kotlin
@Test
fun tamperExpirationDate_DecryptionFails() {
    val engine = EncryptedShareEngine()
    val pkg = engine.createSharePackage(
        "data".toByteArray(),
        "phrase twelve words here now",
        SharePolicy(expirationTimestampMs = System.currentTimeMillis() + 86400000)
    )

    // Modify expiration in policy header to be in the past
    val tampered = modifyPolicyField(pkg, "exp", System.currentTimeMillis() - 1000)

    assertThrows<AEADBadTagException> {
        engine.unpackSharePackage(tampered, "phrase twelve words here now", 0)
    }
}
```

#### `tamperMaxOpens_DecryptionFails`
```kotlin
@Test
fun tamperMaxOpens_DecryptionFails() {
    val engine = EncryptedShareEngine()
    val pkg = engine.createSharePackage("data".toByteArray(), "phrase twelve words here now", SharePolicy(maxOpenCount = 5))
    val tampered = modifyPolicyField(pkg, "maxOpen", 999)

    assertThrows<AEADBadTagException> {
        engine.unpackSharePackage(tampered, "phrase twelve words here now", 0)
    }
}
```

#### `tamperNoExport_DecryptionFails`
```kotlin
@Test
fun tamperNoExport_DecryptionFails() {
    val engine = EncryptedShareEngine()
    val pkg = engine.createSharePackage("data".toByteArray(), "phrase twelve words here now", SharePolicy(disableExport = true))
    val tampered = modifyPolicyField(pkg, "noExport", false)

    assertThrows<AEADBadTagException> {
        engine.unpackSharePackage(tampered, "phrase twelve words here now", 0)
    }
}
```

#### `tamperCreatedTimestamp_DecryptionFails`
```kotlin
@Test
fun tamperCreatedTimestamp_DecryptionFails() {
    val engine = EncryptedShareEngine()
    val pkg = engine.createSharePackage("data".toByteArray(), "phrase twelve words here now", SharePolicy())
    val tampered = modifyPolicyField(pkg, "created", 0)

    assertThrows<AEADBadTagException> {
        engine.unpackSharePackage(tampered, "phrase twelve words here now", 0)
    }
}
```

#### `tamperVersionField_DecryptionFails`
```kotlin
@Test
fun tamperVersionField_DecryptionFails() {
    val engine = EncryptedShareEngine()
    val pkg = engine.createSharePackage("data".toByteArray(), "phrase twelve words here now", SharePolicy())
    val tampered = modifyPolicyField(pkg, "v", 99)

    assertThrows<AEADBadTagException> {
        engine.unpackSharePackage(tampered, "phrase twelve words here now", 0)
    }
}
```

---

### 4.2 `ExportBlockingTest`

#### `noExportPolicy_ExportActionHidden`
```kotlin
@Test
fun noExportPolicy_ExportActionHidden() {
    val scenario = ShareTestScenario.setup()
    val (_, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy(disableExport = true))
    val sharedVault = scenario.recipient.openSharedVault(phrase)

    // Verify export menu items are not visible
    val exportAction = sharedVault.ui.findExportAction()
    assertNull(exportAction)
}
```

#### `noExportPolicy_ShareActionHidden`
```kotlin
@Test
fun noExportPolicy_ShareActionHidden() {
    val scenario = ShareTestScenario.setup()
    val (_, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy(disableExport = true))
    val sharedVault = scenario.recipient.openSharedVault(phrase)

    val shareAction = sharedVault.ui.findShareAction()
    assertNull(shareAction)
}
```

#### `noExportPolicy_ScreenshotBlocked`
```kotlin
@Test
fun noExportPolicy_ScreenshotBlocked() {
    val scenario = ShareTestScenario.setup()
    val (_, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy(disableExport = true))
    val sharedVault = scenario.recipient.openSharedVault(phrase)

    // Verify FLAG_SECURE is set
    val flags = sharedVault.activity.window.attributes.flags
    assertTrue(flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
}
```

#### `exportAllowed_ExportActionVisible`
```kotlin
@Test
fun exportAllowed_ExportActionVisible() {
    val scenario = ShareTestScenario.setup()
    val (_, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy(disableExport = false))
    val sharedVault = scenario.recipient.openSharedVault(phrase)

    val exportAction = sharedVault.ui.findExportAction()
    assertNotNull(exportAction)
    assertTrue(exportAction.isVisible)
}
```

---

### 4.3 `PhraseReuseTest`

#### `phraseSpentAfterClaim_CannotReclaim`
```kotlin
@Test
fun phraseSpentAfterClaim_CannotReclaim() {
    val scenario = ShareTestScenario.setup()
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())

    // First claim
    scenario.recipient1.openSharedVault(phrase)

    // Second claim fails
    assertThrows<ShareClaimedException> {
        scenario.recipient2.openSharedVault(phrase)
    }

    // Even same recipient on different device (same Google account) - per website
    // "A second device on the claimer's Apple Account can still import it"
    // Android: same Google account → can import via Drive sync
    val sharedVault2 = scenario.recipient1.openSharedVaultOnSecondDevice(phrase)
    assertNotNull(sharedVault2) // Should work via Drive sync
}
```

#### `phraseSpent_PersistsAcrossReinstall`
```kotlin
@Test
fun phraseSpent_PersistsAcrossReinstall() {
    val scenario = ShareTestScenario.setup()
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    scenario.recipient.openSharedVault(phrase)

    // Uninstall/reinstall app
    scenario.recipient.reinstallApp()

    // Phrase should still be spent (Drive metadata has spent=true)
    assertThrows<ShareClaimedException> {
        scenario.recipient.openSharedVault(phrase)
    }
}
```

---

### 4.4 `KeySeparationTest`

#### `shareKey_IndependentOfPatternKey`
```kotlin
@Test
fun shareKey_IndependentOfPatternKey() {
    val patternEngine = KeyDerivationEngine(MockDeviceSaltManager())
    val shareEngine = EncryptedShareEngine()

    val pattern = "pattern_bytes".toByteArray()
    val vdk = patternEngine.deriveVaultKey(pattern)

    val phrase = "phrase twelve words here now"
    val salt = "saltsaltsaltsalt".toByteArray(Charsets.UTF_8)
    val shareKey = shareEngine.deriveShareKey(phrase, salt)

    // Keys must be completely different
    assertFalse(vdk.encoded.contentEquals(shareKey.encoded))
}
```

#### `shareKey_IndependentOfRecoveryKey`
```kotlin
@Test
fun shareKey_IndependentOfRecoveryKey() {
    val recoveryEngine = RecoveryPhraseEngine(MockDeviceSaltManager())
    val shareEngine = EncryptedShareEngine()

    val recoveryWords = List(12) { "word" }
    val rvk = recoveryEngine.deriveRecoveryKeyFromPhrase(recoveryWords)

    val phrase = "phrase twelve words here now"
    val salt = "saltsaltsaltsalt".toByteArray(Charsets.UTF_8)
    val shareKey = shareEngine.deriveShareKey(phrase, salt)

    assertFalse(rvk.encoded.contentEquals(shareKey.encoded))
}
```

#### `shareKey_DifferentSaltProducesDifferentKey`
```kotlin
@Test
fun shareKey_DifferentSaltProducesDifferentKey() {
    val engine = EncryptedShareEngine()
    val phrase = "phrase twelve words here now"

    val key1 = engine.deriveShareKey(phrase, "salt1saltsalt1".toByteArray(Charsets.UTF_8))
    val key2 = engine.deriveShareKey(phrase, "salt2saltsalt2".toByteArray(Charsets.UTF_8))

    assertFalse(key1.encoded.contentEquals(key2.encoded))
}
```

---

### 4.5 `ReplayAttackTest`

#### `replayOldPackage_VersionCheckRejects`
```kotlin
@Test
fun replayOldPackage_VersionCheckRejects() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = ["v1.txt"])
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())

    // Owner publishes v2 (adds file)
    scenario.owner.addFileToVault(vault.id, "v2.txt", "".toByteArray())
    scenario.owner.publishUpdatesToShares()

    // Attacker tries to serve v1 package to recipient
    val v1Package = scenario.drive.downloadPackage(shareId, version = 1)
    val tamperedMetadata = scenario.drive.downloadMetadata(shareId).copy(version = 1)

    // Recipient should detect version mismatch and request latest
    scenario.recipient.injectPackage(shareId, v1Package, tamperedMetadata)
    scenario.recipient.syncShares()

    // Should have v2 (latest)
    val vault = scenario.recipient.getSharedVault(shareId)
    assertEquals(2, vault.files.size)
}
```

---

## 5. Edge Case Tests

### 5.1 `EdgeCaseTests`

#### `emptyVaultShare_Works`
```kotlin
@Test
fun emptyVaultShare_Works() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = [])
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    val sharedVault = scenario.recipient.openSharedVault(phrase)

    assertTrue(sharedVault.files.isEmpty())
    assertTrue(sharedVault.isReadOnly)
}
```

#### `unicodeFileNames_HandledCorrectly`
```kotlin
@Test
fun unicodeFileNames_HandledCorrectly() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = ["文档.pdf", "файл.txt", "🔒secret.png"])
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    val sharedVault = scenario.recipient.openSharedVault(phrase)

    assertEquals(3, sharedVault.files.size)
    assertTrue(sharedVault.files.map { it.name }.containsAll(["文档.pdf", "файл.txt", "🔒secret.png"]))
}
```

#### `veryLongVaultName_TruncatedOrHandled`
```kotlin
@Test
fun veryLongVaultName_TruncatedOrHandled() {
    val scenario = ShareTestScenario.setup()
    val longName = "A".repeat(500)
    val vault = scenario.createTestVault(name = longName, files = ["a.txt"])
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    val sharedVault = scenario.recipient.openSharedVault(phrase)

    // Should not crash; name preserved or reasonably truncated
    assertTrue(sharedVault.name.length >= 100)
}
```

#### `policyMaxOpenZero_ThrowsOnFirstOpen`
```kotlin
@Test
fun policyMaxOpenZero_ThrowsOnFirstOpen() {
    val scenario = ShareTestScenario.setup()
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy(maxOpenCount = 0))

    assertThrows<ShareMaxOpensExceededException> {
        scenario.recipient.openSharedVault(phrase)
    }
}
```

#### `policyExpirationInPast_ThrowsImmediately`
```kotlin
@Test
fun policyExpirationInPast_ThrowsImmediately() {
    val scenario = ShareTestScenario.setup()
    val past = System.currentTimeMillis() - 1000
    val (shareId, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy(expirationTimestampMs = past))

    assertThrows<ShareExpiredException> {
        scenario.recipient.openSharedVault(phrase)
    }
}
```

#### `concurrentShareCreation_UniquePackages`
```kotlin
@Test
fun concurrentShareCreation_UniquePackages() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = ["a.txt"])

    // Create two shares rapidly
    val (shareId1, phrase1) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())
    val (shareId2, phrase2) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())

    assertNotEquals(shareId1, shareId2)
    assertNotEquals(phrase1, phrase2)

    // Both should work independently
    val sv1 = scenario.recipient1.openSharedVault(phrase1)
    val sv2 = scenario.recipient2.openSharedVault(phrase2)
    assertNotNull(sv1)
    assertNotNull(sv2)
}
```

---

## 6. Performance Tests

### 6.1 `PerformanceTests`

#### `createShare_100MBVault_Under30Seconds`
```kotlin
@Test
fun createShare_100MBVault_Under30Seconds() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = 10, totalSizeMB = 100)

    val start = System.currentTimeMillis()
    val shareId = scenario.owner.createShare(vault.id, SharePolicy())
    val duration = System.currentTimeMillis() - start

    assertTrue(duration < 30000, "Share creation took ${duration}ms, expected < 30000ms")
    assertNotNull(shareId)
}
```

#### `openShare_50MBVault_Under10Seconds`
```kotlin
@Test
fun openShare_50MBVault_Under10Seconds() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = 5, totalSizeMB = 50)
    val (_, phrase) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())

    val start = System.currentTimeMillis()
    val sharedVault = scenario.recipient.openSharedVault(phrase)
    val duration = System.currentTimeMillis() - start

    assertTrue(duration < 10000, "Share open took ${duration}ms, expected < 10000ms")
    assertNotNull(sharedVault)
}
```

#### `publishUpdate_Incremental_Under5Seconds`
```kotlin
@Test
fun publishUpdate_Incremental_Under5Seconds() {
    val scenario = ShareTestScenario.setup()
    val vault = scenario.createTestVault(files = 100, totalSizeMB = 50)
    val (shareId, _) = scenario.owner.createShareAndGetPhrase(vault.id, SharePolicy())

    scenario.owner.addFileToVault(vault.id, "new.txt", "small".toByteArray())

    val start = System.currentTimeMillis()
    scenario.owner.publishUpdatesToShares()
    val duration = System.currentTimeMillis() - start

    assertTrue(duration < 5000, "Update publish took ${duration}ms, expected < 5000ms")
}
```

---

## 7. Test Infrastructure

### 7.1 `ShareTestScenario` (Test Helper)
```kotlin
class ShareTestScenario {
    val owner: TestUser
    val recipient: TestUser
    val recipient2: TestUser
    val drive: MockDriveService
    val network: NetworkController
    val workManager: TestWorkManager

    companion object {
        fun setup(): ShareTestScenario { ... }
    }

    fun createTestVault(files: List<String>, totalSizeMB: Int = 1): TestVault { ... }
    fun createTestVault(fileNames: List<String> = ["test.txt"]): TestVault { ... }
}

class TestUser {
    fun createShare(vaultId: String, policy: SharePolicy): String { ... }
    fun createShareAndGetPhrase(vaultId: String, policy: SharePolicy): Pair<String, String> { ... }
    fun revokeShare(shareId: String) { ... }
    fun publishUpdatesToShares() { ... }
    fun openSharedVault(phrase: String): SharedVaultHandle { ... }
    fun syncShares() { ... }
    fun getLocalShare(shareId: String): LocalShareMetadata { ... }
    fun reinstallApp() { ... }
}

data class SharedVaultHandle(
    val name: String,
    val files: List<SharedFile>,
    val isReadOnly: Boolean,
    val isAccessible: Boolean,
    val state: SharedVaultState,
    val ui: SharedVaultUI,
    val activity: Activity
)
```

### 7.2 MockDriveService
- In-memory implementation of Drive API for unit/integration tests
- Simulates latency, failures, quota limits
- Tracks file versions, change tokens

### 7.3 NetworkController
- Enable/disable network per user
- Simulate offline/online transitions

---

## 8. CI/CD Integration

```yaml
# .github/workflows/secure-sharing-tests.yml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run unit tests
        run: ./gradlew test --tests "com.vaultaire.android.sharing.*Test"

  integration-tests:
    runs-on: ubuntu-latest
    services:
      # Use firebase test lab or local emulator
    steps:
      - uses: actions/checkout@v4
      - name: Run integration tests
        run: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vaultaire.android.sharing.ShareCreationIntegrationTest

  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run security tests
        run: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vaultaire.android.sharing.security.*

  performance-tests:
    runs-on: ubuntu-latest
    if: github.event_name == 'schedule' || github.event_name == 'workflow_dispatch'
    steps:
      - uses: actions/checkout@v4
      - name: Run performance tests
        run: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vaultaire.android.sharing.PerformanceTests
```

---

## 9. Test Coverage Targets

| Component | Target Coverage |
|-----------|-----------------|
| `EncryptedShareEngine` | 100% (crypto core) |
| `SharePolicy` | 100% |
| `SharePayloadSerializer` | 100% |
| `SharingPhraseGenerator` | 100% |
| Drive transport layer | 90% |
| UI flows | 80% (via Compose UI tests) |
| Sync/revocation logic | 95% |
| Security tests | 100% (all attack vectors) |

---

*Document Version: 1.0*  
*Author: Architecture Subagent*  
*Date: 2026-11-16*