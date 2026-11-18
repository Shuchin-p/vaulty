# Security Architecture — Test Specifications

**Audience**: QA, Security Engineers, Developers  
**Purpose**: Comprehensive test plan for cryptographic correctness, security boundaries, and integration

---

## 1. Cryptographic Correctness Tests

### 1.1 PBKDF2 Key Derivation Tests

| Test ID | Description | Input | Expected Output | Pass Criteria |
|---------|-------------|-------|-----------------|---------------|
| KDF-001 | Pattern → VDK (600k iterations) | Known pattern bytes + known salt | Known VDK (NIST test vector) | Byte-for-byte match |
| KDF-002 | Recovery Phrase → RVK (800k iterations) | Known 12-word phrase + known salt | Known RVK | Byte-for-byte match |
| KDF-003 | Share Phrase → SK (250k iterations) | Known share phrase + known salt | Known SK | Byte-for-byte match |
| KDF-004 | Backup Key derivation (600k iterations) | Known pattern + known salt | Known BK | Byte-for-byte match |
| KDF-005 | Different salts → different keys | Same input, two different salts | Keys differ | No collision |
| KDF-006 | Iteration count configurable | Vary iterations (100k, 600k, 1M) | Output changes | Output varies with iterations |
| KDF-007 | Domain separator in pattern normalization | Pattern with/without domain separator | Different normalized bytes | Domain separator included |
| KDF-008 | Pattern interpolation correctness | Straight line, diagonal, knight moves | Correct intermediate points | Matches iOS reference implementation |

**Implementation**: Use `KeyDerivationEngineTest`, `RecoveryPhraseEngineTest`, `EncryptedShareEngineTest`

### 1.2 AES-256-GCM Encryption Tests

| Test ID | Description | Key | Plaintext | Expected |
|---------|-------------|-----|-----------|----------|
| AES-001 | Single-shot encrypt/decrypt | Random 256-bit | Random bytes | Round-trip exact match |
| AES-002 | Authentication tag verification | Random | Random | Valid tag → decrypt succeeds |
| AES-003 | Tampered ciphertext rejection | Random | Random + flipped bit | Decrypt throws AEADBadTagException |
| AES-004 | Tampered AAD rejection | Random | Random + AAD | Modified AAD → decrypt fails |
| AES-005 | Nonce reuse detection | Same key, same nonce | Two different plaintexts | **Must fail** (security critical) |
| AES-006 | Zero-length plaintext | Random | Empty byte array | Valid ciphertext (tag only) |
| AES-007 | Large plaintext (100MB) | Random | 100MB random | Stream encrypt/decrypt matches |
| AES-008 | Chunked streaming (64KB chunks) | Random | 10MB | Chunk boundaries preserved |

**Implementation**: Use `EnvelopeEncryptionEngineTest`, `EncryptedOutputStreamTest`, `RandomAccessEncryptedSourceImplTest`

### 1.3 Envelope Encryption Tests

| Test ID | Description | Scenario | Pass Criteria |
|---------|-------------|----------|---------------|
| ENV-001 | Wrap/unwrap MVK with VDK | Random MVK + random VDK | Unwrapped MVK == original |
| ENV-002 | Wrong VDK fails unwrap | Correct MVK, wrong VDK | Throws exception (tag mismatch) |
| ENV-003 | Unique IV per wrap | Same MVK + VDK, two wraps | Different IVs, both unwrap correctly |
| ENV-004 | Corrupted envelope detection | Flip bit in envelope | Unwrap throws exception |

### 1.4 Share Package Tests

| Test ID | Description | Scenario | Pass Criteria |
|---------|-------------|----------|---------------|
| SHR-001 | Create/unpack share package | Valid phrase, policy | Policy + payload recovered |
| SHR-002 | Wrong phrase fails | Wrong phrase | Throws SecurityException |
| SHR-003 | Expired share rejected | Policy.exp < now | Throws SecurityException |
| SHR-004 | Max opens exceeded | Open count > policy.maxOpen | Throws SecurityException |
| SHR-005 | Export disabled enforced | Policy.noExport = true | UI prevents export (integration) |
| SHR-006 | Policy tampering detected | Modify policy JSON in package | Unpack fails (AAD mismatch) |
| SHR-007 | Ciphertext tampering detected | Flip bit in ciphertext | Unpack fails (tag mismatch) |
| SHR-008 | Unique salt per share | Two shares, same phrase | Different salts, both work |

### 1.5 Backup Encryption Tests

| Test ID | Description | Scenario | Pass Criteria |
|---------|-------------|----------|---------------|
| BAK-001 | Encrypt/decrypt backup | Random data + BK | Round-trip exact, size preserved |
| BAK-002 | Padding bucket sizes | Various input sizes | Padded to correct bucket (64KB, 128KB, 256KB, 512KB, 1MB, 2MB, 4MB, 8MB, 10MB, 20MB...) |
| BAK-003 | Header AAD binding | Modify header field | Decrypt fails |
| BAK-004 | Original size restoration | Encrypt 12345 bytes | Decrypt yields exactly 12345 bytes |
| BAK-005 | Separate backup key | BK ≠ MVK | Backup encrypted with BK, not MVK |

---

## 2. Key Hierarchy Tests

### 2.1 Layer Independence Tests

| Test ID | Description | Scenario | Pass Criteria |
|---------|-------------|----------|---------------|
| KH-001 | VDK compromise ≠ MVK compromise | Known VDK, unknown MVK | Cannot decrypt files without MVK |
| KH-002 | MVK compromise ≠ other vaults | MVK from vault A | Cannot decrypt vault B files |
| KH-003 | BK compromise ≠ MVK | Known BK | Cannot decrypt live vault files |
| KH-004 | RVK compromise ≠ live vault | Known RVK | Cannot decrypt live vault without recovery envelope |
| KH-005 | SK compromise ≠ other shares | Known SK for share A | Cannot decrypt share B |
| KH-006 | Pattern change rewraps only | Change pattern | MVK unchanged, files not re-encrypted |
| KH-007 | Recovery phrase change | Change phrase | MVK unchanged, only envelope re-wrapped |
| KH-008 | Backup key rotation | Rotate backup key | Live vault unaffected |

### 2.2 Key Derivation Isolation Tests

| Test ID | Description | Scenario | Pass Criteria |
|---------|-------------|----------|---------------|
| KDI-001 | VDK ≠ RVK for same input | Same pattern/phrase + salt | Keys differ |
| KDI-002 | VDK ≠ BK for same pattern | Same pattern + salt | Keys differ (different iteration counts or domain separation) |
| KDI-003 | SK independent per share | Two shares, same phrase | Different salts → different SKs |

---

## 3. Security Boundary Tests

### 3.1 Memory Security Tests

| Test ID | Description | Method | Pass Criteria |
|---------|-------------|--------|---------------|
| MEM-001 | VDK wiped after lock | Heap dump after `VaultLockManager.lockAllVaults()` | No VDK bytes in heap |
| MEM-002 | MVK wiped after lock | Heap dump after lock | No MVK bytes in heap |
| MEM-003 | Pattern bytes wiped after derivation | Heap dump after `deriveVaultKey()` | No pattern bytes in char arrays |
| MEM-004 | Recovery phrase wiped | Heap dump after `deriveRecoveryKeyFromPhrase()` | No phrase words in char arrays |
| MEM-005 | Buffer wiping in EncryptedOutputStream | Heap dump after stream close | No plaintext chunks in buffers |
| MEM-006 | SecureByteBuffer off-heap wipe | JNI memory inspection | Direct buffer memory zeroed |
| MEM-007 | Crash simulation | Kill process mid-operation | No keys in subsequent process (OS reclaim) |

**Tools**: Android Studio Profiler, `heapdump`, `jmap`, custom JNI memory scanner

### 3.2 Screen Security Tests

| Test ID | Description | Method | Pass Criteria |
|---------|-------------|--------|---------------|
| SCR-001 | FLAG_SECURE on vault screens | `media projection` / screenshot API | Screenshot returns blank/black |
| SCR-002 | FLAG_SECURE on secure camera | Screenshot during camera preview | Camera preview not captured |
| SCR-003 | No FLAG_SECURE on disguise | Calculator disguise activity | Screenshot works normally |
| SCR-004 | No FLAG_SECURE on onboarding | First launch screen | Screenshot works normally |
| SCR-005 | Recent apps preview hidden | Overview screen | Vault content not visible in preview |

### 3.3 Platform Security Tests

| Test ID | Description | Method | Pass Criteria |
|---------|-------------|--------|---------------|
| PLT-001 | Device salt in KeyStore | Extract KeyStore entries | Salt not in SharedPreferences |
| PLT-002 | Salt requires unlock | Lock device → try read salt | Fails / returns null |
| PLT-003 | StrongBox backing (if available) | Check `isInsideSecureHardware()` | Returns true on supported devices |
| PLT-004 | EncryptedSharedPreferences for settings | Read XML directly | Settings encrypted, not plaintext |
| PLT-005 | MVK in KeyStore (optional) | Generate MVK with KeyStore | `isInsideSecureHardware()` true |

### 3.4 Anti-Tampering Tests

| Test ID | Description | Method | Pass Criteria |
|---------|-------------|--------|---------------|
| TAM-001 | Root detection | Run on rooted emulator/device | `RootDetector.isDeviceRooted()` = true |
| TAM-002 | Debugger detection | Attach debugger | `DebugDetector.isDebuggerAttached()` = true |
| TAM-003 | Certificate pinning | MITM proxy (Charles/Burp) | Network calls fail with pinning error |
| TAM-004 | App signature verification | Resign APK | `AppIntegrity.verifySignature()` = false |
| TAM-005 | Tamper response | Trigger detection | Keys wiped, vault locked, app exits |

---

## 4. Integration Tests

### 4.1 Vault Lifecycle Tests

| Test ID | Description | Steps | Pass Criteria |
|---------|-------------|-------|---------------|
| INT-001 | Create vault → unlock → add files → lock → unlock | Full cycle | All files accessible, metadata intact |
| INT-002 | Multiple vaults (plausible deniability) | Create 3 vaults, different patterns | Each pattern opens only its vault |
| INT-003 | Duress mode | Set duress pattern → enter duress → verify non-decoy vaults purged | Decoy vault remains, others deleted |
| INT-004 | Pattern change | Create vault → change pattern → unlock with new | Old pattern fails, new works, files intact |
| INT-005 | Recovery phrase setup → new device restore | Create vault + phrase → simulate new device (new salt) → restore | Vault restored, files accessible |

### 4.2 Secure Camera Tests

| Test ID | Description | Steps | Pass Criteria |
|---------|-------------|-------|---------------|
| CAM-001 | Photo capture → encrypted file | Take photo in app | File on disk is encrypted .vlt, not in Photos |
| CAM-002 | Video capture → encrypted file | Record video | File on disk is encrypted .vlt, playable in app |
| CAM-003 | No intermediate plaintext files | Monitor disk during capture | No temporary unencrypted files created |
| CAM-004 | Camera frame memory wiped | Heap dump after capture | No frame buffers in memory |

### 4.3 Sharing Integration Tests

| Test ID | Description | Steps | Pass Criteria |
|---------|-------------|-------|---------------|
| SHR-INT-001 | Share via Google Drive | Pro creates share → uploads to Drive → Free opens via link | Recipient accesses vault, policy enforced |
| SHR-INT-002 | Share via Nearby Share | Two devices → sender shares → receiver accepts | Transfer completes, vault accessible |
| SHR-INT-003 | Share via QR code | Sender shows QR → receiver scans | Deep link opens app, downloads share |
| SHR-INT-004 | Expired share rejected | Create share with 1hr expiry → wait 2hrs → open | Fails with expiration error |
| SHR-INT-005 | Max opens enforced | Create share maxOpens=3 → open 4th time | 4th open fails |
| SHR-INT-006 | No-export enforced | Create share noExport=true → try export | Export button disabled / fails |
| SHR-INT-007 | Revocation | Create share → revoke → try open | Open fails (file deleted from Drive) |
| SHR-INT-008 | Free user can open Pro share | Pro creates → Free opens | Works (Pro to create, Free to open) |

### 4.4 Backup Integration Tests

| Test ID | Description | Steps | Pass Criteria |
|---------|-------------|-------|---------------|
| BAK-INT-001 | Backup to Google Drive | Enable backup → add files → wait for sync | Backup appears in Drive App Data folder |
| BAK-INT-002 | Restore from backup | Backup vault → uninstall app → reinstall → restore | All files restored, metadata intact |
| BAK-INT-003 | Backup key separate from MVK | Backup vault → extract backup key → try decrypt vault | Cannot decrypt vault with backup key |
| BAK-INT-004 | Incremental backup | Backup → add one file → backup again | Only new data uploaded (or full re-upload) |
| BAK-INT-005 | Backup pruning | Keep last 10 → create 15 backups | Oldest 5 deleted from Drive |

### 4.5 Sync Integration Tests

| Test ID | Description | Steps | Pass Criteria |
|---------|-------------|-------|---------------|
| SYNC-INT-001 | Two-device sync | Device A adds file → sync → Device B pulls | File appears on Device B |
| SYNC-INT-002 | Conflict resolution | Both devices modify same file offline → sync | Last-writer-wins (timestamp) |
| SYNC-INT-003 | Offline queue | Device offline → make changes → online → sync | Changes synced when online |
| SYNC-INT-004 | Sync key derivation | Derive sync key from MVK | Sync key ≠ MVK, consistent across devices |

---

## 5. Penetration Test Checklist

### 5.1 Cryptographic Attacks

- [ ] **Nonce reuse**: Verify no (key, nonce) pair ever reused across all encryption operations
- [ ] **PBKDF2 iteration verification**: Confirm 600k/800k/250k iterations actually executed (timing test)
- [ ] **Side-channel resistance**: Constant-time operations for key comparisons (GCM tag verify)
- [ ] **Entropy validation**: Device salt from `SecureRandom` (not `Random`), pattern entropy measured
- [ ] **Key separation**: Verify no cross-protocol key reuse (VDK ≠ BK ≠ RVK ≠ SK)

### 5.2 Memory Attacks

- [ ] **Cold boot simulation**: Fill RAM with keys → power cycle → memory dump → no keys found
- [ ] **Heap inspection**: `jmap -histo:live` during vault open → no plaintext keys in histogram
- [ ] **Swap/file-backed memory**: Disable swap → verify no keys written to disk
- [ ] **GC behavior**: Force GC during key usage → keys not moved to old gen without wiping

### 5.3 Storage Attacks

- [ ] **Metadata tampering**: Modify vault header, index, file headers → all detected via GCM tags
- [ ] **File swap attack**: Swap two encrypted files → decryption fails or detects mismatch
- [ ] **Index injection**: Inject fake file record into index → decryption fails (AAD binding)
- [ ] **Thumbnail injection**: Replace thumbnail ciphertext → decryption fails
- [ ] **Backup header modification**: Change originalSize → decrypt fails (AAD binding)

### 5.4 Network/Transport Attacks

- [ ] **MITM on Drive API**: Certificate pinning blocks custom CA
- [ ] **Drive metadata leakage**: List Drive files → only random UUID filenames visible
- [ ] **Nearby Share eavesdropping**: Capture Bluetooth/WiFi traffic → only ciphertext visible
- [ ] **QR code leakage**: Scan QR → only share phrase + URL, no keys
- [ ] **Share link enumeration**: Guess Drive file IDs → 404 or permission denied

### 5.5 Platform Attacks

- [ ] **Rooted device**: App detects root → locks vault / wipes keys
- [ ] **Debugger attached**: App detects debugger → locks vault / wipes keys
- [ ] **Repackaged APK**: Signature verification fails → app refuses to run
- [ ] **Hooking (Frida/Xposed)**: Anti-tamper detects hooking → response triggered
- [ ] **Emulator detection**: App behaves correctly on emulator (no false positive lock)

### 5.6 Logic Attacks

- [ ] **Pattern brute-force**: Automated pattern entry → rate limited / lockout after N attempts
- [ ] **Recovery phrase brute-force**: Offline attack on recovery envelope → 800k iterations slow
- [ ] **Duress bypass**: Try to trigger duress without losing data → duress works as designed
- [ ] **Vault enumeration**: List vault files → only UUID filenames, no cross-vault metadata

---

## 6. Performance Benchmarks

| Operation | Target (Mid-range Device) | Max Acceptable |
|-----------|---------------------------|----------------|
| Pattern → VDK (600k PBKDF2) | < 1000ms | 1500ms |
| Recovery phrase → RVK (800k PBKDF2) | < 1300ms | 2000ms |
| Share phrase → SK (250k PBKDF2) | < 400ms | 600ms |
| Vault unlock (VDK + unwrap MVK) | < 1200ms total | 1800ms |
| File encrypt (10MB, 64KB chunks) | < 500ms | 1000ms |
| File decrypt (10MB, random access) | < 100ms/seek | 200ms/seek |
| Backup upload (100MB, Wi-Fi) | < 60s | 120s |
| Share creation (Drive) | < 10s | 20s |
| Nearby Share transfer (50MB) | < 30s | 60s |

---

## 7. Test Automation Strategy

### 7.1 Unit Tests (JUnit + Kotlin)
- All crypto engines: `KeyDerivationEngineTest`, `EnvelopeEncryptionEngineTest`, `RecoveryPhraseEngineTest`, `EncryptedShareEngineTest`, `ZeroKnowledgeBackupEngineTest`
- Storage: `EncryptedOutputStreamTest`, `RandomAccessEncryptedSourceImplTest`
- Utilities: `PatternUtilsTest`, `MemoryHygieneTest`

### 7.2 Instrumented Tests (AndroidJUnitRunner)
- KeyStore integration: `DeviceSaltManagerTest`
- FLAG_SECURE: `SecureScreenTest`
- Camera: `SecureCameraManagerTest` (with CameraX test fakes)
- Disguise: `AppDisguiseManagerTest`

### 7.3 Integration Tests (Compose UI + Hilt)
- Vault lifecycle: `VaultManagerIntegrationTest`
- Sharing flows: `ShareManagerIntegrationTest` (with Drive test harness)
- Backup/Restore: `BackupIntegrationTest`
- Sync: `SyncIntegrationTest`

### 7.4 Security-Specific Test Infrastructure

```kotlin
// Test rule for memory analysis
class MemoryAnalysisRule : TestWatcher() {
    override fun succeeded(description: Description) {
        dumpHeapAndAnalyze()
    }
}

// Test rule for tamper simulation
class TamperSimulationRule : TestWatcher() {
    // Simulate root, debugger, hooking
}

// Property-based testing for crypto
@RunWith(JUnitQuickcheck::class)
class CryptoPropertyTests {
    @Property
    fun encryptDecryptRoundTrip(key: SecretKey, plaintext: ByteArray) {
        val ciphertext = AesGcm.encrypt(key, plaintext)
        val decrypted = AesGcm.decrypt(key, ciphertext)
        assertArrayEquals(plaintext, decrypted)
    }
    
    @Property
    fun tamperedCiphertextFails(key: SecretKey, plaintext: ByteArray) {
        val ciphertext = AesGcm.encrypt(key, plaintext)
        val tampered = ciphertext.copyOf().also { it[0] = (it[0] xor 0xFF).toByte() }
        assertThrows<AEADBadTagException> { AesGcm.decrypt(key, tampered) }
    }
}
```

### 7.5 CI/CD Integration

| Pipeline Stage | Tests Run |
|----------------|-----------|
| Pre-merge (PR) | Unit + Instrumented (fast subset) |
| Nightly | Full integration + Property-based + Memory analysis |
| Release candidate | Penetration checklist (manual) + Performance benchmarks |
| Post-release | Regression suite + Monitoring alerts |

---

## 8. Test Data & Vectors

### 8.1 NIST Test Vectors (Reference)
- PBKDF2-HMAC-SHA512: NIST SP 800-132 test vectors
- AES-GCM: NIST SP 800-38D test vectors
- Use for regression validation of crypto primitives

### 8.2 Cross-Platform Compatibility Vectors
- Share package format: Must decrypt iOS-created shares
- Backup format: Must decrypt iOS-created backups
- Vault index: Must read iOS-created vaults (if applicable)

### 8.3 Edge Case Test Data
- Empty files, 0-byte chunks
- Maximum file size (2GB+ for 32-bit chunk index)
- Unicode filenames (emoji, RTL, combining chars)
- Maximum pattern length (25 points on 5x5 grid)
- Expired/maxed-out share policies
- Corrupted headers (truncated, wrong magic, wrong version)

---

## 9. Acceptance Criteria for Security Review

| Category | Criteria | Status |
|----------|----------|--------|
| **Cryptographic Correctness** | All NIST vectors pass; no nonce reuse; constant-time operations | ⬜ |
| **Key Hierarchy** | Compromise containment verified; independent rotation works | ⬜ |
| **Memory Security** | Zero keys in heap dump after lock; SecureByteBuffer verified | ⬜ |
| **Screen Security** | FLAG_SECURE on 100% vault screens; exceptions documented | ⬜ |
| **Platform Security** | Salt in KeyStore; StrongBox used; EncryptedSharedPreferences | ⬜ |
| **Anti-Tampering** | Root/debugger/repackaging detected; response verified | ⬜ |
| **Transport Security** | Drive/Nearby/QR all encrypt-before-transport; pinning works | ⬜ |
| **Compliance** | GDPR rights implementable; data minimization verified | ⬜ |
| **Penetration Testing** | Checklist 100% passed; no critical findings | ⬜ |
| **Performance** | All benchmarks within targets | ⬜ |

---

## 10. Continuous Security Validation

- **Automated**: Nightly crypto property tests, memory analysis, dependency vulnerability scan (OWASP Dependency Check)
- **Monthly**: Third-party penetration test (rotating firms)
- **Per Release**: Full regression + performance benchmarks
- **Incident Response**: Security logging → alerting → forensic capability