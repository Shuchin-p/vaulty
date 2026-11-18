# Screen-level Security Matrix — Test Cases
## Scope — screens.md §33 matrix (23 screens) + §3 Pattern Lock deniability policy + planning.md invariants (plausible-deniability, duress, secure-sharing, secret-phrase, security-architecture, 00-state-model)
## Preconditions
- FLAG_SECURE asserted via `WindowManager.LayoutParams.FLAG_SECURE` on Activity/Dialog window before first frame; verified by `adb shell dumpsys window | grep -A2 FLAG_SECURE`
- Solid panel: `AuroraSecureSurface` renders opaque brand cover over sensitive region; no content pixels underneath
- Clipboard policy: `ClipboardManager` primary clip cleared per screen policy (off/normal/hold-to-paste) on `onPause` / `onStop` / 60s timeout; verified by `adb shell am broadcast -a android.intent.action.PASTE`
- Recents cover: `ActivityManager.TaskDescription` with `setTaskDescription` brand drawable; `blur/solid toggle` = user setting (Settings → Security → Recents preview) — brand = solid cover always; blur/solid = follows setting
- Brand recents: Entry, Onboarding pattern, Pattern lock, Viewer, Camera/review, Recovery display/entry, Share phrase display/entry, Duress setup, Shared vault (read-only), QR handoff, Decoy guidance — always solid brand cover
- Blur/solid toggle recents: Vault home, Timeline, Backup/sync, Settings, Pro, Restore progress, Import, My Shares/revoke, Privacy/Security info, Sync activity log — follows user setting
- Deniability: unknown pattern → same 380ms iris glow + neutral empty vault ("Nothing here yet.") — no shake, no red, no error, no counter (screens.md §3.1)
- Duress: pixel-identical to Resolved; no confirmation, no delay, no badge, no unique haptic (screens.md §3.1, duress planning AC-02)
- Accessibility: `FLAG_SECURE` hides content from TalkBack virtual view; `importantForAutofill=no` on secret fields; phrase TextView hidden from accessibility
- Races: background → lock threshold (default 30s) triggers `G3` → solid cover in recents; process death `G4` → cold start to `/lock`; duress wipe `G6` resumes before decoy interactive

## Test cases

### FLAG_SECURE coverage per screen
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T33-01 | No FLAG_SECURE — content capturable | Entry, Vault home, Timeline, Backup/sync, Settings, Pro, New device/restore entry, Restore progress, Import, My Shares/revoke, Privacy/Security info, Sync activity log | security | P1 | 1. Open each screen<br>2. Attempt screenshot / screen record / cast | Capture succeeds; recents per matrix (brand or blur/solid toggle) |
| T33-02 | FLAG_SECURE full screen — capture blocked | Onboarding pattern, Viewer, Camera/review, Recovery display/entry, Share phrase display/entry, Shared vault (read-only), QR handoff, Decoy guidance | security | P0 | 1. Open each screen<br>2. Attempt screenshot / screen record / cast | Capture blocked; recents shows brand cover (matrix: ✅, brand) |
| T33-03 | FLAG_SECURE partial (grid only) — top bar capturable | Pattern lock | security | P0 | 1. `/lock`<br>2. Attempt screenshot / screen record / cast | Grid area blocked; top bar (avatar/help) may capture; recents brand cover (matrix: ✅ (grid), brand) |
| T33-04 | FLAG_SECURE partial (confirm only) — only confirm step blocked | Duress setup | security | P0 | 1. Settings → Duress → confirm pattern<br>2. Attempt screenshot / screen record / cast on confirm step | Confirm step blocked; recents brand cover (matrix: ✅ (confirm), ✅, off, brand) |

### Clipboard policy per screen
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T33-05 | Clipboard unrestricted (normal) | Entry, Vault home, Timeline, Backup/sync, Settings, Pro, Restore progress, Import, My Shares/revoke, Privacy/Security info, Sync activity log | security | P1 | 1. Open screen<br>2. Copy text field<br>3. Background 60s<br>4. Paste | Paste succeeds; clears on lock/background per global policy (matrix: normal or —) |
| T33-06 | Clipboard off — never holds secrets | Onboarding pattern, Pattern lock, Viewer, Camera/review, Duress setup, Shared vault (read-only), QR handoff, Decoy guidance | security | P0 | 1. Open screen<br>2. Background 60s<br>3. Paste | Clipboard empty; no pattern coordinates, media metadata, or secret data ever in clipboard (matrix: off) |
| T33-07 | Clipboard hold-to-paste — copy works, auto-clears 60s | Recovery display/entry, Share phrase display/entry | security | P0 | 1. Open phrase display<br>2. Long-press → Copy<br>3. Background 60s<br>4. Paste | Paste works immediately; clipboard auto-clears after 60s timeout (matrix: off (hold-to-paste)) |
| T33-08 | Share phrase entry — paste allowed, clears on submit | Share phrase display/entry (recipient entry) | security | P1 | 1. Recipient phrase entry<br>2. Copy phrase from trusted source<br>3. Paste into entry fields<br>4. Submit | Paste succeeds; clipboard clears on submit/background (matrix: off (hold-to-paste) for display; entry allows paste) |

### Recents snapshot per screen
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T33-09 | Brand cover — solid brand always | Entry, Onboarding pattern, Pattern lock, Viewer, Camera/review, Recovery display/entry, Share phrase display/entry, Duress setup, Shared vault (read-only), QR handoff, Decoy guidance, New device/restore entry | security | P0 | 1. Open screen<br>2. Press Recents/Overview | Snapshot shows solid brand cover; no content visible (matrix: brand) |
| T33-10 | Blur/solid toggle — respects user setting | Vault home, Timeline, Backup/sync, Settings, Pro, Restore progress, Import, My Shares/revoke, Privacy/Security info, Sync activity log | security | P0 | 1. Settings → Recents preview = Blur<br>2. Open screen → Recents<br>3. Toggle to Solid → repeat | Blur shows blurred thumbnail; Solid shows brand cover (matrix: blur/solid toggle) |

### Deniability invariants (unknown pattern, duress)
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T33-11 | Unknown pattern — same glow + neutral empty vault | Pattern lock | security | P0 | 1. `/lock`<br>2. Draw pattern not matching any vault<br>3. Observe transition | **Same 380ms iris glow and same navigation into a neutral empty vault.** No shake, no red, no error, no counter (screens.md §3.1) |
| T33-12 | Unknown pattern — empty vault shows "Nothing here yet." | Vault home (empty) | security | P0 | 1. After T33-11<br>2. Observe empty state | Empty state shows only "Nothing here yet." + Add photos / Take a photo. **Never** "This vault doesn't exist." (screens.md §4) |
| T33-13 | Unknown pattern — no failed-attempt counter on unlock | Pattern lock | security | P0 | 1. Draw unknown pattern 5x<br>2. Observe UI | No counter increment; rate limit only shows "Please wait a moment." — never "Too many attempts" (screens.md §3.1) |
| T33-14 | Rate limiting — neutral copy only | Pattern lock | security | P1 | 1. Rapid unknown patterns until rate limited<br>2. Observe text | "Please wait a moment." + `mono.m` countdown; neutral ink, never danger; grid disabled (screens.md §3) |
| T33-15 | Duress pattern — pixel-identical to Resolved | Pattern lock | security | P0 | 1. Configure duress pattern<br>2. Draw duress pattern<br>3. Compare frames to normal unlock | **Pixel-identical to Resolved.** No confirmation, no delay, no badge, no unique haptic (screens.md §3.1, duress planning AC-02) |
| T33-16 | Duress — non-decoy vaults wiped before decoy interactive | Pattern lock / Vault home | security | P0 | 1. Duress pattern<br>2. Verify decoy vault opens<br>3. Check internal storage for other vault files | Non-decoy `.dat` files wiped (duress planning AC-03); decoy opens within same budget as normal unlock (state model §3.1) |
| T33-17 | Duress — sync engine paused/isolated | Backup / sync | security | P1 | 1. Duress trigger<br>2. Observe sync state | SyncEngine paused/isolated (duress planning AC-04) |
| T33-18 | Attempt counter — encrypted, survives restart | Pattern lock | security | P1 | 1. Failed attempts (rate limited)<br>2. Kill process<br>3. Relaunch | Counter persists; stored encrypted (screens.md §3 Security) |
| T33-19 | Forgot pattern — neutral, opens phrase entry | Pattern lock | security | P1 | 1. `/lock` → Forgot pattern?<br>2. Observe flow | Neutral copy; opens phrase entry. **Do not say "recover your vault" if the pattern may address no vault.** (screens.md §3 Copy rules) |

### Accessibility & adaptive interactions with security
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T33-20 | FLAG_SECURE hides content from TalkBack virtual view | Pattern lock, Viewer, Recovery display, Share phrase display | security | P0 | 1. Enable TalkBack<br>2. Navigate to secure screen<br>3. Swipe through elements | No pattern dots, media, phrase words announced; only non-secure chrome (avatar, help) reachable |
| T33-21 | Secret fields — importantForAutofill=no, textNoSuggestions | Recovery entry, Share phrase entry | security | P0 | 1. Open phrase entry<br>2. Inspect EditText attributes via UI Automator | `importantForAutofill=no`, `textNoSuggestions` set; no autofill suggestions appear |
| T33-22 | Phrase TextView hidden from accessibility | Recovery display, Share phrase display | security | P0 | 1. Open phrase display<br>2. TalkBack swipe | Phrase words not announced; only "Write this down on paper" instruction reachable |
| T33-23 | Adaptive layout — FLAG_SECURE on grid only in split-screen | Pattern lock | security | P1 | 1. Enter split-screen (two-pane)<br>2. `/lock` in pane<br>3. Attempt screenshot | Grid area blocked; other pane may capture; recents brand cover |
| T33-24 | Adaptive layout — recents cover updates on resize | All brand-cover screens | security | P1 | 1. Resize window (foldable outer/inner)<br>2. Press Recents | Brand cover maintains aspect; no content leak during transition |
| T33-25 | Hardware keyboard — no pattern echo in input fields | Pattern lock | security | P1 | 1. Connect hardware keyboard<br>2. Attempt to type in grid area | No input accepted; grid remains touch-only; no keystroke logging |
| T33-26 | Mouse/hover — no pattern preview on hover | Pattern lock | security | P1 | 1. Connect mouse<br>2. Hover over dots | No dot enlargement on hover; only touch/pointer-down triggers iris line (screens.md §3 States: Drawing) |

### Races & lifecycle
| ID | Title | Screen | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|---|
| T33-27 | Background → lock (screen off) — immediate solid cover | All unlocked screens | security | P0 | 1. Unlocked on any screen<br>2. Screen off<br>3. Screen on → Recents | Recents shows solid brand cover; no content flash (state model §3.1: "Background → lock (screen off) — immediate") |
| T33-28 | Background → lock (home/recents) — 30s threshold | All unlocked screens | security | P0 | 1. Unlocked on Vault home<br>2. Press Home<br>3. Wait 25s → Recents (content may show if blur)<br>4. Wait 35s → Recents | At 25s: blur/solid per setting; at 35s: solid brand cover (state model §3.1: default 30s) |
| T33-29 | Process death during unlock — cold start to /lock | Pattern lock | security | P0 | 1. Draw pattern<br>2. Kill process during KDF<br>3. Relaunch | Opens `/lock`, buffer empty; no half-unlocked vault (state model §6: "Unlock KDF → /lock") |
| T33-30 | Process death during phrase display — phrase lost, re-auth required | Recovery display | security | P0 | 1. Settings → Recovery phrase (display)<br>2. Kill process<br>3. Relaunch → unlock | Phrase not shown; must re-authenticate (state model §6: "Phrase display → Lock → after unlock, home") |
| T33-31 | Process death during camera capture — capture discarded | Camera / review | security | P0 | 1. Camera → capture<br>2. Kill process before review<br>3. Relaunch → unlock | Capture discarded; no unreviewed item in vault (state model §6: "Camera capture (pre-review) → Capture discarded") |
| T33-32 | Duress wipe interrupted — resumes before decoy interactive | Pattern lock / Vault home | security | P0 | 1. Duress pattern<br>2. Kill process during wipe<br>3. Relaunch | Wipe resumes; decoy opens only after wipe complete (state model §6: "Duress wipe → Wipe resumes before the decoy becomes interactive") |
| T33-33 | Screen dims to 10% after 30s inactivity on /lock | Pattern lock | security | P1 | 1. `/lock` idle 30s<br>2. Observe brightness | Screen dims to 10%; never shows content (screens.md §3 Security) |
| T33-34 | MediaProjection detection — neutral notice only on attempt | Viewer, Camera, Recovery display, Share phrase display | security | P1 | 1. Start screen recording<br>2. Open secure screen<br>3. Observe UI | **One neutral line appears — "Screen capture is blocked on this screen." — only when projection is detected, never as an accusation** (flows 03-camera-and-notifications §R3, accessibility.md §7.1) |
| T33-35 | Biometric invalidation — falls back to pattern, no vault destroy | All unlocked screens | security | P1 | 1. Unlock with biometric<br>2. Change biometric enrollment (Settings)<br>3. Return to app | Falls back to pattern lock; vault not destroyed (state model §3.3 #5) |

## Security invariants to assert — checklist
- [ ] **Screenshot blocked** — `FLAG_SECURE` screens return blank/black via `MediaProjection` / `screencap`
- [ ] **Screen recording blocked** — `FLAG_SECURE` screens show black frames in `screenrecord` output
- [ ] **Casting blocked** — `FLAG_SECURE` screens excluded from `MediaRouter` / Cast sessions
- [ ] **Recents snapshot replaced** — brand cover for matrix "brand" screens; blur/solid toggle respected for others
- [ ] **Clipboard cleared per policy** — off screens: empty on background; hold-to-paste: 60s timeout; normal: global policy
- [ ] **Partial-scope screens** — grid only (Pattern lock), confirm only (Duress setup), media only (Viewer, Shared vault), controls only (Camera/review) — FLAG_SECURE region matches matrix annotation
- [ ] **Deniability** — unknown pattern → same glow + neutral empty vault ("Nothing here yet."), no error styling
- [ ] **Duress** — pixel-identical to Resolved; wipe completes before decoy interactive; sync paused
- [ ] **Accessibility** — TalkBack cannot traverse secure content; autofill disabled on secret fields
- [ ] **Adaptive** — FLAG_SECURE and recents cover survive split-screen, fold, resize, rotation
- [ ] **Races** — background/lock thresholds enforced; process death loses secrets; duress wipe atomic

## Gaps & contradictions
- **Matrix vs. flows contradiction**: screens.md §33 marks Pattern lock as `✅ (grid)` for FLAG_SECURE, but flows/03-camera-and-notifications §R3 table shows `/lock` (Pattern lock) as full FLAG_SECURE with solid brand cover. Need to clarify: is top bar (avatar/help) excluded from FLAG_SECURE per §3 "FLAG_SECURE over the grid"?
- **Matrix vs. state model contradiction**: screens.md §33 shows Vault home recents as "blur/solid toggle", but state model §3.2 G3 says "Recents shows cover, not content" — implies brand cover always. Which takes precedence for unlocked vault home?
- **Clipboard policy gap**: Matrix shows "off (hold-to-paste)" for Share phrase display/entry and Recovery display/entry, but secret-phrase planning says "clipboard auto-clear after 60s" — no mention of hold-to-paste requirement. Is hold-to-paste enforced or just long-press copy?
- **Duress setup FLAG_SECURE scope**: Matrix says `✅ (confirm)` and `✅` for Solid panel — but Duress setup screen (not in screens.md detail) likely has pattern confirmation step only. Need screen spec to confirm which region is secured.
- **New device / restore entry recents**: Matrix shows "brand" but this screen may show cloud sign-in UI with email — brand cover may leak that user is restoring. Should this be blur/solid toggle instead?
- **Decoy guidance FLAG_SECURE**: Matrix shows `✅` full screen, but Decoy guidance (screens.md §29) is informational — no secrets displayed. FLAG_SECURE may be overkill; conflicts with accessibility (TalkBack cannot read guidance).
- **Privacy / Security info recents**: Matrix shows "blur/solid toggle" but these screens document FLAG_SECURE behavior honestly (screens.md §19: "FLAG_SECURE blocks (screenshots, recording, casting) and doesn't (camera pointed at screen)"). If they explain FLAG_SECURE, they should demonstrate it — consider making them `✅` with brand cover.
- **Missing screen in matrix**: "Welcome" screen (screens.md §2) not in matrix — first launch, no vault. Should it have brand cover? Currently unclassified.
- **Partial FLAG_SECURE verification**: No automated test for "grid only" vs "full screen" — need pixel diff on recents snapshot to confirm top bar excluded on Pattern lock.