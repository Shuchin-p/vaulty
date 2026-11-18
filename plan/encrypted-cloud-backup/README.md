# Encrypted Cloud Backup — Planning Package

> **Feature**: Encrypted iCloud Backup (Android: Encrypted Google Drive Backup)
> **Source**: https://vaultaire.app/features/encrypted-icloud-backup/
> **Security context**: https://vaultaire.app/features/security-features/
> **Status**: ⏳ Planning — not yet implemented
> **Android adaptation target**: Google Drive API (resumable upload via `com.google.android.gms:play-services-drive` or Drive REST API), Google Account auth, WorkManager for background scheduling

---

## Status Dashboard

| Component | Status | File |
|---|---|---|
| Backup settings toggle (on/off) | ❌ Not started | — |
| Separate backup key derivation | ❌ Not started | `KeyDerivationEngine.kt` needs `deriveBackupKey()` |
| Fixed-size padded records | ⚠️ Partial | `ZeroKnowledgeBackupEngine.kt` has bucket padding, not fixed-size chunking |
| AES-256-GCM + header AAD | ✅ Implemented | `ZeroKnowledgeBackupEngine.kt` lines 127–131 |
| Cloud upload (Google Drive) | ❌ Not started | — |
| Recovery phrase → backup key | ❌ Not started | — |
| 24-hour backup scheduling | ❌ Not started | — |
| Deduplication | ❌ Not started | — |
| Decoy records | ❌ Not started | — |
| Device Sync integration | ❌ Not started | — |
| Duress mode survival | ❌ Not started | — |
| Restore on new device | ❌ Not started | — |

---

## Quick Navigation

- **[README.md](./README.md)** — This file. Overview, claims, status.
- **[planning.md](./planning.md)** — Product Owner: user stories, flows, acceptance criteria, UX, trust boundaries.
- **[implementation.md](./implementation.md)** — Tech Lead / Architect: architecture, crypto design, Android adaptations, integrations.
- **[tests.md](./tests.md)** — Test specifications: unit, integration, security, edge cases.

---

## Website Claims Summary

### Core Claims
1. **On by default, can be turned off** — Free and Pro tiers. Not Pro-only.
2. **Separate pattern-derived backup key** — Different from the vault key. Derived locally.
3. **Fixed-size padded records** — Each encrypted chunk padded to 10 MB before upload. Decoy records added.
4. **Apple sees only ciphertext** — No readable vault files, no plaintext backup key. Even with a court order.
5. **Encrypted iCloud backup is the transport for Device Sync** — Backup pipeline carries sync data.
6. **AES-256-GCM** — Authenticated encryption. Header used as AAD.
7. **PBKDF2-HMAC-SHA512** — 600K iterations for pattern, 800K for recovery phrase.
8. **Recovery phrase restores on any device** — Pattern restore only works on the device that made the backup.
9. **24-hour cadence** — Checks after unlock, backs up when ≥24h elapsed. Wi-Fi only by default.
10. **Backup counts toward iCloud storage** — Padded chunks + decoys make it larger than raw vault.

### Trust Boundaries (What Apple Sees vs. Doesn't)
| Apple Sees | Apple Does NOT See |
|---|---|
| Ciphertext records | Vault plaintext |
| Record counts | Backup key (plaintext) |
| Padded sizes | File names (encrypted in headers) |
| Access timing | Content hashes |
| Account metadata | Pattern or recovery phrase |

### Key Design Decisions
- **Encryption happens BEFORE upload**, not during/after.
- **Zero-knowledge architecture**: No server-side decryption key exists.
- **Client-side key custody**: Losing pattern + recovery phrase = losing everything. No reset path.
- **Backup ≠ Sync**: Backup is point-in-time recovery. Sync is real-time convergence.

---

## Android Platform Adaptations

| iOS Concept | Android Equivalent |
|---|---|
| iCloud / CloudKit | Google Drive (Drive API v3) |
| Private CloudKit database | Google Drive "App Data" folder (`drive.appdata`) |
| Apple Account auth | Google Account OAuth 2.0 (`com.google.android.gms.auth`) |
| Keychain (WhenUnlockedThisDeviceOnly) | Android Keystore + EncryptedSharedPreferences |
| Background upload/download | `WorkManager` + `ForegroundService` for visible progress |
| 24-hour eligibility check | `WorkManager` periodic task (flex 23h, repeat 24h) |
| iOS Data Protection | Android Keystore hardware-backed keys |

---

## Existing Code Audit

### What We Have
- `ZeroKnowledgeBackupEngine.kt` — AES-256-GCM encrypt/decrypt with header AAD, bucket-based padding
- `KeyDerivationEngine.kt` — PBKDF2-HMAC-SHA512, 600K iterations, derives vault key from pattern
- `EnvelopeEncryptionEngine.kt` — Master vault key generation, wrap/unwrap with GCM

### What We're Missing (Gaps)
1. No separate **backup key** derivation path (currently uses `masterVaultKey` directly for backup encryption)
2. No **fixed-size chunking** (currently pads to variable-size buckets, not fixed 10 MB chunks)
3. No **decoy records**
4. No **Google Drive upload/download** path
5. No **recovery phrase** integration with backup key
6. No **24-hour scheduling** (WorkManager)
7. No **deduplication** of unchanged components
8. No **settings toggle** for backup on/off
9. No **Device Sync integration** (backup as sync transport)
10. No **Duress Mode** awareness (backup survives duress wipe)