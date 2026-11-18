# Vaultaire Android — Component Library Specification

> **Implementation-ready Compose component specs** — anatomy, states, sizes, behavior, API, accessibility.

---

## 1. PatternGrid

### 1.1 Purpose
The hero interaction. 5×5 dot grid for pattern-based vault unlock. Must feel like Android lock screen — muscle memory, zero cognitive load.

### 1.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│                    PatternGrid (280×280dp)                   │
│  ┌────┬────┬────┬────┬────┐  ← Row 0 (y=0)                  │
│  │ ●  │ ●  │ ●  │ ●  │ ●  │     Dot: 24dp, Cell: 56dp       │
│  ├────┼────┼────┼────┼────┤  ← Row 1 (y=1)                  │
│  │ ●  │ ●  │ ●  │ ●  │ ●  │     Spacing: 32dp between centers│
│  ├────┼────┼────┼────┼────┤  ← Row 2 (y=2)                  │
│  │ ●  │ ●  │ ●  │ ●  │ ●  │     Hit target: 56×56dp per dot │
│  ├────┼────┼────┼────┼────┤  ← Row 3 (y=3)                  │
│  │ ●  │ ●  │ ●  │ ●  │ ●  │                                 │
│  ├────┼────┼────┼────┼────┤  ← Row 4 (y=4)                  │
│  │ ●  │ ●  │ ●  │ ●  │ ●  │                                 │
│  └────┴────┴────┴────┴────┘                                 │
│                                                              │
│  Lines: 3dp stroke, round cap, connect centers               │
│  Shadow: elevation3 behind entire grid                       │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 Dot States

| State | Visual | Haptic | Trigger |
|-------|--------|--------|---------|
| `Idle` | `dotIdle` color, 24dp, no stroke | — | Default |
| `Hover` | `dotHover` color, 27.6dp (115%), 2dp stroke | Light | Touch exploration / hover |
| `Active` | `dotActive` color, 28.8dp (120%), 3dp stroke | Light | Finger down |
| `Connected` | `dotConnected` color, 26.4dp (110%), 3dp stroke | Light (per interpolated) | Part of drawn pattern |
| `Error` | `dotError` color, 26.4dp, 3dp stroke | Heavy | Wrong pattern submitted |
| `Success` | `dotSuccess` color, 28.8dp, 3dp stroke | Medium | Correct pattern submitted |

### 1.4 Line States

| State | Visual | Animation |
|-------|--------|-----------|
| `Drawing` | `lineActive`, 3dp, dash-offset anim | Continuous as finger moves |
| `Connected` | `lineConnected`, 3dp | Static after lift |
| `Error` | `lineError`, 3dp | Shake with dots |

### 1.5 Behavior

| Interaction | Response |
|-------------|----------|
| Touch down on dot | Dot → `Active`, haptic light, line starts |
| Drag to adjacent dot | Line draws smoothly, target → `Hover` → `Active` |
| Drag over intermediate (knight move) | Intermediate dots → `Connected` sequentially, haptic each |
| Lift finger | Pattern submitted → `validatePattern()` |
| Valid pattern | All dots → `Success` (300ms), checkmark, vault unlock |
| Invalid pattern | All dots → `Error` (500ms shake), heavy haptic, clear |
| Empty lift (< 6 dots) | Clear, no error (silent) |

### 1.6 Sizes

| Breakpoint | Grid Size | Dot Size | Cell Size |
|------------|-----------|----------|-----------|
| Phone (<600dp) | 280×280dp | 24dp | 56dp |
| Tablet (600-840dp) | 320×320dp | 28dp | 64dp |
| Desktop (>840dp) | 360×360dp | 32dp | 72dp |

### 1.7 Compose API Sketch

```kotlin
@Composable
fun PatternGrid(
    onPatternComplete: (List<PatternPoint>) -> Unit,
    state: PatternGridState = PatternGridState.Idle,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    showNumbers: Boolean = false // Accessibility
) {
    // 5x5 grid of PatternDot
    // Touch handling with gesture detector
    // Line drawing via Canvas
    // Haptic feedback via VibrationEffect
    // TalkBack: custom AccessibilityNodeInfo for grid
}

enum class PatternGridState {
    Idle, Drawing, Validating, Success, Error
}

data class PatternPoint(val row: Int, val col: Int) // 0-4 each

@Composable
fun PatternDot(
    state: DotState,
    position: PatternPoint,
    showNumber: Boolean = false,
    onClick: () -> Unit
) { /* ... */ }
```

### 1.8 Accessibility
- **Role**: `Grid` with 25 items
- **Navigation**: Arrow keys move between dots (row/col)
- **Selection**: Space/Enter selects current dot
- **Announcement**: "Dot row {r+1} column {c+1}, {state}"
- **Focus indicator**: 3dp outline in `primary` color
- **Reduced motion**: Disable scale/line animations
- **High contrast**: `dotIdle` = `onSurface`, `dotActive` = `primary`

---

## 2. VaultCard

### 2.1 Purpose
Used in Settings > Vaults for vault management (rename, delete, switch). **Not shown on main unlock** — hidden vaults principle.

### 2.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  VaultCard (full width, 16dp radius, elevation1)            │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ [Vault Icon]  Vault Name                    [⋮ Menu]  │  │
│  │         247 files · 1.2 GB · Last opened 2h ago       │  │
│  │         [Sync: ●●●]  [Backup: ✓]  [Hidden: 🔒]        │  │
│  └────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 States
| State | Background | Elevation | Outline |
|-------|------------|-----------|---------|
| Default | `surfaceContainer` | 1 | none |
| Hovered | `surfaceContainerHigh` | 2 | none |
| Pressed | `surfaceContainerHighest` | 0 | none |
| Selected | `primaryContainer` | 2 | `primary`, 2dp |
| Disabled | `surfaceContainerLow` | 0 | none, 50% opacity |

### 2.4 Compose API

```kotlin
@Composable
fun VaultCard(
    vault: VaultInfo,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Card with metadata badges
    // Menu: Rename, Delete, Set as Decoy, Export Backup
}
```

---

## 3. FileGrid

### 3.1 Purpose
Masonry grid of encrypted files in unlocked vault. Thumbnails decrypt on-demand.

### 3.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  FileGrid (LazyVGrid / LazyHorizontalGrid)                  │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │  📷     │ │  📷     │ │  📄     │ │  🎬     │           │
│  │ Photo   │ │ Photo   │ │ PDF     │ │ Video   │           │
│  │ 2.1 MB  │ │ 3.4 MB  │ │ 1.2 MB  │ │ 15 MB   │           │
│  │ 14:32   │ │ 14:30   │ │ 14:28   │ │ 14:25   │           │
│  │ [✓]     │ │ [ ]     │ │ [ ]     │ │ [ ]     │  ← Selection
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘           │
│                                                             │
│  Empty State:                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                                                     │   │
│  │           [Vault Illustration]                      │   │
│  │                                                     │   │
│  │           No files in this vault                    │   │
│  │                                                     │   │
│  │           [Add Files]    [Add Sample Files]         │   │
│  │                                                     │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 Item States
| State | Visual | Overlay |
|-------|--------|---------|
| Default | Thumbnail + metadata | — |
| Hovered | Slight scale (1.02) | — |
| Selected | `primaryContainer` bg, 2dp `primary` border | Checkmark badge |
| Loading | Blur placeholder (10dp radius) | Spinner |
| Error | `errorContainer` bg | Error icon, "Tap to retry" |

### 3.4 Column Count per Breakpoint
| Breakpoint | Columns | Aspect Ratio |
|------------|---------|--------------|
| Phone (<600dp) | 2 | 4:3 |
| Tablet (600-840dp) | 3 | 4:3 |
| Desktop (>840dp) | 4-5 | 4:3 |

### 3.5 Compose API

```kotlin
@Composable
fun FileGrid(
    files: List<EncryptedFile>,
    onFileClick: (EncryptedFile) -> Unit,
    onFileLongClick: (EncryptedFile) -> Unit, // Selection mode
    selectedFiles: Set<String> = emptySet(),
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    // LazyVGrid with crossAxisCount by breakpoint
    // Coil for thumbnail loading (decrypt stream)
    // Pull-to-refresh for sync
}

@Composable
fun FileGridItem(
    file: EncryptedFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }

@Composable
fun FileGridEmptyState(
    onAddFiles: () -> Unit,
    onAddSamples: () -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }
```

### 3.6 Accessibility
- **Grid role**: 2D navigation (arrows)
- **Item label**: "{name}, {type}, {size}, {date}"
- **Selection**: "Selected, {name}" / "Not selected, {name}"
- **Long press**: "Double tap to select, swipe to deselect"

---

## 4. CameraViewfinder

### 4.1 Purpose
Secure camera preview with FLAG_SECURE. Full-screen, minimal chrome.

### 4.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  CameraViewfinder (FLAG_SECURE, full screen)                │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ← Back                                    [⚙️]     │   │  Toolbar (85% opaque dark)
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│                                                             │
│        ██████████████████████████████████████████████████  │   ← PreviewView
│        █                                                 █  │
│        █              LIVE PREVIEW                        █  │
│        █            [▢] Focus indicator                   █  │
│        █                                                 █  │
│        ██████████████████████████████████████████████████  │
│                                                             │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ [🔄]  [📷/📹]  [⚡/🔦]              ● SHUTTER ●     │   │  Controls (70% opaque)
│  │  Lens    Mode     Flash                Capture       │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 Controls
| Control | States | Action |
|---------|--------|--------|
| Lens Switch | Front/Rear (only if both exist) | Toggle camera |
| Mode Toggle | Photo/Video | Switch capture mode |
| Flash | Auto/On/Off (photo) | Cycle |
| Torch | On/Off (video, rear only) | Toggle |
| Shutter | Photo: 72dp white ring, 56dp dark inner | Capture |
| Record | 72dp red ring, pulsing when recording | Start/Stop |

### 4.4 Focus Tap
- Tap preview → `focusIndicator` (blue square, 80dp) appears at tap point
- Auto-focus + auto-exposure at point
- Indicator fades after 2s

### 4.5 Compose API

```kotlin
@Composable
fun CameraViewfinder(
    onCapture: (ImageProxy) -> Unit, // Photo
    onRecordStart: () -> Unit,
    onRecordStop: (MediaStoreOutput) -> Unit, // Video
    onClose: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    // CameraX PreviewView with FLAG_SECURE
    // Custom controls overlay
    // Focus tap handling
    // Lifecycle-aware (pause on background)
}

@Composable
fun CameraControls(
    cameraMode: CameraMode,
    flashMode: FlashMode,
    torchEnabled: Boolean,
    onModeToggle: () -> Unit,
    onLensToggle: () -> Unit,
    onFlashCycle: () -> Unit,
    onTorchToggle: () -> Unit,
    onShutterClick: () -> Unit,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }
```

### 4.6 Accessibility
- **Toolbar**: Back, Settings
- **Controls**: contentDescription per state ("Flash auto", "Front camera")
- **Focus order**: Toolbar → Preview (non-focusable) → Controls
- **Announcements**: "Focus locked at top left", "Recording started", "Recording stopped"
- **FLAG_SECURE**: No preview in TalkBack, no screenshots

---

## 5. CameraReview

### 5.1 Purpose
Review captured photo/video before encryption. **Critical UX: Retake discards, Keep encrypts.**

### 5.2 Anatomy (Photo)
```
┌─────────────────────────────────────────────────────────────┐
│  CameraReview (FLAG_SECURE, full screen)                    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ← Retake                                    ✓ Keep  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│        ██████████████████████████████████████████████████  │
│        █                                                 █  │
│        █              CAPTURED IMAGE                      █  │  Full-screen ImageView
│        █                                                 █  │
│        ██████████████████████████████████████████████████  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Photo will be encrypted into vault                 │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 Anatomy (Video)
```
┌─────────────────────────────────────────────────────────────┐
│  CameraReview (FLAG_SECURE)                                 │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ← Retake                                    ✓ Keep  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│        ██████████████████████████████████████████████████  │
│        █                                                 █  │
│        █              VIDEO PLAYBACK                      █  │  Media3 PlayerView
│        █              [0:12 / 0:45]                       █  │
│        █                                                 █  │
│        ██████████████████████████████████████████████████  │
│        █  ▶️  ⏸️  🔊/🔇  🔁  📹/🎵                      █  │  Playback controls
│        ██████████████████████████████████████████████████  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Video will be encrypted into vault                 │   │
│  │  🔇 Audio off                          ← if no mic  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 5.4 Behavior
| Action | Result |
|--------|--------|
| Retake | Discard buffer, return to CameraViewfinder (slide down 200ms) |
| Keep (photo) | Encrypt via AES-256-GCM → `.vlt` → generate thumbnail → return to FileGrid |
| Keep (video) | Finalize encryption → `.vlt` → generate poster → return to FileGrid |
| Back gesture | Same as Retake |
| Screen lock | Discard buffer, lock app |

### 5.5 Compose API

```kotlin
@Composable
fun CameraReview(
    media: ReviewMedia, // Photo(ImageProxy) | Video(MediaItem)
    onRetake: () -> Unit,
    onKeep: (ReviewMedia) -> Unit, // Triggers encryption
    modifier: Modifier = Modifier.fillMaxSize()
) {
    // Photo: Coil/ImageView with decrypted bitmap
    // Video: Media3 PlayerView with MediaItem
    // FLAG_SECURE on window
    // Encryption progress overlay on Keep
}

sealed interface ReviewMedia {
    data class Photo(val imageProxy: ImageProxy) : ReviewMedia
    data class Video(val mediaItem: MediaItem) : ReviewMedia
}
```

---

## 6. RecoveryPhraseDisplay

### 6.1 Purpose
Show 9-word grammatical recovery phrase **once** at vault creation. Also in Settings (re-auth required). FLAG_SECURE, no screenshots, clipboard auto-clear.

### 6.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  RecoveryPhraseDisplay (FLAG_SECURE, dialog/fullscreen)     │
│                                                             │
│  ⚠️  Your Recovery Phrase                                   │
│                                                             │
│  Write this down on paper.                                  │
│  Do not store digitally.                                    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ the  quiet  river  flows                            │   │  3 words/line
│  │ beneath  ancient  stone                             │   │  Monospace 24sp
│  │ bridges  at  midnight                               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  [Copy]                                    [Done / Save]   │
│                                                             │
│  Auto-locks in 30s                                          │
└─────────────────────────────────────────────────────────────┘
```

### 6.3 States
| State | Done Button | Timer |
|-------|-------------|-------|
| Creation (first show) | Disabled 10s → Enabled | 30s auto-lock |
| Settings view | Enabled immediately | 30s auto-lock |

### 6.4 Behavior
| Action | Result |
|--------|--------|
| Copy | Copy normalized phrase (single spaces), toast "Copied — clears in 60s", clipboard clear in 60s |
| Done/Save | Dismiss, mark phrase acknowledged |
| Auto-lock (30s inactivity) | Clear phrase from UI, return to previous screen, require re-auth |
| Screen rotation | Preserve phrase, restart timer |
| Background | Immediate auto-lock |

### 6.5 Compose API

```kotlin
@Composable
fun RecoveryPhraseDisplay(
    phrase: String, // 9 words, normalized
    mode: DisplayMode = DisplayMode.Creation,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    onAutoLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    // FLAG_SECURE via LocalContext.current.window.setFlags(...)
    // Monospace typography, 3 words per line
    // Countdown timer (30s)
    // ClipboardManager with delayed clear
}

enum class DisplayMode { Creation, Settings }
```

### 6.6 Accessibility
- **FLAG_SECURE**: Blocks TalkBack from reading phrase automatically
- **Custom AccessibilityDelegate**: On-demand reading only
- **Copy button**: "Copy recovery phrase to clipboard, clears in 60 seconds"
- **Words**: Each word focusable individually for screen reader

---

## 7. SecretPhraseInput

### 7.1 Purpose
Enter custom recovery phrase (≥6 words) or enter phrase for recovery. Word-by-word input with validation.

### 7.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  SecretPhraseInput                                          │
│                                                             │
│  Enter your recovery phrase                                 │
│                                                             │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐  │
│  │word│ │word│ │word│ │word│ │word│ │word│ │word│ │word│  │  9 fields (generated)
│  │  1 │ │  2 │ │  3 │ │  4 │ │  5 │ │  6 │ │  7 │ │  8 │  │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘  │
│  ┌────┐                                                    │
│  │word│                                                    │
│  │  9 │                                                    │
│  └────┘                                                    │
│                                                             │
│  [Paste]                          [Validate / Continue]    │
│                                                             │
│  Error: "Phrase must contain at least 6 words"             │
└─────────────────────────────────────────────────────────────┘
```

### 7.3 Behavior
| Input | Handling |
|-------|----------|
| Type word | Auto-advance to next field on space/comma |
| Paste | Split by whitespace, fill fields sequentially |
| Backspace empty | Move to previous field |
| Validate | Normalize (lowercase, trim, single space), check word count, strength |
| Generated phrase entry | Exactly 9 fields, all required |
| Custom phrase entry | 6-24 fields, dynamic add/remove |

### 7.4 Compose API

```kotlin
@Composable
fun SecretPhraseInput(
    expectedWordCount: Int? = null, // null = custom (6-24), 9 = generated
    onPhraseEntered: (String) -> Unit, // Normalized phrase
    onValidationError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic field grid (3 columns)
    // Auto-focus management
    // Paste handling
    // Real-time validation feedback
}

@Composable
fun PhraseWordField(
    index: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onFocusNext: () -> Unit,
    onFocusPrevious: () -> Unit,
    isError: Boolean = false
) { /* ... */ }
```

### 7.5 Accessibility
- **Label**: "Word {index} of {total}"
- **Hint**: "Enter word {index}"
- **Error**: Announced on validation failure
- **Paste**: "Pasted {count} words"

---

## 8. DuressConfig

### 8.1 Purpose (Pro)
Configure duress pattern + decoy vault. Pro-gated.

### 8.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  DuressConfig (Pro badge)                                   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Duress Pattern                                     │   │
│  │  Draw a pattern to trigger silent wipe              │   │
│  │                                                     │   │
│  │  [PatternGrid - 5x5]                                │   │
│  │                                                     │   │
│  │  [Confirm Pattern]  (button, enabled after draw)    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Decoy Vault                                        │   │
│  │  Select vault to open after duress trigger          │   │
│  │                                                     │   │
│  │  [VaultCard list - radio selection]                 │   │
│  │  [Create New Decoy Vault]                           │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ⚠️  Duress mode silently destroys all non-decoy data.     │
│  This cannot be undone.                                    │
│                                                             │
│  [Save Configuration]                                       │
└─────────────────────────────────────────────────────────────┘
```

### 8.3 Behavior
1. Draw duress pattern (min 6 dots, same validation)
2. Confirm pattern (draw again)
3. Select decoy vault (must exist or create new)
4. Save → SHA-256(pattern) stored in Keystore
5. On trigger: multi-pass wipe → open decoy

### 8.4 Compose API

```kotlin
@Composable
fun DuressConfigScreen(
    onSave: (DuressConfig) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // PatternGrid for duress pattern (2x confirmation)
    // VaultCard radio group for decoy selection
    // Pro entitlement check
    // Warning notice
}

data class DuressConfig(
    val patternHash: String, // SHA-256 hex
    val decoyVaultId: String
)
```

---

## 9. DisguiseIconPicker

### 9.1 Purpose (Pro)
Choose alternate app icon from 7 utility disguises + default.

### 9.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  DisguiseIconPicker                                         │
│                                                             │
│  Choose how Vaultaire appears on your Home Screen          │
│                                                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │  🔒     │ │  🧮     │ │  🖨️     │ │  📏     │           │
│  │ Vaultaire│ │Calculator│ │Printer  │ │ Units   │           │
│  │   ✓     │ │         │ │         │ │         │           │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │  ℹ️     │ │  📐     │ │  📊     │ │  📶     │           │
│  │DeviceInfo│ │ Level  │ │Sound Mtr│ │ Network │           │
│  │         │ │         │ │         │ │         │           │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘           │
│                                                             │
│  [Pro badge on non-default]                                 │
│                                                             │
│  Tapping non-default (Free): "App Icons is a Pro feature"  │
│  Tapping non-default (Pro): Confirmation dialog → Apply    │
└─────────────────────────────────────────────────────────────┘
```

### 9.3 Grid Layout
| Breakpoint | Columns |
|------------|---------|
| Phone | 2 |
| Tablet | 4 |
| Desktop | 4 |

### 9.4 Compose API

```kotlin
@Composable
fun DisguiseIconPicker(
    currentIcon: DisguiseIcon,
    onIconSelected: (DisguiseIcon) -> Unit,
    isPro: Boolean,
    modifier: Modifier = Modifier
) {
    // LazyVGrid of DisguiseIconOption
    // Checkmark overlay on current
    // Pro gate on click
}

@Composable
fun DisguiseIconOption(
    icon: DisguiseIcon,
    isSelected: Boolean,
    isProFeature: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }

enum class DisguiseIcon {
    VAULTAIRE, CALCULATOR, PRINTER, UNITS, 
    DEVICE_INFO, LEVEL, SOUND_METER, NETWORK
}

@Composable
fun IconChangeConfirmationDialog(
    newIcon: DisguiseIcon,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) { /* ... */ }
```

### 9.5 Confirmation Dialog
```
┌─────────────────────────────────────────────────────────────┐
│  Change App Icon?                                           │
│                                                             │
│  [Icon Preview]                                             │
│                                                             │
│  The app icon will update on your Home Screen.             │
│  Vaultaire name stays unchanged.                           │
│                                                             │
│  [Cancel]                        [Confirm]                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 10. BackupStatus

### 10.1 Purpose
Show encrypted cloud backup status in Settings.

### 10.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  BackupStatus                                               │
│                                                             │
│  Encrypted Cloud Backup                    [ON ▼]          │
│                                                             │
│  Last backup: Today, 14:32                                 │
│  Next backup: In 24h (Wi-Fi only)                          │
│  Backup size: 2.4 GB (47 files)                            │
│  Google Account: user@gmail.com              [Change]      │
│                                                             │
│  [Backup Now]          [View Backup Details]               │
└─────────────────────────────────────────────────────────────┘
```

### 10.3 States
| State | Indicator | Message |
|-------|-----------|---------|
| Enabled, synced | ✅ Green cloud | "Backed up today" |
| Enabled, pending | 🔄 Spinning cloud | "Backup in progress..." |
| Enabled, error | ⚠️ Orange cloud | "Backup failed — tap to retry" |
| Disabled | ⚪ Gray cloud | "Not backing up to cloud" |

### 10.4 Compose API

```kotlin
@Composable
fun BackupStatus(
    state: BackupState,
    onToggle: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onViewDetails: () -> Unit,
    onChangeAccount: () -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }

sealed interface BackupState {
    data class Enabled(
        val lastBackup: Instant?,
        val nextBackup: Instant?,
        val sizeBytes: Long,
        val fileCount: Int,
        val accountEmail: String,
        val status: SyncStatus // Synced, Pending, Error
    ) : BackupState
    object Disabled : BackupState
}
```

---

## 11. SyncStatus

### 11.1 Purpose (Pro)
Show device sync status across linked devices.

### 11.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  SyncStatus (Pro badge)                                     │
│                                                             │
│  Device Sync                                    [ON ▼]     │
│                                                             │
│  This device + 2 other devices                             │
│  Last sync: 5 minutes ago                                  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 📱 This Device (Pixel 8)        ●●●  Synced        │   │
│  │ 📱 Tablet (Galaxy Tab)          ●●●  Synced        │   │
│  │ 📱 Phone (iPhone via Drive)     ●○○  Offline       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  [Pause Sync]          [Disconnect This Device]            │
│                                                             │
│  Conflicts: 0 pending                                       │
└─────────────────────────────────────────────────────────────┘
```

### 11.3 Device Status Indicators
| Indicator | Meaning |
|-----------|---------|
| 🟢 ●●● | Online, synced |
| 🟡 ●○○ | Online, pending changes |
| 🔴 ○○○ | Offline |
| ⚫ ⚫⚫ | Duress-isolated (never shown in list) |

### 11.4 Compose API

```kotlin
@Composable
fun SyncStatus(
    state: SyncState,
    onToggle: (Boolean) -> Unit,
    onPause: () -> Unit,
    onDisconnect: () -> Unit,
    onResolveConflicts: () -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }

data class SyncState(
    val enabled: Boolean,
    val deviceCount: Int,
    val lastSync: Instant?,
    val devices: List<SyncDevice>,
    val conflictCount: Int
)

data class SyncDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val status: DeviceSyncStatus,
    val lastSeen: Instant?
)
```

---

## 12. ShareCreate

### 12.1 Purpose (Pro)
Create a secure share with policy (expiry, max opens, no-export).

### 12.2 Anatomy
```
┌─────────────────────────────────────────────────────────────┐
│  ShareCreate (Pro badge)                                    │
│                                                             │
│  Your Sharing Phrase                                        │
│  ────────────────────                                       │
│  abandon ability able about above absent                   │
│  absorb abstract absurd abuse access                       │
│  accident account accuse achieve                           │
│  [Regenerate]          [Copy]                              │
│                                                             │
│  ────────────────────────────────────────                  │
│                                                             │
│  Access Controls                                            │
│  ┌ Expiration Date          [Never ▼]  [Custom Date]      │
│  ┌ Max Opens                [Unlimited ▼] [1] [3] [5] [10]│
│  ┌ ☑ Disable Export & Screenshots                         │
│                                                             │
│  ────────────────────────────────────────                  │
│                                                             │
│  [Create Share]  (Primary, Pro color)                      │
│                                                             │
│  ⚠ Send the phrase through a trusted channel.              │
└─────────────────────────────────────────────────────────────┘
```

### 12.3 Compose API

```kotlin
@Composable
fun ShareCreateScreen(
    vaultId: String,
    onCreate: (SharePolicy, String) -> Unit, // policy, phrase
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Phrase display with regenerate
    // Policy controls
    // Pro entitlement check
}

data class SharePolicy(
    val expiration: Instant? = null,
    val maxOpens: Int = Int.MAX_VALUE,
    val disableExport: Boolean = false
)
```

---

## 13. ShareOpen

### 13.1 Purpose
Recipient enters sharing phrase to access read-only shared vault.

### 13.2 Anatomy (Phrase Entry)
```
┌─────────────────────────────────────────────────────────────┐
│  ShareOpen                                                  │
│                                                             │
│  Enter the 12-word sharing phrase                          │
│  you received from the vault owner.                        │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ abandon ability able about above absent absorb ...   │  │
│  └──────────────────────────────────────────────────────┘  │
│         [Paste]        [Scan QR]                          │
│                                                             │
│  [Open Vault]  (disabled until 12 valid words)           │
└─────────────────────────────────────────────────────────────┘
```

### 13.3 Anatomy (Shared Vault View)
```
┌─────────────────────────────────────────────────────────────┐
│  Shared Vault • Personal Docs              🔒 Read-only    │
│     Expires in 7 days • 3/5 opens                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📁 Tax Returns          3 files  12 MB                    │
│  📁 Medical Records      1 file   4 MB                     │
│  📄 passport-scan.pdf    2.1 MB                            │
│                                                             │
│  [Pull to refresh for updates]                             │
│                                                             │
│  Long-press → "View" only (no Export/Share)                │
│  Screenshot attempt → Vault locks + "Screenshots disabled" │
└─────────────────────────────────────────────────────────────┘
```

### 13.4 Compose API

```kotlin
@Composable
fun ShareOpenScreen(
    onPhraseSubmitted: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) { /* Phrase entry */ }

@Composable
fun SharedVaultScreen(
    vault: SharedVault,
    onRefresh: () -> Unit,
    onFileClick: (SharedFile) -> Unit,
    modifier: Modifier = Modifier
) { /* Read-only FileGrid */ }

data class SharedVault(
    val id: String,
    val name: String,
    val policy: SharePolicy,
    val expiresAt: Instant?,
    val opensUsed: Int,
    val files: List<SharedFile>
)
```

---

## 14. SettingsScreen

### 14.1 Structure
```
SettingsScreen
├── Security
│   ├── Change Pattern
│   ├── Recovery Phrase (re-auth)
│   ├── Duress Mode (Pro)
│   ├── Biometric Unlock
│   └── Auto-lock Timeout
├── Cloud
│   ├── Encrypted Cloud Backup
│   ├── Device Sync (Pro)
│   └── Secure Sharing (Pro)
├── Appearance
│   ├── Theme (System / Light / Dark)
│   ├── App Icon (Pro)
│   └── Grid Density (Comfortable / Compact)
├── Advanced
│   ├── Analytics (Anonymous)
│   ├── Cache Management
│   └── Diagnostic Data
└── About
    ├── Version
    ├── Licenses
    ├── Privacy Policy
    └── Security Whitepaper
```

### 14.2 Compose API

```kotlin
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Scaffold with top app bar
    // LazyColumn of SettingsSection
    // Each section: Header + SettingsItem list
}

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingsItem>,
    modifier: Modifier = Modifier
) { /* ... */ }

sealed interface SettingsItem {
    data class Toggle(
        val title: String,
        val subtitle: String?,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val proRequired: Boolean = false
    ) : SettingsItem
    
    data class Navigation(
        val title: String,
        val subtitle: String?,
        val icon: ImageVector?,
        val onClick: () -> Unit,
        val proRequired: Boolean = false,
        val badge: String? = null // "PRO"
    ) : SettingsItem
    
    data class Action(
        val title: String,
        val subtitle: String?,
        val onClick: () -> Unit,
        val destructive: Boolean = false
    ) : SettingsItem
}
```

---

## 15. OnboardingFlow

### 15.1 Zero-Config Flow
```
┌─────────────────────────────────────────────────────────────┐
│  OnboardingFlow (no splash, no welcome)                     │
│                                                             │
│  Step 1: PatternLockScreen (empty state)                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Draw your pattern                                  │   │
│  │  [PatternGrid]                                      │   │
│  │  Minimum 6 dots, at least one turn                 │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                    │
│         ▼ Valid pattern (≥6 pts, 1 turn)                   │
│                                                             │
│  Step 2: Auto-create vault (no UI)                         │
│  - Generate MVK, wrap with VDK, save .vlt                  │
│  - Generate 9-word recovery phrase                         │
│         │                                                    │
│         ▼                                                    │
│                                                             │
│  Step 3: RecoveryPhraseDisplay (Creation mode)             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ⚠️ Your Recovery Phrase                            │   │
│  │  Write this down on paper.                          │   │
│  │  [9-word phrase]                                    │   │
│  │  [Copy]                    [I've Saved It] (10s)   │   │
│  └─────────────────────────────────────────────────────┘   │
│         │                                                    │
│         ▼ Dismiss                                            │
│                                                             │
│  Step 4: VaultHomeScreen (empty FileGrid)                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  No files in this vault                             │   │
│  │  [Add Files]    [Add Sample Files]                 │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 15.2 Compose API

```kotlin
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State machine: PatternDraw → VaultCreate → PhraseDisplay → Complete
    // No back navigation during onboarding
    // System back = exit app
}
```

---

## 16. Error / Empty States

### 16.1 Standard Error States

| Error | Visual | Action |
|-------|--------|--------|
| Wrong Pattern | PatternGrid shake + red flash + "Incorrect pattern" | Retry |
| Pattern Lockout (5 failures) | Overlay: "Too many attempts. Try again in 30s" | Countdown |
| No Vault Found | Empty vault state + "Add Files" | Create first vault |
| Backup Failed | ⚠️ Orange banner + "Backup failed. Tap to retry" | Retry |
| Sync Conflict | 🟡 Yellow banner + "Conflicts detected. Review?" | Navigate to conflicts |
| Share Expired | 🔴 Red banner + "This share has expired" | Dismiss |
| Share Revoked | 🔴 Red banner + "Access revoked by owner" | Dismiss |
| Camera Permission Denied | Settings deep link + "Camera access required" | Open Settings |
| Storage Full | 🔴 Red dialog + "Not enough space. Free up storage." | Dismiss |
| Network Error | 🟡 Yellow snackbar + "Offline. Changes queued." | Auto-retry |

### 16.2 Empty States

| Screen | Illustration | Title | Action |
|--------|--------------|-------|--------|
| Vault Browser | Vault illustration | "No files in this vault" | [Add Files] [Add Samples] |
| Shared Vault | Shared illustration | "No files shared" | — |
| My Shares | Share illustration | "No active shares" | [Create Share] (Pro) |
| Sync Devices | Sync illustration | "No devices connected" | [Enable Sync] (Pro) |
| Backup | Cloud illustration | "No backup configured" | [Enable Backup] |

### 16.3 Compose API

```kotlin
@Composable
fun ErrorState(
    error: VaultError,
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) { /* ... */ }

@Composable
fun EmptyState(
    illustration: ImageVector,
    title: String,
    message: String,
    primaryAction: EmptyStateAction?,
    secondaryAction: EmptyStateAction?,
    modifier: Modifier = Modifier
) { /* ... */ }

data class EmptyStateAction(
    val label: String,
    val onClick: () -> Unit
)
```

---

## 17. Component State Matrix Summary

| Component | States | Breakpoints | Pro Gate | FLAG_SECURE |
|-----------|--------|-------------|----------|-------------|
| PatternGrid | 6 dot states, 3 line states | 3 | No | Lock screen only |
| VaultCard | 5 | 3 | No | No |
| FileGrid | 5 item states | 3 cols | No | No |
| CameraViewfinder | 4 control states | 1 (full) | No | ✅ Full |
| CameraReview | 2 (photo/video) | 1 (full) | No | ✅ Full |
| RecoveryPhraseDisplay | 2 modes | 1 | No | ✅ Full |
| SecretPhraseInput | Validation states | 1 | No | ✅ Entry only |
| DuressConfig | Setup steps | 1 | ✅ | No |
| DisguiseIconPicker | Selected/Pro | 3 | Preview only | No |
| BackupStatus | 4 states | 1 | No | No |
| SyncStatus | 4 device states | 1 | ✅ | No |
| ShareCreate | Policy states | 1 | ✅ | No |
| ShareOpen | Entry/View | 1 | Open: No | View: ✅ |
| SettingsScreen | Section states | 3 | Per item | No |
| OnboardingFlow | 4 steps | 1 | No | Phrase step |
| Error/Empty | Per error type | 3 | No | No |