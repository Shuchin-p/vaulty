# Secure Sharing Feature — Planning Package

## Overview
This folder contains the complete planning documentation for the **Secure Sharing** feature of Vaultaire Android. The feature enables a vault owner to share an encrypted, read-only copy of their vault with a single recipient using a one-time 12-word sharing phrase. The files are encrypted on the owner's device, transported via cloud storage (Google Drive on Android), and decrypted on the recipient's device. The owner retains write access and can revoke future access.

---

## Website Claims Summary (from https://vaultaire.app/features/secure-sharing/)

| Claim | Status |
|-------|--------|
| Create a one-time sharing phrase (12 words) | ✅ Implemented in `EncryptedShareEngine.generateShareSecretPhrase()` |
| Encrypts shared files before they leave owner's device | ✅ Core logic in `EncryptedShareEngine.createSharePackage()` |
| Sends encrypted data through iCloud (Android: Google Drive) | ❌ Transport layer not implemented |
| Recipient needs Vaultaire + Google sign-in + unused sharing phrase | ⚠️ Google sign-in / transport not implemented |
| Phrase is separate from pattern and recovery phrase | ✅ Uses independent PBKDF2 derivation with separate salt |
| One recipient gets read-only access | ✅ Policy enforced in `SharePolicy` + AAD binding |
| Owner remains the only writer | ✅ Architecture enforces one-way share |
| Owner can revoke future access | ❌ Revocation mechanism not implemented |
| Pro to create, Free to open | ✅ `E2EE_SHARED_VAULTS` entitlement exists |
| Expiration dates, open limits, export prevention | ✅ Policy fields exist; enforcement partial |

---

## Status Dashboard

| Component | Status | Notes |
|-----------|--------|-------|
| **Cryptographic Core** | ✅ Complete | AES-256-GCM + PBKDF2 (250k iterations) + AAD-bound policy |
| **Share Package Format** | ✅ Complete | Versioned binary format with salt, IV, policy header, ciphertext |
| **Policy Model** | ✅ Complete | Expiration, max opens, no-export, created timestamp |
| **Sharing Phrase Generation** | ✅ Complete | 12-word BIP39-style from custom wordlist |
| **Key Derivation** | ✅ Complete | Independent from pattern/recovery; per-share salt |
| **Transport Layer (Google Drive)** | ❌ Not Started | Needs Google Drive API integration |
| **Share Discovery/Claim Flow** | ❌ Not Started | Recipient-side claim logic missing |
| **Revocation Mechanism** | ❌ Not Started | Cloud-side flag + owner retry logic needed |
| **Sync/Updates** | ❌ Not Started | Incremental encrypted updates via cloud |
| **Android UI: Share Creation** | ❌ Not Started | Compose screen with policy controls |
| **Android UI: Recipient Open** | ❌ Not Started | "Open Shared Vault" entry point |
| **Integration: Billing (Pro/Free)** | ⚠️ Partial | Entitlement exists; gating logic needed |
| **Integration: Duress Mode** | ⚠️ Design Needed | Shared vaults survive local purge per website |
| **Tests** | ❌ Not Started | See `tests.md` for full specification |

---

## Quick Navigation

| Document | Purpose |
|----------|---------|
| [`planning.md`](planning.md) | Product Owner view: user stories, flows, acceptance criteria, UX, trust model |
| [`implementation.md`](implementation.md) | Tech Lead/Architect view: architecture, crypto protocol, transport, revocation, integration |
| [`tests.md`](tests.md) | Test specifications: unit, integration, security, edge cases |

---

## Related Files in Codebase

| File | Role |
|------|------|
| `app/src/main/java/com/vaultaire/android/sharing/EncryptedShareEngine.kt` | Core crypto engine for share package creation/unpacking |
| `app/src/main/java/com/vaultaire/android/crypto/EnvelopeEncryptionEngine.kt` | Master key wrapping (used by vault, reusable for share payload) |
| `app/src/main/java/com/vaultaire/android/crypto/RecoveryPhraseEngine.kt` | Reference: separate key derivation for recovery phrase |
| `app/src/main/java/com/vaultaire/android/crypto/KeyDerivationEngine.kt` | Pattern → VDK derivation (600k iterations) |
| `app/src/main/java/com/vaultaire/android/data/pricing/Entitlement.kt` | `E2EE_SHARED_VAULTS` entitlement for Pro gating |
| `app/src/main/java/com/vaultaire/android/backup/ZeroKnowledgeBackupEngine.kt` | Reference: encrypted streaming + AAD binding pattern |
| `app/src/main/java/com/vaultaire/android/vault/VaultManager.kt` | Vault create/unlock; template for share vault management |

---

## Key Technical Decisions (from website + security page)

1. **AES-256-GCM** for all encryption (files, metadata, share payloads)
2. **PBKDF2-HMAC-SHA512** for key derivation (250k iterations for shares, 600k for pattern, 300k/800k for recovery)
3. **Policy as AAD** — expiration, max opens, no-export bound into GCM auth tag; tampering = decryption failure
4. **Separate key hierarchy** — sharing phrase derives independent key; zero overlap with pattern/recovery
5. **One-time claim** — phrase spent after successful claim; per-share sync material for updates
6. **Cloud boundary** — cloud sees metadata (sizes, timestamps) but not phrase or plaintext
7. **Revocation** — recorded in cloud; recipient learns on next online check
8. **Pro to create, Free to open** — enforced via `E2EE_SHARED_VAULTS` entitlement

---

## Android Adaptations Required

| iOS / iCloud | Android Equivalent |
|--------------|-------------------|
| CloudKit public database | Google Drive App Folder / File Picker |
| CloudKit record names derived from phrase | Drive file ID or custom metadata |
| iCloud sign-in required | Google Account sign-in (Drive scope) |
| Push/sync via CloudKit notifications | Drive Changes API / periodic poll |
| iOS Keychain `WhenUnlockedThisDeviceOnly` | Android Keystore `PURPOSE_ENCRYPT/DECRYPT` + `USER_AUTHENTICATION_REQUIRED` |

---

*Generated: 2026-11-16*  
*Vaultaire Android — Secure Sharing Planning Package*