# Entry / Splash — Test Cases
## Scope — screens.md §1 "Entry / Splash" + flows/01-access-and-recovery.md §1.1 + flows/00-state-model.md §3.1–§3.3, §6 + pattern-encryption/planning.md (FLAG_SECURE, attempt counter) + security-architecture/planning.md (disguise alias, zero-knowledge, no account)
## Preconditions
- Cold start: process not running, no Activity stack
- Vault exists: at least one `.vlt` index present
- No vault: zero `.vlt` indexes on device
- Disguise icon active: launcher alias ≠ default (Pro feature)
- Restore-from-backup pending: `pendingDestination {kind=restore, opaqueId}` persisted
- Warm start: process alive, Activity recreated (config change or G2)
- G4 relaunch: process death anywhere in Access & Recovery family
- Throttle tier > 0: attempt counter persisted from prior session
- Accessibility: TalkBack on, display size 200%, RTL locale, reduced motion
- Form factors: phone (V1), foldable book (V3 tabletop), tablet (V4), desktop (V5), TV (V6)

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T01-01 | Cold start, vault exists, default alias | functional | P0 | 1. Kill process. 2. Tap default launcher icon. | S0 (orbit lines only) → S1 (canvas + "Vaultaire" wordmark) within 300 ms → `/lock` with `reason=first`; no spinner, no "Unlocking…" copy |
| T01-02 | Cold start, no vault, default alias | functional | P0 | 1. Kill process on device with zero vaults. 2. Tap default launcher icon. | S0 → S1 (wordmark shown) → `/welcome`; no "No vault found" copy |
| T01-03 | Cold start, vault exists, disguise alias active | functional | P0 | 1. Enable disguise alias (Calculator). 2. Kill process. 3. Tap disguise icon. | S0 → **S1 disguise** (canvas only, **no wordmark, no glyph**) → `/lock` with `reason=first`; branding never visible |
| T01-04 | Cold start, restore intent pending | functional | P0 | 1. Kill process during restore (G4). 2. Relaunch. | S0 → S1 → `/recovery/enter` with `mode=restore`; pendingDestination consumed |
| T01-05 | Warm start (config change) | functional | P0 | 1. App in foreground on `/`. 2. Rotate device. | Splash **skipped entirely** (G2); previous route preserved; no branding flash |
| T01-06 | Notification tap → vault exists | functional | P0 | 1. Kill process. 2. Tap Backup notification. | S0 → S1 → `/lock` with `pendingDestination={kind=settings,opaqueId=backup}`; after unlock → Settings backup row |
| T01-07 | App link deep link → vault exists | functional | P0 | 1. Kill process. 2. Open `vaultaire://share/xyz`. | S0 → S1 → `/lock` with `pendingDestination={kind=share,opaqueId=xyz}`; after unlock → share destination |
| T01-08 | Widget tap → vault exists | functional | P0 | 1. Kill process. 2. Tap 1×1 launch widget. | S0 → S1 → `/lock` with `reason=first` |
| T01-09 | Default alias, 200% display size | adaptive | P0 | 1. Set display size 200%. 2. Cold start, vault exists. | S1 wordmark scales with `display.l`; layout holds, no truncation; route to `/lock` |
| T01-10 | Disguise alias, 200% display size | adaptive | P0 | 1. Set display size 200%, disguise active. 2. Cold start. | S1 disguise canvas scales; **no wordmark ever**; route to `/lock` |
| T01-11 | Foldable book, tabletop mode | adaptive | P0 | 1. Foldable in tabletop. 2. Cold start, vault exists. | Canvas centered in bottom half; no hinge crossing; route to `/lock` |
| T01-12 | Tablet (V4) centered layout | adaptive | P0 | 1. Tablet. 2. Cold start, vault exists. | Identical centered layout; 300 ms budget met |
| T01-13 | Desktop (V5) windowed | adaptive | P0 | 1. Desktop windowed. 2. Cold start. | Canvas fills window; wordmark centered; route to `/lock` |
| T01-14 | TV (V6) 10-foot UI | adaptive | P0 | 1. TV. 2. Cold start. | Canvas full-screen; wordmark readable at distance; route to `/lock` |
| T01-15 | TalkBack on, cold start, vault exists | accessibility | P0 | 1. Enable TalkBack. 2. Cold start, default alias. | Announcement: "Vaultaire" (wordmark only); no orbit description; focus not trapped |
| T01-16 | TalkBack on, disguise alias | accessibility | P0 | 1. TalkBack on, disguise active. 2. Cold start. | **No announcement** (canvas only, no wordmark); app backgrounds silently on Back |
| T01-17 | RTL locale, cold start | accessibility | P0 | 1. Set RTL locale (ar/he). 2. Cold start. | Canvas mirrors; wordmark centered; route unchanged |
| T01-18 | Reduced motion, cold start | accessibility | P0 | 1. Enable reduced motion. 2. Cold start. | Orbit animation disabled or static; 300 ms budget still met; route to `/lock` |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T01-19 | Probe exceeds 300 ms | performance | P0 | 1. Inject I/O delay on vault-index probe (>300 ms). 2. Cold start. | S1 canvas **held**, no spinner, no "Unlocking…"; transition when probe returns |
| T01-20 | Probe returns multiple vault indexes | functional | P1 | 1. Seed 3 `.vlt` files. 2. Cold start. | Probe reads **one opaque existence bit only**; route to `/lock`; no vault count, no picker |
| T01-21 | Corrupted vault index file | error | P1 | 1. Corrupt one `.vlt` header. 2. Cold start. | Probe treats as "vault exists" (opaque bit set); `/lock` opens; GCM failure handled at unlock |
| T01-22 | Storage full (D3) during probe | error | P1 | 1. Fill storage to D3. 2. Cold start. | Probe completes; route to `/lock` or `/welcome`; no crash; storage banner appears post-unlock |
| T01-23 | Network unavailable, notification tap | error | P1 | 1. Airplane mode. 2. Kill process. 3. Tap Sync notification. | S0 → S1 → `/lock` with `pendingDestination`; offline handled at destination, not splash |
| T01-24 | Invalid deep link opaqueId | error | P1 | 1. Kill process. 2. Open `vaultaire://invalid/xyz`. | S0 → S1 → `/lock`; after unlock, unresolved opaqueId → land on `/vault` silently |
| T01-25 | Process death mid-splash (G4) | lifecycle | P0 | 1. Cold start. 2. Kill at S0 (orbit only). 3. Relaunch. | Identical cold start; **no second branding flash**; no partial state persisted |
| T01-26 | Process death at S1 (wordmark shown) | lifecycle | P0 | 1. Cold start. 2. Kill at S1. 3. Relaunch. | Identical cold start; no wordmark "stuck" on screen |
| T01-27 | Back press on disguise alias | functional | P0 | 1. Disguise alias active. 2. Cold start. 3. Press Back at S1 disguise. | App backgrounds; **nothing rendered twice**; no branding ever shown |
| T01-28 | Back press on default alias | functional | P1 | 1. Default alias. 2. Cold start. 3. Press Back at S1. | App backgrounds; recents shows solid cover |
| T01-29 | Home press during splash | lifecycle | P0 | 1. Cold start. 2. Press Home at S0. 3. Reopen from recents. | Recents shows solid brand cover; reopen resumes or restarts cold (G2/G4) |
| T01-30 | Recents screenshot shows no content | security | P0 | 1. Cold start, vault exists. 2. Open recents at S1. | Recents shows **solid cover**, no wordmark, no orbit, no vault hint |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T01-31 | Disguise alias: zero branding in recents | security | P0 | 1. Disguise active. 2. Cold start. 3. Open recents at S1 disguise. | Recents shows **solid cover only**; no "Vaultaire", no orbit, no disguise icon hint |
| T01-32 | Disguise alias: zero branding in screenshot | security | P0 | 1. Disguise active. 2. Cold start. 3. Take screenshot at S1 disguise. | Screenshot shows **canvas only** (FLAG_SECURE on splash when disguise active) |
| T01-33 | No vault count leaked in any state | security | P0 | 1. Seed 5 vaults. 2. Cold start. 3. Inspect logs, UI, recents. | **Never** shows vault count, vault label, or "No vault found" copy |
| T01-34 | No progress bar or spinner ever | security | P0 | 1. Cold start with slow probe. 2. Observe entire splash. | **No spinner, no progress bar, no "Unlocking…", no logo animation**; canvas held |
| T01-35 | Timing side-channel: probe duration | security | P1 | 1. Measure splash→lock time with 0 vaults vs 10 vaults. | Duration **independent of vault count** (probe reads one bit) |
| T01-36 | Notification tap + disguise: solid cover | security | P0 | 1. Disguise active. 2. Kill. 3. Tap Security notification. | S1 disguise (canvas only) → `/lock`; notification text never names vault content |
| T01-37 | Race: notification during duress wipe resume | security | P0 | 1. Trigger duress wipe (G6). 2. Kill mid-wipe. 3. Tap notification on relaunch. | Wipe **completes first** (G6 precedence); notification destination dropped silently |
| T01-38 | FLAG_SECURE on splash when disguise | security | P0 | 1. Disguise active. 2. Cold start. 3. Verify FLAG_SECURE set before first frame. | FLAG_SECURE applied; recents/screenshot/recording blocked |
| T01-39 | No analytics/event logged during splash | security | P1 | 1. Cold start. 2. Check PostHog/network. | **Zero** analytics events, no crash diagnostics, no install referrer during splash |
| T01-40 | No account/email prompt ever | security | P0 | 1. Cold start, no vault. 2. Observe `/welcome`. | `/welcome` shows **no sign in, no account, no email, no marketing carousel** |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T01-41 | TalkBack: wordmark announced once | accessibility | P0 | 1. TalkBack on, default alias. 2. Cold start. | "Vaultaire" announced once at S1; orbit not described |
| T01-42 | TalkBack: no focus trap | accessibility | P0 | 1. TalkBack on. 2. Cold start. 3. Swipe focus. | Focus not trapped; Back exits to launcher |
| T01-43 | Display size 200%: wordmark scales | adaptive | P0 | 1. Display 200%. 2. Cold start, default alias. | Wordmark scales with `display.l`; no truncation, no overflow |
| T01-44 | Display size 200%: disguise unchanged | adaptive | P0 | 1. Display 200%, disguise active. 2. Cold start. | Canvas scales; **no wordmark introduced** |
| T01-45 | Foldable V3: grid never crosses hinge | adaptive | P0 | 1. Foldable book, tabletop. 2. Cold start. | Canvas in bottom half; orbit centered away from hinge |
| T01-46 | Tablet V4: identical centered layout | adaptive | P0 | 1. Tablet. 2. Cold start. | Same centered canvas + wordmark; 300 ms budget |
| T01-47 | RTL: canvas mirrors, wordmark centered | adaptive | P0 | 1. RTL locale. 2. Cold start. | Orbit mirrors; wordmark remains centered |
| T01-48 | Reduced motion: orbit static | accessibility | P0 | 1. Reduced motion on. 2. Cold start. | Orbit animation disabled/static; S0→S1 transition immediate |
| T01-49 | High contrast: colors meet 4.5:1 | accessibility | P0 | 1. High contrast theme. 2. Cold start. | Canvas orbit lines and wordmark meet 4.5:1 contrast |
| T01-50 | Switch access: no interactive elements | accessibility | P0 | 1. Switch access on. 2. Cold start. | No actionable elements; auto-advances; Back exits |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T01-51 | Warm start (G2) skips splash | lifecycle | P0 | 1. App in foreground. 2. Recreate Activity (rotate). | Splash **skipped entirely**; route preserved; no branding flash |
| T01-52 | G4 relaunch with pendingDestination | lifecycle | P0 | 1. Kill at `/lock` with `pendingDestination`. 2. Relaunch. | Cold start → `/lock` with same `pendingDestination`; no data loss |
| T01-53 | G4 relaunch during restore (F6) | lifecycle | P0 | 1. Kill at `/restore/progress`. 2. Relaunch. | WorkManager resumes; UI reattaches to `/restore/progress` with real counts |
| T01-54 | Screen off during splash | lifecycle | P0 | 1. Cold start. 2. Screen off at S0. 3. Screen on. | App locks immediately (screen-off always locks); relaunch shows `/lock` |
| T01-55 | Auto-lock threshold = 0 (immediate) | lifecycle | P0 | 1. Set auto-lock "immediately". 2. Cold start → Home → reopen. | Returns to `/lock` with `reason=timeout` |
| T01-56 | Manual lock during splash (impossible) | lifecycle | P1 | 1. Attempt to trigger manual lock at S1. | **No effect**; splash is inert, no lock affordance |
| T01-57 | Biometric invalidation during splash | lifecycle | P1 | 1. Change biometric enrollment. 2. Cold start. | No biometric offer on splash; `/lock` shows `reason=biometric-invalidated` |
| T01-58 | Integrity check failed (S12) on cold start | lifecycle | P1 | 1. Tamper detection triggers. 2. Cold start. | `/lock` with `reason=integrity`; neutral explanation sheet after unlock |
| T01-59 | Throttle tier persisted across death | lifecycle | P0 | 1. Reach tier 2 wait. 2. Kill process. 3. Relaunch. | Wait **resumes from persisted deadline**; tier survives restart/force-stop/reboot |
| T01-60 | Clock rollback re-arms wait | lifecycle | P1 | 1. Tier 1 wait active. 2. Roll back wall clock. 3. Relaunch. | Full current-tier wait **re-armed**; monotonic floor prevents bypass |

## Security invariants to assert
- [ ] **No branding under disguise**: S1 disguise shows canvas only — no "Vaultaire" wordmark, no glyph, no color accent
- [ ] **No progress bar / spinner**: Never at any splash state; probe delay holds canvas
- [ ] **Recents snapshot**: Solid cover (brand or disguise) — no content, no wordmark, no orbit
- [ ] **Timing bounds**: Launcher tap → first frame ≤ 300 ms; cold start → `/lock` interactive ≤ 800 ms
- [ ] **Single opaque probe bit**: Splash learns only "vault exists yes/no"; never count, labels, or names
- [ ] **No "No vault found" copy**: `/welcome` reached silently; no negative confirmation
- [ ] **FLAG_SECURE** on splash when disguise alias active (also on default for consistency)
- [ ] **Zero analytics** during splash; no network, no PostHog, no crash diags
- [ ] **Process death idempotent**: Relaunch identical to cold; no second branding flash
- [ ] **PendingDestination** only `{kind, opaqueId}`; never file name, collection, vault label
- [ ] **Duress wipe precedence (G6)**: Completes before any destination resolves

## Gaps & contradictions
- **screens.md §1** says "canvas + orbit lines only" at S0, then "canvas + Vaultaire wordmark" at S1. **flows/01 §1.1** defines S0 as "First composition: Canvas + orbit lines only" and S1 as "Alias + index probe: Canvas + `Vaultaire` wordmark". **Contradiction**: screens.md implies wordmark appears after probe; flows.md implies wordmark appears at S1 (during probe). **Resolution**: flows.md is canonical — wordmark at S1 (probe state).
- **screens.md §1** line 47: "State: cold, warm (skip splash), restore-from-backup (straight to phrase entry)." **flows/01 §1.1** Exit points table shows `/recovery/enter` for restore intent, not phrase entry directly. **Gap**: "straight to phrase entry" vs `/recovery/enter` with `mode=restore`. **Resolution**: flows.md is canonical — routes to `/recovery/enter`.
- **screens.md §1** line 45: "If a disguise icon is active, show no Vaultaire branding — just the canvas." **flows/01 §1.1** S1 disguise: "Canvas only — no wordmark, no glyph". **Consistent**.
- **screens.md §1** line 46: "Never show 'Unlocking…' or a progress bar". **flows/01 §1.1** Edge cases: "Never shows 'Unlocking…', a progress bar, or a logo animation". **Consistent**.
- **pattern-encryption/planning.md** Error Handling Matrix shows "GCM tag mismatch → 'Incorrect pattern'". **screens.md §3.1** explicitly forbids this: "No shake, no red, no 'incorrect pattern', no failed-attempt counter on unlock." **Contradiction resolved**: screens.md §3.1 is canonical for Aurora (deniability-safe behavior).
- **security-architecture/planning.md** line 80: "App Disguise — Pro feature". **flows/01 §1.1** Entry points includes disguise alias. **Consistent** — disguise is Pro-gated but tested here as a state.
- **00-state-model.md §3.1** Timing: "Launcher tap → first frame ≤ 300 ms". **screens.md §1** "300ms maximum". **Consistent**.
- **00-state-model.md §6** Process death contract: "Interrupted during `/` splash → identical cold start; Must not happen: a second branding flash". **flows/01 §1.1** Edge cases: "Process death mid-splash is invisible: S1 has no side effects, relaunch is identical". **Consistent**.
- **Gap**: No explicit test for **Auto (Android Automotive)** form factor in plan docs. **Deferred**: not in current screen spec.
- **Gap**: **X2 battery saver** forces Solid mode per flows/01 §1.7. Splash not listed but should inherit. **Added**: T01-10 covers disguise at 200%; battery saver behavior untested here.