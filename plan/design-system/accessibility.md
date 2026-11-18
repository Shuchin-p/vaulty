# Aurora Accessibility & Secure Surfaces

> Accessibility is not a mode. It is the same interface with fewer assumptions.
> This document also defines the rules for screens that must not be captured, cached, or remembered.

---

## 1. Contrast

Measured against the **worst-case** backdrop: glass at 62% over the darkest canvas (`#C9C3E6` composite ≈ `#E8E8F5`) and against deep pastel tints in Night mode.

| Pair | Ratio | Verdict | Notes |
|---|---|---|---|
| `ink.900` on glass | 14.6:1 | AAA | Titles, numbers |
| `ink.700` on glass | 9.5:1 | AAA | **Default body text** |
| `ink.600` on glass | 7.3:1 | AAA | Secondary text, metadata |
| `ink.500` on glass | 4.4:1 | ⚠️ | Only ≥ 14sp semibold or ≥ 18sp regular |
| `ink.400` on glass | 3.1:1 | ❌ text | Placeholder / decorative only |
| `iris.700` on glass | 6.8:1 | AA | Links, text actions |
| `iris.600` on glass | 4.1:1 | ⚠️ | Icons, focus rings, large glyphs only |
| `#F7F5FB` on `surface.ink` | 16.2:1 | AAA | Primary buttons, nav pill |
| `mint.600` on `mint.100` | 5.6:1 | AA | Success text |
| `danger` on `rose.100` | 6.6:1 | AA | Error text |
| `butter 600` on `butter.100` | 5.6:1 | AA | Warning text |
| `sky 600` on `sky.100` | 6.1:1 | AA | Info text |
| `#F4F1FA` on Night canvas | 15.8:1 | AAA | Night primary text |
| Night mint on deep mint | 7.2:1 | AAA | Night success |
| `iris.300` on Night canvas | 6.4:1 | AA | Night links |

**Rules**

- Normal text (< 18sp regular / < 14sp bold) must reach **4.5:1**. Large text must reach **3:1**.
- Never use color alone for status: pair with an icon and a word.
- Pattern dots: idle vs connected must differ by **size and luminance** (not hue alone). The line adds shape redundancy.
- Focus ring: `iris.600` 2dp + 2dp offset, plus a 1dp `#FFFFFF` inner stroke so it survives on dark media.
- Test with the "grayscale" and "deuteranopia" filters in the preview.

### 1.1 Glass contrast guard

`AuroraGlassCard` samples the backdrop luminance at build time in debug and warns if text contrast < 4.5:1. At runtime, when a card overlaps media, it promotes to `glassRaised` + 12% ink scrim automatically.

**Never** place text directly on `surface.glassSubtle` over media.

---

## 2. Dynamic type

- All text uses `sp`; all containers use `dp` + intrinsic height (no fixed text heights).
- Supported scale: 85% → 200% (Android system max).
- At 150%+:
  - Nav labels hide; icons remain, with content descriptions.
  - Chip rows scroll further; they never wrap into a second line on phone.
  - Hero stats stack 2-up instead of 3-up.
  - Calendar strip shows 5 days with horizontal scroll.
  - Buttons grow in height (min 52 → 60dp), never truncate labels.
- Display text may wrap to 2 lines but is never ellipsized on security copy. User file names may ellipsize at 1 line with full name in the viewer.
- Line height scales proportionally; do not clamp `lineHeight` to a fixed dp.
- Test at 85, 100, 130, 150, 200% in both Glass and Solid.

**Do not use auto-size text** for security instructions; scaling must be predictable.

---

## 3. Focus and input

### 3.1 Order (phone)

| Screen | Order |
|---|---|
| Onboarding pattern | Title → grid → Continue → Back |
| Lock | Help → Grid → Forgot pattern |
| Home | Avatar → Title (static) → Search → More → Filter chips → Hero CTA → Recent items → Banner action → Nav |
| Collection | Back → Share → More → Filters → Hero → File rows |
| Viewer | Back → More → Media (zoomable) → Info → Share → Export → Delete |
| Camera | Back → Flash → Settings → Preview (focus) → Flip → Shutter → Mode |
| Review | Retake → Keep → Media (play) |
| Recovery display | Title → words 1–9 (read-only) → Checkbox → Done → Close |
| Settings | Back → rows top-to-bottom → version |
| Dialog | Title → body → secondary → primary |

- The pattern grid is **one** focusable node; arrow keys / TalkBack swipe explore individual dots via `ExploreByTouchHelper`.
- Do not trap focus in a sheet longer than its content; back and scrim tap dismiss.
- On open, a sheet focuses its title (not the first button) so TalkBack announces context.

### 3.2 Targets

- Minimum 48×48dp; 8dp spacing between adjacent targets.
- Pattern dots: 56dp+ hit area even though the visual dot is 24dp.
- Camera shutter: 76dp visual, 88dp hit.
- Checkboxes: 48dp row target, not just the 20dp box.
- Never place a destructive target within 12dp of a primary target.

### 3.3 Switch / keyboard / pointer

- Full keyboard navigation with visible focus.
- `Tab` order matches visual order; `Esc` closes sheets/dialogs; `Enter` activates.
- Hover states on pointer devices; no hover-only information.
- Trackpad scroll and pinch zoom in the viewer.
- Stylus: pattern drawing accepts stylus with the same smoothing; palm rejection via `MotionEvent`.

---

## 4. TalkBack

### 4.1 Core labels

| Element | Label | Hint |
|---|---|---|
| Pattern grid | "Pattern unlock grid, 5 rows, 5 columns" | "Draw your pattern, or explore dots" |
| Dot | "Dot, row {r}, column {c}, {state}" | "Double tap to add to pattern" |
| Pattern progress | "{n} dots connected" | — |
| Unknown pattern | — (no distinct announcement; proceeds as a normal unlock) | — |
| Invalid new pattern | "Connect at least 6 dots" / "Add at least one turn" | announced assertively |
| Rate limited | "Please wait a moment" | — |
| Vault hero | "{title}, {n} photos, {n} videos, {size} used" | "Double tap to open" |
| Stat | "{value} {label}" | — |
| Media tile | "{type}, {size}, {date}" | "Double tap to open, long press to select" |
| Selected tile | "Selected, {type}, {size}" | "Double tap to deselect" |
| Recovery word | "Word {i} of {n}" | reads the word when focused |
| Secret slot | "Word {i} of {n}, {empty/filled}" | "Type a word" |
| Phrase display | "Recovery phrase, {n} words. Screen readers will read it aloud." | — |
| Camera shutter | "Take photo" / "Start recording" | dynamic |
| Audio badge | "Audio is off. Video will record without sound." | — |
| Share badge | "Read-only shared vault" | — |
| Pro badge | "Pro feature" | "Double tap to learn more" |
| Duress row | "Duress mode" | no hint that reveals behavior in public |
| Backup ring | "Backup complete, 2 hours ago, 1.8 gigabytes" | — |
| Timeline event | "{action}, {time}, {count} items" | "Double tap to open" |

### 4.2 Announcements

| Event | Announcement | Priority |
|---|---|---|
| Pattern success | "Vault unlocked" | assertive |
| Unknown pattern | "Vault unlocked" (identical) | assertive |
| Pattern creation invalid | "Connect at least 6 dots" | assertive |
| Rate limit | "Please wait a moment" | assertive |
| Capture kept | "Saved to vault" | polite |
| Capture discarded | "Photo discarded" | polite |
| Share revoked | "This share is no longer available" | assertive |
| Backup complete | "Backup complete" | polite |
| Sync conflict | "Three items need review" | polite |
| Phrase reveal | "Recovery phrase shown. Screen capture is blocked." | assertive |
| Duress | **No announcement beyond the normal unlock.** | — |

### 4.3 Screen reader and secrets

A screen reader necessarily reads secrets aloud. Handle it honestly:

- On entering a phrase screen with TalkBack active, show a butter banner: "Screen reader active. Your phrase will be spoken aloud. Use headphones."
- **Do not suppress the words.** This is a deliberate deviation from the legacy plan's "hide phrase from accessibility services": hiding a recovery phrase from the only users who cannot read it is not acceptable. Warn, then read.
- Each word is a separate semantics node with index + word. Provide a "Read all words" custom action.
- Do not auto-focus the first word; focus the warning/title first.
- The "I wrote it down" checkbox is a real checkable node with state. The 10-second minimum delay applies to everyone; it is not an accessibility barrier.

---

## 5. FLAG_SECURE and capture protection

### 5.1 Matrix

| Surface | FLAG_SECURE | Recents | Screen record | Cast | Notes |
|---|---|---|---|---|---|
| Pattern lock (grid) | ✅ | solid brand cover | blocked | blocked | Whole activity may be secure |
| Pattern setup | ✅ | cover | blocked | blocked | — |
| Recovery display/entry | ✅ | cover | blocked | blocked | Solid panel only |
| Share phrase display/entry | ✅ | cover | blocked | blocked | — |
| Camera preview | ✅ | cover | blocked | blocked | Also blocks MediaProjection |
| Camera review | ✅ | cover | blocked | blocked | No thumbnails |
| File viewer (media) | ✅ | cover | blocked | blocked | Metadata bar may be visible in-app only |
| Duress setup confirm | ✅ | cover | blocked | blocked | — |
| Vault home / settings | ❌ | blur or solid per setting | allowed | n/a | User can enable "Hide previews" |

### 5.2 Implementation

- `window.setFlags(FLAG_SECURE, FLAG_SECURE)` is applied per destination in the Compose host (`DisposableEffect`), not globally, so home/settings can support legit screenshots.
- On Android 13+, also set `WindowManager.LayoutParams.FLAG_SECURE` before first frame to avoid a one-frame leak.
- Do not cache sensitive bitmaps in the app switcher. Clear the `DecorView` snapshot by setting a solid `window.setBackgroundDrawable` cover on `onPause` for secure screens.
- Block `MediaProjection` capture on camera/recovery screens; detect and show a neutral notice if projection is active. **No accusation, no detail.**
- The clipboard: for secrets, disable the selection toolbar; if copy is explicitly allowed (share phrase), mark the clip with `ClipDescription.EXTRA_IS_SENSITIVE` (API 33) and auto-clear after 60s.
- Disable autofill and personalized IME learning on secret fields (`IME_FLAG_NO_PERSONALIZED_LEARNING`).
- Keyboard: use `KeyboardType.Password`/`VisiblePassword` for secrets; disable suggestions and spellcheck.
- Never log field contents, even in debug. Strip secrets from crash reports.

### 5.3 Notification and recents previews

- Notification content never includes file names or vault labels.
- Recents: default is a blurred snapshot for non-secure screens, a solid brand cover for secure screens. "Hide previews" setting forces solid everywhere.
- The app never writes decrypted media to the cache directory. Thumbnails are decrypted in memory and drawn directly.

---

## 6. Reduced motion and vestibular safety

| Motion | Default | Reduced |
|---|---|---|
| Ambient bloom drift | 45s loop | Off |
| Screen enter (12dp translate) | 240ms | 120ms fade only |
| Sheet slide | 440ms | 120ms fade |
| Pattern success glow | 380ms scale/glow | 120ms color change |
| Pattern error shake | 420ms, 3 oscillations | Single 200ms rose border pulse |
| Nav pill morph | spring | 120ms crossfade |
| Parallax on media | subtle | Off |
| Confetti/celebration | not used | — |
| Duress | **instant** | **instant** |

- Respect `Settings.Global.ANIMATOR_DURATION_SCALE` and `TRANSITION_ANIMATION_SCALE`.
- Also expose an in-app toggle: Settings → Appearance → Reduce motion.
- No autoplay video, no looping hero video, no flashing > 3 Hz.

---

## 7. Haptics

| Event | Effect |
|---|---|
| Button / chip tap | `EFFECT_CLICK` |
| Toggle | `EFFECT_TICK` |
| Pattern dot connect | `EFFECT_CLICK` per dot |
| Pattern success | `EFFECT_HEAVY_CLICK` ×1 |
| Pattern failure | `EFFECT_HEAVY_CLICK` ×2 @ 80ms |
| Shutter | `EFFECT_CLICK` |
| Video start/stop | `EFFECT_DOUBLE_CLICK` |
| Keep / save | `EFFECT_TICK` |
| Destructive confirm | `EFFECT_HEAVY_CLICK` |
| Duress | **none** |
| Scroll / ambient / Pro badge | none |

- Haptics respect system touch-feedback settings; never required to understand state.
- Provide a **Haptics** toggle in Appearance for users who find them distracting.

---

## 8. Cognitive accessibility

- One primary action per screen.
- Plain language: "That pattern didn't work" not "Authentication failed."
- No time pressure except lockout, which shows a visible countdown.
- No memory tests beyond the pattern itself.
- Destructive actions are reversible where possible: delete moves to an encrypted trash for 7 days (opt-in), except duress.
- Pro features remain visible and explained; nothing is hidden behind an unexplained lock.
- Keep copy ≤ 2 sentences per block; use `body.m` at 15sp minimum.

---

## 9. Localization and RTL

- All strings externalized; no concatenated sentences.
- Use `start`/`end`; mirror chevrons and back arrows.
- Pattern grid mirrors column order in RTL; dot numbering mirrors too.
- Camera controls mirror horizontally, but the shutter stays centered.
- Numbers remain LTR within RTL text (`mono` + bidi isolation).
- Word tiles wrap per locale; phrase length may differ for custom phrases.
- Avoid fixed-width buttons; German and Finnish labels are long.

---

## 10. Testing matrix

### 10.1 Modes

| Dimension | Values |
|---|---|
| Surface | Glass, Solid |
| Theme | Light, Night |
| Font scale | 85, 100, 130, 150, 200% |
| Motion | Full, Reduced |
| Width | 320, 360, 412, 600, 840, 1280dp |
| Input | Touch, keyboard, TalkBack, switch access |
| Security | FLAG_SECURE on/off (debug), screen reader on/off |

### 10.2 Must-pass checks

- [ ] No text clipped at 200% on lock, recovery, share, camera review.
- [ ] All targets ≥ 48dp (measured in Layout Inspector).
- [ ] All text passes 4.5:1 in Glass and Solid, Light and Night.
- [ ] Pattern grid usable with TalkBack and with keyboard only.
- [ ] Pattern creation errors are announced; unlock has no failure announcement (deniability).
- [ ] Unknown pattern and empty vault screens are structurally identical apart from the grid transition.
- [ ] Duress and normal unlock produce identical screenshots (automated pixel diff).
- [ ] Decoy and primary vault home produce identical screenshots given identical content.
- [ ] FLAG_SECURE verified on all secure screens via `adb shell dumpsys window`.
- [ ] Recents shows a solid cover on secure screens.
- [ ] No secret appears in logcat, crash reports, or clipboard history.
- [ ] Reduced motion removes ambient drift and shake.
- [ ] Solid mode has zero blur calls.
- [ ] 44dp camera controls remain reachable one-handed in landscape.
- [ ] TalkBack announces every state change in §4.2.
- [ ] App works with a screen reader at 200% font scale without overlap.

### 10.3 Automated pixel-diff guardians

1. `lock_normal.png` vs `lock_duress.png` → **0 differing pixels** (excluding clock).
2. `home_primary.png` vs `home_decoy.png` with identical fixtures → **0 differing pixels**.
3. `empty_existing.png` vs `empty_nonexistent.png` → **0 differing pixels**.

These tests protect the security promises more than any visual polish.
