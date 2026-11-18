# Aurora Foundations

> The raw material of the interface: color, type, space, shape, glass, depth, motion, icons, and imagery.
> Every value here exists in [`tokens.json`](./tokens.json). If a value is not there, it is not a token.

---

## 1. Surface modes

Aurora ships one palette in three renderings. The mode is a user setting **and** an automatic fallback.

| Mode | Trigger | Look | Security behavior |
|---|---|---|---|
| **Aurora Glass** | Default; API 31+; transparency enabled | Frosted white cards, blurred canvas, ambient light | Same content, no reduced redaction |
| **Aurora Solid** | Reduce transparency · battery saver · API < 31 · low-RAM device | Opaque `surface.solid` cards, no blur, same layout | Same content, identical hierarchy |
| **Aurora Night** | System dark theme or manual | Deep aubergine canvas, smoked glass, brightened iris | Same content, same redaction |

**Never** change information, ordering, or security affordances between modes. Mode is cosmetic only.

User setting lives at **Settings → Appearance → Surface style**: `Glass / Solid / Auto`, plus `Light / Night / Auto`.

---

## 2. Color

### 2.1 Ink (text and dark surfaces)

| Token | Light | Night | Use |
|---|---|---|---|
| `ink.900` | `#17151F` | `#F4F1FA` | Primary text, primary button fill (light) |
| `ink.800` | `#24212E` | `#E3DFEC` | Headings on tinted cards |
| `ink.700` | `#3A3647` | `#CFC9DB` | Body text on glass (safe body ink) |
| `ink.600` | `#4A4557` | `#C3BDD2` | Secondary text, icons |
| `ink.500` | `#6B6578` | `#A69FB6` | Tertiary text ≥ 14sp semibold |
| `ink.400` | `#8C8799` | `#8B849C` | Placeholders, metadata ≥ 12sp |
| `ink.300` | `#ADA8B8` | `#6F687F` | Disabled text, inactive icons |
| `ink.200` | `#CFCBD8` | `#4A4458` | Hairlines, disabled fills |
| `ink.100` | `#E7E4EE` | `#302B3D` | Subtle fills, dividers |
| `ink.050` | `#F4F2F8` | `#221E2E` | Recessed tracks |

> **Body text default is `ink.700`, not `ink.900`.** It is softer and still 8.9:1 on glass. Reserve `ink.900` for titles and numbers.

### 2.2 Canvas and orbit

| Token | Light | Night |
|---|---|---|
| `canvas.base` | `#C9C3E6` | `#14121C` |
| `canvas.top` | `#D9D4F0` | `#1B1728` |
| `canvas.bottom` | `#BCB4DD` | `#0F0D16` |
| `canvas.orbit` | `rgba(255,255,255,0.30)` | `rgba(255,255,255,0.06)` |
| `canvas.vignette` | `rgba(86,64,140,0.10)` | `rgba(0,0,0,0.40)` |
| `scrim` | `rgba(23,21,31,0.32)` | `rgba(0,0,0,0.56)` |

The canvas is a vertical wash (`canvas.top → canvas.base → canvas.bottom`) with two soft bloom ellipses and 2–3 **orbit** curves. Orbit lines are 1dp, never interactive, never over text.

### 2.3 Glass and solid surfaces

| Token | Light | Night | Use |
|---|---|---|---|
| `surface.glass` | `rgba(255,255,255,0.62)` | `rgba(40,35,56,0.58)` | Standard cards, list groups |
| `surface.glassRaised` | `rgba(255,255,255,0.80)` | `rgba(50,44,70,0.76)` | Sheets, nav, text-heavy cards |
| `surface.glassSubtle` | `rgba(255,255,255,0.42)` | `rgba(40,35,56,0.36)` | Decorative wells, behind media |
| `surface.solid` | `#F7F5FB` | `#1B1826` | Solid mode cards, secure panels |
| `surface.solidRaised` | `#FFFFFF` | `#242031` | Solid mode sheets, inputs |
| `surface.ink` | `#17151F` | `#F4F1FA` | Dark pills, active nav, primary button |
| `surface.border` | `rgba(255,255,255,0.62)` | `rgba(255,255,255,0.14)` | 1dp glass edge |
| `surface.hairline` | `rgba(23,21,31,0.08)` | `rgba(255,255,255,0.08)` | Dividers inside cards |

> **Contrast rule:** any text placed on glass must be measured against the *darkest and busiest* thing that can sit behind it. If that fails, the card promotes itself to `glassRaised` or `solid`.

### 2.4 Brand — Iris

| Token | Light | Night |
|---|---|---|
| `iris.050` | `#F3F0FF` | `#1E1938` |
| `iris.100` | `#E7E2FF` | `#2A2350` |
| `iris.200` | `#CFC6FF` | `#3A3170` |
| `iris.300` | `#B4A8FF` | `#5A4DA8` |
| `iris.400` | `#9A8CF7` | `#9A8CF7` |
| `iris.500` | `#7C6BF2` | `#A192FF` |
| `iris.600` | `#6A58E8` | `#B4A8FF` |
| `iris.700` | `#5744C4` | `#CFC6FF` |

Iris is a **signal**, not a paint bucket. Allowed uses: focus rings, progress, selected chips, links, pattern line, small brand marks. Disallowed: full-screen backgrounds, primary buttons, gradients behind text.

### 2.5 Pastel tints (content blocks)

Each tint has a background, a deep ink for text, and an accent for icons/marks.

| Tint | 100 | 300 | 600 (ink/accent) | Typical use |
|---|---|---|---|---|
| **Lilac** | `#E9E3FB` | `#D3C7F4` | `#4E3F91` | Vault metadata, storage |
| **Rose** | `#F6DDE2` | `#EDBDC7` | `#8E3347` | Sensitive/duress-adjacent info, video |
| **Peach** | `#F8E3D3` | `#F2CBB0` | `#8A4B23` | Imports, documents |
| **Mint** | `#DDF3D8` | `#B9E6B0` | `#2A6B45` | Success, backup healthy |
| **Sky** | `#DCE8FA` | `#BFD6F5` | `#2C5B93` | Sync, devices |
| **Butter** | `#F8EFC4` | `#F1E19A` | `#7A5D0C` | Warnings, expiring shares |
| **Sand** | `#F2EDE3` | `#E7DDCB` | `#6B5B43` | Archive, free tier |

In Night mode use deep tints (`#2A2342`, `#3A2028`, `#3E2A1D`, `#1E3A2C`, `#1E2E44`, `#3A3218`, `#322D26`) with `onTint = #EFEBF7` and the corresponding bright accent.

> **Never use a pastel as a status color by itself.** Status always pairs an icon and text. Color is a helper, not the message.

### 2.6 Semantic status

| Token | Light | Night | Soft bg | Use |
|---|---|---|---|---|
| `success` | `#2A6B45` | `#8FE0AC` | `mint.100` | Backup complete, sync healthy |
| `warning` | `#7A5D0C` | `#F2CE7A` | `butter.100` | Sync paused, share expiring |
| `danger` | `#8E3347` | `#FF9DA8` | `rose.100` | Invalid new pattern, delete, revoke |
| `info` | `#2C5B93` | `#9AC4FF` | `sky.100` | Tips, device added |
| `focus` | `#6A58E8` | `#B4A8FF` | — | Keyboard/switch focus ring |

Status text on a soft background uses the 600 value, not the 500. Never white text on a pastel.

### 2.7 Gradients

| Token | Value | Use |
|---|---|---|
| `gradient.hero` | `135° #D8CCFF → #E9DFF5 → #F5D9E4` | Vault hero card, empty state art |
| `gradient.creative` | `140° #C9B6F2 → #8B6BC7` | Camera / media hero |
| `gradient.dawn` | `160° #F6DDE2 → #F8E3D3 → #F8EFC4` | Recovery/onboarding warmth |
| `gradient.mint` | `145° #DDF3D8 → #DCE8FA` | Backup healthy, storage |
| `gradient.ink` | `160° #24212E → #17151F` | Primary button, nav pill |

Gradients appear **only** on decorative art, covers, and hero cards. Never behind body text without a scrim.

### 2.8 Color usage ratio (per screen)

- 70% neutral (canvas + glass + ink)
- 20% one pastel family
- 8% ink solids (buttons, nav pill)
- 2% iris

If a screen has three pastel families competing, remove one.

---

## 3. Typography

### 3.1 Families

| Role | Family | Fallback | Notes |
|---|---|---|---|
| Display / UI | **Instrument Sans** | Roboto, system sans | Weights 400 / 500 / 600. Tight tracking at large sizes. |
| Numeric | Instrument Sans with `tnum` | Roboto | Use tabular figures for counters and stats. |
| Secrets / mono | **Roboto Mono** | monospace | Recovery words, share phrases, timers, fingerprints. |

Bundle fonts in `res/font/` with `fontVariationSettings` disabled for consistent metrics. Do not rely on downloadable fonts for security screens (offline first launch).

### 3.2 Scale

| Token | Weight | Size / line | Tracking | Use |
|---|---|---|---|---|
| `display.xl` | 500 | 48 / 50 | −0.04em | Lock greeting, one hero per screen max |
| `display.l` | 500 | 40 / 44 | −0.035em | Vault name on home |
| `display.m` | 500 | 34 / 38 | −0.03em | Section hero, camera title |
| `headline.l` | 500 | 28 / 34 | −0.02em | Screen titles |
| `headline.m` | 500 | 24 / 30 | −0.015em | Sheet titles, card titles |
| `title.l` | 600 | 20 / 26 | −0.01em | File names, list hero |
| `title.m` | 600 | 17 / 24 | 0 | Row titles, buttons |
| `body.l` | 400 | 16 / 25 | 0 | Primary body |
| `body.m` | 400 | 15 / 22 | 0 | Secondary body |
| `body.s` | 400 | 13 / 19 | 0.005em | Metadata, hints |
| `label.l` | 600 | 14 / 18 | 0.01em | Button labels, chips |
| `label.m` | 600 | 12 / 16 | 0.02em | Tab labels, badges |
| `overline` | 700 | 11 / 14 | 0.12em uppercase | Section eyebrows |
| `mono.l` | 500 | 30 / 36 | −0.01em | Counts (24, 40, 72%) |
| `mono.m` | 500 | 15 / 22 | 0.02em | Phrase words, timers |
| `mono.s` | 500 | 11 / 16 | 0.06em | Fingerprints, hashes |

### 3.3 Rules

1. **One display size per screen.** The rest is headline and below.
2. **Numbers are the headline** on stat cards: `mono.l`, tabular, followed by a small `label.m` unit.
3. **Never letterspace lowercase body text.** Tracking is for caps labels only.
4. **Line length:** 28–42 characters on phone for display, 45–70 for body.
5. **Dynamic type:** all sizes in `sp`; line heights scale. Test 85 / 100 / 130 / 150 / 200%.
6. **Secrets** always use `mono` and `body.l` or larger; never italic, never all-caps.
7. **No text over photography** except the vault cover title, which sits on a bottom-to-top ink scrim (`0% → 55%`).

### 3.4 Text color pairing

| Context | Default | Secondary |
|---|---|---|
| Glass card | `ink.900` title, `ink.700` body | `ink.600` metadata |
| Pastel card | tint 600 title, `ink.700` body | tint 600 at 80% metadata |
| Ink surface | `#F7F5FB` title, `rgba(247,245,251,0.72)` body | `rgba(...,0.56)` metadata |
| Canvas | `ink.900` | `ink.600` |

---

## 4. Spacing and layout

### 4.1 Base scale (dp)

`2 · 4 · 6 · 8 · 12 · 16 · 20 · 24 · 32 · 40 · 48 · 64 · 80`

No values outside this scale except 1dp hairlines and 44dp minimum touch.

### 4.2 Layout tokens

| Token | Phone | Tablet ≥ 600dp | Desktop ≥ 840dp |
|---|---|---|---|
| Screen padding | 20 | 32 | 40 |
| Content max width | — | 720 | 1040 |
| Card padding | 20 | 24 | 24 |
| Card gap | 12 | 16 | 16 |
| Section gap | 28 | 32 | 36 |
| Top bar height | 56 | 64 | 64 |
| Floating nav height | 68 | 72 | — (rail) |
| Nav inset | 16 | 20 | — |
| Min touch target | 48 | 48 | 48 |

### 4.3 Vertical rhythm

```
[status bar inset]
20  Top bar (avatar · title · round actions)
16  Eyebrow / greeting
8   Display title
20  Filter chips (horizontal scroll, bleed 20)
20  Hero card
12  Data cards (2-up)
16  Section header + action
12  List group
24  Breathing room above nav
88  Bottom nav clearance (68 + 20)
```

### 4.4 Grid

- Phone: 4-column fluid, 12dp gutter.
- Tablet: 8-column, 16dp gutter.
- Cards snap to columns; the hero spans full width.
- Use `Arrangement.spacedBy` rather than manual padding between siblings.

### 4.5 Edge-to-edge

- Draw canvas behind system bars.
- Status bar icons: dark on light canvas, light on night canvas.
- Bottom nav floats above the gesture inset (`navigationBarsPadding() + 12dp`).
- Never place a tappable target inside the gesture exclusion zone.

---

## 5. Shape

| Token | Value | Use |
|---|---|---|
| `radius.xs` | 10dp | Badges, tiny tags |
| `radius.sm` | 14dp | Inputs (compact), thumbnails |
| `radius.md` | 18dp | Text fields, small cards |
| `radius.lg` | 22dp | Media tiles, list groups |
| `radius.xl` | 28dp | **Standard card** |
| `radius.2xl` | 32dp | **Hero card, feature panel** |
| `radius.3xl` | 36dp | Floating nav capsule, dialogs |
| `radius.4xl` | 40dp | Bottom sheet (top corners only) |
| `radius.full` | 999dp | Pills, avatars, icon buttons, pattern dots |

Rules:
- Nested surfaces: inner radius = outer radius − padding (minimum 10dp).
- Buttons are always full pills. Icon buttons are circles.
- Image crops use `radius.xl`; full-bleed camera preview uses 0 with a 28dp top mask only when in a sheet.
- Never mix sharp and soft corners on the same screen.

---

## 6. Glass

### 6.1 Recipe

| Property | Card | Raised / nav | Sheet |
|---|---|---|---|
| Fill | `surface.glass` | `surface.glassRaised` | `surface.glassRaised` |
| Blur | 24dp | 32dp | 40dp |
| Saturation | 1.15 | 1.2 | 1.2 |
| Border | 1dp `surface.border` | 1dp `surface.border` | 1dp top highlight |
| Inner highlight | 1dp white 35% top edge | 1dp white 45% | 1dp white 50% |
| Shadow | `elevation.2` | `elevation.3` | `elevation.4` |
| Content scrim | none | none | `scrim` behind sheet |

### 6.2 Composition rules

1. **Maximum two glass layers** on screen (e.g. card over canvas, nav over content). A glass card inside a glass sheet is forbidden; use a solid tint instead.
2. **Text-bearing surfaces promote.** If a card contains more than two lines of body text or an input, use `glassRaised` or `surface.solid`.
3. **No glass over video or camera preview** except the floating control pills, which use `surface.ink` at 70% opacity.
4. **Blur is never the only separation.** Always keep the 1dp border and shadow.
5. **Media never shows through a card.** Blur the backdrop, but do not let identifiable imagery leak around text.
6. **Secure panels use solid.** Recovery phrase, secret phrase entry, and share phrase display are `surface.solid` with zero transparency.

### 6.3 Fallbacks

| Condition | Behavior |
|---|---|
| API < 31 (no RenderEffect blur) | Solid fill + 1dp hairline + shadow; no visual "hole" |
| Reduce transparency / battery saver | Same as above, automatically |
| Low contrast requested | Promote all glass to `surface.solidRaised` |
| HDR / wide-gamut photo behind card | Apply a 12% canvas scrim under the glass |

### 6.4 Performance budget

- At most **3 blurred layers** per frame; cards share one backdrop pass where possible.
- Never animate blur radius. Animate opacity and translation instead.
- Reuse a single `RenderEffect` per screen; do not create one per list item.
- Lists > 8 items use solid surfaces (scrolling glass is expensive and visually noisy).

---

## 7. Elevation and depth

Shadows are **lavender-tinted**, never neutral black.

| Token | Light | Night | Use |
|---|---|---|---|
| `elevation.0` | none | none | Flat canvas, inline sections |
| `elevation.1` | `0 2 8 rgba(60,48,110,0.06)` | `0 2 8 rgba(0,0,0,0.35)` | Chips, small tiles |
| `elevation.2` | `0 10 28 rgba(60,48,110,0.10)` | `0 10 28 rgba(0,0,0,0.40)` | Standard cards |
| `elevation.3` | `0 22 56 rgba(60,48,110,0.16)` | `0 22 56 rgba(0,0,0,0.48)` | Nav, floating actions |
| `elevation.4` | `0 36 88 rgba(60,48,110,0.22)` | `0 36 88 rgba(0,0,0,0.56)` | Sheets, dialogs |
| `elevation.glow` | `0 12 40 rgba(124,107,242,0.35)` | `0 12 40 rgba(124,107,242,0.45)` | Focused pattern grid, success burst |

Depth order (back to front): canvas → orbit → ambient blooms → content cards → floating nav → sheet scrim → sheet → dialog.

Never use elevation to fake a disabled state. Disabled = color + opacity, not shadow.

---

## 8. Motion

### 8.1 Duration

| Token | ms | Use |
|---|---|---|
| `duration.instant` | 0 | Duress path, secure wipes |
| `duration.micro` | 90 | Ripple, icon press |
| `duration.fast` | 160 | Chip select, toggle |
| `duration.base` | 240 | Standard state change |
| `duration.slow` | 380 | Card expand, success check |
| `duration.sheet` | 440 | Sheet enter/exit |
| `duration.ambient` | 45000 | Background bloom drift |

### 8.2 Easing and springs

| Token | Value | Use |
|---|---|---|
| `easing.standard` | `cubic-bezier(0.2, 0, 0, 1)` | Most transitions |
| `easing.enter` | `cubic-bezier(0.16, 1, 0.3, 1)` | Elements arriving |
| `easing.exit` | `cubic-bezier(0.4, 0, 1, 1)` | Elements leaving |
| `spring.soft` | damping 0.82, stiffness 260 | Cards, nav |
| `spring.snappy` | damping 0.90, stiffness 520 | Buttons, chips |
| `spring.bouncy` | damping 0.62, stiffness 300 | Playful success only (never credentials) |

### 8.3 Choreography

- **Screen enter:** content fades 0→1 and translates 12dp up, 240ms `enter`; canvas does not move.
- **Sheet:** scrim fades 160ms; sheet translates from bottom 440ms `enter`; drag-to-dismiss tracks finger 1:1.
- **Nav:** active pill morphs between destinations with `spring.soft`; icon crossfades 90ms.
- **Pattern lock:** dot press scales to 1.18 with `spring.snappy`; connected line draws continuously; success = 380ms iris glow + check; error = 420ms shake, 3 oscillations, ±10dp.
- **Camera review:** slides up 440ms; keep = progress ring; retake = slide down 240ms.
- **Backup progress:** ring sweep 600ms `standard`; no confetti.
- **Ambient:** two blooms drift 8–12dp over 45s, `linear`, alternating.

### 8.4 Reduced motion

Respect `Settings.Global.ANIMATOR_DURATION_SCALE == 0` and the in-app toggle:

- Ambient off.
- Springs → 120ms fades.
- Shake → single 200ms red border pulse.
- Sheet → 120ms fade, no translate.
- **Duress path is always instant, regardless of setting.**

---

## 9. Iconography

- **Set:** Aurora Line — based on Material Symbols Rounded, redrawn at 24dp with 1.6dp stroke and round caps.
- **Sizes:** 20 (inline), 24 (default), 28 (nav), 32 (feature).
- **Active state:** nav and segmented controls use filled glyphs; everything else is line.
- **Color:** `ink.600` default, `ink.900` active, `ink.300` disabled, `iris.600` selected accent.
- **Never** put an icon in a pastel circle unless it is a category tile; use glass circles for actions.
- **Directional icons** (`arrow_back`, `chevron_right`, `share`) auto-mirror in RTL.
- **Brand marks:** vault glyph and pattern grid are custom and never outlined at small sizes below 20dp.

Custom glyph list: vault, pattern-grid, recovery-phrase, secret-phrase, duress, secure-camera, camera-review, encrypted-backup, device-sync, secure-share, calendar, timeline, storage-ring, disguise (7), decoy (internal only — never rendered).

Disguise icons are **app launcher art**, not in-app icons. See `screens.md § App Icons`.

---

## 10. Imagery and illustration

- **Mood:** soft 3D matter — frosted glass, brushed lilac metal, translucent petals. Abstract only. No people in default art, no stock photography of "security".
- **Covers:** user media is the color. Apply a 28dp squircle crop and, when a title is needed, a bottom ink scrim (`transparent → rgba(23,21,31,0.62)`).
- **Empty states:** one abstract object + one sentence + one action. Never a cartoon padlock.
- **No screenshots of real vault content** in marketing, onboarding, or previews. Use the demo vault.
- **Sensitive previews:** blur user thumbnails in the recents switcher via FLAG_SECURE; do not render a blurred thumbnail that is still recognizable.
- **Image formats:** AVIF/WebP for bundled art; 2× and 3× densities; dark variants for hero art.

---

## 11. Sound and haptics

Sound is **off by default** and never used for security events. Haptics are the only tactile channel.

| Event | Haptic |
|---|---|
| Chip / button tap | `EFFECT_CLICK` |
| Toggle | `EFFECT_TICK` |
| Pattern dot connect | `EFFECT_CLICK` per dot |
| Pattern success | `EFFECT_HEAVY_CLICK` once |
| Pattern error | `EFFECT_HEAVY_CLICK` ×2, 80ms apart |
| Camera shutter | `EFFECT_CLICK` |
| Video start / stop | `EFFECT_DOUBLE_CLICK` |
| Keep / save complete | `EFFECT_TICK` |
| Duress trigger | **None. Zero. Silent.** |
| Destructive confirm | `EFFECT_HEAVY_CLICK` |

Haptics respect the system "touch feedback" setting. Never haptic on scroll, ambient motion, or Pro badges.

---

## 12. Token naming

```
aurora.color.<group>.<role>.<state>       ex: aurora.color.surface.glass
aurora.type.<scale>                        ex: aurora.type.display.l
aurora.space.<step>                        ex: aurora.space.20
aurora.radius.<step>                       ex: aurora.radius.xl
aurora.elevation.<level>                   ex: aurora.elevation.2
aurora.glass.<property>.<size>             ex: aurora.glass.blur.card
aurora.motion.<kind>.<name>                ex: aurora.motion.duration.base
```

Component tokens may alias foundation tokens but never introduce raw values.
