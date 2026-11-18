# Plausible Deniability — Planning Package

**Feature**: Plausible Deniability & Pattern-Separated Vaults  
**Source**: https://vaultaire.app/features/plausible-deniability/  
**Related**: https://vaultaire.app/features/hidden-vaults/  
**Platform**: Android (Kotlin 2.x, Jetpack Compose + Material 3)  
**Status**: Planning — Major gaps identified, redesign required  
**Pro Feature**: No (core feature); Duress Mode is Pro (`Entitlement.DURESS_MODE`)

---

## What This Feature Is

Plausible Deniability in Vaultaire is an **interface compartmentalization** property combined with **pattern-addressed vault resolution**. It is NOT information-theoretic deniability.

### Core Definition (Per Website)
- The app does **not** open to a catalog listing vault names or patterns.
- The user draws a pattern on a 5×5 grid → derives a Vault Derivation Key (VDK) → derives a locator → attempts authenticated decryption of the encrypted index at that locator.
- If the index exists and AES-GCM verifies, the vault opens. Otherwise, the interface presents an empty state.
- Different patterns address different encrypted vault indexes.
- **No plaintext cross-vault catalog exists** — no registry mapping patterns to vault names or labels.
- Root index files sit in a **padded pool of 64 slots** with random cover files.
- Storage files have random names in a **shared folder** — file names do not reveal vault membership.

### What It Does NOT Mean (Honest Forensic Limits)
- It does **not** hide vault count from a forensic examiner with filesystem access.
- It does **not** hide that multiple vault indexes exist.
- It does **not** make file allocation, modification timestamps, or total volume invisible.
- It does **not** prevent offline pattern guessing against copied index files (key-derived locator + AES-GCM tag = offline guess verification).
- It does **not** erase filesystem evidence (arena count, file times, total volume).

---

## Critical Gap: Current Code Contradicts Website Claims

| Website Claim | Current Code (`VaultHeader.kt`, `VaultManager.kt`) | Severity |
|---------------|---------------------------------------------------|----------|
| "No plaintext cross-vault catalog" | `VaultHeader` stores `vaultId` (UUID string) + `isDuressDecoy` (plaintext byte) | **CRITICAL** |
| "Key-derived locator names index file" | Files named `vaultId + ".vlt"` (UUID), NOT key-derived locator | **CRITICAL** |
| "Root files in padded pool of 64" | Flat directory `vaultsBaseDir`, no pool, no cover files | **CRITICAL** |
| "Pattern addresses vault without vault list" | `unlockVault(vaultId, vdk)` requires `vaultId` parameter | **CRITICAL** |
| "Duress vault not labeled" | `isDuressDecoy` flag in plaintext on header | **CRITICAL** |

**Conclusion**: The current `VaultHeader` design **directly leaks** vault identity and decoy status to any examiner. A complete redesign of the vault envelope header and discovery mechanism is required.

---

## Status Dashboard

| Area | Website Claim | Current Code Status | Gap |
|------|--------------|---------------------|-----|
| No vault list UI | App launches to pattern grid, not vault catalog | Not implemented | Full unlock screen needed |
| Pattern → vault key derivation | PBKDF2-HMAC-SHA512, 600K rounds, 256-bit output | ✅ `KeyDerivationEngine` matches | None |
| Key-derived locator naming | Locator from VDK names index file | ❌ Uses UUID `vaultId` + `.vlt` | Major: rename strategy |
| Encrypted vault index | AES-256-GCM encrypted index per vault | ⚠️ `VaultHeader` exists but leaks metadata | Header must be fixed-size, no plaintext fields |
| Anti-brute-force throttling | 600K PBKDF2 rounds per guess | ✅ KDF iteration count correct | Need attempt throttling on lock screen |
| Random file names | Storage files random, no vault association | ⚠️ Unknown — storage engine takes caller `File` | Need audit of storage naming |
| Root file pooling | Padded pool of 64 slots + cover files | ❌ Plain directory | Major: implement `StoragePoolManager` |
| Plausible deniability integration | Different patterns → different vaults, no list | ❌ `isDuressDecoy` plaintext flag | No pattern-addressed routing |
| Duress mode purge | Purges non-duress vaults | ✅ `purgeNonDecoyVaultFiles()` exists | Needs integration with pattern flow |
| Forensic limit transparency | Admits examiner can estimate vault count | ❌ Not documented | Needs UX guardrails & help text |

---

## Quick Navigation

| Document | Audience | Purpose |
|----------|----------|---------|
| [`planning.md`](./planning.md) | Product Owner | User stories, acceptance criteria, UX flows, honest limitations |
| [`implementation.md`](./implementation.md) | Tech Lead / Architect | Architecture diagram, fixed-size envelope header (96 bytes), crypto specs, migration path |
| [`tests.md`](./tests.md) | QA / Test Lead | Unit, security, integration, and performance test specifications |

---

## Key Website Claims (Verified)

1. ✅ **"Different patterns can open separate encrypted vaults without a visible vault picker"** — True on iOS; Android not yet built
2. ✅ **"No visible cross-vault picker"** — True in interface; Android not implemented
3. ✅ **"Limits interface disclosure"** — True; but filesystem examiner can estimate count
4. ✅ **"An examiner with your files can still estimate how many vaults you have"** — **Honest limitation; must document**
5. ✅ **"File names on disk do not show which vault a file belongs to"** — Design confirmed; storage naming needs audit
6. ✅ **"Encrypted index contents remain private"** — True with AES-GCM; current header leaks `vaultId` and `isDuressDecoy`
7. ✅ **"Root index files sit in a pool padded with random cover files"** — Design confirmed; not implemented on Android
7. ✅ **"1 encrypted index per vault"** — True; but current design needs fixed-size envelope
8. ✅ **"600K PBKDF2 rounds per guess"** — ✅ Implemented in `KeyDerivationEngine`
9. ✅ **"No plaintext map from locators to patterns or readable contents"** — True; current UUID breaks this

---

## Dependencies on Other Features

| Dependency | Status | Required By |
|------------|--------|-------------|
| `KeyDerivationEngine` | ✅ Implemented | VDK derivation (600K PBKDF2) |
| `EnvelopeEncryptionEngine` | ✅ Implemented | Wrap/unwrap MVK with VDK (AES-GCM) |
| `DeviceSaltManager` | ⚠️ Uses SharedPreferences | Salt for KDF (needs KeyStore migration) |
| `VaultStorageEngine` | ✅ Implemented | File encryption/decryption |
| `VaultManager` | ⚠️ Major refactor needed | Vault CRUD, discovery, purge |
| **New: `VaultLocator`** | ❌ Needed | Derive locator from VDK |
| **New: `StoragePoolManager`** | ❌ Needed | Manage 64-slot padded pool |
| **New: `VaultEnvelopeHeader`** | ❌ Needed | Replace `VaultHeader` (96 bytes, no plaintext) |
| **New: `VaultDiscoveryEngine`** | ❌ Needed | Try VDK against all envelopes |
| **New: `AttemptThrottler`** | ❌ Needed | Anti-brute-force on pattern entry |

---

## References

- [Plausible Deniability feature page](https://vaultaire.app/features/plausible-deniability/)
- [Hidden Vaults feature page](https://vaultaire.app/features/hidden-vaults/)
- [Duress Mode feature page](https://vaultaire.app/features/duress-mode/)
- [Hidden Vaults Planning](../hidden-vaults/planning.md)
- [Security Architecture Planning](../security-architecture/planning.md)
- [Pattern Encryption Planning](../pattern-encryption/planning.md)
- [NIST SP 800-132: Password-Based Key Derivation](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-132.pdf)
- [NIST SP 800-38D: GCM Mode](https://csrc.nist.gov/pubs/sp/800/38/d/final)
- [PEARL: Plausibly Deniable Flash Translation Layer (USENIX Security '21)](https://www.usenix.org/conference/usenixsecurity21/presentation/chen-chen)
- [Shufflecake: Plausible Deniability for Multiple Hidden Filesystems](https://eprint.iacr.org/2020/1071)