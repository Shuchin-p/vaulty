# Device Sync — Planning Package

**Feature**: Cross-device synchronization of Vaultaire vaults  
**Status**: Greenfield — no implementation exists yet  
**Entitlement**: `CROSS_DEVICE_SYNC` (Pro)  
**Platform**: Android (Google Drive transport) — equivalent to iOS iCloud sync  

---

## Website Claims Summary (from vaultaire.app)

### Core Promises
- Keep one encrypted vault in sync across your own Android devices
- Uses your private Google Drive database (Android equivalent of iCloud)
- Each change encrypted **on the device that made it** before upload
- Other device **merges the encrypted result** locally
- Never turns your pattern into an account password
- **Sync bookkeeping lives inside the encrypted vault** (not in plaintext cloud metadata)
- Pro feature (`CROSS_DEVICE_SYNC` entitlement)
- Differs from Google Photos: syncs a vault, not a library — photos never appear in Google Photos app

### Conflict Resolution Model
- **Adds win over deletes** that did not see the added file
- **Identical files collapse** (deduplication at encrypted chunk level)
- **Merge is order-independent** — running twice or with different arrival order produces same vault state
- Offline changes survive merge

### Trust Boundaries
- Google receives: encrypted padded records, record counts, padded sizes, timing metadata
- Google does NOT receive: pattern, backup key, readable file payload, vault names, file names, device names, membership fields
- Duress wipe does NOT propagate through sync — isolated device cut from sync
- "Disconnect This Device" is local: stops upload/download, keeps vault, doesn't revoke peers or delete cloud records

### Recovery & Setup
- V3 pattern restore: only on device that made backup + holds enrollment key
- Recovery phrase: restores on any device with matching encrypted backup records
- New device setup: install Vaultaire → enter recovery phrase → vault decrypts locally

---

## Current Codebase Audit

### Existing Foundation (Ready for Sync)
| Component | File | Status |
|-----------|------|--------|
| **Zero-Knowledge Backup Engine** | `backup/ZeroKnowledgeBackupEngine.kt` | ✅ Complete — AES-256-GCM, padded chunks, header auth |
| **Vault Management** | `vault/VaultManager.kt` | ✅ Complete — MVK wrapping, multi-vault, plausible deniability |
| **Storage Engine** | `storage/VaultStorageEngine.kt` | ✅ Complete — chunked AES-GCM, random access |
| **Key Derivation** | `crypto/KeyDerivationEngine.kt` | ✅ Complete — PBKDF2-HMAC-SHA512, 600k iterations |
| **Envelope Encryption** | `crypto/EnvelopeEncryptionEngine.kt` | ✅ Complete — MVK wrapping/unwrapping |
| **Recovery Phrase** | `crypto/RecoveryPhraseEngine.kt` | ✅ Complete — 12-word mnemonic, PBKDF2 300k |
| **Device Salt** | `crypto/DeviceSaltManager.kt` | ✅ Complete — per-device enrollment key |
| **Entitlement** | `data/pricing/Entitlement.kt` | ✅ `CROSS_DEVICE_SYNC` enum exists |

### Missing for Sync (Greenfield)
| Area | Status |
|------|--------|
| Google Drive API integration | ❌ Not started |
| Sync metadata model (inside encrypted vault) | ❌ Not started |
| Conflict resolution / merge logic | ❌ Not started |
| Background sync scheduler (WorkManager) | ❌ Not started |
| Sync status UI / device count | ❌ Not started |
| Duress isolation from sync | ❌ Not started |
| Disconnect device logic | ❌ Not started |
| Incremental sync (changed chunks only) | ❌ Not started |

---

## Quick Navigation

| Document | Purpose |
|----------|---------|
| [`planning.md`](planning.md) | Product Owner view: user stories, flows, acceptance criteria, UX, trust boundaries |
| [`implementation.md`](implementation.md) | Tech Lead / Architect view: architecture diagram, transport, encryption model, key management, platform adaptations, integration points |
| [`tests.md`](tests.md) | Test specifications: unit, integration, security, edge cases |

---

## Key Technical Decisions (from Website)

| Decision | Rationale |
|----------|-----------|
| **Transport = Encrypted Google Drive Backup** | Reuses existing backup infrastructure; same zero-knowledge model |
| **Sync metadata INSIDE encrypted vault** | No plaintext membership, file names, or device IDs in cloud |
| **Client-side merge, not server** | Google never decides conflicts; order-independent convergence |
| **Add-wins over unseen deletes** | Photo vault semantics: adding memories beats accidental deletes |
| **Duress wipe isolates, doesn't propagate** | Coercion protection: wipe stays local |
| **Recovery phrase = cross-device key** | Same phrase unlocks vault on any device with backup records |

---

## Next Steps
1. Review `planning.md` for product requirements
2. Review `implementation.md` for architectural design
3. Review `tests.md` for validation strategy
4. Begin implementation: Google Drive transport → sync metadata → merge engine → background sync → UI