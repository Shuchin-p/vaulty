# Empty / Error / System States — Test Cases

## Scope — screens.md §24 + flows/07-system-states.md + flows/00-state-model.md + security/planning.md + plausible-deniability/planning.md

## Preconditions — bullets (which screens can host each state)
- Empty vault: `/vault` (no vaults), `/vault/collection/{id}` (empty collection), `/search` (no results)
- Unknown pattern: `/lock` (always possible)
- Rate limited: Network operations (backup, sync, share), camera capture
- Invalid pattern: Pattern creation screen only
- Recovery wrong: `/recovery/verify`
- Share expired: `/shared/{id}`
- Backup failed: `/settings/cloud/backup`
- Sync conflict: `/vault/timeline`
- Storage full: Camera, import, backup
- Permission denied: Camera, mic, notifications
- Unsupported file: `/vault/file/{id}`
- Decryption failure: Any media viewer
- Duress: All screens (must be identical to normal)

## Test cases

### Happy path

| ID | Title | State | Type | Priority | Steps | Expected |
|----|-------|-------|------|----------|-------|----------|
| T24-01 | Empty vault first open | Empty vault | Happy | P0 | 1. Fresh install<br>2. Open app | "Nothing here yet."<br>`Add photos`<br>`Take a photo` |
| T24-02 | Empty collection | Empty collection | Happy | P0 | 1. Create empty collection<br>2. Open it | "This collection is empty."<br>`Add items` |
| T24-03 | No search results | No search results | Happy | P1 | 1. Search for non-existent term | "No matches in this vault."<br>`Clear filters` |
| T24-04 | Valid pattern unlock | Unknown pattern | Happy | P0 | 1. Draw correct pattern | Normal vault UI, no error |
| T24-05 | Valid recovery phrase | Recovery wrong | Happy | P1 | 1. Enter correct phrase | Normal unlock flow |
| T24-06 | Active share opens | Share expired | Happy | P1 | 1. Open active share | Normal share viewer |
| T24-07 | Backup completes | Backup failed | Happy | P1 | 1. Successful backup | No banner, normal state |
| T24-08 | Sync resolves conflicts | Sync conflict | Happy | P1 | 1. Conflicts resolve automatically | Normal timeline, no banner |
| T24-09 | Storage available | Storage full | Happy | P1 | 1. Free up space<br>2. Retry action | Normal operation, no banner |
| T24-10 | Permission granted | Permission denied | Happy | P1 | 1. Grant permission | Normal operation, no banner |
| T24-11 | Supported file opens | Unsupported file | Happy | P1 | 1. Open supported media | Normal viewer |
| T24-12 | Decryption succeeds | Decryption failure | Happy | P1 | 1. Open valid encrypted file | Normal viewer |
| T24-13 | Duress pattern | Duress | Happy | P0 | 1. Draw duress pattern | Identical to normal unlock |

### Edge & error

| ID | Title | State | Type | Priority | Steps | Expected |
|----|-------|-------|------|----------|-------|----------|
| T24-14 | Unknown pattern | Unknown pattern | Edge | P0 | 1. Draw unused pattern | Same glow/transition as success<br>"Nothing here yet."<br>`Add photos`<br>`Take a photo` |
| T24-15 | Invalid pattern creation | Invalid new pattern | Edge | P0 | 1. Draw 5-dot straight line | "Connect at least 6 dots."<br>under grid |
| T24-16 | Invalid pattern no turn | Invalid new pattern | Edge | P0 | 1. Draw 6-dot straight line | "Add at least one turn."<br>under grid |
| T24-17 | Wrong recovery phrase | Recovery wrong | Edge | P0 | 1. Enter incorrect phrase | "That phrase didn't work."<br>field clears |
| T24-18 | Expired share | Share expired | Edge | P0 | 1. Open expired share | "This share is no longer available."<br>solid notice, no retry |
| T24-19 | Backup interrupted | Backup failed | Edge | P0 | 1. Cancel backup mid-transfer | "Backup didn't finish. Your files are safe on this device."<br>`Retry`<br>`Why` |
| T24-20 | Sync interruption | Sync conflict | Edge | P0 | 1. Sync interrupted mid-conflict | "3 items need your review."<br>banner → review list |
| T24-21 | Storage full during capture | Storage full | Edge | P0 | 1. Full storage<br>2. Open camera | "This device is out of space."<br>"Free up space to take photos."<br>shutter blocked |
| T24-22 | Storage full during import | Storage full | Edge | P0 | 1. Full storage<br>2. Open import | "This device is out of space."<br>"Free up space."<br>blocked |
| T24-23 | Camera denied once | Permission denied | Edge | P0 | 1. First camera denial | Rationale sheet: "Vaultaire needs camera access."<br>`Open settings`<br>`Not now` |
| T24-24 | Camera denied permanently | Permission denied | Edge | P0 | 1. Second camera denial | "Camera access is off."<br>"You can still import photos."<br>`Open settings` |
| T24-25 | Mic denied | Permission denied | Edge | P1 | 1. Mic denied in camera | "Video will record without sound."<butter pill |
| T24-26 | Unknown media format | Unsupported file | Edge | P0 | 1. Open unsupported file | "This file can't be previewed here."<br>icon<br>`Open with…` |
| T24-27 | Corrupt encrypted file | Decryption failure | Edge | P0 | 1. Open corrupt file | "We couldn't open this item."<br>"It may be damaged."<br>danger banner |
| T24-28 | Rate limited network | Rate limited | Edge | P0 | 1. Exceed API rate | "Please wait a moment."<br>mono.m countdown |

### Security & deniability

| ID | Title | State | Type | Priority | Steps | Expected |
|----|-------|-------|------|----------|-------|----------|
| T24-29 | Duress no visual difference | Duress | Security | P0 | 1. Draw duress pattern | Identical to normal success<br>no special styling |
| T24-30 | Duress doesn't expose crypto | Duress | Security | P0 | 1. Duress pattern<br>2. Check logs/UI | No stack traces, file paths, or crypto terms |
| T24-31 | Unknown pattern no red | Unknown pattern | Security | P0 | 1. Wrong pattern<br>2. Observe UI | No red, no shake, neutral empty state |
| T24-32 | Error tone neutral | All errors | Security | P0 | 1. Trigger any error | Plain, short, no "oops", no exclamation |
| T24-33 | No vault count leak | Empty vault | Security | P1 | 1. Multiple vaults<br>2. Check empty state | "Nothing here yet." doesn't imply others exist |
| T24-34 | No pattern validity leak | Unknown pattern | Security | P1 | 1. Wrong pattern<br>2. Check UI | Same glow/transition as correct pattern |
| T24-35 | Error never on unlock | All errors | Security | P0 | 1. Any error during unlock | No error styling on pattern grid |
| T24-36 | Duress identical success | Duress | Security | P0 | 1. Duress pattern<br>2. Compare to normal | Exactly identical UI, no decoy indicators |

### Accessibility & adaptive

| ID | Title | State | Type | Priority | Steps | Expected |
|----|-------|-------|------|----------|-------|----------|
| T24-37 | TalkBack empty state | Empty vault | Accessibility | P1 | 1. Enable TalkBack<br>2. Open empty vault | Grid becomes one node<br>explore-by-touch dots<br>state change announced |
| T24-38 | Switch access error | Rate limited | Accessibility | P1 | 1. Enable Switch Access<br>2. Trigger rate limit | Linear focus order<br>no drag-only gesture |
| T24-39 | Large font error | All errors | Accessibility | P1 | 1. Set 200% font<br>2. Trigger error | No truncation<br>buttons grow to 60dp |
| T24-40 | Reduced motion error | All errors | Accessibility | P1 | 1. Enable reduced motion<br>2. Trigger error | Ambient off<br>120ms fades<br>200ms border pulse |
| T24-41 | RTL empty state | Empty vault | Accessibility | P1 | 1. Set RTL<br>2. Open empty vault | Grid column mirrors<br>chevrons mirror<br>numbers stay LTR |
| T24-42 | Multi-window error | Rate limited | Accessibility | P1 | 1. Split screen<br>2. Trigger error | Graceful degradation<br>minimum 320×480dp |

### Races & lifecycle

| ID | Title | State | Type | Priority | Steps | Expected |
|----|-------|-------|------|----------|-------|----------|
| T24-43 | Process death mid-import | Storage full | Lifecycle | P1 | 1. Start import<br>2. Kill process<br>3. Relaunch | Import queue resumes<br>no partial files listed |
| T24-44 | Background during error | All errors | Lifecycle | P1 | 1. Trigger error<br>2. Send to background<br>3. Return | Error state preserved<br>no crash |
| T24-45 | Screen rotation during error | All errors | Lifecycle | P1 | 1. Trigger error<br>2. Rotate device | Stable scroll anchor<br>no jump to top |
| T24-46 | Network restore mid-error | Rate limited | Lifecycle | P1 | 1. Trigger rate limit<br>2. Restore network | Auto-retry<br>no manual action needed |
| T24-47 | Storage freed mid-error | Storage full | Lifecycle | P1 | 1. Trigger storage error<br>2. Free space<br>3. Return | Normal operation restored |
| T24-48 | Permission change mid-error | Permission denied | Lifecycle | P1 | 1. Trigger permission error<br>2. Grant permission in settings<br>3. Return | Normal operation restored |

## Security invariants to assert
- [ ] Unknown pattern has NO error state - same glow/transition as success
- [ ] Duress identical to success - no visual, haptic, or timing differences
- [ ] Error tone rules - plain, short, no "oops", no exclamation marks
- [ ] Never expose stack traces/paths/crypto terms in any error message
- [ ] No error styling on pattern unlock screen
- [ ] Empty states never imply vault count or existence of other vaults

## Gaps & contradictions
- screens.md §24 says "Unknown pattern: No error" but state model requires every screen to handle S9 (locked) - need to verify unlock flow doesn't show S9 for unknown patterns
- plausible-deniability.md says "empty vault" should not reveal other vaults exist, but empty state copy "Nothing here yet" might imply this is the only vault - need copy review
- system-states.md §1.1 says offline banner only on transport screens, but screens.md §24 has "No matches in this vault" for search - need to clarify if search counts as transport
- security-architecture.md says "no account required" but backup/sync needs Google Drive - need to clarify this is user's own account, not Vaultaire's
- duress mode should be identical to success but plausible-deniability.md says it opens decoy - need to verify UI is identical even if underlying data differs