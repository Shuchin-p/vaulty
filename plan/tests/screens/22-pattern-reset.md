# Pattern Reset — Test Cases

## Scope
screens.md §22 Pattern Reset + §11 Security Settings + §2 Onboarding step rules + flows/01-access-and-recovery.md §1.10 + pattern-encryption, secret-phrase, hidden-vaults planning.md invariants

## Preconditions
- App has a vault; user is on `/settings/security` (Security Settings screen)
- User taps "Change pattern" row → full-screen interstitial shown first (screens §11)
- Interstitial copy: "Resetting your pattern creates a new recovery phrase. Your files stay encrypted and stay yours."
- Require current pattern **or** recovery phrase, never both
- Four screens: interstitial → prove identity → new pattern draw → new pattern confirm → new phrase display → save confirmation
- Cancel at any point returns to Security settings; nothing changes until final save
- New pattern forces new phrase; old phrase invalidated on commit
- Files stay encrypted; never mixes hidden-vault catalog

## Test Cases

### Happy Path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T22-01 | Entry via Security Settings — interstitial displays correct copy | functional | P0 | 1. Open `/settings/security` 2. Tap "Change pattern" 3. Observe interstitial | Interstitial shows "Resetting your pattern creates a new recovery phrase. Your files stay encrypted and stay yours." |
| T22-02 | Entry via Security Settings — Cancel on interstitial returns to Security Settings | functional | P0 | 1. Open interstitial 2. Tap back/cancel | Returns to `/settings/security`; old pattern and phrase remain valid |
| T22-03 | Prove identity with current pattern — success path | functional | P0 | 1. Past interstitial 2. Draw current valid pattern 3. Observe next screen | Advances to new pattern draw screen; no error styling (creation hints only) |
| T22-04 | Prove identity with current pattern — wrong pattern handled deniably | functional | P0 | 1. Past interstitial 2. Draw pattern not matching any vault | Same 380 ms iris glow → neutral empty vault (no "incorrect", no shake, no red) per screens §3.1 |
| T22-05 | Prove identity with recovery phrase — success path | functional | P0 | 1. Tap "Forgot pattern?" on lock → `/recovery/enter` 2. Enter valid phrase 3. Choice sheet → "Reset pattern" | Enters reset flow at prove-identity step skipped (phrase already verified); advances to new pattern draw |
| T22-06 | New pattern draw — valid pattern (≥6 dots, ≥1 turn) enables Continue | functional | P0 | 1. At new pattern draw 2. Draw pattern with 6+ dots and a turn | "Continue" enabled; no inline hint |
| T22-07 | New pattern draw — <6 dots shows creation hint | functional | P0 | 1. Draw 5 dots 2. Tap Continue | Inline hint: "Connect at least 6 dots." (screens §24) |
| T22-08 | New pattern draw — straight line shows creation hint | functional | P0 | 1. Draw 6 dots in straight line 2. Tap Continue | Inline hint: "Add at least one turn." (screens §24) |
| T22-09 | New pattern confirm — match advances to phrase display | functional | P0 | 1. Draw same valid pattern again | 380 ms iris glow → advances to new phrase display (screens §2 step 3 rules) |
| T22-10 | New pattern confirm — mismatch shakes, no penalty | functional | P0 | 1. Draw different pattern | 420 ms shake (creation-only); no counter, no throttle; redraw allowed (screens §2) |
| T22-11 | New phrase display — 9 words shown, FLAG_SECURE active | security | P0 | 1. Reach phrase display step | 9-word grammatical phrase in 3×3 tiles, `mono.m` 22sp; FLAG_SECURE prevents screenshot; no copy button on first display |
| T22-12 | New phrase display — "I've saved it" enabled after checkbox + 10s | functional | P0 | 1. Wait <10s, checkbox unchecked → button disabled 2. Check checkbox + wait 10s | "I've saved it" enables only after both conditions (screens §2 rule) |
| T22-13 | New phrase display — leave triggers loss-confirmation dialog | functional | P0 | 1. Tap back/close before saving | Dialog: "Leave this screen? Without the phrase you can lose access to your vault." + checkbox "I understand I can lose access" (screens §2) |
| T22-14 | Save — atomic commit updates credentials, invalidates old phrase | functional | P0 | 1. Complete phrase confirm 2. Tap "Save" | Inline "Pattern updated." → returns to `/settings/security`; old pattern + phrase invalid; new credentials active |
| T22-15 | Save — background during commit never leaves mixed state | security | P0 | 1. Tap Save 2. Kill process mid-write | On relaunch: either old credentials valid (write failed) or new credentials valid (write succeeded); never both, never partial |
| T22-16 | Biometric re-auth on Settings entry — pattern reset uses pattern proof | functional | P1 | 1. Enable biometric unlock 2. Open Settings → Change pattern | Still requires current pattern draw (not biometric) per screens §11 "requires the current pattern" |
| T22-17 | Four-step progress indicator visible, no percentage | ui | P1 | 1. Observe all four steps | Step labels never truncate at 200% font (A4); buttons stack (flows §1.10) |
| T22-18 | Post-save — new pattern unlocks vault, old pattern opens neutral empty vault | security | P0 | 1. Save reset 2. Lock app 3. Draw old pattern | Same glow, neutral empty vault, "Nothing here yet." (screens §3.1 deniability) |
| T22-19 | Post-save — new phrase works for recovery, old phrase rejected | security | P0 | 1. Save reset 2. Lock → Forgot pattern 3. Enter old phrase | "That phrase didn't work." (screens §24) — generic, no timing leak |
| T22-20 | Post-save — new phrase works for recovery, new phrase accepted | security | P0 | 1. Save reset 2. Lock → Forgot pattern 3. Enter new phrase | Choice sheet: "Reset pattern" or "Open vault" (flows §1.8) |

### Edge & Error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T22-21 | Cancel on prove-identity step — returns to Security Settings | functional | P0 | 1. At prove identity 2. Tap cancel | "Cancel reset? Nothing has changed yet." dialog → confirm returns to `/settings/security`; old creds intact |
| T22-22 | Cancel on new pattern draw — returns to Security Settings | functional | P0 | 1. At new pattern draw 2. Tap cancel | Same cancel dialog → nothing changed |
| T22-23 | Cancel on new pattern confirm — returns to Security Settings | functional | P0 | 1. At confirm 2. Tap cancel | Same cancel dialog → nothing changed |
| T22-24 | Cancel on new phrase display — triggers loss-confirmation dialog | functional | P0 | 1. At phrase display 2. Tap back | "Leave this screen? Without the phrase you can lose access..." + loss checkbox (screens §2) |
| T22-25 | Cancel after phrase confirm, before Save — returns to Security Settings | functional | P0 | 1. Confirm phrase 2. Tap cancel before Save | Cancel dialog → nothing changed; old creds still valid |
| T22-26 | Process death (G4) at any step — old credentials valid on relaunch | lifecycle | P0 | 1. Kill app at each step (interstitial, prove, draw, confirm, phrase, save) | Relaunch → `/lock`; old pattern + phrase still work; no half-committed state |
| T22-27 | Process death during atomic Save — credentials consistent | lifecycle | P0 | 1. Kill during Save write | Relaunch → either old or new creds valid; never both, never partial (flows §1.10) |
| T22-28 | Rotation during any step — state preserved | config | P1 | 1. Rotate device at each step | Pattern buffer, phrase, step position survive; no data loss |
| T22-29 | Background >5s on phrase display — zeroizes, requires re-auth | security | P0 | 1. Reach phrase display 2. Background 6s 3. Foreground | Phrase gone; returns to `/lock`; full re-auth required (screens §20) |
| T22-30 | Throttle on prove-identity (pattern) — neutral wait copy | security | P1 | 1. Fail pattern proof 5x in 2 min (rolling window) | "Please wait a moment." + `mono.m` countdown; no "incorrect" implication (R1 §1.8) |
| T22-31 | Throttle on prove-identity (phrase) — separate counter, same copy | security | P1 | 1. Fail phrase entry 3x | "Please wait a moment." + countdown; recovery throttle independent from lock throttle (R1) |
| T22-32 | Invalid new pattern — creation hints only, no danger styling | ui | P0 | 1. Draw <6 dots or straight line | "Connect at least 6 dots." / "Add at least one turn." inline under grid; no red, no shake (screens §24) |
| T22-33 | Phrase entry wrong — "That phrase didn't work.", last field clears | functional | P0 | 1. Enter wrong phrase at prove-identity (phrase mode) | Generic error; only last field clears; mode + other fields preserved (flows §1.8) |
| T22-34 | Write failure on Save — inline error, old creds valid, retry offered | error | P1 | 1. Simulate storage write failure at Save | "Your files are safe. The reset didn't finish." inline; "Retry" / "Cancel"; old pattern+phrase work |
| T22-35 | Accessibility — TalkBack announces step titles, not word 1 first | a11y | P1 | 1. Enable TalkBack, reach phrase display | Warning banner first: "Screen reader active. Your phrase will be spoken aloud. Use headphones." then title (screens §1.5) |
| T22-36 | Accessibility — 200% font: steps wrap, grid shrinks, no scroll | a11y | P1 | 1. Set display size 200% 2. Run full flow | Four-step labels wrap; 5×5 grid shrinks, never scrolls; buttons 60dp height (flows §1.10, §1.3) |
| T22-37 | Accessibility — reduced motion: shake → rose border pulse | a11y | P1 | 1. Enable reduced motion 2. Mismatch on confirm | 420 ms shake replaced by 200 ms rose border pulse; announcement unchanged (flows §1.4) |
| T22-38 | Foldable — grid never across hinge; tabletop puts grid in bottom half | adaptive | P1 | 1. Open on foldable in tabletop | Grid in bottom half; no hinge crossing (flows §1.3) |
| T22-39 | RTL — slot order logical, numbers LTR in mono | i18n | P1 | 1. Set RTL locale 2. Run phrase entry | Word 1 first visually; index numbers remain LTR `mono` (flows §1.8) |
| T22-40 | Deep link into reset flow without vault — resolves to `/lock` | nav | P1 | 1. Send intent to `/recovery/reset-pattern` on no-vault device | Silent redirect to `/lock` (never shows interstitial without vault) |

### Security & Deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T22-41 | Old pattern after reset — opens neutral empty vault, no error | security | P0 | 1. Reset pattern 2. Lock 3. Draw old pattern | 380 ms iris glow → neutral empty vault; "Nothing here yet."; no shake, no red, no counter (screens §3.1) |
| T22-42 | Old phrase after reset — rejected generically, no timing leak | security | P0 | 1. Reset pattern 2. Forgot pattern → enter old phrase | "That phrase didn't work." (screens §24); constant-time comparison |
| T22-43 | New phrase after reset — works for recovery, opens choice sheet | security | P0 | 1. Reset pattern 2. Forgot pattern → enter new phrase | Choice: "Reset pattern" or "Open vault" (flows §1.8) |
| T22-44 | Duress pattern during reset — not applicable (reset requires auth) | security | P0 | 1. Attempt duress pattern at prove-identity | Duress pattern treated as unknown pattern → neutral empty vault (R3) |
| T22-45 | Hidden vaults never enumerated — no catalog leak | security | P0 | 1. Observe all reset screens | No vault list, count, names, or picker anywhere (hidden-vaults planning §1) |
| T22-46 | Files stay encrypted — no plaintext exposure at any step | security | P0 | 1. Memory dump at each step | Vault files encrypted; master key only in memory during active session; phrase zeroized after 5s background |
| T22-47 | Phrase never persisted before final Save | security | P0 | 1. Kill process before Save 2. Check storage | New phrase not on disk; only in memory; old phrase verifier still valid |
| T22-48 | Atomic commit — old and new creds never simultaneously valid | security | P0 | 1. Save 2. Verify immediately after | Single atomic write: pattern verifier + phrase verifier + old phrase invalidation |
| T22-49 | No crypto terms in any UI copy | security | P0 | 1. Review all reset screen text | No "PBKDF2", "GCM", "salt", "key", "envelope", "unwrap", "tag" (screens §24 error tone) |
| T22-50 | Interstitial copy matches exactly — no variation | security | P0 | 1. Open Change pattern from Settings | "Resetting your pattern creates a new recovery phrase. Your files stay encrypted and stay yours." verbatim |

### Accessibility & Adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T22-51 | TalkBack — pattern grid announces "Pattern grid, 5 rows, 5 columns" | a11y | P0 | 1. Enable TalkBack on prove-identity or new pattern draw | Announcement on focus; dot position announced on touch |
| T22-52 | TalkBack — phrase display warns before reading words | a11y | P0 | 1. Enable TalkBack on new phrase display | "Screen reader active. Your phrase will be spoken aloud. Use headphones." before any word |
| T22-53 | Switch access — can navigate all steps, draw pattern via scanning | a11y | P1 | 1. Enable switch access 2. Complete reset | All controls reachable; pattern draw via row/col scanning; no auto-advance traps |
| T22-54 | 200% font — interstitial, hints, buttons wrap, no truncation | a11y | P1 | 1. Set display size 200% 2. Run flow | All text wraps; grid shrinks; buttons 60dp; step indicator labels intact |
| T22-55 | Reduced motion — shake→pulse, no timing class change | a11y | P1 | 1. Enable reduced motion 2. Trigger mismatch | 420 ms shake → 200 ms rose pulse; resolve timing identical (380 ms glow ±1 frame) |
| T22-56 | Tablet (V4) — centered single column, 320dp grid | adaptive | P1 | 1. Run on tablet | Layout centered; grid 320dp; progress indicator single column (flows §1.3) |
| T22-57 | Foldable (V3) — grid in bottom half, no hinge split | adaptive | P1 | 1. Run on foldable half-folded | Grid in bottom pane; never crosses hinge (flows §1.3) |
| T22-58 | High contrast — 4.5:1 minimum, colorblind-safe pattern feedback | a11y | P1 | 1. Enable high contrast 2. Draw pattern | Dot fill + line use shape + contrast, not color alone |
| T22-59 | Landscape — grid centered, no horizontal scroll | adaptive | P1 | 1. Rotate to landscape mid-flow | Grid centered; steps stack vertically; no content cropped |
| T22-60 | Keyboard navigation — arrow keys + Enter for pattern grid | a11y | P1 | 1. Connect keyboard 2. Navigate pattern grid | Arrow moves focus; Enter selects dot; Tab order logical across all screens |

### Races & Lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T22-61 | Background during KDF (prove identity) — cancelled, buffers zeroized | lifecycle | P0 | 1. Start prove identity 2. Home during KDF | Derivation cancelled; buffers zeroized; returns to idle prove-identity screen |
| T22-62 | Background during Save commit — write completes or rolls back atomically | lifecycle | P0 | 1. Tap Save 2. Home immediately | On foreground: either Save succeeded (new creds) or failed (old creds + error inline) |
| T22-63 | Rotation during KDF — derivation continues, result delivered | lifecycle | P1 | 1. Start prove identity 2. Rotate during KDF | Config change survives; result applied to new activity instance |
| T22-64 | Kill during phrase display (G4) — phrase never re-rendered | lifecycle | P0 | 1. Reach phrase display 2. Kill process 3. Relaunch | Relaunch → `/lock`; phrase gone forever; must reset again to see new phrase |
| T22-65 | Cancel dialog — back/Esc dismisses, scrim tap does not | ui | P1 | 1. Trigger cancel dialog 2. Press back / tap scrim | Back/Esc → cancel dialog dismissed; scrim tap → no action (destructive dialog pattern) |
| T22-66 | Multiple rapid Cancel taps — single dialog, no stack | ui | P1 | 1. Tap cancel 5x rapidly | Only one dialog shown; no ANR, no dialog stack |
| T22-67 | Biometric invalidation mid-reset — pattern proof still required | lifecycle | P1 | 1. Start reset with pattern proof 2. Remove fingerprint 3. Continue | Pattern proof still works; biometric never substitutes for pattern in reset (screens §11) |
| T22-68 | Auto-lock during reset — not possible (reset is foreground flow) | lifecycle | P1 | 1. Leave reset screen in background > threshold | Auto-lock doesn't apply; reset screens are FLAG_SECURE, don't auto-lock independently |
| T22-69 | Clock rollback during throttle wait — re-arms full wait | lifecycle | P1 | 1. Trigger throttle wait 2. Roll system clock back | Wait re-arms to full current-tier duration (R1 clock rule) |
| T22-70 | Clock forward across deadline — wait ends early | lifecycle | P1 | 1. Trigger throttle wait 2. Jump clock past deadline | Wait ends; accepted cost per R1 |
| T22-71 | Low memory kill during reset — old creds valid on relaunch | lifecycle | P1 | 1. OOM kill at each step 2. Relaunch | Always `/lock`; old pattern + phrase valid; no partial state |
| T22-72 | Notification tap during reset — destination dropped | nav | P1 | 1. Start reset 2. Tap notification | Notification destination dropped; reset continues; after Save, notification re-evaluated |

## Security Invariants to Assert

- [ ] Nothing changes until final Save atomic commit
- [ ] Cancel at any point returns to `/settings/security` with old credentials intact
- [ ] New pattern always implies new phrase; old phrase invalidated on commit
- [ ] Files remain encrypted throughout; no plaintext written to storage
- [ ] Hidden-vault catalog never exposed (no list, count, names, picker)
- [ ] Old pattern after reset → neutral empty vault (deniability)
- [ ] Old phrase after reset → generic rejection, no timing leak
- [ ] New phrase never persisted before Save confirmation
- [ ] Phrase zeroized on background >5s, process death, cancel
- [ ] Atomic write: pattern verifier + phrase verifier + old phrase invalidation together
- [ ] No crypto terms in UI copy
- [ ] Interstitial copy verbatim from screens.md §22
- [ ] Error copy verbatim from screens.md §24
- [ ] Throttle copy verbatim "Please wait a moment." + `mono.m` countdown
- [ ] No "incorrect pattern", no shake, no red on unknown pattern (screens §3.1)
- [ ] Duress pattern treated as unknown during reset (R3)

## Gaps & Contradictions

- **screens.md §22** says "Require current pattern **or** recovery phrase, not both" but **flows §1.10** shows two entry points: `proof=current-pattern` (from Settings) and `proof=phrase` (from recovery choice sheet). Both paths converge but the interstitial is only mentioned for Settings entry. Clarify: does recovery-entry reset show the interstitial?
- **screens.md §11** "Changing the pattern requires the current pattern, then redraw twice. It generates a **new recovery phrase**; show a full-screen interstitial before starting." — "redraw twice" implies confirm step, but reset flow has new pattern draw + confirm (2 draws) + new phrase. Consistent.
- **screens.md §22** "Four screens, no shortcuts" but flow has: interstitial, prove identity, draw, confirm, phrase, save = 6 steps. "Four screens" likely counts: prove, draw, confirm, phrase (interstitial + save as overlays). Document which are "screens".
- **flows §1.10** "Interstitial → step 1" but prove identity is conditional on entry path. For `proof=phrase`, prove identity is skipped. For `proof=current-pattern`, prove identity = draw current pattern. The step numbering is ambiguous.
- **secret-phrase planning AC-19** "New recovery phrase generated; old phrase invalidated" — matches. But AC-20 "User must confirm new phrase before vault access" — reset flow shows phrase then Save; vault access only after Save. Consistent.
- **hidden-vaults planning AC-3** "Empty state for unknown patterns" — reset uses this for old pattern post-reset. But reset prove-identity with wrong pattern also uses deniable handling. Ensure same code path.
- **pattern-encryption planning AC-003** "GCM tag mismatch → 'Incorrect pattern'" — contradicted by screens §3.1 deniability behavior. Aurora ships deniability-safe; legacy error only in research builds. Reset prove-identity must use deniable handling.
- **screens.md §24** "Recovery wrong: 'That phrase didn't work.'" — used in both recovery entry and reset prove-identity (phrase mode). Consistent.
- **flows §1.10** "The new phrase display follows every §1.5 rule, including background-zeroization" — but §1.5 is onboarding phrase (10s gate, checkbox). Settings phrase display (§1.9) has 10s gate + two-step confirm. Which applies? Reset uses onboarding-style (§1.5) per flows.
- **No spec for**: what happens if user proves identity with phrase, then cancels before Save, then later re-enters reset via Settings (pattern proof)? The phrase was already verified but not used. Does it expire?
- **No spec for**: concurrent reset attempts (e.g., two devices signed into same cloud). Not applicable — reset is local-only.
- **No spec for**: reset during active backup/sync. Should be allowed; files stay encrypted.
- **No spec for**: reset on Pro vs Free. Duress is Pro; reset is Free. No feature gate mentioned.