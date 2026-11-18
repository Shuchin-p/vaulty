# Restore and Import — Test Cases
## Scope
screens.md §27 (27.1 Restore entry, 27.2 Restore progress, 27.3 Gallery import); flows 01-access-and-recovery (F6, F10), 02-vault-core (§2.6 import), 05-cloud-and-sharing (restore entry, backup key mismatch); feature docs: encrypted-cloud-backup (Story 3, Drive AppData, WorkManager resume), ease-of-use (US-08, US-09), secret-phrase (Flow 4, device-bound salt).

## Preconditions
- New device with no local vault: `/welcome` shows two paths without confirming/denying vault existence
- Device with existing vault: `/welcome` unreachable; Settings → Restore requires pattern re-auth
- Recovery phrase derives backup key; Google Drive discovered only after key derivation
- Import is encrypt-in-place from picker stream; no plaintext temp copy ever written
- "Delete originals from Photos" toggle **off by default**; enabled → deletion in system gallery after successful import only; cannot be undone by Vaultaire
- Duplicate detection by content hash on encrypted side; per-file resolution `Skip` / `Add anyway`, default **Skip**
- Restore progress shows real counts (`mono.m` "42 of 128"); no file names, no vault labels, no thumbnails
- Cancel restore confirmation copy: "Stop restoring? Downloaded data stays encrypted and can resume later."
- Failure reasons in plain language only; never expose record IDs, sizes beyond total, or crypto terms
- No import progress in notifications; in-app only
- WorkManager resumes restore/import after G4 process death; UI reconstructed from durable record
- Pattern-only restore works only on originating device (device-bound salt); new device requires phrase

## Test cases

### 27.1 Restore entry — `/welcome`

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T27-01 | New device shows welcome with two paths | Happy path | P0 | 1. Fresh install, launch app | Screen shows orbit art, "Your vault, here." / "Restore it with the phrase you saved." / `[ Restore with phrase ]` (ink pill) / `Start fresh` (ghost) |
| T27-02 | "Start fresh" goes to pattern creation | Happy path | P0 | 1. Tap `Start fresh` | Navigates to `/onboarding/pattern` (no account, no email, no "sign in") |
| T27-03 | "Restore with phrase" opens recovery entry | Happy path | P0 | 1. Tap `Restore with phrase` | Navigates to `/recovery/enter` in restore mode |
| T27-04 | Screen never says "No vault found" | Security & deniability | P0 | 1. Inspect all copy on `/welcome` | No text confirms or denies local vault existence |
| T27-05 | Existing device: `/welcome` unreachable directly | Security & deniability | P1 | 1. Device with vault → kill app → launch | Opens `/lock` (or `/vault` if session valid), never `/welcome` |
| T27-06 | Existing device: Settings → Restore requires re-auth | Security & deniability | P0 | 1. `/settings/cloud/backup` → `Restore on a new device` | Prompts pattern/biometric re-auth before `/recovery/enter` (restore) |
| T27-07 | Phrase entry derives backup key before Drive discovery | Security & deniability | P0 | 1. `/recovery/enter` (restore) → enter valid phrase | Backup key derived → Google Drive AppData queried; no Drive call before phrase |
| T27-08 | Invalid phrase: generic error, last field clears | Edge & error | P0 | 1. Enter wrong phrase → submit | Shows "That phrase didn't work."; last field clears; no vault/Drive disclosure |
| T27-09 | 3 wrong phrases → 30 s throttle on recovery counter | Edge & error | P1 | 1. Submit 3 wrong phrases | Tier 1 wait: "Please wait a moment." + `mono.m` countdown; `Forgot pattern?` reachable |
| T27-10 | Phrase buffer memory-only; cleared on back | Security & deniability | P1 | 1. Enter partial phrase → back to `/welcome` → re-enter `/recovery/enter` | Fields empty; no phrase persists in memory |
| T27-11 | Accessibility: TalkBack reads both actions | Accessibility & adaptive | P1 | 1. Enable TalkBack → navigate `/welcome` | "Restore with phrase, button" and "Start fresh, button" announced |
| T27-12 | Large text: copy reflows, buttons stay reachable | Accessibility & adaptive | P1 | 1. Font scale 200% → `/welcome` | No truncation; orbit art scales; both actions tappable |
| T27-13 | Tablet/desktop: centered layout, two-column if wide | Accessibility & adaptive | P2 | 1. Run on tablet/desktop | Layout adapts per Grid/FlexBox; orbit art centered; actions in column |
| T27-14 | No-account recovery: phrase entry identical UX | Happy path | P0 | 1. No vault, no Drive backup → `/recovery/enter` (restore) → valid phrase | "Vault restored." → new pattern → `/vault` (same as F6 flow) |

### 27.2 Restore progress — `/restore/progress`

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T27-15 | Restore starts after valid phrase, shows iris ring + real counts | Happy path | P0 | 1. Valid phrase on new device | Iris ring animating; `mono.m` "42 of 128" (real downloaded/decrypted count) |
| T27-16 | No file names, vault labels, or thumbnails on progress | Security & deniability | P0 | 1. Observe `/restore/progress` during restore | Only iris ring and count; no item names, labels, or thumbnails |
| T27-17 | Cancel shows confirmation with exact copy | Happy path | P0 | 1. Tap back/cancel mid-restore | Dialog: "Stop restoring? Downloaded data stays encrypted and can resume later." |
| T27-18 | Cancel confirm → returns to `/welcome`, state resumable | Happy path | P1 | 1. Confirm cancel → re-enter phrase | Restore resumes from last byte; WorkManager continues in background |
| T27-19 | Success: "Vault restored." + `Open vault`, no personalization | Happy path | P0 | 1. Wait for restore completion | Shows "Vault restored." + `Open vault` (ink); no confetti, no "welcome back" |
| T27-20 | `Open vault` → new pattern creation → `/vault` | Happy path | P0 | 1. Tap `Open vault` | `/onboarding/pattern` (2-step, no phrase step) → `/vault` |
| T27-21 | Failure: "Not enough space" + `Retry` | Edge & error | P0 | 1. Fill device storage → start restore | "Not enough space" + `Retry`; no sizes beyond total; no record IDs |
| T27-22 | Failure: "Connection lost" + `Retry` | Edge & error | P0 | 1. Disable network mid-restore | "Connection lost" + `Retry`; resumable from last chunk |
| T27-23 | Failure: "Backup key doesn't match" + `Retry` + `Why?` | Edge & error | P0 | 1. Restore with phrase from different vault | "Backup key doesn't match" + `Retry`; `Why?` opens plain-language sheet |
| T27-24 | Backup key mismatch never deletes/overwrites cloud records | Security & deniability | P1 | 1. After T27-23, check Drive AppData | Existing backup records intact; no deletion attempted |
| T27-25 | G4 process death: relaunch → `/lock` → resume from WorkManager | Races & lifecycle | P1 | 1. Kill app mid-restore → relaunch | `/lock` → unlock → `/restore/progress` reconstructed with real count |
| T27-26 | Battery saver/Doze: restore deferred, says so once | Races & lifecycle | P2 | 1. Enable battery saver mid-restore | "Backup is paused while battery saver is on." shown once; resumes when cleared |
| T27-27 | Low storage (≤500 MB): butter banner, continues with warning | Edge & error | P1 | 1. Restore with ≤500 MB free | "Storage is almost full." banner; restore continues |
| T27-28 | Full storage (≤50 MB): pauses, "Free up space" action | Edge & error | P0 | 1. Restore with ≤50 MB free | "This device is out of space. Free up space or connect a drive." + action |
| T27-29 | Offline: sky banner "Last backed up X days ago", `Retry` queued | Edge & error | P1 | 1. Restore offline → reconnect | Banner shows last success; `Retry` queued; auto-resumes on reconnect |
| T27-30 | Accessibility: progress announced as "Restoring, 42 of 128" | Accessibility & adaptive | P1 | 1. TalkBack on `/restore/progress` | "Restoring, 42 of 128 items" announced; count updates live |
| T27-31 | Large text: count `mono.m` scales, ring stays centered | Accessibility & adaptive | P1 | 1. Font scale 200% → `/restore/progress` | Count readable; iris ring centered; `Retry`/`Why?` buttons large |
| T27-32 | Pattern-only restore: originating device works, new device blocked | Security & deniability | P1 | 1. Same device: Settings → Restore (no phrase) | Works if device salt matches; new device requires phrase (device-bound salt) |

### 27.3 Gallery import — system picker → `/vault/import`

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T27-33 | Import entry from empty vault "Add photos" | Happy path | P0 | 1. `/vault` S4 → `Add photos` → pick 3 photos | `/vault/import` S0: "Import 3 items", 3-up thumbnails, "Encrypting into Vault" |
| T27-34 | Import entry from collection "Add items" | Happy path | P0 | 1. Open collection → `Add items` → pick 2 videos | `/vault/import` S0 with `collectionId`; imports into that collection |
| T27-35 | Encrypt-in-place: no plaintext temp copy ever written | Security & deniability | P0 | 1. Import 1 photo → inspect app storage | No plaintext file in cache/files; only encrypted `.vlt` entries created |
| T27-36 | "Delete originals" toggle off by default | Happy path | P0 | 1. Open `/vault/import` with items | Toggle shows `[ off ]`; "Why?" link present |
| T27-37 | Enable delete originals → shows explanation card | Happy path | P1 | 1. Toggle "Delete originals" on | Card explains: deletion in system gallery after success; cannot be undone by Vaultaire |
| T27-38 | Delete originals runs only after successful import | Security & deniability | P0 | 1. Toggle on → import 3 → 1 fails | First 2 originals deleted; failed item's original untouched |
| T27-39 | Delete originals off → originals remain in Photos as plaintext | Security & deniability | P0 | 1. Toggle off → import 3 → check Photos | All 3 originals still in system gallery |
| T27-40 | Duplicate detection: "3 items are already in this vault." | Happy path | P0 | 1. Import 5, 3 already in vault | S5: "3 items are already in this vault." + `Skip` / `Add anyway` per file |
| T27-41 | Duplicate default resolution is Skip | Happy path | P1 | 1. T27-40 → tap `Import` without changing | All 3 duplicates skipped; 2 new imported |
| T27-42 | Duplicate Add anyway → imports as separate entry | Happy path | P1 | 1. T27-40 → choose `Add anyway` on 1 dup | Duplicate imported as new item (different encrypted blob) |
| T27-43 | Duplicate Skip → not imported, count excluded | Happy path | P1 | 1. T27-40 → choose `Skip` on 1 dup | Item not imported; success count reflects only new items |
| T27-44 | Large batch (>200): S1 Checking with real progress, cancellable | Happy path | P1 | 1. Pick 500 items → `/vault/import` | "Checking 500 items…" with real progress; `Cancel` returns to S5/S15/S0 |
| T27-45 | Single item: singular copy "Import 1 item", no progress jitter | Happy path | P1 | 1. Pick 1 photo → `/vault/import` | S3: "Import 1 item"; progress animates 0→1 real |
| T27-46 | Zero items: S4 "Nothing selected." + `Choose photos` | Edge & error | P1 | 1. Picker returns 0 items | S4: "Nothing selected." + `Choose photos`; never an error |
| T27-47 | Import commits: S15 real progress "7 of 12", cancellable | Happy path | P0 | 1. Tap `Import 12` → observe S15 | Real count "7 of 12"; `Cancel` stops queueing, keeps imported |
| T27-48 | Cancel mid-import: keeps imported, shows count, no partial files | Happy path | P0 | 1. Import 12 → cancel at 7 | S17 snackbar: "Imported 7 items. Undo?" 7 s; 7 items in vault; no partial `.vlt` |
| T27-49 | Undo in snackbar → removes imported items | Happy path | P1 | 1. T27-48 → tap `Undo` within 7 s | 7 items removed; back to caller |
| T27-50 | Success: S16 "Imported 12 items." + `Done` → caller | Happy path | P0 | 1. Complete import of 12 | "Imported 12 items." + `Done`; tap → returns to `/vault` or collection |
| T27-51 | Partial failure: "Ran out of space at 42 of 500. The first 42 were saved." | Edge & error | P0 | 1. Import 500 with space for 42 | S6: exact copy + `Retry` (remaining only); first 42 saved |
| T27-52 | Retry after space freed → imports remaining only, no duplicates | Edge & error | P0 | 1. T27-51 → free space → `Retry` | Processes items 43–500 only; idempotency key prevents re-encryption |
| T27-53 | App backgrounded mid-import → WorkManager continues | Races & lifecycle | P0 | 1. Import 100 → home → wait → reopen | UI re-locked if threshold passed; progress re-shown from durable record |
| T27-54 | Auto-lock mid-import → cover shown, WorkManager continues | Races & lifecycle | P0 | 1. Import 50 → wait for auto-lock | `/lock` shown; unlock → `/vault/import` progress restored |
| T27-55 | G4 process death → WorkManager resumes, UI reconstructed | Races & lifecycle | P1 | 1. Import 50 → force stop → relaunch | `/lock` → unlock → `/vault/import` shows real progress; imported items present |
| T27-56 | No import progress in notifications | Security & deniability | P0 | 1. Import 20 → check notification shade | No import progress notification (could reveal activity) |
| T27-57 | Picker cancel → no screen change, nothing queued | Edge & error | P1 | 1. Open picker → cancel | Returns to caller unchanged; no import screen |
| T27-58 | Offline import (N2): no banner, import is local | Edge & error | P1 | 1. Airplane mode → import 5 | S8: no banner; import succeeds; backup queued separately |
| T27-59 | Duplicate hashing >300ms → S1 Checking with progress | Edge & error | P1 | 1. Import 100 with many duplicates | "Checking 100 items…" with real progress; cancellable |
| T27-60 | Accessibility: thumbnails have content descriptions | Accessibility & adaptive | P1 | 1. TalkBack on `/vault/import` | Each thumbnail: "Photo, imported [date]" or similar; no file names |
| T27-61 | Large text: toggle, progress, buttons scale, no truncation | Accessibility & adaptive | P1 | 1. Font scale 200% → `/vault/import` | All copy readable; 3-up grid adjusts; `Import N` button full-width |
| T27-62 | Tablet/desktop: two-pane or dialog sizing per Grid | Accessibility & adaptive | P2 | 1. Run on tablet/desktop | Layout adapts; picker integration works; progress centered |

## Security invariants to assert

- [ ] **Never says "No vault found"** on `/welcome` or `/recovery/enter` (restore)
- [ ] **No plaintext temp copy** on import; encrypt-in-place from picker stream only
- [ ] **No file names, vault labels, or thumbnails** on `/restore/progress`
- [ ] **Cancel restore confirmation copy exact**: "Stop restoring? Downloaded data stays encrypted and can resume later."
- [ ] **Failure reasons in plain language only**: "Not enough space", "Connection lost", "Backup key doesn't match" — never record IDs, sizes beyond total, or crypto terms
- [ ] **No import progress in notifications** (in-app only)
- [ ] **Delete originals off by default**; runs only after successful import; failed item's original never deleted; action cannot be undone by Vaultaire
- [ ] **Duplicate resolution per-file** (`Skip`/`Add anyway`), default **Skip**; choice not silently applied to all
- [ ] **WorkManager idempotency**: Retry processes remaining only; already-imported/restored items never re-encrypted or duplicated
- [ ] **G4 process death**: restore/import UI reconstructed from durable record; no partial files listed
- [ ] **Pattern-only restore** works only on originating device (device-bound salt); new device requires phrase
- [ ] **Backup key mismatch** never deletes or overwrites existing cloud records
- [ ] **Error tone**: plain, short, no blame, no "oops", no exclamation marks; never stack traces, file paths, or crypto terms

## Gaps & contradictions

- **Pattern-only restore UX**: `encrypted-cloud-backup` claims pattern restore works only on originating device; `screens.md §27.2` and `05-cloud-and-sharing §5.1` show no UI distinction between phrase and pattern restore on `/restore/progress`. Need decision: does restore entry state the difference?
- **Duplicate resolution scope**: `02-vault-core §2.6` specifies per-file `Skip`/`Add anyway` with running tally; `screens.md §27.3` says "Skip or add anyway" without scope. Confirm per-file vs. bulk and default (per-file Skip is implemented).
- **Restore progress cancel copy**: `screens.md §27.2` has exact copy; `05-cloud-and-sharing` doesn't repeat it. No contradiction but single source of truth needed.
- **Delete originals "Why?" link**: `screens.md §27.3` shows `[ off ] Why?`; `02-vault-core §2.6` says enabling shows explanation card. Consistent but card content not fully specified in screens.md.
- **Import progress announcement**: `02-vault-core §2.6` lists announcements per state; `screens.md §27.3` has no a11y copy. Need to ensure TalkBack reads "Importing 7 of 12" from S15 announcement.
- **Restore throttle on recovery entry**: `01-access-and-recovery` says recovery entry runs same ladder on its own counter armed at 3 wrong submissions; `screens.md §24` shows "That phrase didn't work." but no throttle copy. Throttle copy "Please wait a moment." applies per §24.
- **New pattern after restore**: `secret-phrase` Flow 4 shows new pattern creation post-restore; `01-access-and-recovery` F6 says "new pattern → `/vault`". `screens.md §27.2` says "new pattern" but no screen reference. Confirm `/onboarding/pattern` (2-step, no phrase) is used.