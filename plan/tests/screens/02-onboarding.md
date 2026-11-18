# Onboarding — Test Cases

## Scope
screens.md §2 (pattern create → confirm → phrase → done) + flows/01-access-and-recovery.md §1.3–1.6 (F1 journey, G4 death rules) + pattern-encryption (pattern strength, commit atomicity) + secret-phrase (9-word generation, FLAG_SECURE, leave dialog, background zeroize)

## Preconditions
- Fresh install, no local vault, no pending restore intent
- Device at default locale, TalkBack off, reduced motion off, display scale 100%
- No permissions granted (camera, storage, notifications)
- Secure flag not yet enforced (pattern steps); enforced from phrase step onward
- Phrase step rules: no skip, 10 s gate, leave dialog with loss checkbox, background > 5 s zeroizes
- No permissions requested before camera/import tap on done screen

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T02-01 | Complete onboarding end-to-end | functional | P0 | 1. Launch app → `/welcome`<br>2. Tap `Start fresh` → `/onboarding/pattern`<br>3. Draw valid pattern (≥ 6 dots, ≥ 1 turn) → `Continue`<br>4. On confirm, draw identical pattern → release<br>5. On phrase screen, wait 10 s → check `I wrote it down` → tap `I've saved it`<br>6. On done screen, tap `Add photos` | 1. Lands on `/welcome` with orbit art, "Your vault, here." / "Restore it with the phrase you saved."<br>2. `/onboarding/pattern` shows `gradient.dawn` bg, "Draw a pattern you'll remember.", 3 progress dots (ink.200, active ink pill 20 dp)<br>3. `Continue` enabled at ≥ 6 dots + ≥ 1 turn; advances to `/onboarding/confirm`<br>4. Match → 380 ms iris glow → auto-advance to `/onboarding/phrase`; pattern committed (verifier + wrapped key + vault arena written atomically)<br>5. Phrase screen: `Plain` bg + solid card, "This is your only way back in. Keep this screen open while you write it down.", 9 words in 3×3 `mono.m` 22sp tiles, checkbox + `I've saved it` disabled until both conditions met; after tap → `/onboarding/done`<br>6. Done screen: "Your vault is ready.", primary `Add photos`, ghost `Take a photo`; tapping `Add photos` → `/vault/import` |
| T02-02 | Pattern create: minimum valid pattern (6 dots, 1 turn) | functional | P0 | 1. `/onboarding/pattern`<br>2. Draw exactly 6 dots with 1 turn<br>3. Tap `Continue` | `Continue` enabled; advances to confirm step |
| T02-03 | Pattern confirm: match on first try | functional | P0 | 1. `/onboarding/confirm` with held pattern in memory<br>2. Draw identical pattern<br>3. Release | 380 ms iris glow → auto-advance to phrase; no shake, no error copy |
| T02-04 | Phrase step: save after 10 s gate | functional | P0 | 1. `/onboarding/phrase`<br>2. Wait 10 s (no timer shown)<br>3. Check `I wrote it down`<br>4. Tap `I've saved it` | Button enabled only after both checkbox checked AND ≥ 10 s elapsed; tap → `/onboarding/done`; phrase verifier stored encrypted; plaintext zeroized |
| T02-05 | Done screen: `Take a photo` opens camera flow | functional | P0 | 1. `/onboarding/done`<br>2. Tap `Take a photo` | → `/vault/camera`; camera permission rationale owned by camera flow (not requested here) |
| T02-06 | Progress indicator shows 3 dots, not percentage | visual | P1 | 1. `/onboarding/pattern` → `Continue` → `/onboarding/confirm` → auto-advance → `/onboarding/phrase` | Three dots (`ink.200`, active ink pill 20 dp) visible on pattern and confirm steps; hidden on phrase step (`mode=post-restore` only) |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T02-07 | Pattern create: < 6 dots shows inline hint | functional | P0 | 1. `/onboarding/pattern`<br>2. Draw 5 dots<br>3. Tap `Continue` | Hint under grid: "Connect at least 6 dots."; 420 ms shake (creation-only error styling); `Continue` stays disabled |
| T02-08 | Pattern create: ≥ 6 dots but no turn shows inline hint | functional | P0 | 1. `/onboarding/pattern`<br>2. Draw 6+ dots in straight line (no turn)<br>3. Tap `Continue` | Hint: "Add at least one turn."; shake; `Continue` disabled |
| T02-09 | Pattern create: redraw clears buffer | functional | P1 | 1. `/onboarding/pattern`<br>2. Draw partial pattern<br>3. Lift finger or tap cleared start | Buffer resets to idle (S3); grid ready for new draw |
| T02-10 | Pattern confirm: mismatch → shake + retry, no penalty | functional | P0 | 1. `/onboarding/confirm`<br>2. Draw different pattern<br>3. Release | 420 ms shake, 3 oscillations ±10 dp; returns to drawing state (S13); no throttle counter incremented; no "incorrect" copy |
| T02-11 | Pattern confirm: `Start over` returns to create, nothing committed | functional | P0 | 1. `/onboarding/confirm`<br>2. Tap `Start over` | → `/onboarding/pattern`; held pattern cleared; no vault created |
| T02-12 | Pattern confirm: Back returns to create, nothing committed | functional | P1 | 1. `/onboarding/confirm`<br>2. Press system Back | → `/onboarding/pattern`; held pattern cleared |
| T02-13 | Phrase step: `I've saved it` disabled until checkbox + 10 s | functional | P0 | 1. `/onboarding/phrase`<br>2. Immediately check checkbox<br>3. Wait 10 s without checkbox<br>4. Check checkbox + wait 10 s | Button disabled in steps 2 & 3; enabled only in step 4 |
| T02-14 | Phrase step: Back/close opens leave-confirm dialog | functional | P0 | 1. `/onboarding/phrase`<br>2. Press system Back or close affordance | Dialog: "Leave this screen? Without the phrase you can lose access to your vault." + checkbox "I understand I can lose access"; primary disabled until checked |
| T02-15 | Phrase step: Leave confirmed → phrase lost, vault opens | functional | P0 | 1. `/onboarding/phrase`<br>2. Back → dialog<br>3. Check loss checkbox → confirm | → `/vault` (not `/onboarding/done`); pattern kept; phrase **never shown again**; plaintext zeroized |
| T02-16 | Phrase step: Leave canceled returns to phrase | functional | P1 | 1. `/onboarding/phrase`<br>2. Back → dialog<br>3. Cancel or press Back/Esc | Dialog dismisses; phrase screen remains; plaintext still in memory |
| T02-17 | Pattern create: rotation survives config change | functional | P1 | 1. `/onboarding/pattern`<br>2. Draw partial pattern<br>3. Rotate device | Buffer survives; grid redraws; no data loss |
| T02-18 | Pattern confirm: rotation survives config change | functional | P1 | 1. `/onboarding/confirm` with held pattern<br>2. Rotate device | Held pattern survives; grid redraws |
| T02-19 | Phrase step: rotation survives config change | functional | P1 | 1. `/onboarding/phrase`<br>2. Rotate device | Checkbox state, elapsed timer survive; phrase stays in memory; tiles reflow to one column at A4 200% |
| T02-20 | Done screen: Back grounds app, session continues | functional | P1 | 1. `/onboarding/done`<br>2. Press Back | App grounded; session continues under auto-lock rules; relaunch after death → `/lock` → `/vault` (done never restored as destination) |
| T02-21 | Pattern create: stylus input works with same smoothing | functional | P2 | 1. `/onboarding/pattern`<br>2. Draw with stylus | Dots accumulate; palm rejection via `MotionEvent`; same grid behavior |
| T02-22 | Pattern create: V3 foldable places grid in bottom half (tabletop) | visual | P2 | 1. Foldable in tabletop posture<br>2. `/onboarding/pattern` | Grid never placed across hinge; positioned in bottom half |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T02-23 | Phrase step: FLAG_SECURE set from first frame | security | P0 | 1. `/onboarding/phrase`<br>2. Attempt screenshot / screen record / recents view | Screenshot blocked; recents shows solid cover; no phrase visible in any capture |
| T02-24 | Pattern confirm: FLAG_SECURE set from first frame | security | P0 | 1. `/onboarding/confirm`<br>2. Attempt screenshot / recents | Screenshot blocked; recents shows solid cover |
| T02-25 | Pattern create: no FLAG_SECURE (per screens.md) | security | P1 | 1. `/onboarding/pattern`<br>2. Attempt screenshot | Screenshot allowed (only phrase step and confirm step are Secure) |
| T02-26 | No vault created until confirm match | security | P0 | 1. `/onboarding/pattern` → draw valid → `Continue`<br>2. Kill app before confirm<br>3. Relaunch | Relaunch → `/welcome` (nothing committed); no vault exists |
| T02-27 | Phrase never persisted before confirmation | security | P0 | 1. `/onboarding/phrase`<br>2. Kill app before `I've saved it`<br>3. Relaunch | Relaunch → `/lock` (vault exists, pattern committed); phrase never re-rendered; plaintext zeroized at death |
| T02-28 | Background > 5 s on phrase step zeroizes plaintext, locks app | security | P0 | 1. `/onboarding/phrase`<br>2. Background app (Home)<br>3. Wait 6 s<br>4. Foreground app | Solid cover shown immediately; at 5 s plaintext zeroized; foreground → `/lock` (vault exists) |
| T02-29 | Background ≤ 5 s on phrase step lifts cover, phrase visible | security | P0 | 1. `/onboarding/phrase`<br>2. Background app<br>3. Wait 3 s<br>4. Foreground app | Cover lifts; phrase still visible; no re-auth required |
| T02-30 | No skip button on phrase step | security | P0 | 1. `/onboarding/phrase`<br>2. Inspect UI for skip affordance | No skip button, no bypass; only `I've saved it` (gated) or leave dialog |
| T02-31 | Leave dialog has no scrim dismiss | security | P0 | 1. `/onboarding/phrase`<br>2. Back → dialog<br>3. Tap outside dialog / press Esc | Dialog not dismissed; only Back (cancel) or confirm with checkbox works |
| T02-32 | Phrase display: no copy, no share, clipboard disabled, autofill off, IME learning off | security | P0 | 1. `/onboarding/phrase`<br>2. Long-press word tile<br>3. Check clipboard / autofill / IME | Long-press selection disabled; clipboard disabled; `autofillHints` off; `IME_FLAG_NO_PERSONALIZED_LEARNING` set |
| T02-33 | No permissions requested during onboarding | security | P0 | 1. Complete onboarding to done screen<br>2. Inspect permission requests | No camera, storage, notification prompts before user taps `Take a photo` or `Add photos` |
| T02-34 | No duress / hidden vaults introduced during onboarding | security | P0 | 1. Complete onboarding<br>2. Inspect done screen and all steps | No mention of duress, decoy, hidden vaults, plausible deniability |
| T02-35 | Pattern commit is atomic; crash mid-write leaves no vault | security | P0 | 1. `/onboarding/confirm` → match<br>2. Force-kill during commit write<br>3. Relaunch | Relaunch → `/welcome` (uncommitted) OR `/lock` (committed); never partial vault; atomic write ensures either old state or new state |
| T02-36 | Constant-time comparison on confirm step | security | P1 | 1. `/onboarding/confirm`<br>2. Measure comparison timing for match vs mismatch | Timing does not leak match length; constant-time implementation |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T02-37 | TalkBack on phrase step: warn-then-read banner | a11y | P0 | 1. Enable TalkBack<br>2. `/onboarding/phrase` | Butter banner: "Screen reader active. Your phrase will be spoken aloud. Use headphones."; warning first — focus lands on title, never word 1; custom action to read all words |
| T02-38 | TalkBack on done screen: order title → Add photos → Take a photo | a11y | P1 | 1. Enable TalkBack<br>2. `/onboarding/done` | Announcement order: "Your vault is ready" → `Add photos` → `Take a photo` |
| T02-39 | A4 200%: phrase tiles wrap to one column, page scrolls, notice not truncated | adaptive | P0 | 1. Display size 200%<br>2. `/onboarding/phrase` | 3×3 tiles → single column; page scrolls; butter notice fully visible; grid shrinks, never scrolls on pattern steps |
| T02-40 | A4 200%: hint text wraps to 2 lines, grid shrinks, buttons grow to 60 dp | adaptive | P1 | 1. Display size 200%<br>2. `/onboarding/pattern` → draw invalid → `Continue` | Hint wraps to 2 lines; grid shrinks (no scroll); `Continue` / `Back` buttons 60 dp height |
| T02-41 | A4 200%: done screen buttons stack, 60 dp height | adaptive | P1 | 1. Display size 200%<br>2. `/onboarding/done` | Buttons stack vertically; 60 dp height; centered |
| T02-42 | A5 reduced motion: shake replaced by 200 ms rose border pulse on confirm mismatch | adaptive | P1 | 1. Reduced motion on<br>2. `/onboarding/confirm` → mismatch | No 420 ms shake; 200 ms rose border pulse; announcement unchanged |
| T02-43 | A5 reduced motion: 10 s gate unchanged (not animation) | adaptive | P1 | 1. Reduced motion on<br>2. `/onboarding/phrase` | 10 s gate still enforced; no animation to reduce |
| T02-44 | V3 foldable: grid never across hinge; tabletop → bottom half | adaptive | P1 | 1. Foldable, tabletop posture<br>2. `/onboarding/pattern` and `/onboarding/confirm` | Grid confined to one half; bottom half in tabletop |
| T02-45 | V4 tablet: identical centered layout, 320 dp grid on lock (pattern steps same) | adaptive | P1 | 1. Tablet layout<br>2. `/onboarding/pattern` | Centered layout; grid 280 dp (pattern steps); progress dots centered |
| T02-46 | V6 multi-window: single column; actions in bottom third | adaptive | P1 | 1. Multi-window narrow width<br>2. `/onboarding/done` | Single column layout; two actions stay in bottom third, thumb-reachable |
| T02-47 | Pattern create: announcement "Draw a pattern you'll remember. Pattern grid, 5 rows, 5 columns" | a11y | P1 | 1. TalkBack on<br>2. `/onboarding/pattern` idle | Exact announcement spoken |
| T02-48 | Pattern drawing: announcement "{n} dots connected" per dot | a11y | P1 | 1. TalkBack on<br>2. Draw pattern on create/confirm | Each dot connection announced |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T02-49 | Process death on pattern create → `/welcome` | lifecycle | P0 | 1. `/onboarding/pattern` → draw pattern<br>2. Kill process (G4)<br>3. Relaunch | `/welcome` (nothing committed); no partial pattern restored |
| T02-50 | Process death on pattern confirm (uncommitted) → `/welcome` | lifecycle | P0 | 1. `/onboarding/confirm` → drawing, no match yet<br>2. Kill process<br>3. Relaunch | `/welcome` (nothing committed) |
| T02-51 | Process death on pattern confirm (committed) → `/lock` | lifecycle | P0 | 1. `/onboarding/confirm` → match → auto-advance starts<br>2. Kill process before phrase screen renders<br>3. Relaunch | `/lock` (pattern committed, vault exists); phrase never existed |
| T02-52 | Process death on phrase step → `/lock` (phrase unrecoverable) | lifecycle | P0 | 1. `/onboarding/phrase` displaying phrase<br>2. Kill process<br>3. Relaunch | `/lock` (vault exists); phrase never re-rendered; unrecoverable by design |
| T02-53 | Process death on done screen → `/lock` → `/vault` | lifecycle | P0 | 1. `/onboarding/done`<br>2. Kill process<br>3. Relaunch | `/lock` → unlock → `/vault`; done screen never restored as destination |
| T02-54 | Background during pattern create → buffer cleared on background | lifecycle | P1 | 1. `/onboarding/pattern` → drawing<br>2. Background app<br>3. Foreground app | Buffer cleared; returns to idle (S3) |
| T02-55 | Background during confirm drawing → buffer cleared | lifecycle | P1 | 1. `/onboarding/confirm` → drawing<br>2. Background app<br>3. Foreground | Buffer cleared; returns to idle (S3) |
| T02-56 | Background during phrase step ≤ 5 s → cover lifts, phrase visible | lifecycle | P0 | 1. `/onboarding/phrase`<br>2. Background 3 s<br>3. Foreground | Cover lifts; phrase still visible; no re-auth |
| T02-57 | Background during phrase step > 5 s → zeroize, relaunch → `/lock` | lifecycle | P0 | 1. `/onboarding/phrase`<br>2. Background 6 s<br>3. Kill process<br>4. Relaunch | `/lock`; phrase zeroized at 5 s; never recoverable |
| T02-58 | Rotation during pattern drawing → buffer survives config change | lifecycle | P1 | 1. `/onboarding/pattern` → drawing<br>2. Rotate device<br>3. Continue drawing | Buffer survives; grid redraws correctly |
| T02-59 | Rotation during phrase display → checkbox + timer survive | lifecycle | P1 | 1. `/onboarding/phrase` → checkbox checked, 7 s elapsed<br>2. Rotate device | Checkbox remains checked; timer continues from 7 s; phrase in memory |
| T02-60 | Race: notification tap during onboarding → ignored until vault ready | lifecycle | P1 | 1. Start onboarding<br>2. Send notification (backup/sync/share)<br>3. Tap notification during pattern step | Notification tap → `/` → `/lock` (vault not yet exists) → handled after vault created; pending destination dropped if lock abandoned |

## Security invariants to assert

- [ ] FLAG_SECURE on `/onboarding/confirm` and `/onboarding/phrase` from first frame
- [ ] No skip affordance on phrase step; leave always routes through confirm dialog with loss checkbox
- [ ] No vault created before pattern confirm match (atomic commit at confirm)
- [ ] Phrase never persisted in plaintext before `I've saved it` confirmation
- [ ] Background > 5 s on phrase step zeroizes plaintext; foreground → `/lock`
- [ ] Process death on phrase step → phrase unrecoverable; relaunch → `/lock`
- [ ] No permissions requested before camera/import tap on done screen
- [ ] No duress / hidden vaults mentioned or introduced during onboarding
- [ ] Constant-time comparison on confirm step; no timing leak
- [ ] Attempt throttle not active on create/confirm (only on unlock surfaces)
- [ ] Creation validation hints only place with error styling (shake/hint); confirm mismatch = shake only, no penalty
- [ ] Leave dialog: no scrim dismiss, Back/Esc cancel, checkbox required for primary

## Gaps & contradictions

- **screens.md §2** says "Secure: phrase step only" but **flows §1.4** marks `/onboarding/confirm` as Secure (FLAG_SECURE). Test T02-24 asserts FLAG_SECURE on confirm; T02-25 asserts NO FLAG_SECURE on pattern create. This is a plan contradiction — flows take precedence for security-critical screens.
- **screens.md §2** says progress dots "hidden in `mode=post-restore`" but flows §1.3 shows progress dots on pattern create always. The `mode=post-restore` only applies to restore flow (§1.11), not standard onboarding. Test T02-06 covers standard onboarding.
- **pattern-encryption planning.md** Flow 2 shows "Validate Pattern Strength" step with "Show Strength Guide" on weak pattern, but **screens.md §2** and **flows §1.3** explicitly state "No strength meter (pattern strength guidance lives in the confirm step's help)" and "No skip button; no strength meter". The planning doc appears outdated. Tests follow screens.md/flows (no strength meter).
- **secret-phrase planning.md** says "12-word recovery phrase" in Flow 1, but **screens.md §2**, **flows §1.5**, and **secret-phrase Flow 1** all specify **9-word** phrase. Tests use 9 words.
- **flows §1.5** says phrase step background > 5 s zeroizes; **screens §20** (referenced in §1.9) says "background > 5 s, 30 s inactivity, or G4". The 30 s inactivity applies to `/recovery/display` (settings re-display), not onboarding phrase step. Tests use 5 s for onboarding.
- **flows §1.6** done screen: "Notification permission is **not** asked here — only after the first backup is enabled or the first share is created (07 §8.4)". No test for notification permission here (correctly omitted).
- **V3 foldable** behavior for onboarding not explicitly specified in screens.md; tests infer from lock screen rules (never across hinge, tabletop → bottom half).