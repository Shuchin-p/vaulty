# Duress Mode — Product Owner Planning

## UX Design Principles: Android
- **Effortless**: Real security should feel effortless. No setup wizard.
- **Direct Launch**: First launch -> pattern screen. No RecyclerView of vaults, no names, no counts.
- **Silent**: No confirmation, no warning sound, no progress bar, no "wiped" notification.
- **Immediate**: Triggered on pattern entry.

## User Flows

### Triggering Duress (Android)
1. **Launch**: User opens app. `MainActivity` shows `PatternLockScreen` (no vault list).
2. **Input**: User draws pattern.
3. **Check**: `AttemptThrottler` checks state. `DuressConfigManager` (via constant-time check) verifies against SHA-256 pattern hash.
4. **Trigger**: If duress pattern:
   - `DuressWipeProtocol` starts async multi-pass wipe of non-decoy files in `context.filesDir`.
   - `SyncEngine` is paused/isolated.
   - Decoy vault opens.

## Acceptance Criteria
- AC-01: App launches directly to pattern screen, no vault list ever displayed.
- AC-02: Duress pattern checked before vault index lookup.
- AC-03: Silent, multi-pass wipe of internal storage `.dat` files.
- AC-04: Sync engine (Google Drive) paused during/after trigger.
- AC-05: `AttemptThrottler` persists through app restarts and config changes.
