# Secure Sharing — Technical Implementation Specification

## 1. Architecture Overview

```mermaid
flowchart TB
    subgraph OwnerDevice["Owner Device (Pro)"]
        O1[Vault Data\n(Master Key Encrypted)]
        O2[Share Creation UI]
        O3[EncryptedShareEngine]
        O4[Google Drive\nApp Folder]
    end

    subgraph Cloud["Google Drive (App Folder)"]
        C1[EncryptedSharePackage\nv1: salt, iv, policyAAD, ciphertext]
        C2[ShareMetadata.json\n{phraseHash, driveFileId, policy, revoked, version}]
        C3[Updates: EncryptedSharePackage v2, v3...]
    end

    subgraph RecipientDevice["Recipient Device (Free/Pro)"]
        R1[Open Shared Vault UI]
        R2[EncryptedShareEngine.unpackSharePackage]
        R3[Local Read-Only Vault Instance]
        R4[Sync Worker\n(polls Drive Changes)]
    end

    O1 -->|decrypt with MVK| O3
    O2 -->|policy + phrase| O3
    O3 -->|AES-256-GCM\npolicy as AAD| C1
    O3 -->|metadata| C2
    C1 -.->|download| R2
    C2 -.->|download/check| R4
    R2 -->|derive key from\nphrase + salt| R3
    R4 -->|detect updates/revocation| R3
```

---

## 2. Cryptographic Protocol

### 2.1 Threat Model
- **Attacker capabilities**: Network interception, cloud storage access (Drive), recipient device compromise, phrase interception
- **Protected**: File contents, metadata, policy integrity, phrase secrecy, owner identity
- **Not protected**: Traffic analysis (sizes/timing), recipient-side exfiltration (camera, root), cloud metadata leakage

### 2.2 Key Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                    SHARING KEY HIERARCHY                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  12-Word Sharing Phrase (user-generated)                   │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────────────────────────────┐               │
│  │ PBKDF2-HMAC-SHA512                      │               │
│  │   iterations: 250,000                   │               │
│  │   salt: 16 bytes (random, per-share)    │               │
│  │   output: 256-bit Share Encryption Key  │               │
│  └─────────────────────────────────────────┘               │
│           │                                                 │
│           ▼                                                 │
│  ┌─────────────────────────────────────────┐               │
│  │ AES-256-GCM                             │               │
│  │   IV: 12 bytes (random)                 │               │
│  │   AAD: Policy JSON (canonical)          │               │
│  │   Plaintext: Vault Payload              │               │
│  └─────────────────────────────────────────┘               │
│           │                                                 │
│           ▼                                                 │
│  EncryptedSharePackage (v1)                                 │
│  { version, salt, iv, policyHeader, ciphertext }           │
│                                                             │
└─────────────────────────────────────────────────────────────┘

KEY SEPARATION GUARANTEES:
├── Pattern derivation:     PBKDF2(600k, deviceSalt)     → Vault Derivation Key
├── Recovery derivation:    PBKDF2(300k/800k, deviceSalt) → Recovery Vault Key
├── Sharing derivation:     PBKDF2(250k, shareSalt)       → Share Encryption Key
└── Master Vault Key:       Random 256-bit (wrapped by VDK) → File encryption
```

### 2.3 Sharing Phrase Generation

```kotlin
// Current implementation in EncryptedShareEngine (lines 83-95)
fun generateShareSecretPhrase(): String {
    val randomBytes = ByteArray(16)
    SecureRandom().nextBytes(randomBytes)

    val digest = MessageDigest.getInstance("SHA-256").digest(randomBytes)
    val words = mutableListOf<String>()
    val secureRandom = SecureRandom()
    for (i in 0 until 12) {
        val index = secureRandom.nextInt(WORDLIST.size)
        words.add(WORDLIST[index])
    }
    return words.joinToString(" ")
}
```

**Issues to fix:**
1. Current wordlist only has 80 words (needs 2048 for BIP39 compatibility or at least 1000+)
2. No checksum word (BIP39 uses last word as checksum)
3. Should use standardized BIP39 English wordlist for interoperability

**Recommended:** Use full BIP39 2048-word list, generate 128-bit entropy → 12 words with checksum.

### 2.4 Share Package Format (Binary)

```
EncryptedSharePackage (version 0x01)
┌────────────────────────────────────────────────────────────┐
│ Byte 0:           version (0x01)                           │
│ Bytes 1-16:       salt (16 bytes)                          │
│ Bytes 17-28:      IV (12 bytes)                            │
│ Bytes 29-32:      policyHeader length (uint32, big-endian) │
│ Bytes 33..N:      policyHeader (JSON, UTF-8)               │
│ Bytes N+1..end:   ciphertext (includes 16-byte GCM tag)    │
└────────────────────────────────────────────────────────────┘
```

**Policy Header (AAD) — Canonical JSON:**
```json
{
  "v": 1,
  "exp": 1767225600000,
  "maxOpen": 5,
  "noExport": true,
  "created": 1767139200000
}
```
- Fields omitted when null/false (except `v` and `created`)
- Canonical serialization: no whitespace, keys sorted alphabetically

### 2.5 Payload Structure (Plaintext before encryption)

```kotlin
data class SharePayload(
    val version: Int = 1,
    val vaultName: String,
    val ownerId: String,           // Pseudonymous, not PII
    val files: List<SharedFile>,
    val masterKeyWrapped: ByteArray,  // MVK wrapped with share key
    val fileIndex: ByteArray          // Encrypted vault index (reuse existing format)
)

data class SharedFile(
    val fileId: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val createdMs: Long,
    val thumbnail: ByteArray?  // Encrypted, small
)
```

**Note:** The payload contains the **wrapped Master Vault Key** (encrypted with the share key) so the recipient can decrypt files using the same storage engine. This maintains code reuse with `VaultStorageEngine`.

---

## 3. Access Controls as AAD

### 3.1 Policy Binding Mechanism
The policy JSON is passed to `Cipher.updateAAD()` **before** `doFinal()`. This means:

```
Encryption:  ciphertext = AES-GCM-Encrypt(key, iv, plaintext, AAD=policyJSON)
Decryption:  plaintext  = AES-GCM-Decrypt(key, iv, ciphertext, AAD=policyJSON)
```

**Tampering any policy field** (expiration, maxOpen, noExport) → **GCM tag verification fails** → `AEADBadTagException`.

### 3.2 Policy Enforcement Points

| Control | Enforcement Location | Bypass Risk |
|---------|---------------------|-------------|
| Expiration | 1. AAD binding (cryptographic) 2. Local check in `unpackSharePackage` | Low — crypto + app |
| Max Opens | Local counter in recipient's shared vault metadata | Medium — client-enforced |
| No Export | UI hiding + `FLAG_SECURE` on Activity | High — camera/root |
| Revocation | Cloud flag + sync check | Medium — eventual |

### 3.3 Max Opens Counter Design
- Stored in recipient's local `SharedVaultMetadata` (encrypted with device key)
- Incremented on **successful** `unpackSharePackage` (after GCM verify)
- Survives app restart/reinstall only if metadata persists
- **Not cryptographic** — explicitly documented as app-enforced

---

## 4. Transport Layer: Google Drive App Folder

### 4.1 Why App Folder?
- Per-user, per-app isolated storage
- No user-facing Drive UI clutter
- Automatic sync across user's devices
- 15 GB free tier (shared with user's Drive)
- Changes API for efficient polling

### 4.2 File Structure in Drive
```
App Folder (root)
├── shares/
│   ├── {shareId}/
│   │   ├── package.v1.bin          # EncryptedSharePackage v1
│   │   ├── package.v2.bin          # Update v2 (optional)
│   │   └── metadata.json           # ShareMetadata
│   └── {shareId2}/
│       └── ...
└── .sync-token                     # Drive Changes API start token
```

### 4.3 ShareMetadata.json
```json
{
  "shareId": "uuid-v4",
  "phraseHash": "sha256(phrase) hex",
  "ownerId": "pseudonymous-id",
  "vaultName": "Personal Docs",
  "policy": { "exp": 1767225600000, "maxOpen": 5, "noExport": true },
  "version": 1,
  "revoked": false,
  "createdMs": 1767139200000,
  "updatedMs": 1767139200000,
  "spent": false
}
```

### 4.4 Drive Operations

| Operation | API Call | Retry Policy |
|-----------|----------|--------------|
| Upload package | `Files.create` (multipart) | Exponential backoff 1s, 2s, 4s, 8s, max 5 tries |
| Upload metadata | `Files.update` | Same |
| Download package | `Files.get` (alt=media) | Same |
| Check updates | `Changes.getStartPageToken` → `Changes.list` | Periodic (15 min) + push via FCM (optional) |
| Mark phrase spent | `Files.update` (metadata.spent=true) | Immediate retry |

### 4.5 Authentication
- Google Sign-In with `DriveScopes.DRIVE_APPDATA`
- Token stored in `AccountManager` / `GoogleSignInAccount`
- Silent refresh via `GoogleAccountCredential`

---

## 5. Revocation Mechanism

### 5.1 Design: Cloud-Side Flag + Key Rotation

**Option A: Revocation Flag (Chosen)**
- Owner sets `metadata.revoked = true` + `updatedMs`
- Recipient checks on every sync; if `revoked && updatedMs > lastSeenMs` → lock vault
- Simple, eventual consistency, no key rotation needed

**Option B: Key Rotation (Rejected)**
- Owner generates new share key, re-encrypts, uploads
- Recipient cannot decrypt old package
- Complex: requires recipient to have "current key" pointer

### 5.2 Revocation Flow
```
Owner taps "Revoke"
       │
       ▼
Write metadata.json { revoked: true, updatedMs: now }
       │
       ├── Online? ──Yes──▶ Upload immediately
       │
       └── No ──▶ Queue in WorkManager (periodic retry)
                        │
                        ▼
              On connectivity restored: upload
                        │
                        ▼
              Recipient next sync: detects revoked=true
                        │
                        ▼
              Lock local shared vault; show "Access revoked"
```

### 5.3 Duress Mode Interaction
Per website: *"cloud backups remain"* after duress wipe.

| Scenario | Behavior |
|----------|----------|
| Owner triggers Duress | Local vaults purged; **shares metadata preserved** in Drive |
| Owner re-installs app | Can recover shares from Drive metadata; continue revoking/publishing |
| Recipient triggers Duress | Their local shared vault purged; phrase spent; cannot re-claim |

---

## 6. Sync & Incremental Updates

### 6.1 Update Publication
```
Owner modifies vault
       │
       ▼
Re-serialize vault payload (files + wrapped MVK + index)
       │
       ▼
Encrypt with SAME share key (same salt) + NEW IV
       │
       ▼
Upload as package.v{version}.bin
       │
       ▼
Update metadata.json { version: version+1, updatedMs: now }
```

### 6.2 Recipient Sync
- `WorkManager` periodic (15 min) + foreground on app open
- `Changes.list` from last saved `startPageToken`
- For each changed share file:
  - Download new `metadata.json`
  - If `version > localVersion`: download `package.v{version}.bin`
  - Decrypt, replace local read-only vault
  - If `revoked`: lock vault

---

## 7. Android Adaptations (vs iOS/CloudKit)

| iOS Feature | Android Equivalent | Implementation Notes |
|-------------|-------------------|---------------------|
| CloudKit public DB | Drive App Folder | Private per-user; no public discovery |
| CloudKit record names | Drive file IDs + metadata | Phrase hash in metadata for lookup |
| iCloud sign-in | Google Sign-In (Drive scope) | `GoogleSignInOptions.requestScopes(DriveScopes.DRIVE_APPDATA)` |
| Push notifications | Drive Changes API polling | 15-min WorkManager; FCM for instant (optional) |
| Keychain `WhenUnlockedThisDeviceOnly` | Android Keystore `AES256_GCM` + `USER_AUTHENTICATION_REQUIRED` | Encrypt local share metadata at rest |
| Biometric gating | BiometricPrompt + CryptoObject | Protect share creation/revocation |
| QR code sharing | ZXing / ML Kit barcode scanning | Encode phrase as `vaultaire://share?phrase=...` |

### 7.1 Local Share Metadata Protection
```kotlin
// Encrypt ShareMetadata with device-bound key before storing in SharedPreferences/DataStore
class ShareMetadataStore(private val context: Context) {
    private val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "share_metadata",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    // Store: shareId -> serialized metadata
}
```

---

## 8. Integration Points

### 8.1 Billing: Pro to Create, Free to Open
```kotlin
// In ShareCreationViewModel
fun canCreateShare(): Boolean {
    return entitlementManager.hasEntitlement(Entitlement.E2EE_SHARED_VAULTS)
}

// In OpenSharedVaultViewModel
fun canOpenShare(): Boolean {
    return true  // Free users can open
}
```

### 8.2 Duress Mode
- `VaultManager.purgeNonDecoyVaultFiles()` — **exclude** share metadata from purge
- Share metadata stored in Drive + encrypted local prefs (not in vault files)
- On duress: owner loses local vaults but retains cloud shares → can revoke after recovery

### 8.3 Existing Crypto Engines
| Engine | Reuse Strategy |
|--------|----------------|
| `EnvelopeEncryptionEngine` | Wrap MVK with share key for payload |
| `KeyDerivationEngine` | Pattern → VDK (unchanged) |
| `RecoveryPhraseEngine` | Reference for separate derivation |
| `ZeroKnowledgeBackupEngine` | Streaming encryption pattern for large payloads |
| `VaultStorageEngine` | Recipient uses same engine to read shared files |

---

## 9. Security Checklist

| Item | Status | Notes |
|------|--------|-------|
| AES-256-GCM for payload | ✅ | `EncryptedShareEngine` |
| PBKDF2 250k iterations | ✅ | Configurable constant |
| Policy as AAD | ✅ | `cipher.updateAAD(policyHeader)` |
| Unique salt per share | ✅ | `SecureRandom 16 bytes` |
| Unique IV per package | ✅ | `SecureRandom 12 bytes` |
| Phrase normalization | ✅ | `trim().lowercase()` |
| Constant-time compare | ⚠️ | Use `MessageDigest.isEqual` for phrase hash |
| Memory hygiene | ⚠️ | Clear char arrays, key bytes after use |
| Keystore for local metadata | ❌ | Need to implement |
| Certificate pinning for Drive | ❌ | Use Google Play Services (pinned) |
| Replay protection | ⚠️ | Version in metadata + package |
| Forward secrecy | ❌ | Not applicable (symmetric) |
| Post-compromise security | ❌ | Revocation only; no key rotation |

---

## 10. Implementation Phases

### Phase 1: Core Crypto & Package Format (Week 1-2)
- [ ] Expand BIP39 wordlist to 2048 words
- [ ] Add checksum to sharing phrase
- [ ] Implement `SharePayload` serialization
- [ ] Unit tests for package create/unpack
- [ ] AAD tampering tests

### Phase 2: Google Drive Transport (Week 2-3)
- [ ] Google Sign-In + Drive scope
- [ ] App Folder file operations (CRUD)
- [ ] Changes API polling with WorkManager
- [ ] Offline queue + retry logic
- [ ] Integration tests with real Drive

### Phase 3: Owner UI & Share Management (Week 3-4)
- [ ] Share creation screen (Compose)
- [ ] Policy controls (date, count, export toggle)
- [ ] My Shares list with revoke/publish
- [ ] QR code generation for phrase

### Phase 4: Recipient UI & Read-Only Vault (Week 4-5)
- [ ] "Open Shared Vault" entry point
- [ ] Phrase entry + QR scan
- [ ] Read-only vault view (reuse Vault UI)
- [ ] Export/screenshot blocking
- [ ] Sync indicator + pull-to-refresh

### Phase 5: Revocation, Sync, Polish (Week 5-6)
- [ ] Revocation flow + offline retry
- [ ] Incremental update publishing
- [ ] Recipient sync worker
- [ ] Duress mode integration
- [ ] Billing gating
- [ ] Security audit + penetration testing

### Phase 6: Tests & Release (Week 6-7)
- [ ] All test specs from `tests.md`
- [ ] Automated CI pipeline
- [ ] Beta testing
- [ ] Release

---

## 11. Open Questions

| Question | Recommendation |
|----------|----------------|
| BIP39 vs custom wordlist? | **BIP39** — standard, interoperable, 2048 words, checksum |
| Push notifications for sync? | **Phase 2: polling only**; FCM optional later |
| Max opens enforcement? | **Local counter + cloud sync**; document as app-enforced |
| Large vault handling (>100MB)? | **Streaming encryption** (ref `ZeroKnowledgeBackupEngine`) |
| Multiple recipients per share? | **No** — website: "Create a separate share for each person" |
| Share expiration cleanup? | **Lazy** — on recipient sync; owner UI shows expired |
| Conflict: owner updates while recipient viewing? | **Version check on open**; show "Update available" banner |

---

*Document Version: 1.0*  
*Author: Architecture Subagent*  
*Date: 2026-11-16*