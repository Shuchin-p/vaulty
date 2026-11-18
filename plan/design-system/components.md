# Aurora Components

> Implementation-ready specification for every reusable surface in Vaultaire Android.
> All components are Compose-first, token-driven, and must render in **Glass / Solid / Night**.

Legend: **L** = light, **N** = night, **G** = glass, **S** = solid.

---

## 1. Structure

### 1.1 `AuroraBackground`

**Purpose:** the lavender world. One per screen, drawn behind everything, never interactive.

```
Canvas
├── vertical wash (canvas.top → base → bottom)
├── bloom A: 420dp ellipse, iris.200 @ 55%, blur 80, top-right, drifts 12dp / 45s
├── bloom B: 320dp ellipse, rose.100 @ 45%, blur 72, bottom-left, drifts 8dp / 45s
├── orbit lines: 2–3 cubic curves, 1dp canvas.orbit, no fill
└── vignette: radial canvas.vignette at edges
```

| Prop | Type | Default |
|---|---|---|
| `variant` | `Orbit \| Bloom \| Plain` | `Orbit` |
| `ambientMotion` | `Boolean` | `true` (auto-off in reduced motion) |
| `content` | `@Composable BoxScope.() -> Unit` | — |

Rules: blooms never sit behind body text at > 55% opacity; `Plain` is used behind camera, sheets, and secure panels.

### 1.2 `AuroraTopBar`

```
[avatar 40] [eyebrow + title]            [icon 44] [icon 44]
```

- Height 56dp; horizontal padding 20dp.
- Title uses `headline.m`; optional eyebrow `overline` in `ink.500`.
- Round actions are 44dp `surface.glassRaised` circles with `ink.700` icons.
- Avatar is 40dp, ring 2dp `glassRaised`; defaults to an abstract gradient monogram — **never a real user photo by default**.
- On scroll, title collapses into a 20dp centered label over `glassRaised`; no elevation jump.

### 1.3 `AuroraNavBar` (phone)

Floating capsule, 68dp tall, inset 16dp, `radius.3xl`, `glassRaised`, `elevation.3`.

```
 ( All )   ( Vault )   [ Camera ]   ( Shares )   ( Settings )
```

- 3–5 destinations. Active destination = ink pill 52×44 with icon + optional label.
- Inactive = 24dp line icon + `label.m` in `ink.600`, stacked, 4dp gap.
- Center camera action is **not** the nav pill; it is a separate `AuroraFab` that overlaps the capsule by 12dp and uses `surface.ink`.
- Hides on scroll down (`translationY = height + inset`), returns on scroll up. Never hides on lock, camera, or secure screens.
- In Solid mode, capsule becomes `surface.solidRaised`; active pill stays ink.

### 1.4 `AuroraNavRail` (tablet ≥ 600dp)

84dp wide, `glassRaised`, 28dp radius, 16dp margin. Icon + label destinations vertically; active item is an ink pill 64×56. The rail never floats over content; content is inset.

### 1.5 `AuroraSheet`

- Top corners `radius.4xl`, bottom 0; fill `glassRaised`; blur 40; scrim `scrim`; drag handle 36×4 `ink.200`.
- Enter 440ms `easing.enter`; exit 240ms; drag tracks finger.
- Peek height 96dp, half 45%, full 92%. Never taller than the safe area.
- Content padding 24dp; actions pinned to bottom with a 1dp hairline above.

### 1.6 `AuroraDialog`

- Width min(360dp, screen − 48dp); `radius.3xl`; `surface.solidRaised`; `elevation.4`.
- Icon well 44dp glass circle, title `headline.m`, body `body.m`, actions right-aligned pills.
- Destructive action uses `danger` text on glass, not a red fill.
- Secure dialogs (phrase reveal, duress confirm) are **solid, non-blurred, FLAG_SECURE**.

### 1.7 `AuroraScaffold`

Handles insets, background, snackbar (glass), and bottom bar. Provides `contentPadding`; screens must consume it. No screen draws its own system bar padding.

---

## 2. Actions

### 2.1 `AuroraButton`

| Variant | Fill | Label | Border | Use |
|---|---|---|---|---|
| `Primary` | `surface.ink` | `#F7F5FB` | none | One per screen: Unlock, Keep, Save |
| `Secondary` | `glassRaised` | `ink.900` | 1dp `surface.border` | Cancel, Retake, Not now |
| `Ghost` | transparent | `ink.700` | none | Tertiary actions |
| `Danger` | `rose.100` | `danger` | 1dp rose.300 | Delete vault, Revoke share |
| `Pro` | `surface.ink` + iris spark | `#F7F5FB` | none | Upgrade (never blocks data) |

- Height 52dp (large), 44dp (medium), 36dp (small). Full pill.
- Horizontal padding 24dp; icon 20dp with 8dp gap.
- Disabled: 38% opacity, no shadow, `ink.300` label.
- Pressed: scale 0.97, `spring.snappy`; no ripple color beyond 8% white.
- Loading: label swaps to a 18dp iris spinner; width is preserved.
- **Never** put two primary buttons on one screen. **Never** place Delete next to Keep.

```kotlin
AuroraButton(
    text = "Keep",
    onClick = { keep() },
    variant = AuroraButtonVariant.Primary,
    leadingIcon = Icons.Rounded.Check,
    loading = isEncrypting,
)
```

### 2.2 `AuroraIconButton`

44dp glass circle (40dp in dense rows). Icon 20dp. Hover/press: fill 1.06× scale, `glassRaised` → `solid`. Content description required. On camera overlays, use ink 70% background and white icon.

### 2.3 `AuroraFab`

56dp ink circle, `elevation.3`, white 24dp icon. One per screen. In the vault home it opens the secure camera; on scroll it shrinks to 44dp and docks into the nav. Long-press shows a `label.m` tooltip.

### 2.4 `AuroraChip`

- Height 36dp; full pill; padding 14dp.
- **Selected:** `surface.ink` fill, `#F7F5FB` label, optional 16dp icon.
- **Unselected:** `glass` fill, 1dp `surface.border`, `ink.700` label.
- **Filter:** leading 16dp filter glyph inside a 24dp circle; selected state inverts the glyph.
- Chips scroll horizontally with 20dp edge bleed and 8dp gap; never wrap to two lines on phone.
- Never use a chip for a destructive action.

### 2.5 `AuroraSegmentedControl`

Glass track (`glassSubtle`), 4dp inset, ink thumb (`surface.ink`) with `spring.soft` morph. Height 40dp. 2–3 segments. Label `label.l`; inactive `ink.600`.

### 2.6 `AuroraSwitch`

44×26 track, 22dp thumb. Off: `ink.200`; on: `iris.600` with a 2dp white thumb. 8dp inner shadow. Haptic `EFFECT_TICK`. Never animate the entire row.

### 2.7 `AuroraSlider`

For storage threshold, video quality, auto-lock timeout. 4dp track, 24dp thumb (glass with iris core). Value bubble `mono.m` above thumb. Steps are always labeled; no unlabeled sliders.

---

## 3. Surfaces

### 3.1 `AuroraGlassCard`

The workhorse. `radius.xl`, `surface.glass`, blur 24, 1dp border, `elevation.2`, padding 20dp.

| Prop | Notes |
|---|---|
| `tint` | `None \| Lilac \| Rose \| Peach \| Mint \| Sky \| Butter \| Sand` |
| `contentPadding` | default 20dp |
| `onClick` | optional; adds press scale 0.985 and focus ring |
| `secure` | if true, forces `surface.solid`, disables blur |

Tinted cards use the 100 fill at 92% over glass; text uses the tint 600. Never tint the border.

### 3.2 `AuroraHeroCard`

`radius.2xl`, height 220–260dp, full-bleed art with a bottom scrim, or a pastel gradient.

```
┌──────────────────────────────┐
│  overline                    │
│  Display title (2 lines max) │
│  body.s subtitle             │
│                              │
│  [stat]  [stat]        (→)   │
└──────────────────────────────┘
```

- One per screen. The arrow is a 44dp ink circle with a white 20dp arrow.
- Art is abstract; if user media, it is heavily cropped and blurred at 8dp with a 24% ink scrim.
- Title contrast measured against the darkest scrim point (≥ 4.5:1).

### 3.3 `AuroraStat`

```
 24          ← mono.l, tabular
 Pages       ← label.m, ink.500
```

Used in 2-up or 3-up rows inside hero cards. No icon unless it adds meaning. Values are never truncated; use `mono.l` down to `title.l` at 200% scale.

### 3.4 `AuroraProgressRing`

- Diameter 120dp (hero), 72dp (card), 44dp (inline); stroke 10/8/5dp.
- Track `ink.100`; progress `iris.600`; success `mint.600`; warning `butter.600`; danger `danger`.
- Center label `mono.l` + `label.m` unit.
- Sweep animates 600ms `easing.standard`; value changes never animate backwards.
- Reduced motion: instant value, no sweep.
- Always paired with a text value; never color-only.

### 3.5 `AuroraListRow`

Height 64dp (single line) / 76dp (two line). Leading 44dp glass/tinted square, title `title.m`, subtitle `body.s` `ink.500`, trailing chevron or switch. Divider inset 64dp, 1dp `surface.hairline`. Pressed = 4% ink overlay, no ripple.

Group rows inside a `AuroraListGroup`: one glass card, rows separated by hairlines, `radius.xl`.

### 3.6 `AuroraSectionHeader`

`overline` in `ink.500` + optional trailing text button (`label.l`, `iris.600`). 28dp top margin, 12dp bottom. No icons.

### 3.7 `AuroraBadge` / `AuroraStatusPill`

- `Pro`: 28dp pill, `iris.100` fill, `iris.700` label, `label.m`. Never animated after first view.
- `New`: mint tint, 1dp mint.300 border.
- `Status`: icon 14dp + `label.m`; success/warning/danger/info soft backgrounds.
- Badges never sit on top of text; they sit in the trailing slot or above a title.

### 3.8 `AuroraAvatar`

40dp circle. Default is a generated aurora gradient with the first letter of the vault label (`title.m`, ink). Options: abstract art, monogram, user photo (opt-in). Never show a face on the lock screen or in recents.

### 3.9 `AuroraBanner`

Inline notice, full width, `radius.lg`, soft status background, 20dp padding, 20dp icon, title `title.m`, body `body.s`, optional action. Used for: sync paused, backup failing, share expiring, duress configured, storage low. Dismissible only if non-critical; security warnings are not dismissible.

### 3.10 `AuroraEmptyState`

Centered, 240dp max width: one abstract 120dp object (gradient + orbit), `headline.m`, `body.m` `ink.600`, one primary action, optional ghost action. **Neutral copy only** — an empty vault and a non-existent vault must render the same component with the same copy.

---

## 4. Navigation and data

### 4.1 `AuroraCalendarStrip`

Seven day cells in a glass card. Day label `label.m` (`Mon`), date `title.m`. Selected = 40dp ink circle with white date; today = iris dot under date. Swipe horizontally to change week; tap month header to open the month sheet. Height 84dp.

### 4.2 `AuroraTimeline`

Vertical list grouped by hour. Left gutter 44dp: hour `mono.s` `ink.400` + dotted rail. Right: event cards (`radius.lg`, tinted by category).

- Cards never overlap the rail.
- Current-hour marker is a 2dp iris line + 6dp dot.
- Empty hours collapse to a single 24dp row.
- Used for vault activity (added, shared, backed up) and camera roll by time.

### 4.3 `AuroraMediaTile`

1:1, 4:5, or 16:9 squircle. 28dp radius, 2dp gap in grids. Bottom-left `label.m` chip for video duration; top-right 28dp glass circle for selection. Selection = 2dp iris border + 8% iris overlay + check. Long-press starts multi-select; haptic `EFFECT_CLICK`.

### 4.4 `AuroraFileCard`

Horizontal: 56dp tinted square (file-type glyph), title `title.m` (2 lines), metadata `body.s` (`PDF · 2.1 MB · Nov 14`), trailing more. Used in lists where thumbnails do not exist.

### 4.5 `AuroraStorageMeter`

Segmented horizontal bar (photos / videos / docs / other) with 8dp radius and 12dp height. Legend below with 8dp dots. Numbers `mono.m`. One color per segment from the pastel family; never more than four segments.

### 4.6 `AuroraDeviceRow`

Sky tint icon (32dp), device name `title.m`, last-sync `body.s`, trailing status pill (`Active`, `Offline`, `Paused`). Current device has a 2dp iris ring around the icon and the label "This device". Never expose device IDs.

---

## 5. Inputs

### 5.1 `AuroraTextField`

- Height 56dp; fill `glassRaised` (or `solidRaised` on secure panels); 1dp border; `radius.md`.
- Label floats inside at 12dp when empty; becomes `label.m` above when focused/filled.
- Focus: 2dp iris ring, no glow outside the field. Error: 2dp danger ring + 20dp error icon + `body.s` message below.
- Password fields have a reveal button; reveal auto-hides after 8s.
- **Secrets:** `keyboardType = VisiblePassword`, no suggestions, no autofill, no clipboard menu, FLAG_SECURE.

### 5.2 `AuroraSearchField`

44dp glass pill, 20dp search icon, placeholder `body.m`. Expands to full width on focus, shows Cancel. No voice icon unless offline dictation is implemented. Search is local-only; copy states "Search this vault" — never a cloud search.

### 5.3 `AuroraPatternGrid`

The hero interaction. 5×5, 280dp (phone), 320dp (tablet), 360dp (desktop). Cell 56/64/72dp, dot 24/28/32dp, line 3dp round cap.

| State | Visual |
|---|---|
| Idle | 24dp dot `ink.200` (L) / `#4A4458` (N), 1dp inner ring `ink.300` |
| Hover | scale 1.15, `iris.200` |
| Active | scale 1.18, `iris.600`, soft glow |
| Connected | scale 1.10, `iris.600` + white 20% center |
| Error | scale 1.10, `danger`, shake 420ms — **creation validation only** |
| Success | scale 1.18, `mint.600`, 380ms glow then fade |

- Line: `iris.600` at 3dp with round joints; creation-error line `danger`.
- Grid sits on a `glassSubtle` 360dp circle with `elevation.glow` on focus.
- **No labels, no numbers, no helper text** during unlock. Accessibility numbers are invisible overlays only.
- The grid never shows the vault name or a count. One pattern, one outcome.
- **Unlock is always neutral:** an unknown pattern glows and resolves exactly like a known one. No shake, no red, no error copy, no attempt counter. Error styling belongs to pattern *creation* (too few dots / no turn) and destructive confirmations.
- Rate limiting, if used, is neutral ("Please wait a moment.") and never implies that a specific pattern was wrong.

### 5.4 `AuroraSecretPhrase`

Display and entry for recovery / share phrases.

**Display (one-time):**
- Solid panel, FLAG_SECURE, no blur, no shadow.
- 3×3 grid of word tiles (9 words) or 2×6 (12); tile 64dp tall, `radius.md`, `surface.solidRaised`, word `mono.m` 22sp.
- Word index `mono.s` `ink.400` in the corner.
- "Reveal" is not a thing — the phrase is either visible on this screen or hidden behind biometrics later.
- Bottom: checkbox "I wrote this down" → primary `I've saved it`; copy loss warning above; no screenshot button, no copy button.
- Leaving the screen mid-flow opens a confirm dialog. Backgrounding immediately replaces the phrase with a solid cover; the plaintext is zeroized after 5 s. Returning inside 5 s lifts the cover; after 5 s the app locks and a fresh unlock plus re-entry is required. Process death loses the phrase permanently.

**Entry:**
- 9 slots in 3×3 (or 12 in 2×6), each `AuroraTextField` with autocomplete off.
- Paste is disabled by default; "Paste words" is a deliberate secondary action with a 1.5s hold.
- Word validation is local and silent until all slots are filled.
- Strength meter on custom phrases: 4 segments, labeled `Weak / Fair / Good / Strong`; never color-only.

### 5.5 `AuroraCodeInput`

6-digit PIN/backup code. Six 48dp cells, `radius.sm`, `mono.l`. Auto-advance, backspace retreats. Paste fills all. No dots after entry — digits visible by default with a global "hide digits" setting.

---

## 6. Security surfaces

### 6.1 `AuroraSecureSurface`

Wrapper that applies:

- `FLAG_SECURE` on the host activity/window
- `surface.solid` background
- screenshot/screen-record block
- recents thumbnail = solid brand cover
- clipboard disabled for descendants
- autofill disabled
- IME personalized learning disabled (`IME_FLAG_NO_PERSONALIZED_LEARNING`)
- optional `biometricGate` on resume

Used by: recovery display/entry, share phrase display/entry, pattern setup, duress setup, secret settings.

### 6.2 `AuroraCameraViewfinder`

Full-bleed preview, `radius` 0, ink scrim top/bottom 20%. Controls:

```
[back]  [flash]                 [settings]
              [focus ring]
   [gallery]  [ shutter ]  [flip]
        [Photo]   [Video]
```

- Shutter 76dp white ring + 62dp inner white circle; recording = inner red rounded square.
- Control pills 44dp at 62% ink, white icons, 16dp from edges.
- Timer `mono.m` top center with a 6dp danger dot.
- Audio-off badge: 28dp butter pill bottom-left, always visible when mic denied.
- Grid lines 1dp white 25%, off by default.
- No gallery strip (nothing is written unencrypted); the left slot is **Review last capture** only if it is still in memory.
- Orientation: controls follow rotation; preview never letterboxes.

### 6.3 `AuroraCameraReview`

- Full-bleed media on `#000`, top gradient scrim.
- Bottom sheet `glassRaised` at 38% height with: capture type, resolution/duration, "Encrypting into <Vault label>" once kept.
- Two 64dp round actions: Retake (glass, left) and Keep (ink, right). Keep is thumb-right.
- Video: 44dp play/pause center, scrubber 4dp, `mono.s` time.
- No share, export, or "save to gallery" affordance. Ever.
- Swipe down = retake with confirm only if > 5s recorded.

### 6.4 `AuroraThreatBanner`

Non-dismissible, solid ink card with rose icon: "Screen capture blocked", "Clipboard disabled on this screen", "This vault is hidden from search". Used only where the platform behavior needs explanation; one line, no fear language.

### 6.5 `AuroraDuressSetup`

Must look like an ordinary security setting. Rose tint is allowed here **only because it is pre-coercion**; in-the-moment unlock has zero unique styling.

- **No vault picker.** The decoy is designated by redrawing its pattern or by using "the vault I just opened". A list would leak the vault count.
- Explanation card: what happens, what survives, what is erased, restore path.
- Confirmation: hold-to-confirm 2s ink pill, not a red panic button.
- After setup: a neutral row in Security settings, same shape as other rows.

### 6.6 `AuroraSharePhraseCard`

12-word display for share creation. Solid, FLAG_SECURE, 2×6 grid, one-time copy allowed via explicit "Copy phrase" with a 60s clipboard auto-clear and a toast "Cleared from clipboard". Recipient never sees the owner's recovery phrase.

---

## 7. Feedback

### 7.1 `AuroraSnackbar`

Glass pill, 48dp, bottom above nav, max 2 lines, one text action. Auto-dismiss 4s; security messages 8s and include an icon. No snackbar for errors that need a decision — use a banner or dialog.

### 7.2 `AuroraTooltip`

Ink pill, `label.m`, 6dp radius, 8dp offset, long-press only. Never used for essential instructions.

### 7.3 `AuroraLoading`

- Inline: 18dp iris spinner.
- Page: skeleton cards with `ink.100` at 60% shimmer (1.4s), plus `body.s` "Decrypting…" after 800ms.
- Pattern validation: no spinner until 400ms; the KDF takes time, so show a subtle iris pulse inside the grid, never a blocking dialog.
- Never show a percentage for key derivation (it is not measurable).

### 7.4 `AuroraSkeleton`

Same geometry as the loaded component, `radius` preserved, `ink.100` L / `ink.100` at 12% N. Shimmer respects reduced motion (static).

---

## 8. Paywall and Pro

### 8.1 `AuroraProCard`

Glass card, iris eyebrow "VAULTARE PRO", `headline.m` benefit, 3 benefit rows with mint checks, price `mono.l` + `/month`, primary `Continue`, ghost `Restore purchase`, legal `body.s`. Never uses countdown timers, fake discounts, or blocked buttons.

### 8.2 `AuroraProGate`

Inline lock state on a feature row or screen:

- Keep the feature visible and explain it in one sentence.
- Primary `Try Pro`; ghost `Not now`.
- **Never hide the user's existing data** behind a gate. Existing shares/backups remain accessible.
- No blurring of user content as a paywall.

---

## 9. Component state matrix (minimum)

Every interactive component must define these states and screenshot-test them:

| State | Requirement |
|---|---|
| Default | Token colors only |
| Hover (pointer) | 4% ink overlay + 2dp focus-ready |
| Pressed | Scale 0.97 + 6% overlay |
| Focused | 2dp `focus` ring, 2dp offset |
| Disabled | 38% opacity, no shadow, no haptic |
| Loading | Preserved footprint, spinner, disabled |
| Error | Danger ring + icon + text, not color alone |
| Selected | Ink pill or iris border + check |
| Glass fallback | Solid fill, same layout |
| Night | Contrast re-checked |
| 200% font | No clipping, primary action still visible |

---

## 10. Do / Don't

| Do | Don't |
|---|---|
| One ink primary button per screen | Two competing filled buttons |
| Pastel for data categories | Pastel for status only |
| Glass for chrome and cards | Glass inside glass |
| Big quiet type | All-caps body copy |
| Solid panels for secrets | Blur behind recovery words |
| Neutral empty states | "No vault found" copy that leaks existence |
| Identical duress and normal unlock | Any badge, color, or haptic on duress |
| 48dp targets | 32dp icon-only taps |
| Text + icon for status | Color-only status |
| Motion that explains | Confetti, bouncing credentials |
