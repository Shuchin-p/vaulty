# Hidden Vaults — Planning Package

**Feature**: Hidden Vaults (no visible vault list / pattern-addressed vault resolution)
**Source**: https://vaultaire.app/features/hidden-vaults/
**Related**: https://vaultaire.app/features/plausible-deniability/
**Platform**: Android (Kotlin 2.x, Jetpack Compose + Material 3)
**Status**: Planning — gaps identified, not yet implemented

---

## What This Feature Is

Hidden Vaults is an **interface and key-routing property**, not a cryptographic invisibility claim. The app does not show a catalog of named vaults. Instead, the user draws a pattern on a 5×5 grid, the app derives a 256-bit vault key with PBKDF2-HMAC-SHA512 (600,000 iterations), derives a locator from that key to name the expected encrypted index file, and attempts authenticated decryption of that index. If the index exists and its AES-GCM tag verifies, the vault opens. Otherwise the interface presents an empty state.

**Critical distinction**: This design hides the cross-vault catalog *from the ordinary user interface*. It does **not** hide vault count from a forensic examiner with app-container access. Root index files sit in a padded pool of 64 slots with random cover files. Storage files have random names and share one folder. An examiner can still estimate how many vaults hold files from arena count, file times, and total volume.

### Android-Native UX & Zero-Config Onboarding

| Principle | Android Implementation |
|-----------|------------------------|
| **Direct to Pattern** | App launches straight to 5×5 pattern lock screen — no RecyclerView, no vault list, no setup wizard |
| **Zero-Config First Vault** | First pattern drawn automatically creates the first vault (encrypted index + master key) |
| **No "Create New" Button** | The act of drawing a pattern IS vault creation; empty vaults show "Add Files" immediately |
| **Internal Storage Only** | All files in `context.filesDir` (app-private) — no external storage, no user-visible directories |
| **Randomized Naming** | Index files: `vault_index_<32-char-locator>.bin`; Arena files: `arena_<random>.dat` — no vault association |

---

## Status Dashboard

| Area | Website Claim | Current Code Status | Gap |
|------|--------------|---------------------|-----|
| No vault list UI | App launches to pattern grid, not vault catalog | **Not implemented** — no unlock screen exists in the codebase | Full implementation needed |
| **Android-Native UX: Direct Pattern Launch** | App opens to pattern grid (iOS) | **Not implemented** — no MainActivity/UnlockScreen | New: Compose pattern grid as entry point |
| **Zero-Config Onboarding** | First pattern = first vault | **Not implemented** | New: `openOrCreateVault` in `VaultIndexManager` |
| Pattern → vault key derivation | PBKDF2-HMAC-SHA512, 600K rounds, 256-bit output | ✅ `KeyDerivationEngine` matches spec | None |
| Key-derived locator naming | Locator derived from vault key names the index file | **Not implemented** — `VaultManager` uses UUID `vaultId` + `.vlt` suffix | Major gap: files are named by UUID, not by key-derived locator |
| Encrypted vault index | AES-256-GCM encrypted index per vault | ⚠️ Partial — `VaultHeader` exists but stores plaintext `vaultId` and `isDuressDecoy` flag | Index is not fully encrypted; metadata leaks |
| Random file names | Storage files have random names, no vault association | ⚠️ Unknown — storage engine writes to caller-specified `File` | Need to verify naming strategy in storage layer |
| Root file pooling | Padded pool of 64 slots with random cover files | **Not implemented** — `vaultsBaseDir` is a plain directory | Major gap: no pool, no cover files, no band padding |
| Plausible deniability integration | Different patterns → different vaults, no cross-vault list | ⚠️ Partial — `isDuressDecoy` flag exists on `VaultHeader` | No pattern-addressed routing; duress handling is flag-based, not key-derived |
| Duress mode purge | Purges non-decoy vaults | ✅ `purgeNonDecoyVaultFiles()` exists | Needs integration with pattern resolution flow |
| Honest forensic limits | Admits examiner can estimate vault count | ⚠️ Not documented in code | Needs documentation and UX guardrails |

---

## Quick Navigation

| Document | Audience | Purpose |
|----------|----------|---------|
| [`planning.md`](./planning.md) | Product Owner | User stories, acceptance criteria, **Zero-Config Onboarding**, UX flows, honest limitations |
| [`implementation.md`](./implementation.md) | Tech Lead / Architect | Architecture diagram, **Android-Native UX** (direct pattern launch), storage layout, crypto integration, file naming |
| [`tests.md`](./tests.md) | QA / Test Lead | Unit, UI, security, and integration test specifications |

---

## Key Website Claims (Verified)

1. ✅ "Vaultaire does not open to a catalog of named vaults" — **true on iOS; not yet built on Android**
2. ✅ "App resolves only the encrypted vault index associated with that derived key" — **design confirmed; not implemented**
3. ✅ "No cross-vault list in the unlock interface" — **design confirmed; not implemented**
4. ✅ "Encrypted index contents remain private" — **partially; current `VaultHeader` leaks plaintext metadata**
5. ✅ "File names on disk do not show which vault a file belongs to" — **unverified; needs audit of storage naming**
6. ✅ "Root files sit in a padded pool" — **not implemented; current code uses a flat directory**
7. ✅ "An examiner can still estimate how many vaults hold files" — **website is honest about this; our docs must be too**

---

## References

- [Hidden Vaults feature page](https://vaultaire.app/features/hidden-vaults/)
- [Plausible Deniability feature page](https://vaultaire.app/features/plausible-deniability/)
- [Creating a Secret Folder on iPhone guide](https://vaultaire.app/guides/secret-folder-iphone/)
- [NIST SP 800-132: Password-Based Key Derivation](https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-132.pdf)
- [NIST SP 800-38D: GCM Mode](https://csrc.nist.gov/pubs/sp/800-38/d/final)
- [PEARL: Plausibly Deniable Flash Translation Layer (USENIX Security '21)](https://www.usenix.org/conference/usenixsecurity21/presentation/chen-chen)
- [Shufflecake: Plausible Deniability for Multiple Hidden Filesystems](https://eprint.iacr.org/2020/1071)