# Duress Mode — Planning Package

**Feature**: Duress Mode  
**Source**: https://vaultaire.app/features/duress-mode/  
**Related**: https://vaultaire.app/features/plausible-deniability/, https://vaultaire.app/features/hidden-vaults/  
**Platform**: Android (Kotlin 2.x, Jetpack Compose + Material 3)  
**Status**: Planning — major implementation required  
**Pro Feature**: Yes (requires `Entitlement.DURESS_MODE`)

---

## What This Feature Is

Duress Mode is a **coercion-resistant local wipe** that opens one designated "decoy" vault while silently destroying local access to all other vaults on the device. It is the last line of defense when a user is physically forced to unlock their phone.

### Core Behavior (Android Implementation)
1. **Direct Launch**: App launches directly to pattern lock screen. No setup wizard, no vault list UI.
2. **Designation**: User designates exactly one vault as the duress decoy.
3. **Trigger**: If the duress pattern is drawn, the app:
   - Opens the designated vault silently.
   - **Immediately wipes** all other vault indexes and their `.dat` (arena/page) files in internal storage using multi-pass overwrites.
4. **Security**: Duress pattern is verified BEFORE any vault discovery.
5. **Sync Isolation**: Google Drive sync is immediately paused/isolated so deletions are not propagated.
6. **Recovery**: Only the decoy vault is accessible; others require encrypted backup restoration + recovery phrase.

---

## Quick Navigation

| Document | Audience | Purpose |
|----------|----------|---------|
| [`planning.md`](./planning.md) | Product Owner | UX principles (direct launch), user stories, threat model |
| [`implementation.md`](./implementation.md) | Tech Lead / Architect | Architecture, `.dat` file overwrite, AttemptThrottler lifecycle, sync isolation |
| [`tests.md`](./tests.md) | QA / Test Lead | Test specifications for Android lifecycle, storage, and security |

---

## Android-Specific Requirements
- **No Setup Wizard**: First launch goes straight to pattern creation/unlock.
- **Internal Storage**: All vault data (indexes, `.dat` files) lives in `context.filesDir`.
- **Silent Wipe**: Multi-pass secure overwrite (DoD 5220.22-M) of `.dat` files.
- **AttemptThrottler**: Must be persistent (using DataStore/EncryptedSharedPreferences) to survive Android app lifecycle (restarts, configuration changes).
- **Sync**: Must pause `WorkManager` or custom sync jobs during wipe.
