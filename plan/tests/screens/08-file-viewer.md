# File Viewer — Test Cases

## Scope
screens.md §8 File Viewer; flows/02-vault-core.md §2.5 File viewer; components.md §6 Security surfaces; secure-sharing/planning.md export policy & read-only shared vault invariants

## Preconditions
- FLAG_SECURE on Activity/window while media visible; recents shows solid brand cover
- Media types: photo (JPEG/HEIC/PNG), video (MP4/HEVC), document (PDF) — all decryptable
- Decryption failure path: corrupted blob or integrity mismatch → S6 Corrupt
- Export/share policy: owner vault = allowed; read-only shared vault with "Disable export" = actions absent, toast "Export disabled by owner"
- Keyboard/mouse: wheel zoom, arrow keys navigate, `Esc` closes viewer
- Process death (G4): viewer never auto-reopens decrypted item; pending destination honored after unlock only if id resolves

## Test cases

### Happy path

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T08-01 | Open photo from grid | functional | P0 | 1. Unlock vault<br>2. Tap photo tile on `/vault` | Viewer opens at S3 Content; media displayed; metadata "4.2 MB · 4032×3024 · Nov 14" in mono.s; action row with 4 glass circles (info, share, export, delete); delete separated by 12dp gap using danger |
| T08-02 | Open video from timeline | functional | P0 | 1. Unlock vault<br>2. Tap video event on `/vault/timeline` | Viewer opens at S3; 44dp play overlay visible; 4dp scrubber with 44dp hit target; mono.s time readout |
| T08-03 | Double-tap zoom photo | functional | P0 | 1. Open photo<br>2. Double-tap image | Zoom 1× → 2.5×; second double-tap → 1×; clamped 1–5× |
| T08-04 | Pinch/wheel zoom | functional | P0 | 1. Open photo<br>2. Pinch out / mouse wheel up | Continuous zoom clamped 1–5×; releasing under 1× springs back to 1× |
| T08-05 | Pan while zoomed | functional | P0 | 1. Zoom to 3×<br>2. Drag image | Move within bounds; image edge never leaves shorter screen dimension |
| T08-06 | Swipe-down dismiss photo | functional | P0 | 1. Open photo (not zoomed)<br>2. Swipe down | Scale 0.85 backdrop animation; return to caller; FLAG_SECURE cleared; scroll position preserved |
| T08-07 | Play/pause video | functional | P0 | 1. Open video<br>2. Tap 44dp play overlay | Toggles playback; scrubber updates; at end pauses, controls linger |
| T08-08 | Seek video | functional | P0 | 1. Open video<br>2. Drag scrubber | Seeking clamps to [0, duration]; mono.s time updates; scrubbing while paused does not auto-play |
| T08-09 | Next/prev via arrow keys | functional | P0 | 1. Open photo (item 3 of 24)<br>2. Press → arrow key | Moves to item 4; S1 Loading → S3 Content; announcement "Item 4 of 24" |
| T08-10 | Next/prev at edges | functional | P0 | 1. Open first item<br>2. Press ← arrow key | Soft stop (elastic resistance); no wrap; no error; remains on item 1 |
| T08-11 | Info sheet | functional | P0 | 1. Open photo<br>2. Tap (more) → Info | Sheet shows size, dimensions, date in mono.s |
| T08-12 | Share photo (owner vault) | functional | P0 | 1. Open photo in owner vault<br>2. Tap (more) → Share | System share sheet receives decrypted copy; no filename exposed |
| T08-13 | Export photo (owner vault) | functional | P0 | 1. Open photo in owner vault<br>2. Tap (more) → Export | Explicit Export action saves to MediaStore; filename not in system share sheet |
| T08-14 | Delete photo confirm | functional | P0 | 1. Open photo<br>2. Tap delete (danger, 12dp gap)<br>3. Confirm "Delete this item?" | S14 Confirming → S15 → S17 Undo; snackbar "Item moved to trash" 7s with Undo |
| T08-15 | Undo delete | functional | P0 | 1. Delete photo<br>2. Tap Undo within 7s | Item restored; viewer exits to caller; announcement "Item moved to trash" cleared |
| T08-16 | Swipe through shoot (5 items) | performance | P1 | 1. Open item 3<br>2. Swipe next ×5 | Each adjacent decodes in <300ms; S1 → S3 smooth |
| T08-17 | Keyboard navigation: Esc closes | functional | P0 | 1. Open photo<br>2. Press Esc | Viewer closes; returns to caller |
| T08-18 | Keyboard navigation: wheel zoom | functional | P1 | 1. Open photo<br>2. Mouse wheel up/down | Zoom in/out clamped 1–5× |
| T08-19 | Video end state | functional | P0 | 1. Open video<br>2. Let play to end | Playback pauses; controls remain visible; no auto-loop |
| T08-20 | Rotation preserves item & position | functional | P1 | 1. Open video, seek to 0:30<br>2. Rotate device | Item preserved; zoom resets to 1×; play position kept; media letterboxes |

### Edge & error

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T08-21 | Corrupt file → S6 Corrupt | error | P0 | 1. Open item with decryption/integrity failure | Danger banner "We couldn't open this item. It may be damaged." + Retry button; announcement "Couldn't open this item" |
| T08-22 | Retry corrupt file | error | P0 | 1. Trigger S6 Corrupt<br>2. Tap Retry | Re-attempts decrypt + decode only; neighbouring items untouched; → S3 or exit |
| T08-23 | Decoder crash → S7 Fatal | error | P0 | 1. Open item causing hard decoder crash | Full-bleed "This item couldn't open. Your vault is safe." + "Back to vault"; announcement "This item couldn't open" |
| T08-24 | Unsupported codec → S12 | error | P0 | 1. Open video with unsupported codec (export policy allows) | "This file can't be previewed here." + Open with… action; announcement "This file can't be previewed here" |
| T08-25 | Unsupported codec, export disabled | error | P0 | 1. Open unsupported video in read-only shared vault with export disabled | "This file can't be previewed here."; Open with… absent; only exit is Back |
| T08-26 | Deleted under you (sync) | race | P0 | 1. Open photo<br>2. Delete same item from another device/sync | Media clears to ambient backdrop; snackbar "This item was removed." with Undo if locally reversible; pop to caller; no blank screen |
| T08-27 | Auto-lock while viewing | lifecycle | P0 | 1. Open photo<br>2. Wait for auto-lock timeout | S9 Locked: brand cover shown; key zeroized; decrypted bitmap wiped; unlock → /lock |
| T08-28 | Background → recents cover | lifecycle | P0 | 1. Open photo<br>2. Press Home / open recents | Recents shows solid brand cover, never the media frame |
| T08-29 | Process death (G4) while viewing | lifecycle | P0 | 1. Open photo<br>2. Kill process<br>3. Relaunch app | Reopens locked; viewer does NOT auto-reopen decrypted item; pending destination honored after unlock only if id resolves; decrypted bitmap never persisted |
| T08-30 | Deep link unresolved id | lifecycle | P0 | 1. App locked<br>2. Trigger deep link with invalid opaqueId | After unlock → /vault silently (no error) |
| T08-31 | Swipe-down suppressed while zoomed | gesture | P1 | 1. Zoom to 2×<br>2. Swipe down | Pan gesture owns; swipe-down dismiss suppressed |
| T08-32 | Multi-window short height | layout | P1 | 1. Open video in split-screen short window | Media letterboxes; controls accessible; item & position preserved |
| T08-33 | Screen capture attempt | security | P0 | 1. Open photo<br>2. Attempt screenshot / screen record | FLAG_SECURE blocks capture; neutral one-line notice only if attempted; recents shows brand cover |
| T08-34 | Clipboard disabled on viewer | security | P0 | 1. Open photo<br>2. Attempt copy from any field | Clipboard disabled for descendants (AuroraSecureSurface) |
| T08-35 | Autofill/IME learning disabled | security | P0 | 1. Open photo<br>2. Check IME flags | IME_FLAG_NO_PERSONALIZED_LEARNING set; autofill disabled |

### Security & deniability

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T08-36 | Read-only shared vault: export absent | security | P0 | 1. Open shared vault with "Disable export" policy<br>2. Open photo<br>3. Tap (more) | Export action absent (not disabled); Share action absent |
| T08-37 | Read-only shared vault: toast on export attempt | security | P0 | 1. Open shared vault with export disabled<br>2. Attempt export via any vector | Toast "Export disabled by owner" |
| T08-38 | Read-only shared vault: FLAG_SECURE enforced | security | P0 | 1. Open shared vault with export disabled<br>2. Attempt screenshot | FLAG_SECURE blocks; recents shows brand cover |
| T08-39 | Read-only shared vault: delete absent | security | P0 | 1. Open shared vault (read-only)<br>2. Open photo<br>3. Check action row | Delete action absent (not disabled); no 12dp danger gap rendered for missing delete |
| T08-40 | No filename in system share sheet | security | P0 | 1. Open photo<br>2. Tap Share (owner vault)<br>3. Inspect share intent | Intent contains decrypted media; no original filename exposed |
| T08-41 | Decrypted bitmap never in cache/recents/logs | security | P0 | 1. Open photo<br>2. Check disk cache, recents, logcat | No decrypted bitmap persisted; no plaintext temp files |
| T08-42 | Vault label never exposed in viewer | security | P0 | 1. Open photo<br>2. Check all UI text, share intent, logs | Vault label never appears |
| T08-43 | Delete confirmation uses danger tone | security | P0 | 1. Tap delete on owned item | Dialog: "Delete this item?" with danger styling; Keep/Delete options |
| T08-44 | Delete on read-only item: action absent | security | P0 | 1. Open read-only shared vault item<br>2. Check action row | Delete action absent; no danger gap |

### Accessibility & adaptive

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T08-45 | TalkBack: content description on media | a11y | P1 | 1. Open photo with TalkBack | Announces "Item 3 of 24" on S3 Content entry |
| T08-46 | TalkBack: action buttons labeled | a11y | P1 | 1. Open photo<br>2. Navigate to action row | Info, Share, Export, Delete each have content descriptions; Delete announced as danger |
| T08-47 | TalkBack: S6 Corrupt announcement | a11y | P1 | 1. Trigger S6 Corrupt | Announces "Couldn't open this item" |
| T08-48 | TalkBack: S7 Fatal announcement | a11y | P1 | 1. Trigger S7 Fatal | Announces "This item couldn't open" |
| T08-49 | TalkBack: S12 Unsupported announcement | a11y | P1 | 1. Trigger S12 Unsupported | Announces "This file can't be previewed here" |
| T08-50 | Switch access: all actions reachable | a11y | P1 | 1. Open photo with switch access | All actions (zoom, play, seek, info, share, export, delete, next/prev, close) reachable via switch |
| T08-51 | Large font: metadata readable | a11y | P1 | 1. Set font to largest<br>2. Open photo | Metadata "4.2 MB · 4032×3024 · Nov 14" wraps/reads; action row 44dp min touch targets maintained |
| T08-52 | High contrast: danger delete visible | a11y | P1 | 1. Enable high contrast<br>2. Open photo | Delete button uses danger color with 12dp gap; contrast meets WCAG |
| T08-53 | Landscape on foldable: two-pane not used | adaptive | P2 | 1. Open on foldable in landscape | Viewer remains full-screen single pane; no list-detail (Navigation3 Scenes not used for viewer) |
| T08-54 | Desktop window: mouse hover states | adaptive | P1 | 1. Open on desktop ChromeOS<br>2. Hover action buttons | Glass circles show hover ripple; wheel zoom works; arrow keys navigate |

### Races & lifecycle

| ID | Title | Type | Priority | Steps | Expected |
|----|-------|------|----------|-------|----------|
| T08-55 | Rapid next/prev during decode | race | P1 | 1. Open item<br>2. Spam → arrow key during S1 Loading | Generation counter discards in-flight decode; no crash; lands on correct adjacent item |
| T08-56 | Delete during video playback | race | P0 | 1. Play video<br>2. Tap delete → confirm | Playback stops; deletion commits; S17 Undo shown; exits to caller |
| T08-57 | Auto-lock during delete confirmation | race | P0 | 1. Tap delete → S14 Confirming<br>2. Auto-lock triggers | S9 Locked takes priority; on unlock → /lock; delete not committed |
| T08-58 | Background during S1 Loading | lifecycle | P1 | 1. Open item → S1 Loading<br>2. Press Home<br>3. Return to app | Recovers at same state; if decode completes in bg → S3 on resume |
| T08-59 | Configuration change during zoom | lifecycle | P1 | 1. Zoom to 3×<br>2. Rotate / resize window | Zoom resets to 1× (rotated zoom ambiguous); item index & play position kept |
| T08-60 | Undo snackbar dismissed by next delete | race | P1 | 1. Delete item A → S17 Undo showing<br>2. Open item B → delete B | Item A undo window cancelled; Item B delete proceeds; no double-delete |

## Security invariants to assert

- [ ] FLAG_SECURE set on Activity/window while any media is visible (S0–S3, S6, S12, S14)
- [ ] Recents thumbnail shows solid brand cover, never media frame (verified via `ActivityManager.getRecentTasks`)
- [ ] Delete button separated by 12dp gap from other actions; uses danger styling
- [ ] No filename exposed to system share sheet (only explicit Export writes to MediaStore)
- [ ] Decrypted bitmap never written to disk cache, recents, logs, or WorkManager inputs
- [ ] Vault label never appears in viewer UI, share intents, or logs
- [ ] Read-only shared vault: export/share/delete actions absent (not disabled); FLAG_SECURE enforced; toast "Export disabled by owner" on attempt
- [ ] Clipboard disabled for all descendant views (AuroraSecureSurface)
- [ ] Autofill disabled; IME_FLAG_NO_PERSONALIZED_LEARNING set
- [ ] Process death (G4): decrypted bitmap wiped; viewer does not auto-reopen; pending destination resolved after unlock only

## Gaps & contradictions

- **Gap**: screens.md §8 says "No filename exposure to the system share sheet beyond an explicit Export" but flows.md §2.5 Data contract says "Must never appear: filename in the system share sheet (except explicit Export)" — consistent but needs verification that Export uses MediaStore INSERT not ShareSheet
- **Gap**: flows.md §2.5 states "FLAG_SECURE cleared on exit" but components.md §6.1 AuroraSecureSurface applies FLAG_SECURE on host Activity — unclear if viewer uses separate Activity or composable with WindowCompat.setDecorFitsSystemWindows; test T08-28 assumes Activity-level FLAG_SECURE
- **Contradiction**: screens.md §8 shows "(info) (share) (export) (delete)" as 4 actions but §24 "Unsupported file" shows only `Open with…` (if policy allows) — S12 state should have only Back or Open with…, not the full action row; flow §2.5 confirms S12 actions: "Back" only (or Open with…)
- **Gap**: flows.md §2.5 Edge cases mentions "Recents shows the brand cover, not the frame" but no spec for what "brand cover" looks like (color, logo, animation) — components.md §6.1 says "recents thumbnail = solid brand cover"
- **Gap**: secure-sharing/planning.md line 265 says "FLAG_SECURE on Activity" for no-export policy but viewer already has FLAG_SECURE for media — unclear if additional FLAG_SECURE needed or same surface covers both
- **Gap**: flows.md §2.5 says "video uses a 44dp play overlay and a 4dp scrubber" but components.md §6.3 AuroraCameraReview has same controls — viewer extends AuroraCameraReview's controls per §2.5 Flows table "AuroraVideoTransport — extends AuroraCameraReview's controls"
- **Gap**: No spec for PDF/document viewing in viewer — only photo/video mentioned; "document" in preconditions assumes PDF rendering but no controls defined
- **Gap**: flows.md §2.5 Announcement column uses "Item 3 of 24" but no spec for how count is determined (filtered list? collection? all vault?) — test T08-09 assumes list context known