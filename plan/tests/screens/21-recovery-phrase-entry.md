# Recovery Phrase Entry — Test Cases

## Scope
screens.md §21 "Recovery Phrase Entry" + flow 1.8 in 01-access-and-recovery.md + secret-phrase/planning.md + pattern-encryption/planning.md

## Preconditions
- App has a local vault (pattern exists) or is in restore mode (no local vault)
- Secure screen: `FLAG_SECURE` set on whole Activity/Dialog
- Word modes: segmented control `9 words` / `12 words` / `Custom` (6–24 words)
- Word slots auto-advance; no suggestions, no autocorrect, no clipboard menu by default
- `Paste` is a ghost action requiring 1.5s hold with confirmation dialog
- On submit: full-width iris progress line, no percentage, cancel disabled, 800k KDF runs
- Wrong phrase: generic message `"That phrase didn't work."`, clear only last field, keep rest, throttle after 3 wrong submissions (30s wait, R1 ladder on recovery counter)
- After success in `mode=open`: choice sheet `Reset pattern` → `/recovery/reset-pattern` or `Open vault` → `/vault`
- After success in `mode=restore`: auto-advance to `/restore/progress`
- After success in `mode=reauth`: return to origin settings action
- Typed words are memory-only, zeroized on back/G4, never persisted

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T21-01 | Open mode — valid 9-word phrase unlocks vault | functional | P0 | 1. Launch app → `/lock`<br>2. Tap `Forgot pattern?` → `/recovery/enter` (`mode=open`)<br>3. Select `9 words` (default)<br>4. Type all 9 words correctly<br>5. Tap `Unlock` | Choice sheet appears with `Reset pattern` and `Open vault`; selecting `Open vault` opens `/vault` |
| T21-02 | Restore mode — valid phrase starts restore | functional | P0 | 1. Fresh install → `/welcome`<br>2. Tap `Restore with phrase` → `/recovery/enter` (`mode=restore`)<br>3. Type valid 9-word phrase<br>4. Tap `Restore` | Auto-advance to `/restore/progress`; backup key derived, Drive discovery begins |
| T21-03 | Reauth mode — valid phrase returns to settings | functional | P0 | 1. Unlock vault → Settings → Security → `Recovery phrase` → `Reveal`<br>2. Re-auth with pattern → `/recovery/enter` (`mode=reauth`)<br>3. Type valid phrase<br>4. Tap `Continue` | Returns to Settings Security screen; phrase re-derived in memory |
| T21-04 | 12-word mode — valid phrase accepted | functional | P0 | 1. Open `/recovery/enter`<br>2. Switch segmented control to `12 words`<br>3. Type 12 valid words<br>4. Submit | Phrase verifies; success path per mode |
| T21-05 | Custom mode — 6-word phrase accepted | functional | P0 | 1. Open `/recovery/enter`<br>2. Switch to `Custom`<br>3. Type 6 valid words (passes strength)<br>4. Submit | Phrase verifies; strength meter shows ≥ `Fair` |
| T21-06 | Custom mode — 24-word phrase accepted | functional | P0 | 1. Open `/recovery/enter` → `Custom`<br>2. Type 24 valid words<br>3. Submit | Phrase verifies; all 24 slots filled |
| T21-07 | Auto-advance between slots while typing | ui | P0 | 1. Open `/recovery/enter`<br>2. Type first word + space<br>3. Continue typing remaining words | Focus moves to next slot automatically; no manual tab needed |
| T21-08 | Backspace retreats to previous slot | ui | P0 | 1. Fill 3 slots<br>2. In slot 3, press backspace until empty<br>3. Press backspace again | Focus moves to slot 2; slot 3 remains empty |
| T21-09 | Hold-to-paste fills all slots | ui | P0 | 1. Copy valid 9-word phrase to clipboard<br>2. Open `/recovery/enter`<br>3. Long-press `Paste words` ghost button (1.5s)<br>4. Confirm dialog → `Paste` | All 9 slots filled; clipboard cleared immediately; submit enabled |
| T21-10 | Paste confirmation dialog shows on hold | ui | P0 | 1. Long-press `Paste words` (1.5s) | Dialog: "Paste 9 words from clipboard?" with `Cancel` / `Paste` |
| T21-11 | Paste with empty clipboard shows error | edge | P1 | 1. Clear clipboard<br>2. Long-press `Paste words` → confirm | Dialog says "Clipboard is empty" or "No valid words found"; no fill |
| T21-12 | Paste with non-word content rejected | edge | P1 | 1. Copy "hello world 123" to clipboard<br>2. Long-press `Paste words` → confirm | Dialog says "Clipboard doesn't contain valid words"; no fill |
| T21-13 | Mode switch clears entered words with confirmation | ui | P0 | 1. Type 3 words in `9 words` mode<br>2. Switch to `12 words` | Confirm dialog: "Switching modes clears your words. Continue?"; on confirm, all slots empty |
| T21-14 | Mode switch without words — no confirmation | ui | P1 | 1. Open `/recovery/enter` (empty)<br>2. Switch `9 words` → `12 words` → `Custom` | No confirmation; slot count updates instantly |
| T21-15 | Custom mode shows strength meter | ui | P0 | 1. Switch to `Custom`<br>2. Type weak phrase (common words)<br>3. Type strong phrase | 4 segments labeled `Weak / Fair / Good / Strong`; updates live; never color-only |
| T21-16 | Submit button enabled only when all slots filled | ui | P0 | 1. Open `/recovery/enter`<br>2. Fill 8 of 9 slots | `Unlock`/`Restore`/`Continue` disabled<br>3. Fill 9th slot | Button enabled |
| T21-17 | Iris progress line on submit — no percentage | ui | P0 | 1. Fill all slots<br>2. Tap submit | Full-width iris line animates; text "Checking your phrase. This can take a few seconds."; no percentage; controls disabled not removed; cancel disabled |
| T21-18 | Cancel disabled during KDF | security | P0 | 1. Submit valid phrase<br>2. During iris progress, press back / close | Nothing happens; back disabled; screen stays on progress |
| T21-19 | Wrong phrase — generic message, last field clears | functional | P0 | 1. Enter 8 correct words + 1 wrong word<br>2. Submit | Toast/banner: `"That phrase didn't work."` (exact copy); only last field cleared; other 8 preserved; mode and scroll position preserved |
| T21-20 | Wrong phrase — retry preserves other fields | functional | P0 | 1. After T21-19, fix last word<br>2. Submit | If correct: success; if wrong again: only last field clears again |
| T21-21 | Three wrong submissions → 30s throttle | functional | P0 | 1. Submit wrong phrase 3 times<br>2. Observe state | Neutral ink cover: `"Please wait a moment."` + `mono.m` countdown; fields preserved behind wait; `Forgot pattern?` still reachable |
| T21-22 | Throttle uses recovery counter (separate from lock) | security | P0 | 1. Trigger 3 wrong recoveries<br>2. Wait 30s<br>3. Go to `/lock`, draw wrong pattern 5x | Recovery throttle independent; lock throttle unaffected (R1 ladder separate) |
| T21-23 | Throttle persists across restart | security | P0 | 1. Trigger 3 wrong recoveries → throttle active<br>2. Force-stop app<br>3. Relaunch → `/recovery/enter` | Wait continues from persisted deadline; tier/attempt buckets in encrypted prefs |
| T21-24 | Clock rollback re-arms throttle wait | security | P1 | 1. Throttle active with 20s remaining<br>2. Set device clock back 1 hour<br>3. Open `/recovery/enter` | Full current-tier wait re-armed (30s) |
| T21-25 | Clock forward across deadline ends wait | security | P1 | 1. Throttle active<br>2. Set device clock forward past deadline<br>3. Open `/recovery/enter` | Wait ends; grid re-enabled (accepted cost per R1) |
| T21-26 | Open mode success → choice sheet | functional | P0 | 1. Valid phrase in `mode=open`<br>2. Submit | Choice sheet: `Reset pattern` → `/recovery/reset-pattern`, `Open vault` → `/vault` |
| T21-27 | Open mode — choose `Open vault` | functional | P0 | 1. Success → choice sheet<br>2. Tap `Open vault` | Navigates to `/vault`; session starts |
| T21-28 | Open mode — choose `Reset pattern` | functional | P0 | 1. Success → choice sheet<br>2. Tap `Reset pattern` | Navigates to `/recovery/reset-pattern` (interstitial first) |
| T21-29 | Restore mode success → restore progress | functional | P0 | 1. Valid phrase in `mode=restore`<br>2. Submit | Auto-advance to `/restore/progress`; "Restoring…" iris ring |
| T21-30 | Reauth mode success → returns to origin | functional | P0 | 1. Valid phrase in `mode=reauth`<br>2. Submit | Returns to Settings action that requested re-auth |
| T21-31 | Back button clears words, returns to lock/welcome | functional | P0 | 1. Type 5 words<br>2. Press back | Words zeroized; returns to `/lock` (or `/welcome` if restore intent) |
| T21-32 | Process death during entry — words never persist | security | P0 | 1. Type 7 words<br>2. Kill process<br>3. Relaunch | `/lock` (or `/welcome`); no words survive; buffers memory-only |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T21-33 | Empty submit — not possible (button disabled) | edge | P1 | 1. Open `/recovery/enter`<br>2. Try to submit with 0 words | Submit button disabled; no action |
| T21-34 | Partial fill submit — not possible | edge | P1 | 1. Fill 5 of 9 slots<br>2. Try to submit | Submit disabled |
| T21-35 | 8 words in 9-word mode — submit fails validation | edge | P0 | 1. Select `9 words`<br>2. Enter 8 words (last empty)<br>3. Try submit | Submit disabled until all 9 filled |
| T21-36 | 10 words in 9-word mode — impossible (auto-advance stops) | edge | P0 | 1. Select `9 words`<br>2. Try to enter 10th word | 10th slot doesn't exist; input stops at 9 |
| T21-37 | Custom mode — 5 words rejected | edge | P0 | 1. Switch to `Custom`<br>2. Enter 5 words<br>3. Try submit | Error: `"Phrase must contain at least 6 words"`; submit disabled |
| T21-38 | Custom mode — 25 words rejected | edge | P1 | 1. Switch to `Custom`<br>2. Try to enter 25th word | 24 slots max; 25th input ignored |
| T21-39 | Custom mode — weak phrase rejected on submit | edge | P0 | 1. `Custom` mode<br>2. Enter 6 common words (e.g., "the the the the the the")<br>3. Submit | Strength meter shows `Weak`; submit may be disabled or shows inline error per strength estimator |
| T21-40 | Valid words, wrong order — generic error | security | P0 | 1. Take valid phrase, swap two words<br>2. Submit | `"That phrase didn't work."` — no hint about typo vs wrong phrase vs wrong order |
| T21-41 | Extra whitespace normalized | edge | P0 | 1. Enter phrase with double spaces / leading / trailing<br>2. Submit | Normalized (lowercase, single spaces, trim, NFKC); accepted if correct |
| T21-42 | Mixed case normalized | edge | P0 | 1. Enter "The Quiet River Flows..."<br>2. Submit | Normalized to lowercase; accepted if correct |
| T21-43 | Unicode homoglyphs rejected | security | P1 | 1. Enter phrase with lookalike characters (NFKC not in wordlist)<br>2. Submit | Rejected; generic wrong phrase message |
| T21-44 | Numbers/symbols in phrase rejected | edge | P0 | 1. Enter "word1 word2 word3..."<br>2. Submit | Rejected (generated phrases never contain these) |
| T21-45 | Offline mode=restore — submit shows connection error | edge | P0 | 1. Disable network<br>2. `mode=restore`, valid phrase<br>3. Submit | `"Restoring needs a connection."` → S6 state; retry when online |
| T21-46 | Offline mode=open — submit works locally | functional | P0 | 1. Disable network<br>2. `mode=open`, valid phrase<br>3. Submit | Success; choice sheet appears (no network needed) |
| T21-47 | Submit during throttle — ignored | edge | P0 | 1. Trigger throttle (3 wrong)<br>2. Try to submit during wait | Submit disabled; wait cover shows countdown |
| T21-48 | Segmented control accessibility — announced correctly | accessibility | P1 | 1. TalkBack on<br>2. Focus segmented control | "9 words, selected" / "12 words" / "Custom, not selected" |
| T21-49 | Word slot announcement — index + state | accessibility | P0 | 1. TalkBack on<br>2. Focus slot 3 of 9 | `"Word 3 of 9, empty. Type a word"` (idle) or `"Word 3 of 9, filled"` (editing) |
| T21-50 | Paste button announced as hold-to-paste | accessibility | P1 | 1. TalkBack on<br>2. Focus `Paste words` | `"Paste words, double tap and hold to paste from clipboard"` |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T21-51 | FLAG_SECURE set — no screenshot | security | P0 | 1. Open `/recovery/enter`<br>2. Try screenshot (hardware buttons / ADB) | Screenshot blocked / black image |
| T21-52 | FLAG_SECURE — no screen recording | security | P0 | 1. Start screen recording<br>2. Open `/recovery/enter` | Recording shows black / secure content blocked |
| T21-53 | No suggestions / autocorrect on word fields | security | P0 | 1. Open `/recovery/enter`<br>2. Focus word slot<br>3. Check IME | `textNoSuggestions`, `importantForAutofill="no"`, `IME_FLAG_NO_PERSONALIZED_LEARNING` |
| T21-54 | Clipboard menu disabled by default | security | P0 | 1. Long-press word slot (normal tap)<br>2. Check context menu | No paste/cut/copy menu; only hold-to-paste ghost button works |
| T21-55 | Paste clears clipboard immediately | security | P0 | 1. Paste valid phrase<br>2. Check clipboard content | Clipboard empty / cleared |
| T21-56 | Wrong phrase clears only last field — no leak | security | P0 | 1. Submit wrong phrase<br>2. Observe which fields clear | Only last field; first 8 preserved; no field-by-field validation leak |
| T21-57 | Generic error message — no timing leak | security | P0 | 1. Submit wrong phrase<br>2. Measure response time | Constant-time tag verification; same UI delay regardless of which word wrong |
| T21-58 | Cancel disabled during KDF — no interrupt | security | P0 | 1. Submit phrase<br>2. During KDF, press back / home / overview | KDF continues; no cancel; screen stays on progress |
| T21-59 | Background during KDF — derivation cancelled | security | P0 | 1. Submit phrase<br>2. Press home during KDF<br>3. Return to app | Returns to `/lock`; derivation cancelled; buffers zeroized |
| T21-60 | Process death during KDF — no half-unlocked state | security | P0 | 1. Submit phrase<br>2. Kill process during KDF<br>3. Relaunch | `/lock`; no half-unlocked vault (derivation atomic in memory) |
| T21-61 | Throttle counter separate from lock counter | security | P0 | 1. 3 wrong recoveries → throttle<br>2. Go to `/lock`, 5 wrong patterns → lock throttle<br>3. Verify independence | Two independent ladders; recovery throttle doesn't affect lock throttle and vice versa |
| T21-62 | No autofill on word fields | security | P0 | 1. Open `/recovery/enter`<br>2. Check autofill service | `importantForAutofill="no"` on all word fields |
| T21-63 | IME learning disabled | security | P0 | 1. Type words in slots<br>2. Check IME personal dictionary | `IME_FLAG_NO_PERSONALIZED_LEARNING`; words not learned |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T21-64 | TalkBack — screen reader warning on entry | accessibility | P0 | 1. TalkBack enabled<br>2. Open `/recovery/enter` | Butter banner: `"Screen reader active. Your phrase will be spoken aloud. Use headphones."` |
| T21-65 | TalkBack — focus lands on title, not word 1 | accessibility | P0 | 1. TalkBack on<br>2. Enter `/recovery/enter` | Focus on screen title; warning announced first |
| T21-66 | TalkBack — custom action to read all words | accessibility | P1 | 1. TalkBack on<br>2. Focus phrase area | Custom action "Read all words" available |
| T21-67 | A4 200% — slots wrap to one column | adaptive | P0 | 1. Set display size 200%<br>2. Open `/recovery/enter` | Slots stack vertically; hold-to-paste button 60dp reachable; no truncation |
| T21-68 | A5 reduced motion — progress line respects | adaptive | P1 | 1. Enable reduced motion<br>2. Submit phrase | Iris progress line animation reduced/disabled; 10s gate unchanged |
| T21-69 | A6 RTL — slot order logical, numbers LTR | adaptive | P0 | 1. Set RTL locale (Arabic/Hebrew)<br>2. Open `/recovery/enter` | Slot 1 on right (logical first); word numbers in `mono` remain LTR |
| T21-70 | V3 foldable — grid not across hinge | adaptive | P1 | 1. Open on foldable<br>2. Half-folded (tabletop) | Slots in bottom half; not spanning hinge |
| T21-71 | V4 tablet — single centered column | adaptive | P0 | 1. Open on tablet<br>2. Observe layout | Single centered column; slots don't stretch full width |
| T21-72 | V6 multi-window — reachable in split | adaptive | P1 | 1. Split screen<br>2. Open `/recovery/enter` | All controls reachable; hold-to-paste 60dp target |
| T21-73 | Keyboard navigation — arrow keys move between slots | accessibility | P1 | 1. Connect hardware keyboard<br>2. Tab to slot 1<br>3. Arrow right/down | Moves to slot 2; Enter submits when all filled |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T21-74 | Rotation during entry — words preserved | lifecycle | P0 | 1. Type 5 words<br>2. Rotate device<br>3. Check slots | All 5 words preserved; focus maintained |
| T21-75 | Rotation during KDF — progress continues | lifecycle | P0 | 1. Submit phrase<br>2. Rotate during iris progress | Progress continues; no restart; cancel still disabled |
| T21-76 | Background < 5s — return lifts cover | lifecycle | P0 | 1. Submit valid phrase → choice sheet<br>2. Press home < 5s<br>3. Return | Choice sheet still visible; no re-auth |
| T21-77 | Background > 5s — requires re-auth | lifecycle | P0 | 1. Submit valid phrase → choice sheet<br>2. Press home > 5s<br>3. Return | App locks; `/lock` shown; choice sheet gone |
| T21-78 | G4 death during entry — words zeroized | lifecycle | P0 | 1. Type 7 words<br>2. Process death (crash/kill)<br>3. Relaunch | `/lock` or `/welcome`; no words in memory |
| T21-79 | G4 death during KDF — no half-unlocked | lifecycle | P0 | 1. Submit phrase<br>2. Kill during KDF<br>3. Relaunch | `/lock`; atomic derivation — no partial state |
| T21-80 | Restore intent persists across death | lifecycle | P0 | 1. `/` with restore intent pending<br>2. Kill before recovery entry<br>3. Relaunch | `/recovery/enter` with `mode=restore` |
| T21-81 | Deep link to restore — mode=restore | lifecycle | P0 | 1. App link with restore intent<br>2. Cold start | `/recovery/enter` `mode=restore` (after `/` probe) |
| T21-82 | Multiple "Forgot pattern?" taps — single flow | lifecycle | P1 | 1. On `/lock`, tap `Forgot pattern?` rapidly 3x | Single `/recovery/enter` pushed; no duplicate flows |
| T21-83 | Pattern change during recovery — recovery cancelled | lifecycle | P1 | 1. Start recovery entry<br>2. Meanwhile pattern changed (other device/user)<br>3. Submit phrase | Recovery fails (envelope mismatch); generic error |
| T21-84 | Duress pattern on lock — recovery still reachable | security | P0 | 1. Draw duress pattern → decoy vault<br>2. In decoy, tap `Forgot pattern?` | Recovery entry reachable; uses real vault's recovery phrase |

## Security invariants to assert

| # | Invariant | Source |
|---|-----------|--------|
| 1 | `FLAG_SECURE` set on whole screen (Activity/Dialog) — blocks screenshot, recording, remote view | screens.md §21, secret-phrase §249-255 |
| 2 | No suggestions, no autocorrect, no clipboard menu by default on word fields | screens.md §21, secret-phrase §257 |
| 3 | `Paste` only via 1.5s hold ghost action with confirmation; clipboard cleared immediately | screens.md §21, secret-phrase §256 |
| 4 | Generic wrong-phrase message `"That phrase didn't work."` — clears only last field | screens.md §21, §24 row "Recovery wrong", flow 1.8 |
| 5 | Cancel disabled during KDF (800k iterations, no percentage) | screens.md §21, flow 1.8 S15 |
| 6 | Throttle after 3 wrong submissions — 30s wait, R1 ladder on recovery counter | screens.md §21, flow 1.8 R1, §24 row "Rate limited" |
| 7 | Post-success in `mode=open`: choice `Reset pattern` (forces new phrase) or `Open vault` | screens.md §21, flow 1.8 exit points |
| 8 | Post-success in `mode=restore`: auto-advance to `/restore/progress` | flow 1.8 exit points |
| 9 | Post-success in `mode=reauth`: return to origin settings action | flow 1.8 exit points |
| 10 | Typed words memory-only, zeroized on back/G4/background > 5s, never persisted | flow 1.8 G4, secret-phrase §236-243 |
| 11 | Recovery throttle counter separate from lock throttle counter | flow 1.8 R1, edge cases |
| 12 | Throttle persists across restart/force-stop/reboot (encrypted prefs) | flow 1.8 R1 persistence |
| 13 | Clock rollback re-arms full current-tier wait | flow 1.8 R1 clock |
| 14 | Word validation silent until all slots filled (no per-field leak) | flow 1.8 edge cases |
| 15 | Autofill off, IME learning off, `KeyboardType.Password` semantics | flow 1.8 edge cases, secret-phrase §257-258 |
| 16 | Custom mode 6–24 words, strength meter 4 segments labeled (never color-only) | screens.md §21, secret-phrase §197-204 |
| 17 | Segmented control switches mode; switching clears words with confirmation if any filled | flow 1.8 actions |
| 18 | Paste confirmation dialog; empty/non-word clipboard shows error, no fill | flow 1.8 actions |
| 19 | Offline `mode=restore` submit shows `"Restoring needs a connection."` | flow 1.8 states N2 |
| 20 | TalkBack warning banner on entry; focus on title; custom "Read all words" action | flow 1.8 states A2, secret-phrase §247-260 |

## Gaps & contradictions

- **Gap**: screens.md §21 says `9 words` / `12 words` / `Custom` but secret-phrase planning.md only specifies generated = 9 words, custom = 6–24. Where does `12 words` come from? Flow 1.8 mentions `9 words` default, `12 words` option, `Custom` 6–24. Possible BIP39 12-word support not fully specified in secret-phrase doc.
- **Gap**: §24 "Recovery wrong" copy is `"That phrase didn't work."` but secret-phrase AC-18 says `"Phrase not recognized"`. Screens.md is canonical for UI copy.
- **Gap**: Flow 1.8 says `Custom` accepts 6–24 and shows strength meter. Secret-phrase says min 6 for custom. Screens.md doesn't specify min for custom. Assume 6 per secret-phrase.
- **Gap**: Flow 1.8 mentions "strength meter" in Custom mode with 4 segments labeled `Weak / Fair / Good / Strong`. Screens.md §21 doesn't mention strength meter. Flow doc is more detailed.
- **Gap**: §21 says "throttle after 3 attempts" but R1 in flow says recovery counter arms at 3 wrong submissions with 30s wait (tier 1). R1 table shows lock throttle starts at 5th attempt. Recovery throttle is separate but uses identical ladder. Consistent.
- **Contradiction**: Secret-phrase AC-18 error message `"Phrase not recognized"` vs screens.md/flow `"That phrase didn't work."` — use screens.md copy.
- **Gap**: Flow 1.8 says "choice sheet" for `mode=open` but doesn't specify copy for the two buttons. Screens.md §22 Pattern Reset mentions interstitial copy but not choice sheet copy.
- **Gap**: No specification for what happens if user force-closes during the choice sheet (post-success, pre-choice). G4 says relaunch is `/lock` but choice sheet state?
- **Gap**: A4 200% says "hold-to-paste control stays reachable (60 dp button)" but no spec for segmented control scaling at 200%.
- **Gap**: V3 foldable tabletop mode — "grid never placed across hinge" refers to pattern grid; for recovery entry, slots should be in bottom half but no explicit spec.