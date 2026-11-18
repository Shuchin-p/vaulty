# Pattern Lock — Test Cases
## Scope — screens.md §3 + §3.1; flows 01-access-and-recovery.md (R1–R4, F2–F4, F7–F8); 00-state-model.md (session lifecycle, zeroization, persistence); pattern-encryption, hidden-vaults, plausible-deniability, duress-mode planning.md
## Preconditions
- Vault exists (local index present); device salt generated; attempt counter at tier 0
- Duress configured: decoy vault index persisted; wipe marker armed for next duress unlock
- Throttle state: encrypted attempt buckets + tier + deadline survive restart/force-stop/reboot
- Screen dims to 10% after 30 s inactivity on `/lock`; recents shows solid brand cover
- Process death at any point → relaunch to `/lock` with empty buffer; pending destination `{kind, opaqueId}` persisted

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T03-01 | Known pattern unlock — cold start | functional | P0 | 1. Launch app from default alias<br>2. Draw correct 6+ dot pattern with ≥1 turn<br>3. Release | 380 ms iris glow (±1 frame) → nav enters +120 ms → `/vault`; announcement "Vault unlocked" (assertive) |
| T03-02 | Known pattern unlock — warm start (activity recreated) | functional | P0 | 1. App in background, process alive<br>2. Return to app before auto-lock threshold<br>3. Draw correct pattern | Same resolve path and timing as cold start; no splash |
| T03-03 | Known pattern unlock — auto-lock timeout (default 30 s) | functional | P0 | 1. Unlock vault<br>2. Background app > 30 s<br>3. Return to app<br>4. Draw correct pattern | `/lock` shown with `reason=timeout`; pending destination = last route; unlock restores route |
| T03-04 | Known pattern unlock — screen off | functional | P0 | 1. Unlock vault<br>2. Turn screen off<br>3. Turn screen on<br>4. Draw correct pattern | `/lock` shown with `reason=screen-off`; unlock restores route |
| T03-05 | Known pattern unlock — manual lock | functional | P0 | 1. Unlock vault<br>2. Nav overflow → Lock<br>3. Draw correct pattern | `/lock` shown with `reason=manual`; unlock restores route |
| T03-06 | Biometric unlock — enrolled + enabled + not throttled | functional | P0 | 1. On `/lock`, tap "Unlock with biometrics" ghost row<br>2. System prompt succeeds | Identical resolve path, glow, timing class, and announcement "Vault unlocked" as pattern unlock |
| T03-07 | Biometric unlock — 1 retry then success | functional | P0 | 1. Tap biometric row<br>2. First prompt fails (sensor)<br>3. Second prompt succeeds | Same as T03-06; pattern unaffected |
| T03-08 | Biometric unlock — system lockout after 5 fails | functional | P0 | 1. Fail biometric 5 times<br>2. Observe `/lock` | Biometric row absent; no in-app copy; pattern unlock still works |
| T03-09 | Notification tap → pending destination after unlock | functional | P0 | 1. Tap Backup notification<br>2. Draw correct pattern | Unlock → `/settings/cloud/backup` row opens; destination dropped if unlock abandoned |
| T03-10 | Deep link → pending destination after unlock | functional | P0 | 1. Open `vaultaire://settings/security`<br>2. Draw correct pattern | Unlock → `/settings/security` opens; unresolved id → lands on `/vault` silently |
| T03-11 | Re-auth from settings → returns to origin | functional | P0 | 1. In `/settings/security`, tap "Recovery phrase"<br>2. Draw correct pattern on `/lock` (`reason=reauth`) | Returns to `/settings/security` → pushes `/recovery/display` |
| T03-12 | Restore completion → first-open pattern (no phrase step) | functional | P0 | 1. Restore succeeds<br>2. Tap "Open vault"<br>3. Draw new pattern + confirm | `/onboarding/pattern` (mode=post-restore) → confirm → `/vault`; phrase unchanged |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T03-13 | Release < 6 dots — ignored silently | edge | P0 | 1. Draw 5 dots<br>2. Release | No attempt counted; buffer clears; grid returns to Idle; no copy, no haptic beyond per-dot |
| T03-14 | Invalid pattern (no turn) on create — inline hint | edge | P0 | 1. On `/onboarding/pattern`, draw 6 dots straight line<br>2. Tap Continue | "Add at least one turn." under grid; 420 ms shake (creation only); no throttle |
| T03-15 | Unknown pattern — resolves to neutral empty vault | security | P0 | 1. On `/lock`, draw pattern not matching any vault<br>2. Release | **Same 380 ms iris glow and same navigation** into empty vault; empty state shows "Nothing here yet."; no shake, no red, no error, no counter |
| T03-16 | Unknown pattern — empty vault has no vault indicator | security | P0 | 1. After T03-15, observe `/vault` | Title "Vault" (generic); no vault count, label, ID, or "this vault doesn't exist" |
| T03-17 | Rate limiting — tier 1 (5th attempt in 2 min) | edge | P0 | 1. Submit 5 patterns (≥6 dots) within 2 min (any mix known/unknown/duress)<br>2. 6th attempt | Grid disabled; neutral ink cover "Please wait a moment." + `mono.m` countdown from 30 s; "Forgot pattern?" reachable; biometrics not |
| T03-18 | Rate limiting — tier 2 (attempt within 2 min after tier 1 ends) | edge | P0 | 1. Wait for tier 1 countdown to end<br>2. Submit 1 pattern immediately | Tier 2 armed: 2 m wait; grid stays disabled through wait |
| T03-19 | Rate limiting — tier 3 (attempt within 2 min after tier 2 ends) | edge | P0 | 1. Wait for tier 2 to end<br>2. Submit 1 pattern immediately | Tier 3 armed: 10 m wait |
| T03-20 | Rate limiting — tier 4 cap (attempt within 2 min after tier 3 ends) | edge | P0 | 1. Wait for tier 3 to end<br>2. Submit 1 pattern immediately | Tier 4 armed: 30 m wait; further violations re-arm 30 m |
| T03-21 | Rate limiting — decay (10 min no attempt) | edge | P1 | 1. Reach tier 2<br>2. Wait 10 min with no attempts | Tier drops to 1; deadline persists |
| T03-22 | Rate limiting — clock rollback re-arms wait | edge | P1 | 1. Tier 1 wait active (20 s left)<br>2. Set device clock back 5 min | Wait re-armed to full 30 s (wall-clock + monotonic floor) |
| T03-23 | Rate limiting — survives process death & reboot | edge | P0 | 1. Arm tier 1 (20 s left)<br>2. Force-stop app / reboot device<br>3. Relaunch | `/lock` shows countdown resumed from persisted deadline; attempt buckets intact |
| T03-24 | Forgot pattern? — clears buffer, opens recovery | functional | P0 | 1. On `/lock`, tap "Forgot pattern?" | Pushes `/recovery/enter` (mode=open); pattern buffer zeroized |
| T03-25 | Forgot pattern? — reachable during throttle wait | edge | P0 | 1. Arm tier 1 wait<br>2. Tap "Forgot pattern?" | Opens `/recovery/enter`; recovery uses own throttle ladder (armed at 3 wrong submissions) |
| T03-26 | Help icon — opens pattern safety sheet | functional | P0 | 1. On `/lock`, tap help icon | Sheet: shoulder surfing, screen protectors; never account recovery |
| T03-27 | Screen dim — 30 s inactivity on `/lock` | edge | P1 | 1. Stay on `/lock` 30 s without touch<br>2. Observe screen | Brightness 10%; content never shown; touch restores brightness |
| T03-28 | Recents cover — solid brand, no content | security | P0 | 1. On `/lock`, open recents | Solid brand cover; no grid, no pattern visible |
| T03-29 | Rotation during draw — buffer survives | edge | P1 | 1. Draw 4 dots<br>2. Rotate device | Buffer preserved; grid re-layout; no attempt triggered |
| T03-30 | Background during draw — buffer cleared | edge | P0 | 1. Draw 3 dots<br>2. Press Home | Buffer zeroized; solid recents cover; relaunch → `/lock` Idle |
| T03-31 | Background during KDF — derivation cancelled | edge | P0 | 1. Release pattern (KDF in flight)<br>2. Press Home | Derivation cancelled; buffers zeroized; relaunch → `/lock` Idle |
| T03-33 | Screen capture blocked — FLAG_SECURE on grid | security | P0 | 1. On `/lock`, attempt screenshot/screenrecord | Capture blocked; one neutral line if attempted: "Screen capture is blocked on this screen" |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T03-34 | Unknown pattern — identical glow & transition to known | security | P0 | 1. Draw unknown pattern<br>2. Draw known pattern<br>3. Compare frame-by-frame | Pixel-identical 380 ms iris glow (±1 frame); identical nav enter +120 ms; identical announcement |
| T03-35 | Unknown pattern — no shake, red, error, counter | security | P0 | 1. Draw unknown pattern<br>2. Observe | No shake animation; no red flash; no "incorrect" copy; no failed-attempt counter visible |
| T03-36 | Duress pattern — pixel-identical to normal unlock | security | P0 | 1. Draw duress pattern<br>2. Draw normal pattern<br>3. Compare | Identical grid feedback (iris line, EFFECT_CLICK/dot); identical 380 ms glow; identical nav timing; single EFFECT_HEAVY_CLICK; no extra haptic, no badge, no confirmation |
| T03-37 | Duress pattern — silent wipe before decoy interactive | security | P0 | 1. Draw duress pattern<br>2. Observe decoy vault | Other vault indexes removed first; their patterns resolve to neutral empty vaults during wipe; no progress, sound, notification; decoy opens |
| T03-38 | Duress pattern — wipe resumes after process death | security | P0 | 1. Draw duress pattern<br>2. Kill process mid-wipe<br>3. Relaunch | Wipe resumes before decoy becomes interactive; no half-wiped decoy visible |
| T03-39 | Duress pattern — partial wipe failure leaves residue only in Security Info | security | P1 | 1. Simulate storage error during wipe<br>2. Open `/settings/security-info` | Residue acknowledged only there; never surfaced on unlock (coercion tell) |
| T03-40 | Duress pattern checked before vault index lookup | security | P0 | 1. Trace execution on duress draw | DuressConfigManager constant-time check runs before vault index probe |
| T03-41 | Attempt counter stored encrypted, survives restart | security | P0 | 1. Submit 3 attempts<br>2. Force-stop & relaunch<br>3. Submit 2 more | Tier 1 armed on 5th total attempt; counter persisted in encrypted prefs |
| T03-42 | No pattern preview / show pattern toggle | security | P0 | 1. Inspect `/lock` UI | No "show pattern" control; no preview of drawn path after release |
| T03-43 | FLAG_SECURE on grid area only (not whole screen) | security | P0 | 1. Verify window flags | Grid area has FLAG_SECURE; top bar (avatar/help) may not |
| T03-44 | Biometric invalidation — next unlock requires pattern | security | P0 | 1. Enroll biometric, enable unlock<br>2. Change system biometric enrollment<br>3. Return to app | `/lock` shown with `reason=biometric-invalidated`; biometric row absent; pattern required; one-time dismissible sheet offers re-enrollment after unlock |
| T03-45 | Recovery throttle separate from lock throttle | security | P0 | 1. Fail recovery phrase 3 times<br>2. Observe lock throttle | Lock throttle unchanged; recovery uses identical ladder but independent counter |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T03-46 | TalkBack — grid announced as "Pattern unlock grid, 5 rows, 5 columns" | a11y | P0 | 1. Enable TalkBack<br>2. Focus grid | Announcement exactly as specified; per-dot: "{n} dots connected" during draw |
| T03-47 | TalkBack — rate-limited countdown announced | a11y | P0 | 1. Arm tier 1<br>2. Focus countdown | "Please wait a moment" (assertive) announced; countdown updates announced |
| T03-48 | TalkBack — biometric row announced only when available | a11y | P0 | 1. Enable TalkBack, biometric enrolled<br>2. Focus "Unlock with biometrics" | Row present; after system lockout, row absent from accessibility tree |
| T03-49 | Reduced motion (A5) — shake replaced by 200 ms rose border pulse | a11y | P0 | 1. Enable reduced motion<br>2. Trigger creation mismatch (onboarding) | 420 ms shake → 200 ms rose border pulse; announcement unchanged |
| T03-50 | Font scale 200% (A4) — grid shrinks, never scrolls | adaptive | P0 | 1. Set font scale 200%<br>2. Open `/lock` | Grid ≤280 dp, never scrolls; buttons grow to 60 dp; copy wraps |
| T03-51 | Foldable (V3) — grid never across hinge | adaptive | P0 | 1. Open on foldable, tabletop mode<br>2. Observe grid | Grid placed in bottom half; never spans hinge |
| T03-52 | Tablet (V4) — 320 dp grid | adaptive | P0 | 1. Open on tablet (≥600 dp width)<br>2. Measure grid | Grid 320 dp centered; same behavior |
| T03-53 | Battery saver (X2) — Solid mode, no behavior change | adaptive | P0 | 1. Enable battery saver<br>2. Open `/lock` | Glass effects → Solid; grid interaction identical |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T03-55 | Process death during draw — relaunch `/lock`, buffer empty | lifecycle | P0 | 1. Draw 4 dots<br>2. Kill process<br>3. Relaunch | `/lock` Idle; no partially drawn pattern restored |
| T03-56 | Process death during KDF — relaunch `/lock`, no half-unlocked vault | lifecycle | P0 | 1. Release pattern (KDF in flight)<br>2. Kill process<br>3. Relaunch | `/lock` Idle; derivation atomic in memory; no half-unlocked state |
| T03-57 | Duress draw while normal KDF in flight — in-flight discarded | race | P0 | 1. Release normal pattern (KDF running)<br>2. Immediately draw duress pattern | In-flight derivation cancelled; duress resolves within normal budget |
| T03-58 | Auto-lock during KDF — derivation cancelled | race | P0 | 1. Release pattern<br>2. Screen turns off before resolve | Derivation cancelled; buffers zeroized; screen-off locks immediately |
| T03-59 | Biometric prompt over throttle wait — not offered | race | P0 | 1. Arm tier 1 wait<br>2. Tap biometric row | Row not offered during wait (R2); biometrics disabled until wait ends |
| T03-60 | Pending destination — unresolvable id → `/vault` silently | lifecycle | P0 | 1. Deep link with stale opaqueId<br>2. Unlock | Lands on `/vault` silently; no error |
| T03-61 | Integrity check failed (S12) — locks, explains neutrally post-unlock | lifecycle | P1 | 1. Trigger integrity failure<br>2. Unlock with pattern | `/lock` shown with `reason=integrity`; after unlock, neutral explanation sheet |

## Security invariants to assert

| # | Invariant | Verified by |
|---|-----------|-------------|
| 1 | Unknown pattern: identical glow/transition/empty vault — no shake/red/counter on unlock | T03-15, T03-16, T03-34, T03-35 |
| 2 | Duress: pixel-identical to normal unlock — no confirmation, delay, badge, unique haptic | T03-36, T03-37 |
| 3 | FLAG_SECURE on grid area; recents shows solid cover | T03-33, T03-43 |
| 4 | Encrypted attempt counter survives restart/force-stop/reboot | T03-23, T03-41 |
| 5 | No pattern preview, no "show pattern" toggle, no pattern hints | T03-42 |
| 6 | Rate-limit copy exactly "Please wait a moment." + `mono.m` countdown; neutral ink; never implies specific pattern wrong | T03-17, T03-47 |
| 7 | Duress wipe silent: no progress, sound, notification; other vault indexes removed first | T03-37 |
| 8 | Biometric unlock identical resolve path, glow, timing, announcement as pattern | T03-06, T03-07 |
| 9 | Attempt timestamps truncated to minute in encrypted store | (covered by T03-23 encrypted persistence) |
| 10 | Clock rollback re-arms full current-tier wait | T03-22 |

## Gaps & contradictions

- **Plan contradiction (already noted in §3.1):** `pattern-encryption` planning.md specifies visible "Incorrect pattern" error + counter on GCM tag failure (AC-003, error matrix row 224), but `hidden-vaults`, `plausible-deniability`, and `duress-mode` require unknown pattern reveals nothing. **Aurora ships deniability-safe behavior as canonical** (§3.1) — the legacy error variant exists in `PatternFeedback` for research builds only and must not ship.
- **Biometric target vault undesigned** (04 §Gaps #3): biometric re-opens "last unlocked vault"; multi-vault users get no choice; "last used" is a coercion/forensic leak — needs product decision.
- **Duress partial-wipe ledger** (04 §Gaps #4): mid-wipe failure never surfaced (re-armed before decoy interactive; residue only in Security Info), but no durable wipe-ledger design guarantees resume — crash loop could strand residue silently.
- **Post-restore pattern creation** (04 §Gaps #2): device-bound salt means restored vault cannot verify old pattern; 01 designs 2-step pattern create (no phrase step) — confirm crypto spec binds salt to device.
- **Throttle counting basis** (04 §Gaps #1): with no wrong-pattern state, "5 failures" unobservable; 01 redefines as attempt rate (5 KDF submissions/2 min), which also throttles 6 legitimate quick unlocks — alternative (time-damping only) changes brute-force economics; needs sign-off.