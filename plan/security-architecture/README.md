# Security Architecture — Planning Package

**Feature**: Security Architecture  
**Source**: [https://vaultaire.app/features/security-features/](https://vaultaire.app/features/security-features/)  
**Master Cryptographic Specification**: This is the authoritative document for all security claims.

---

## Website Claims Summary

| # | Claim | Category |
|---|-------|----------|
| 1 | AES-256-GCM protects file content, metadata, thumbnails, indexes, and key wrappers | Encryption |
| 2 | PBKDF2-HMAC-SHA512 derives separate vault, backup, and recovery keys for their defined roles | Key Derivation |
| 3 | No identity account required | Architecture |
| 4 | Readable vault content is not sent to a Vaultaire-operated storage server | Zero-Knowledge |
| 5 | Layered cryptographic architecture (failure of one layer doesn't compromise others) | Architecture |
| 6 | Random master key protects files | Key Hierarchy |
| 7 | Device-wide Keychain salt (Android: KeyStore) | Platform Security |
| 8 | Pattern key derivation: 600,000 PBKDF2 iterations | Key Derivation |
| 9 | Recovery phrase derivation: 800,000 PBKDF2 iterations | Key Derivation |
| 10 | Deterministic pattern derivation produces separate cloud backup key | Key Derivation |
| 11 | Per-file initialization vectors (nonces) prevent deterministic ciphertext | Encryption |
| 12 | Memory management: clearing active key state on lock/crash | Memory Security |
| 13 | Screen security: FLAG_SECURE usage | Screen Security |
| 14 | Anti-tampering: root detection, debugger detection, certificate pinning | Anti-Tampering |
| 15 | Analytics/crash diagnostics opt-out (PostHog, Google Play Install Referrer) | Privacy |
| 16 | Authenticated metadata (AES-GCM tags on headers, indexes, thumbnails) | Encryption |
| 17 | Encrypted Google Drive backup (separate backup key, padded records) | Backup |
| 18 | Secure sharing (one-time phrase, policy enforcement, AAD) | Sharing |
| 19 | Duress mode (silent vault destruction) | Plausible Deniability |
| 20 | Hidden vaults / plausible deniability (no cross-vault list in UI) | Plausible Deniability |
| 21 | Secure Camera (direct capture to encrypted files) | Secure Input |
| 22 | App disguise (calculator, printer, etc. icons) | Disguise |

---

## Complete Gap Analysis

### ✅ Implemented

| Component | File(s) | Notes |
|-----------|---------|-------|
| Pattern representation (5×5 grid) | `PatternPoint.kt` | Data class with index computation |
| Pattern normalization & interpolation | `PatternUtils.kt` | Domain separator `VAULTAIRE_PATTERN_V1`, straight-line interpolation |
| Vault key derivation (PBKDF2-HMAC-SHA512, 600k) | `KeyDerivationEngine.kt` | Correct algorithm and iterations |
| Device-wide salt generation | `DeviceSaltManager.kt` | 32-byte random salt |
| Master vault key generation (256-bit random) | `EnvelopeEncryptionEngine.kt` | `SecureRandom` |
| Envelope encryption (wrap/unwrap MVK with VDK) | `EnvelopeEncryptionEngine.kt` | AES-256-GCM, 12-byte IV, 128-bit tag |
| Recovery phrase key derivation | `RecoveryPhraseEngine.kt` | 12-word mnemonic, PBKDF2 |
| Memory hygiene (zeroing buffers/keys) | `MemoryHygiene.kt` | `Arrays.fill` for byte arrays and key material |
| Streaming file encryption (chunked AES-GCM) | `EncryptedOutputStream.kt` | 64KB chunks, per-chunk IV derivation |
| Random-access decryption | `RandomAccessEncryptedSourceImpl.kt` | Chunk-aligned reads, IV derivation matches writer |
| Vault header with wrapped master key | `VaultHeader.kt` | Magic `VLTHDR1`, vault ID, duress flag, envelope |
| Multi-vault management (plausible deniability) | `VaultManager.kt` | Separate `.vlt` files per vault, duress purge |
| Secure camera capture (photo/video → encrypted) | `SecureCameraManager.kt` | Direct to `EncryptedOutputStream`, no Photos library |
| App disguise (alias activities) | `AppDisguiseManager.kt` | 7 icons, Pro-gated, component enable/disable |
| Secure sharing (one-time phrase, policies) | `EncryptedShareEngine.kt` | 12-word phrase, 250k PBKDF2, AAD for policy, expiration/max-opens/no-export |
| Zero-knowledge backup (encrypted, padded) | `ZeroKnowledgeBackupEngine.kt` | AES-GCM, header AAD, bucket padding, master key |
| Entitlement system (Pro features) | `Entitlement.kt` | 6 entitlements including duress, sharing, sync |

---

### ⚠️ Partial (specify what's missing)

| Component | File(s) | Missing / Deviation |
|-----------|---------|---------------------|
| **Device salt storage** | `DeviceSaltManager.kt` | Uses `SharedPreferences` (plaintext XML) — **should use Android KeyStore (TEE/StrongBox)** per website "Keychain" claim |
| **Recovery phrase iterations** | `RecoveryPhraseEngine.kt` | Uses **300,000** iterations — website claims **800,000** |
| **Backup key derivation** | `ZeroKnowledgeBackupEngine.kt` | Uses **master vault key directly** — website: "deterministic pattern derivation produces the separate cloud backup key" |
| **Sharing key iterations** | `EncryptedShareEngine.kt` | Uses **250,000** iterations — no website spec, but inconsistent with 600k/800k pattern |
| **Vault index encryption** | `VaultManager.kt` + storage | Index files (file records, dates, sizes, wrapped MVK) — **no visible encryption of index content** |
| **Thumbnail encryption** | `SecureCameraManager.kt` + storage | Thumbnails — **not explicitly encrypted separately**; may be embedded in file |
| **Metadata encryption (filenames, timestamps, MIME)** | `VaultStorageEngine` | File headers contain magic/version/salt/IV/chunk size — **filenames/MIME not shown as encrypted** |
| **FLAG_SECURE (screen security)** | — | **Not implemented anywhere** — website implies secure screen |
| **Root / debugger detection** | — | **Not implemented** — website claims anti-tampering |
| **Certificate pinning** | — | **Not implemented** — website claims anti-tampering |
| **Analytics / crash opt-out** | — | **No infrastructure** — website: "Anonymous usage and crash data go to PostHog by default, and you can turn them off" |
| **Audit logging** | — | **Not implemented** — website: "what's logged, what's never logged" |

---

### ❌ Missing

| Component | Website Reference | Notes |
|-----------|-------------------|-------|
| Android KeyStore integration for device salt | "Device-wide Keychain salt (Android: KeyStore)" | Critical: salt must be in TEE/StrongBox |
| Separate backup key derivation (PBKDF2 from pattern) | "deterministic pattern derivation produces the separate cloud backup key" | Must not use master vault key |
| Key rotation strategy | "Key rotation strategy: when and how keys are rotated" | Pattern change → rewrap MVK; backup key rotation; recovery phrase change |
| Authenticated metadata binding | "Authenticated metadata: how metadata is bound to content" | File headers, index records, thumbnails need AAD |
| Platform security: EncryptedSharedPreferences | "Platform security: Android KeyStore usage, TEE/StrongBox, EncryptedSharedPreferences" | For any persisted secrets |
| GDPR right to deletion / data minimization | "Compliance considerations" | No deletion API, no data minimization evidence |
| Crash diagnostics / PostHog integration | "Anonymous usage and crash data go to PostHog by default" | Opt-out UI + network layer |
| Apple Ads attribution token handling | "While analytics are on, an Apple Ads attribution token..." | Android equivalent (Google Play Install Referrer) |
| Secure Enclave / StrongBox key storage for MVK | "iOS Keychain... Vaultaire does not describe its vault cipher as Secure Enclave AES" | Android: StrongBox-backed keys for MVK wrapping |
| Index file format specification | "encrypted vault index contains file records, dates, size information, storage layout, and the wrapped master key" | Not implemented as encrypted structure |

---

### 🔄 Contradicts

| Website Claim | Code Reality | Severity |
|---------------|--------------|----------|
| Recovery phrase: 800,000 PBKDF2 iterations | `RecoveryPhraseEngine.kt`: 300,000 | **High** — weaker than advertised |
| Device salt in Keychain/KeyStore | `DeviceSaltManager.kt`: `SharedPreferences` (world-readable XML) | **Critical** — salt not hardware-protected |
| Separate backup key from pattern | `ZeroKnowledgeBackupEngine.kt`: uses master vault key directly | **High** — backup compromise = vault compromise |
| "Vaultaire does not operate an account or storage service that receives your pattern, secret phrase, decryption keys, or readable vault contents" | No network layer visible — but **no opt-out for analytics/crash** | **Medium** — privacy claim unverifiable |
| "Keychain and Data Protection create a meaningful device boundary, especially while the phone is locked" | No `EncryptedSharedPreferences`, no KeyStore usage for salt | **High** — device-locked protection not implemented |
| "Vaultaire uses ordinary iOS Keychain generic-password items... WhenUnlockedThisDeviceOnly" | Android equivalent not implemented | **High** — platform parity gap |

---

## Quick Navigation

- [Product Owner Perspective → `planning.md`](planning.md)
- [Tech Lead / Architect Perspective → `implementation.md`](implementation.md)
- [Test Specifications → `tests.md`](tests.md)