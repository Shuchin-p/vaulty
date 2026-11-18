# Privacy and Security Info — Test Cases
## Scope — §30 Privacy and Security Info (30.1 Privacy, 30.2 Security Info / Help) from screens.md, using flow docs 04-security-settings.md and 07-system-states.md
## Preconditions — crash reports/diagnostics off by default; activity log on+encrypted; clipboard auto-clear options (30/60/off); export-my-data produces encrypted archive only; no analytics SDK/advertising ID/trackers; threat model document present; FLAG_SECURE on all vault screens per security-architecture invariants; honest limits stated about forensic vault-count estimation and flash residue; no military-grade claims; FLAG_SECURE reality stated (blocks screenshots/recording/casting, does not block another camera pointed at screen)

## Test cases

### 30.1 Privacy (/settings/privacy)

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T30-01 | Privacy screen loads with correct top copy | Happy path | P0 | Open /settings/privacy | Top copy reads exactly: "Vaultaire has no server that can read your vault. There is no account to delete." |
| T30-02 | Crash reports toggle off by default | Happy path | P0 | Observe Privacy screen on fresh install | Crash reports toggle is OFF |
| T30-03 | Diagnostics toggle off by default | Happy path | P0 | Observe Privacy screen on fresh install | Diagnostics toggle is OFF |
| T30-04 | Local activity log on by default, encrypted | Happy path | P0 | Observe Privacy screen on fresh install | Local activity log toggle is ON; tapping it shows "encrypted" subtitle |
| T30-05 | Clipboard auto-clear defaults to 30s | Happy path | P0 | Observe Privacy screen | Clipboard auto-clear selected value is "30s" |
| T30-06 | Hide previews in recents on by default | Happy path | P0 | Observe Privacy screen | Hide previews in recents toggle is ON |
| T30-07 | Export my data produces encrypted archive only | Happy path | P0 | Tap Export my data → confirm → save to Files | Archive file is AES-256-GCM encrypted; file extension not .zip; no plaintext ZIP produced |
| T30-08 | No analytics SDK/advertising ID/trackers stated | Security & deniability | P0 | Read top copy and screen copy | One sentence states: "Vaultaire has no analytics, no advertising ID, and no third-party trackers." No badge wall. |
| T30-09 | Top copy quotes exactly from screens.md | Happy path | P0 | Verify copy appears verbatim | Copy matches: "Vaultaire has no server that can read your vault. There is no account to delete." (character-for-character) |
| T30-10 | Crash reports toggle can be turned on | Edge & error | P1 | Toggle Crash reports ON | Toggle state changes to ON; value persists across config changes |
| T30-11 | Diagnostics toggle can be turned on | Edge & error | P1 | Toggle Diagnostics ON | Toggle state changes to ON; tapping reveals local-encrypted badge |
| T30-12 | Local activity log can be cleared (requires confirmation) | Edge & error | P1 | Tap "Clear local activity log" → confirm | Dialog: "Clear local activity log? This can't be undone." After confirm, log is cleared locally; no network call |
| T30-13 | Clipboard auto-clear can be set to 60s or off | Edge & error | P1 | Select "60s" then "off" from value sheet | Values cycle: 30s → 60s → off → 30s; each selection saves immediately |
| T30-14 | Hide previews in recents can be toggled off | Edge & error | P1 | Toggle Hide previews OFF | Toggle state changes to OFF; recents will show blurred snapshot per §8.1 recents table |
| T30-15 | Export fails when storage full (D3) — shows "Free up space to export" | Edge & error | P2 | Set device storage to ≤50 MB (D3) → Tap Export my data | Butter banner: "Free up space to export"; Retry/Cancel disabled until space freed |
| T30-16 | Export archive never plaintext ZIP | Security & deniability | P0 | Inspect exported file in Files app | File is opened with a viewer that requires recovery phrase; file header confirms AES-256-GCM, not ZIP |
| T30-17 | Diagnostics are local, encrypted, never include keys/phrases/file names | Security & deniability | P1 | Enable Diagnostics → perform action → inspect logs | Log entries contain no recovery phrase, no vault labels, no file names, no pattern coordinates |
| T30-18 | Hide previews toggle respects FLAG_SECURE for recents snapshot | Security & deniability | P1 | Toggle Hide previews ON/OFF → switch to home → app switcher | When ON: solid brand cover in recents (FLAG_SECURE). When OFF: blurred snapshot per §8.1. |
| T30-19 | Process death during export — partial archive deleted, user can retry | Races & lifecycle | P2 | Simulate process death (G4) during S15 Committing state | Partial archive is deleted; user returns to Privacy screen with retry enabled; no corrupt archive left |
| T30-20 | Screen orientation change preserves toggle states | Races & lifecycle | P2 | Open Privacy screen → rotate device → restore | All toggle states (Crash reports, Diagnostics, Clipboard auto-clear, Hide previews) persist; scroll position preserved |
| T30-21 | 200% font scale — row labels wrap, no truncation | Accessibility & adaptive | P1 | Set system font scale to 200% → view Privacy screen | Row labels wrap naturally; value chips stack; "Vaultaire has no server..." copy scrolls without truncation; touch targets remain ≥48dp |
| T30-22 | TalkBack announces all toggles and copy | Accessibility & adaptive | P1 | Launch screen with TalkBack enabled → navigate | Each toggle announcement reads its label and state; top copy announced verbatim; "Export my data" button announced |
| T30-23 | Reduced motion — no unexpected animations | Accessibility & adaptive | P1 | Enable Reduced motion (A5) → toggle Crash reports | Iris progress animation is suppressed; state changes use fade/transition per A5 spec; no spring animations |
| T30-24 | Minimum touch targets ≥48dp for all interactive elements | Accessibility & adaptive | P1 | Measure touch targets | All toggles, buttons, and rows have effective hit target ≥48dp × 48dp per accessible design system |
| T30-25 | High contrast mode — copy remains readable | Accessibility & adaptive | P2 | Enable Android high-contrast → view Privacy screen | Top copy and all row labels remain legible; no color inversion that removes meaning; contrast ratio ≥4.5:1 |

### 30.2 Security Info / Help (/settings/security-info)

| ID | Title | Type | Priority | Steps | Expected |
|---|---|---|---|---|---|
| T30-26 | Security info screen loads with threat model content | Happy path | P0 | Open /settings/security-info | Screen renders; scrollable content visible |
| T30-27 | "What the app protects against" section visible | Happy path | P0 | Scroll to top of threat model | Section heading and body copy are visible, listing coercion, device theft, cloud leakage protection |
| T30-28 | "What the app does not protect against" section visible | Happy path | P0 | Scroll further down | Section heading and body copy are visible, listing forensic vault-count estimation, flash residue after duress wipe, offline pattern guessing against copied indexes, loss of pattern + phrase = permanent loss |
| T30-29 | "Read the full threat model" action opens in-app document | Happy path | P0 | Tap "Read the full threat model" | Local markdown document opens within app with 44dp close button; no network request; copy matches planning.md threat model |
| T30-30 | Threat model copy states honest limits about forensic vault-count estimation | Security & deniability | P0 | Read "What the app does not protect against" | Copy acknowledges: "Forensic vault-count estimation" as a known limit (no fake score or checklist) |
| T30-31 | Threat model copy states honest limits about flash residue after duress wipe | Security & deniability | P0 | Read "What the app does not protect against" | Copy acknowledges: "Flash residue after a duress wipe" as a known limit (no claim of complete erase) |
| T30-32 | Threat model copy states honest limits about offline pattern guessing | Security & deniability | P0 | Read "What the app does not protect against" | Copy acknowledges: "Offline pattern guessing against copied indexes" as a known limit |
| T30-33 | Threat model copy states loss of pattern + phrase = permanent loss | Security & deniability | P0 | Read "What the app does not protect against" | Copy states clearly: "Loss of pattern + phrase = permanent loss" — no ambiguity, no downgrade copy |
| T30-34 | What FLAG_SECURE blocks and does not block | Security & deniability | P0 | Read threat model copy | Copy states: "FLAG_SECURE blocks (screenshots, recording, casting) and does not (another camera pointed at the screen)" — quoted from screens.md §30.2 |
| T30-35 | No "military-grade" claims, no lock illustrations, no fear copy | Security & deniability | P0 | Scan entire screen visually and via screen reader | Zero occurrences of "military-grade", "unbreakable", "100% secure", lock illustrations, fear-inducing copy; per global copy table banned words (07 §10) |
| T30-36 | FLAG_SECURE reality stated on screen | Security & deniability | P1 | View security-info screen; attempt screenshot | System confirms screenshot/recording is blocked on this screen; a toast or banner states: "Screen capture is blocked on this screen." (copy from 07 §10) |
| T30-37 | 200% font scale — threat model text wraps, no truncation | Accessibility & adaptive | P1 | Set font scale to 200% → view security info | All body text wraps; no horizontal scroll; no truncation of key limits (forensic vault-count, flash residue, pattern guessing, permanent loss) |
| T30-38 | TalkBack announces threat model sections | Accessibility & adaptive | P1 | Launch with TalkBack → navigate threat model | Each section heading and body paragraph announced in order; no "missing" content; "Read the full threat model" button announced |
| T30-39 | Process death during threat model read returns to correct state | Races & lifecycle | P2 | Simulate process death (G4) while reading | On resume, security info screen returns to /settings/security with content intact; no stale copy displayed |
| T30-40 | Back navigation returns to security settings | Races & lifecycle | P0 | Tap back arrow / hardware back | User returns to /settings/security; no partial state; scroll position not needed (list screen) |

## Security invariants to assert

- [ ] No analytics SDK, no advertising ID, no third-party trackers stated anywhere on Privacy or Security Info screens — stated in one sentence, not a badge wall
- [ ] Encrypted archive (Export my data) is never a plaintext ZIP — always AES-256-GCM encrypted, requires recovery phrase to open
- [ ] Honest limits about forensic vault-count estimation are stated — acknowledged as a known limit, no scoring, no checklist
- [ ] Honest limits about flash residue after a duress wipe are stated — acknowledged as a known limit, no claim of complete erasure
- [ ] No "military-grade" claims, no lock illustrations, no fear copy on any screen
- [ ] FLAG_SECURE reality is stated — screenshots, screen recording, and casting are blocked; another camera pointed at the screen is not blocked (as copy on 30.2 and 07 §10)
- [ ] No fear copy — per global copy table 07 §10, banned words include "oops", "uh-oh", "error code" (unless in collapsed detail), "military-grade", "unbreakable", "100% secure", "failed" without a next action
- [ ] Diagnostics are opt-in, local, and never include keys, phrases, or file names (per §19 and 04 §4.7)
- [ ] Export archive is encrypted with user's recovery phrase (or derived key); never plaintext (per 04 §4.7 data contract)

## Gaps & contradictions — bullets; flag plan contradictions, never invent features

- [ ] Plan note 07 §7.1: "Solid mode is a legitimate mode, not an error" — ensure Privacy screen does not label Solid preference as "reduced" or "basic" when battery saver forces it; preserve user preference for when it clears.
- [ ] Plan note 07 §2.2: "Never delete user data to make room. Ever." — verify export failure (D3) does not delete existing archive; only blocks write with "Free up space to export" banner.
- [ ] Plan note 04 §652: "Encrypted trash setting is unspecified" — flag that no row exists in Privacy for "7-day encrypted trash"; this is a gap vs. the opt-in setting changing delete semantics everywhere.
- [ ] Plan note 04 §657: "Backup key reveal gate" — the exact flow for revealing backup key after biometric/pattern auth is not specified in screens.md §30; flag for product sign-off.
- [ ] Plan note 01 §R1: "Throttle counting basis needs product sign-off" — with no wrong-pattern state in Privacy, ensure any future attempt throttling is consistent with the 5-KDF/2-min rate defined in 01 §R1.
- [ ] Plan note 01 §R3: "Duress partial-wipe has no durable ledger" — flag that partial wipe residue is only acknowledged in /settings/security-info, but no wipe-ledger design guarantees resume; a crash loop could strand residue silently.
- [ ] Do not invent features not present in planning docs (e.g., no analytics toggle since there is no analytics per §19; no "military-grade" claims per 30.2 invariants).