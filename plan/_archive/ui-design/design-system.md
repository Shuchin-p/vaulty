# Vaultaire Android — Design System Documentation

> **Single source of truth for all visual, motion, and interaction design decisions.**  
> Implements Material 3 Expressive with a security-first, vault aesthetic.

---

## 1. Color System

### 1.1 Philosophy
- **Dark mode default** — Vault aesthetic, reduces screen glare, hides content in peripheral vision
- **Light mode supported** — Full parity, same semantic roles
- **Semantic colors only** — No hardcoded hex in components; all reference design tokens
- **Security-aware** — Error/success states communicate without relying solely on color

### 1.2 Light / Dark Palettes

| Role | Light | Dark | Usage |
|------|-------|------|-------|
| `primary` | `#4A6FA5` | `#7BA3D9` | Brand, primary actions, pattern grid active state |
| `onPrimary` | `#FFFFFF` | `#121212` | Text/icons on primary |
| `primaryContainer` | `#D6E4F0` | `#2D4A6B` | Hover/pressed states, selected chips |
| `secondary` | `#5C7A5C` | `#8FB88F` | Success, confirmed actions |
| `tertiary` | `#7D5A7D` | `#C5A3C5` | **Pro feature accent**, premium badges |
| `surface` | `#F8F9FA` | `#1A1C1E` | Main backgrounds, cards |
| `surfaceContainer` | `#E8E9EC` | `#222427` | Elevated cards, input fields |
| `surfaceContainerHigh` | `#E0E1E4` | `#2A2D30` | Pressed states, hovered cards |
| `onSurface` | `#1A1C1E` | `#E6E6E6` | Primary text |
| `onSurfaceVariant` | `#44474B` | `#C4C6CB` | Secondary text, labels |
| `outline` | `#74777B` | `#8E9093` | Dividers, disabled borders |
| `outlineVariant` | `#C4C6CC` | `#44474B` | Input field borders |
| `error` | `#BA1A1A` | `#FFB4AB` | Destructive actions, wrong pattern |
| `success` | `#2E7D32` | `#81C784` | Correct pattern, backup complete |
| `warning` | `#F57F17` | `#FFB74D` | Pending sync, expiring shares |

### 1.3 Specialized Color Groups

#### Pattern Grid States
```kotlin
// Dot states
dotIdle      = color.patternGrid.dotIdle      // Unselected
dotHover     = color.patternGrid.dotHover     // Finger approaching (touch exploration)
dotActive    = color.patternGrid.dotActive    // Currently pressed
dotConnected = color.patternGrid.dotConnected // Part of drawn pattern
dotError     = color.patternGrid.dotError     // Wrong pattern shake
dotSuccess   = color.patternGrid.dotSuccess   // Correct pattern flash

// Line states
lineActive   = color.patternGrid.lineActive   // Currently drawing
lineConnected= color.patternGrid.lineConnected// Confirmed segment
lineError    = color.patternGrid.lineError    // Wrong pattern
```

#### Camera Overlay (FLAG_SECURE surfaces)
```kotlin
toolbarBackground     = rgba(26,28,30,0.85)  // Semi-transparent dark
controlBackground     = rgba(26,28,30,0.7)   // Control button backgrounds
controlIcon           = #FFFFFF              // White icons on dark overlay
shutterButton         = #FFFFFF              // White outer ring
shutterButtonInner    = #1A1C1E              // Dark inner circle
recordButton          = #BA1A1A (light) / #FF5252 (dark)
focusIndicator        = color.primary        // Blue focus square
reviewKeepButton      = color.primary        // Primary action
reviewRetakeButton    = color.onSurfaceVariant // Secondary action
audioOffBadge         = color.error          // Mic denied indicator
```

#### Disguise Icons (Pro)
| Disguise | Background Color | Icon Style |
|----------|------------------|------------|
| Calculator | `#333333` | Monochrome calculator glyph |
| Printer | `#0066CC` | Monochrome printer glyph |
| Units | `#009966` | Monochrome ruler glyph |
| Device Info | `#663399` | Monochrome info glyph |
| Level | `#CC6600` | Monochrome level glyph |
| Sound Meter | `#CC0066` | Monochrome waveform glyph |
| Network | `#006699` | Monochrome signal glyph |
| Default (Vaultaire) | `color.primary` | Vault glyph |

---

## 2. Typography Scale

### 2.1 Material 3 Base + Custom Extensions

| Style | Font | Weight | Size | Line Height | Letter Spacing | Usage |
|-------|------|--------|------|-------------|----------------|-------|
| `displayLarge` | Google Sans | 400 | 57sp | 64sp | -0.25sp | Hero headlines (unused in vault) |
| `displayMedium` | Google Sans | 400 | 45sp | 52sp | 0sp | — |
| `displaySmall` | Google Sans | 400 | 36sp | 44sp | 0sp | — |
| `headlineLarge` | Google Sans | 400 | 32sp | 40sp | 0sp | Screen titles: "Vaultaire", "Recovery Phrase" |
| `headlineMedium` | Google Sans | 400 | 28sp | 36sp | 0sp | Section headers in settings |
| `headlineSmall` | Google Sans | 400 | 24sp | 32sp | 0sp | Dialog titles, bottom sheet headers |
| `titleLarge` | Google Sans | 500 | 22sp | 28sp | 0sp | Card titles, file names |
| `titleMedium` | Google Sans | 500 | 16sp | 24sp | 0.15sp | List item titles, chip labels |
| `titleSmall` | Google Sans | 500 | 14sp | 20sp | 0.1sp | Metadata labels |
| `bodyLarge` | Roboto | 400 | 16sp | 24sp | 0.5sp | Primary body text, descriptions |
| `bodyMedium` | Roboto | 400 | 14sp | 20sp | 0.25sp | Secondary text, hints |
| `bodySmall` | Roboto | 400 | 12sp | 16sp | 0.4sp | Timestamps, file sizes |
| `labelLarge` | Google Sans | 500 | 14sp | 20sp | 0.1sp | Button labels, FAB |
| `labelMedium` | Google Sans | 500 | 12sp | 16sp | 0.5sp | Chip text, tab labels |
| `labelSmall` | Google Sans | 500 | 11sp | 16sp | 0.5sp | Overline, badges |

### 2.2 Custom Security Typography

| Style | Font | Weight | Size | Line Height | Usage |
|-------|------|--------|------|-------------|-------|
| `patternNumber` | Roboto Mono | 500 | 14sp | 20sp | Accessibility: dot position numbers |
| `recoveryPhrase` | Roboto Mono | 400 | 24sp | 36sp | **Full phrase display** — high readability |
| `recoveryPhraseWord` | Roboto Mono | 500 | 24sp | 36sp | Individual word in 3×3 grid |
| `secretPhraseInput` | Roboto | 400 | 18sp | 28sp | Word input fields (custom phrase) |
| `cameraTimer` | Roboto Mono | 500 | 16sp | 24sp | Recording timer (00:00) |
| `fileMetadata` | Roboto | 400 | 12sp | 16sp | File grid: size, date, type |

### 2.3 Dynamic Type Support
- **Base unit**: `sp` (scale-independent pixels)
- **Maximum scale**: 200% (Android system max)
- **Minimum touch targets preserved**: 48dp at all scales
- **Line height scales proportionally** to maintain readability
- **Test at**: 85%, 100%, 130%, 150%, 200%

---

## 3. Spacing System

### 3.1 Base Unit: 4dp
All spacing values are multiples of 4dp.

| Token | Value | Usage |
|-------|-------|-------|
| `xs` | 4dp | Icon gaps, internal padding |
| `sm` | 8dp | Component internal padding, chip gaps |
| `md` | 16dp | **Standard screen padding**, card gaps |
| `lg` | 24dp | Section gaps, dialog margins |
| `xl` | 32dp | Major section separation |
| `xxl` | 48dp | Screen-level vertical rhythm |

### 3.2 Specialized Spacing
| Token | Value | Usage |
|-------|-------|-------|
| `patternGridCell` | 56dp | 5×5 grid cell (dot + hit area) |
| `patternGridTotal` | 280dp | Full grid width/height |
| `shutterButton` | 72dp | Camera shutter diameter |
| `shutterButtonInner` | 56dp | Inner capture circle |
| `controlButton` | 48dp | Camera control buttons (lens, flash, mode) |
| `fab` | 56dp | Floating action button |

### 3.3 Responsive Breakpoints (see `responsive-breakpoints.md`)
| Breakpoint | Width | Layout |
|------------|-------|--------|
| Phone | < 600dp | Single column, bottom nav |
| Tablet | 600–840dp | Two-pane, nav rail |
| Desktop/Foldable | > 840dp | Three-pane, permanent nav rail |

---

## 4. Shape System

### 4.1 Corner Radius Scale

| Token | Value | Usage |
|-------|-------|-------|
| `cornerNone` | 0dp | Full-bleed backgrounds |
| `cornerXS` | 4dp | Small chips, badges |
| `cornerSM` | 8dp | Input fields, small cards |
| `cornerMD` | 12dp | Standard cards, dialogs (legacy) |
| `cornerLG` | 16dp | **Default card radius**, bottom sheets |
| `cornerXL` | 28dp | **Dialogs, bottom sheets (M3 Expressive)** |
| `cornerFull` | 9999dp | Pills, FABs, pattern dots, shutter |

### 4.2 Component-Specific Shapes
| Component | Radius | Token |
|-----------|--------|-------|
| Pattern Dot | Full circle | `patternDot` |
| Shutter Button | Full circle | `shutterButton` |
| Card | 16dp | `card` |
| Dialog | 28dp | `dialog` |
| Bottom Sheet | 28dp | `bottomSheet` |
| Text Field | 12dp | `textField` |
| Chip | Full pill | `chip` |
| Button | 100dp (pill) | `button` |

---

## 5. Shadow / Elevation System

### 5.1 Elevation Levels

| Level | Offset | Blur | Spread | Color | Usage |
|-------|--------|------|--------|-------|-------|
| `elevation0` | 0,0 | 0 | 0 | transparent | Flat surfaces |
| `elevation1` | 0,1 | 2 | 0 | `rgba(0,0,0,0.3)` | Cards, default FAB |
| `elevation2` | 0,2 | 4 | -1 | `rgba(0,0,0,0.3)` | Hovered cards, focused inputs |
| `elevation3` | 0,4 | 8 | -2 | `rgba(0,0,0,0.3)` | Pressed FAB, pattern grid container |
| `elevation4` | 0,6 | 12 | -4 | `rgba(0,0,0,0.3)` | Bottom sheets |
| `elevation5` | 0,8 | 16 | -6 | `rgba(0,0,0,0.3)` | Dialogs, modals |

### 5.2 Specialized Shadows
| Name | Usage |
|------|-------|
| `patternGrid` | Behind 5×5 grid container on lock screen |
| `shutterButton` | Camera shutter button (stronger for tactile feel) |
| `cameraControl` | Camera overlay control buttons |
| `fab` | Floating action button |
| `dialog` | Full-screen dialogs, alerts |
| `bottomSheet` | Modal bottom sheets (negative Y offset) |

---

## 6. Motion & Choreography

### 6.1 Duration Tokens

| Token | Duration | Usage |
|-------|----------|-------|
| `durationShort1` | 50ms | Micro-interactions (ripple start) |
| `durationShort2` | 100ms | Dot press, hover |
| `durationShort3` | 150ms | Line draw segment |
| `durationShort4` | 200ms | State transitions |
| `durationMedium1` | 250ms | Standard transitions |
| `durationMedium2` | 300ms | **Pattern completion checkmark** |
| `durationMedium3` | 350ms | Screen transitions |
| `durationMedium4` | 400ms | Bottom sheet enter |
| `durationLong1` | 450ms | Vault unlock transition |
| `durationLong2` | 500ms | **Wrong pattern shake** |
| `durationLong3` | 550ms | Camera review enter |
| `durationLong4` | 600ms | Duress wipe progress |

### 6.2 Easing Curves

| Token | Curve | Usage |
|-------|-------|-------|
| `easingStandard` | `cubic-bezier(0.2, 0, 0, 1)` | Most transitions |
| `easingEmphasized` | `cubic-bezier(0.2, 0, 0, 1)` | Important state changes |
| `easingLegacy` | `cubic-bezier(0.4, 0, 1, 1)` | Linear-ish, durable |
| `easingPatternDraw` | `cubic-bezier(0.25, 0.1, 0.25, 1)` | **Smooth line drawing** |
| `easingVaultUnlock` | `cubic-bezier(0.05, 0.7, 0.1, 1)` | **Quick start, gentle settle** |
| `easingCameraReview` | `cubic-bezier(0.16, 1, 0.3, 1)` | Review screen slide up |
| `easingDuressWipe` | `cubic-bezier(0.4, 0, 1, 1)` | **Linear, no bounce — serious** |

### 6.3 Key Choreographies

#### Pattern Draw (Hero Interaction)
```
Touch Down (50ms) → Dot scales 120% + haptic light
    ↓
Drag (continuous) → Line draws with easingPatternDraw
    ↓
Interpolated dot (150ms) → Dot lights up + haptic per dot
    ↓
Lift (300ms) → Checkmark animation (success) OR Shake 500ms (error)
    ↓
Success: Vault unlock transition (450ms, easingVaultUnlock)
Error:   Red flash 300ms + heavy haptic → clear pattern
```

#### Vault Unlock
```
Pattern valid → Grid fades out (200ms)
    ↓
Vault content fades in + slides up (450ms, easingVaultUnlock)
    ↓
FAB enters with spring (300ms delay)
```

#### Camera Review
```
Capture → Review screen slides up (350ms, easingCameraReview)
    ↓
Image/Video fades in (200ms)
    ↓
Keep: Encrypting spinner (indeterminate) → Vault grid (300ms)
Retake: Slide down (200ms) → Camera preview
```

#### Duress Wipe (No Delight)
```
Duress pattern matched → Immediate (no animation)
    ↓
Progress indicator: linear, no easing
    ↓
Decoy vault opens directly (no transition)
```

---

## 7. Iconography

### 7.1 Material Icons (Primary)
- **Set**: Material Icons Rounded (filled for active, outlined for inactive)
- **Size tokens**: `iconSizeSmall` (16dp), `iconSizeMedium` (24dp), `iconSizeLarge` (32dp)
- **Weight**: 400 (regular)

### 7.2 Custom Icons (Security-Specific)

| Icon | Name | Usage | Style |
|------|------|-------|-------|
| Vault | `ic_vault` | App icon, default disguise | Rounded, 24dp |
| Pattern Grid | `ic_pattern_grid` | Accessibility placeholder | 24dp |
| Recovery Phrase | `ic_recovery_phrase` | Settings entry | 24dp |
| Duress | `ic_duress` | Settings (hidden) | 24dp |
| Secure Camera | `ic_camera_secure` | FAB, toolbar | 24dp |
| Encrypted Backup | `ic_backup_encrypted` | Settings status | 24dp |
| Device Sync | `ic_sync_devices` | Settings status | 24dp |
| Secure Share | `ic_share_secure` | Share creation | 24dp |
| Disguise Calculator | `ic_calculator` | App icon alternate | Adaptive |
| Disguise Printer | `ic_printer` | App icon alternate | Adaptive |
| Disguise Units | `ic_units` | App icon alternate | Adaptive |
| Disguise Device Info | `ic_device_info` | App icon alternate | Adaptive |
| Disguise Level | `ic_level` | App icon alternate | Adaptive |
| Disguise Sound Meter | `ic_sound_meter` | App icon alternate | Adaptive |
| Disguise Network | `ic_network` | App icon alternate | Adaptive |

### 7.3 Pro Feature Badges
- **Style**: Small pill chip with `tertiary` background, `onTertiary` text
- **Text**: "PRO" in `labelSmall`
- **Position**: Top-right of feature entry points
- **Animation**: Subtle pulse on first visit (once per install)

---

## 8. Accessibility

### 8.1 TalkBack / Screen Reader Labels

| Component | Label Format | Example |
|-----------|--------------|---------|
| Pattern Dot | "Dot, row {row}, column {col}, {state}" | "Dot, row 2, column 3, unselected" |
| Pattern Line | "Connecting dot {from} to dot {to}" | "Connecting dot 7 to dot 12" |
| Camera Shutter | "Capture photo" / "Record video" | Dynamic by mode |
| Camera Controls | "{Control} {state}" | "Flash auto", "Front camera" |
| File Grid Item | "{Name}, {type}, {size}, {date}" | "Passport scan, PDF, 2.1 MB, November 14" |
| Recovery Phrase Word | "Word {index} of 12: {word}" | "Word 5 of 12: river" |
| Secret Phrase Input | "Word {index} of {total}, {hint}" | "Word 3 of 9, enter word" |
| Pro Badge | "Pro feature, requires subscription" | — |

### 8.2 Focus Order

#### Pattern Lock Screen
```
1. App title (if present)
2. Pattern grid (entire grid as single focusable, arrow keys navigate dots)
3. "Forgot pattern?" link
4. Emergency info (if enabled)
```

#### Camera Viewfinder
```
1. Back button
2. Settings (gear)
3. Preview view (tap to focus - not focusable)
4. Lens switch
5. Mode toggle (photo/video)
6. Flash/torch
7. Shutter/Record button
```

#### Vault Browser
```
1. Toolbar: Back (if nested), Vault title, Menu
2. FAB: Add file
3. File grid (2D navigation: arrows move between items)
4. Bottom nav (if tablet/desktop)
```

#### Settings
```
1. Toolbar back
2. Section headers (Security, Cloud, Appearance, Advanced, About)
3. Items within each section (top to bottom)
4. Save/Done (if modal)
```

### 8.3 Dynamic Type Scaling
- All text uses `sp` units
- **Maximum scale**: 200% (system limit)
- **Touch targets**: Minimum 48dp preserved at all scales
- **Pattern grid**: Dot size scales, cell size fixed at 56dp (hit area unchanged)
- **Recovery phrase**: Monospace scales, line height scales proportionally

### 8.4 Color Contrast (WCAG 2.1 AA Minimum)

| Combination | Ratio | Status |
|-------------|-------|--------|
| `onSurface` / `surface` | 15.8:1 (dark) / 12.6:1 (light) | ✅ AAA |
| `onSurfaceVariant` / `surface` | 7.2:1 (dark) / 5.8:1 (light) | ✅ AA |
| `primary` / `onPrimary` | 8.4:1 (dark) / 6.2:1 (light) | ✅ AA |
| `error` / `onError` | 6.8:1 (dark) / 5.1:1 (light) | ✅ AA |
| `patternGrid.dotActive` / `patternGrid.gridBackground` | 4.8:1 (dark) / 4.5:1 (light) | ✅ AA |
| `cameraOverlay.controlIcon` / `cameraOverlay.controlBackground` | 8.2:1 | ✅ AAA |

### 8.5 Haptic Feedback Patterns

| Interaction | Haptic Type | Pattern |
|-------------|-------------|---------|
| Pattern dot press | `VibrationEffect.EFFECT_CLICK` | Light |
| Pattern interpolated dot | `VibrationEffect.EFFECT_CLICK` | Light (per dot) |
| Pattern complete (success) | `VibrationEffect.EFFECT_HEAVY_CLICK` | Medium |
| Pattern error (shake) | `VibrationEffect.EFFECT_HEAVY_CLICK` | Heavy |
| Camera shutter | `VibrationEffect.EFFECT_CLICK` | Light |
| Camera record start/stop | `VibrationEffect.EFFECT_DOUBLE_CLICK` | Double |
| Duress trigger | **None** | Silent |
| Vault unlock | `VibrationEffect.EFFECT_TICK` | Subtle |

### 8.6 Screen Reader Announcements

| State Change | Announcement |
|--------------|--------------|
| Pattern dot selected | "Dot {position} selected" |
| Pattern dot connected | "Connected, {count} dots in pattern" |
| Pattern wrong | "Incorrect pattern, try again" |
| Pattern correct | "Vault unlocked" |
| Camera focus locked | "Focus locked at {position}" |
| File added | "{Name} added to vault" |
| File deleted | "{Name} deleted" |
| Backup complete | "Backup completed, {size} uploaded" |
| Sync complete | "Synced with {count} devices" |
| Share revoked | "Access revoked by owner" |

### 8.7 FLAG_SECURE Implications for Accessibility

| Screen | FLAG_SECURE | A11y Impact | Mitigation |
|--------|-------------|-------------|------------|
| Pattern Lock | ✅ | TalkBack works, no screenshots | Standard |
| Recovery Phrase Display | ✅ | **TalkBack reads words** — use `importantForAccessibility="no"` on phrase TextView, custom delegate reads on demand | Custom AccessibilityDelegate |
| Camera Preview | ✅ | No camera preview in TalkBack | Announce controls only |
| Camera Review | ✅ | No image preview in TalkBack | Announce "Captured photo, double tap to keep" |
| Secret Phrase Entry | ✅ | Standard input | Standard |

---

## 9. Dark Mode Implementation

### 9.1 Default: Dark Mode
```kotlin
// App theme defaults to dark
<item name="android:forceDarkAllowed">true</item>
// But we provide custom dark palette, not auto-invert
```

### 9.2 Color Role Mapping (Automatic via MaterialTheme)
```kotlin
MaterialTheme.colorScheme.surface           // → surface (dark: #1A1C1E)
MaterialTheme.colorScheme.primary           // → primary (dark: #7BA3D9)
MaterialTheme.colorScheme.onSurface         // → onSurface (dark: #E6E6E6)
// etc.
```

### 9.3 Component Theming
- All components use `MaterialTheme.colorScheme.*` semantic roles
- No hardcoded colors in composables
- Custom components (PatternGrid, CameraViewfinder) use token references

---

## 10. RTL Support
- All layouts use `start`/`end` not `left`/`right`
- Pattern grid: RTL mirrors column order (col 5 → col 1)
- Camera controls: RTL mirrors horizontal order
- Icons: Directional icons (back, forward) auto-mirror via `autoMirrored="true"`

---

## 11. Design Token Usage in Compose

```kotlin
// colors.kt
val vaultaireColors = VaultaireColors(
    primary = Color(0xFF4A6FA5),
    // ... all tokens from design-tokens.json
)

// Theme.kt
@Composable
fun VaultaireTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = vaultaireTypography,
        shapes = vaultaireShapes,
        content = content
    )
}

// Usage in component
@Composable
fun PatternDot(state: DotState) {
    val colors = MaterialTheme.colorScheme
    val tokenColors = VaultaireTokens.colors.patternGrid
    
    val fillColor = when (state) {
        DotState.Idle -> tokenColors.dotIdle[darkTheme]!!
        DotState.Hover -> tokenColors.dotHover[darkTheme]!!
        DotState.Active -> tokenColors.dotActive[darkTheme]!!
        DotState.Connected -> tokenColors.dotConnected[darkTheme]!!
        DotState.Error -> tokenColors.dotError[darkTheme]!!
        DotState.Success -> tokenColors.dotSuccess[darkTheme]!!
    }
    
    Canvas(modifier = Modifier.size(24.dp)) {
        drawCircle(fillColor, radius = 12.dp)
    }
}
```