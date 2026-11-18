# Recovery Phrase Display — Test Cases
## Scope
§20 Recovery Phrase Display (screens.md) + Flow 1.9 Recovery Phrase Re-display (01-access-and-recovery.md) + Secret Phrase planning.md + Pattern Encryption planning.md

## Preconditions
- App has a vault (pattern committed, recovery phrase envelope stored)
- Entry via `/settings/security` → `Recovery phrase` → `Reveal` after fresh biometric OR pattern re-auth
- 9-word phrase (generated) or 12-word phrase (custom) in memory only; never persisted plaintext
- `FLAG_SECURE` set on Activity/Dialog before first frame render
- Background timer threshold: 5 s → solid cover; 30 s inactivity → auto-lock to settings
- Device locked > 5 s → next reveal requires full re-auth (pattern/biometric)

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T20-01 | Reveal opens after pattern re-auth | functional | P0 | 1. Unlock vault with pattern<br>2. Open Settings → Security → Recovery phrase<br>3. Tap Reveal<br>4. Draw current pattern correctly | Screen renders at `/recovery/display` with 9-word phrase in 3×3 tiles, `mono.m` 22sp, index `mono.s`, solid `Plain` background, no glass/blur/elevation |
| T20-02 | Reveal opens after biometric re-auth | functional | P0 | 1. Unlock vault with pattern<br>2. Open Settings → Security → Recovery phrase<br>3. Tap Reveal<br>4. Authenticate with enrolled biometric | Same render as T20-01; biometric success path identical timing class to pattern |
| T20-03 | 12-word custom phrase renders 2×6 grid | functional | P0 | 1. Create vault with 12-word custom phrase<br>2. Re-auth and reveal | 2 rows × 6 columns, same typography and `Plain` background |
| T20-04 | "I wrote it down" checkbox announces state | functional | P1 | 1. Reveal phrase<br>2. Tap checkbox | Checkbox toggles; TalkBack announces "I wrote it down, checked/unchecked" |
| T20-05 | "I've saved it" enabled after checkbox + 10 s | functional | P0 | 1. Reveal phrase<br>2. Wait 9 s → tap button<br>3. Wait 11 s → check checkbox → tap button | Button disabled until both conditions met; after 10 s + checked → enabled |
| T20-06 | Two-step save confirm: first dialog copy | functional | P0 | 1. Reveal → check checkbox → wait 10 s → tap "I've saved it" | Dialog shows: "Did you write down all 9 words?" (or 12) with [Cancel] [Yes, I have them] |
| T20-07 | Two-step save confirm: second tap exits | functional | P0 | 1. Reveal → check checkbox → wait 10 s → tap "I've saved it" → tap "Yes, I have them" | Returns to Settings → Security; plaintext zeroized |
| T20-08 | Close before confirm shows leave dialog | functional | P0 | 1. Reveal phrase<br>2. Tap Close (or system back) | Dialog: "Leave this screen? Without the phrase you can lose access to your vault." + checkbox "I understand I can lose access" |
| T20-09 | Leave confirmed exits to settings | functional | P0 | 1. Reveal → Close → check loss checkbox → tap Confirm | Returns to Settings → Security; plaintext zeroized |
| T20-10 | Leave cancelled returns to phrase | functional | P1 | 1. Reveal → Close → tap Cancel | Phrase still visible; no re-auth required |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T20-11 | Rate limited: throttle copy + countdown | functional | P0 | 1. Trigger recovery throttle (3 wrong submissions at `/recovery/enter`)<br>2. Attempt reveal from settings | Screen shows neutral "Please wait a moment." + `mono.m` countdown; no phrase rendered |
| T20-12 | Background 4.9 s → return lifts cover | lifecycle | P0 | 1. Reveal phrase<br>2. Home at 4.9 s<br>3. Return immediately | Cover lifts; phrase still visible; no re-auth |
| T20-13 | Background 5.1 s → return requires re-auth | lifecycle | P0 | 1. Reveal phrase<br>2. Home at 5.1 s<br>3. Return | App shows `/lock` with `reason=timeout`; phrase zeroized |
| T20-14 | Background 30 s inactivity → auto-lock | lifecycle | P0 | 1. Reveal phrase<br>2. Do nothing for 30 s | Auto-locks to `/lock`; phrase zeroized; return requires pattern |
| T20-15 | Process death (G4) during display → `/lock` | lifecycle | P0 | 1. Reveal phrase<br>2. Kill process (adb shell am kill)<br>3. Relaunch | Cold start → `/lock`; phrase never re-rendered |
| T20-16 | Rotation preserves phrase + timer + checkbox | lifecycle | P1 | 1. Reveal → check checkbox → rotate<br>2. Verify phrase, checkbox, elapsed timer | All state survives; 10 s gate continues |
| T20-17 | Foldable: grid never spans hinge | adaptive | P1 | 1. Open on foldable (tabletop)<br>2. Reveal phrase | Grid confined to one half; tiles wrap per A4 200% rules |
| T20-18 | Tablet (V4): single centered column | adaptive | P1 | 1. Open on tablet<br>2. Reveal phrase | Single centered column; tiles wrap per tile; notice never truncates |
| T20-19 | 200% font: tiles wrap, page scrolls | accessibility | P1 | 1. Set display size 200%<br>2. Reveal phrase | Tiles wrap to one column; vertical scroll; notice fully visible |
| T20-20 | Reduced motion: no animation changes | accessibility | P1 | 1. Enable reduced motion<br>2. Reveal → Close dialog | 10 s gate unchanged (not animation); dialog appears without motion |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T20-21 | FLAG_SECURE blocks screenshot | security | P0 | 1. Reveal phrase<br>2. Take screenshot (hardware buttons or adb) | Screenshot is black/blank; phrase not captured |
| T20-22 | FLAG_SECURE blocks screen recording | security | P0 | 1. Reveal phrase<br>2. Start screen recording (MediaProjection) | Recording shows black/blank for this screen |
| T20-23 | FLAG_SECURE blocks remote desktop view | security | P0 | 1. Reveal phrase<br>2. View via scrcpy/remote desktop | Remote view shows black/blank |
| T20-24 | No copy button in UI | security | P0 | 1. Reveal phrase<br>2. Inspect UI hierarchy | No copy/share button present; no clipboard action |
| T20-25 | Long-press selection disabled on words | security | P0 | 1. Reveal phrase<br>2. Long-press any word tile | No text selection handles; no copy/paste menu; no clipboard fill |
| T20-26 | Autofill disabled on phrase screen | security | P0 | 1. Reveal phrase<br>2. Trigger autofill service | No autofill suggestions; `importantForAutofill="no"` on word views |
| T20-27 | IME learning disabled | security | P0 | 1. Reveal phrase<br>2. Check IME flags | `textNoSuggestions` + `IME_FLAG_NO_PERSONALIZED_LEARNING` |
| T20-28 | App switcher shows solid cover | security | P0 | 1. Reveal phrase<br>2. Open recents/overview | Recents thumbnail is solid brand cover; no phrase visible |
| T20-29 | Phrase never in plaintext storage | security | P0 | 1. Reveal phrase<br>2. Dump app data (root/backup) | No plaintext phrase in SharedPreferences, DB, files, Keystore; only encrypted envelope |
| T20-30 | Re-derivation from wrapped key only | security | P0 | 1. Reveal phrase<br>2. Trace key derivation | Phrase derived in memory from pattern/biometric-wrapped key; never read from plaintext store |
| T20-31 | Biometric invalidation → pattern required | security | P0 | 1. Reveal via biometric<br>2. Add new fingerprint in system settings<br>3. Reveal again | Next reveal requires pattern (biometric row absent per R2) |
| T20-32 | Duress pattern at lock → decoy, no phrase reveal | security | P0 | 1. Lock app<br>2. Draw duress pattern<br>3. Open Settings → Recovery phrase | Decoy vault opens; no recovery phrase entry possible (decoy has no phrase) |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T20-33 | TalkBack entry: warn-then-read banner | accessibility | P0 | 1. Enable TalkBack<br>2. Reveal phrase | Butter banner: "Screen reader active. Your phrase will be spoken aloud. Use headphones." announced before any word |
| T20-34 | TalkBack focus lands on title, not word 1 | accessibility | P0 | 1. Enable TalkBack<br>2. Reveal phrase | Focus on "Your recovery phrase" title; custom action "Read all words" available |
| T20-35 | Custom action reads all 9/12 words in order | accessibility | P0 | 1. Enable TalkBack<br>2. Reveal → trigger "Read all words" | Words announced sequentially with indices: "1 garden, 2 silver, 3 orbit..." |
| T20-36 | High contrast: tiles meet 4.5:1 ratio | accessibility | P1 | 1. Enable high contrast<br>2. Reveal phrase | Word tiles and indices meet WCAG AA contrast on `Plain` background |
| T20-37 | RTL: slot order logical, numbers LTR | accessibility | P1 | 1. Set RTL locale (ar/he)<br>2. Reveal phrase | Word 1 first visually; index numbers remain LTR in `mono` |
| T20-38 | Switch access: all controls reachable | accessibility | P1 | 1. Enable Switch Access<br>2. Reveal phrase | Checkbox, "I've saved it", Close all selectable via switch scanning |
| T20-39 | Keyboard navigation: Tab order correct | accessibility | P1 | 1. Connect keyboard<br>2. Reveal phrase → Tab | Tab order: Close → checkbox → "I've saved it" → word tiles (arrow keys) |
| T20-40 | A4 200%: notice never truncates | accessibility | P0 | 1. Set display 200%<br>2. Reveal phrase | "This is the only way back in. We can't reset it." fully visible, wrapped |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T20-41 | Race: background at 4.9 s → return no re-auth | race | P0 | 1. Reveal<br>2. Home at exactly 4.9 s<br>3. Return instantly | Cover lifts; phrase visible; timer continues |
| T20-42 | Race: background at 5.0 s → kill → relaunch | race | P0 | 1. Reveal<br>2. Home at 5.0 s<br>3. Kill process<br>4. Relaunch | Cold start → `/lock`; phrase zeroized on background |
| T20-43 | Race: biometric prompt during reveal | race | P1 | 1. Reveal phrase<br>2. System biometric prompt appears (enrollment change) | Prompt handled; if auth fails → phrase zeroized, return to settings |
| T20-44 | Race: config change during 10 s gate | race | P1 | 1. Reveal → check checkbox at 5 s<br>2. Rotate<br>3. Verify button still disabled until 10 s | 10 s gate survives rotation; button enables at correct elapsed time |
| T20-45 | Race: two rapid Close taps | race | P1 | 1. Reveal → double-tap Close rapidly | Single leave dialog; no duplicate dialogs |
| T20-46 | Race: Save confirm + background simultaneously | race | P1 | 1. Reveal → tap "I've saved it" → immediate Home | If confirm completes before 5 s → settings; if after → `/lock` |
| T20-47 | G4 during two-step confirm: first dialog | race | P0 | 1. Reveal → tap "I've saved it" → kill at first dialog<br>2. Relaunch | `/lock`; phrase zeroized; no confirm persisted |
| T20-48 | G4 during two-step confirm: second dialog | race | P0 | 1. Reveal → "Yes, I have them" → kill during commit<br>2. Relaunch | `/lock`; phrase zeroized; commit either completes or rolls back atomically |
| T20-49 | Deep link to reveal without auth → denied | security | P0 | 1. Send intent to `/recovery/display` directly<br>2. App cold start | Redirects to `/lock` with `reason=reauth`; no phrase shown |
| T20-50 | Notification tap during reveal → ignored | race | P1 | 1. Reveal phrase<br>2. Tap backup notification | Notification opens settings after unlock; reveal unaffected |

## Security invariants to assert

- [ ] `FLAG_SECURE` set on Window before first frame (screenshot/recording/remote blocked)
- [ ] Solid `Plain` panel — no glass, blur, elevation, or translucency
- [ ] No copy button, no share action, no screenshot affordance in UI
- [ ] Long-press text selection disabled on all word tiles
- [ ] Autofill disabled (`importantForAutofill="no"`), IME learning off
- [ ] App switcher/recents shows solid brand cover from first frame
- [ ] Two-step confirm exact copy: "Did you write down all 9 words?" → "Yes, I have them"
- [ ] Leave dialog exact copy: "Leave this screen? Without the phrase you can lose access to your vault."
- [ ] Phrase never retrievable in plaintext from any persistent storage (Keystore, SharedPrefs, DB, files)
- [ ] Phrase re-derived in memory only from pattern/biometric-wrapped key on each reveal
- [ ] Background > 5 s → solid cover; return requires pattern/biometric re-auth
- [ ] 30 s inactivity → auto-lock to `/lock`; phrase zeroized
- [ ] Process death (G4) during display → phrase never re-rendered on relaunch
- [ ] Rate limited state shows neutral "Please wait a moment." + `mono.m` countdown

## Gaps & contradictions

- **Copy button contradiction**: secret-phrase planning.md §UX shows `[Copy]` button with "Copied — clears in 60s" toast, but screens.md §20 explicitly states "No copy button. No screenshot. No share." — screens.md is canonical; copy button must not exist
- **Auto-lock duration mismatch**: secret-phrase planning.md says "Auto-lock on inactivity: 30s countdown timer", but screens.md §20 and flow §1.9 specify 5 s background cover + 30 s inactivity; both apply (5 s cover, 30 s full lock)
- **Clipboard auto-clear**: secret-phrase planning.md mentions clipboard auto-clear (60 s) but screens.md forbids copy entirely — no clipboard interaction possible
- **Phrase word count**: screens.md §20 shows 9 words (3×3); flow §1.9 says "all 9 words" in confirm dialog; secret-phrase planning.md allows 12-word custom — confirm dialog must dynamically say "9" or "12"
- **Biometric re-auth**: flow §1.9 says "Biometric or pattern re-entry just passed"; R2 in flow says biometric never offered during throttle wait — reveal entry must respect throttle
- **Duress at reveal**: no explicit duress handling for reveal screen; duress only defined at `/lock` — if user under duress at reveal, no escape path documented
- **Custom phrase 6–24 words**: secret-phrase allows 6–24; screens.md §20 only shows 9 (3×3) or 12 (2×6) — layout for other counts unspecified
- **Strength meter on reveal**: secret-phrase shows strength meter for custom entry; not mentioned for display — not needed on display