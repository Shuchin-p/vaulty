# Vaultaire Android — Screen-by-Screen Specifications

> **Complete screen specs** — purpose, layout (portrait/landscape/foldable), components, states, navigation, Compose structure.

---

## Screen Index

| # | Screen | Route | FLAG_SECURE | Pro Gate |
|---|--------|-------|-------------|----------|
| 1 | Splash / App Entry | `/` | No | No |
| 2 | Pattern Lock | `/lock` | Yes (grid only) | No |
| 3 | Vault Browser | `/vault/{vaultId}` | No | No |
| 4 | File Viewer | `/vault/{vaultId}/file/{fileId}` | No | No |
| 5 | Secure Camera | `/vault/{vaultId}/camera` | Yes | No |
| 6 | Camera Review | `/vault/{vaultId}/camera/review` | Yes | No |
| 7 | Recovery Phrase Display | `/recovery/display` | Yes | No |
| 8 | Secret Phrase Entry | `/recovery/enter` | Yes (input) | No |
| 9 | Duress Config | `/settings/security/duress` | No | Yes |
| 10 | App Icons | `/settings/appearance/icons` | No | Preview only |
| 11 | Backup Settings | `/settings/cloud/backup` | No | No |
| 12 | Sync Settings | `/settings/cloud/sync` | No | Yes |
| 13 | Share Create | `/vault/{vaultId}/share/create` | No | Yes |
| 14 | Share Open (Entry) | `/share/open` | Yes (input) | No |
| 15 | Share Open (Vault) | `/share/{shareId}` | Yes | No |
| 16 | Settings Main | `/settings` | No | No |
| 17 | Settings Security | `/settings/security` | No | No |
| 18 | Settings Cloud | `/settings/cloud` | No | No |
| 19 | Settings Appearance | `/settings/appearance` | No | No |
| 20 | Settings Advanced | `/settings/advanced` | No | No |
| 21 | Settings About | `/settings/about` | No | No |
| 22 | Onboarding Flow | `/onboarding/*` | Phrase step | No |
| 23 | Error/Recovery | Various | Varies | No |

---

## 1. Splash / App Entry

### Purpose
Cold start entry. Reconcile disguise icon, check vault existence, route to lock or onboarding.

### Layout

| Breakpoint | Layout |
|------------|--------|
| Phone | Full screen, centered logo |
| Tablet | Centered, 400dp max width |
| Foldable | Centered on primary display |
| Landscape | Same, centered |

### Components
- App logo (Vaultaire default or disguised)
- Loading spinner (if vault check async)
- No user interaction

### States
| State | Condition | Next Screen |
|-------|-----------|-------------|
| First launch | No vault exists | Onboarding → Pattern Lock |
| Returning | Vault exists | Pattern Lock |
| Disguise changed | Pending icon reconciliation | Apply icon → Pattern Lock |

### Navigation
```kotlin
// AppEntryViewModel
when {
    isFirstLaunch -> navigateTo(OnboardingPatternDraw)
    hasPendingIconChange -> reconcileIcon(); navigateTo(PatternLock)
    else -> navigateTo(PatternLock)
}
```

### Compose Structure
```kotlin
@Composable
fun AppEntryScreen(
    viewModel: AppEntryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        AnimatedImageVector(icon = vaultaireLogo)
        CircularProgressIndicator(modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp))
    }
}
```

---

## 2. Pattern Lock

### Purpose
Main entry point. Draw pattern to unlock vault. Also used for: create, change, duress trigger.

### Layout

#### Portrait (Phone)
```
┌─────────────────────────────────────────────────────────────┐
│  Status Bar (transparent)                                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    Vaultaire                                │  ← HeadlineSmall, centered
│                                                             │
│                    ○   ○   ○   ○   ○                       │
│                                                             │
│                    ○   ○   ○   ○   ○                       │  ← PatternGrid (280×280dp)
│                                                             │
│                    ○   ○   ●   ○   ○                       │     Centered horizontally
│                                                             │     64dp from top title
│                    ○   ○   ○   ○   ○                       │
│                                                             │
│                    ○   ○   ○   ○   ○                       │
│                                                             │
│                    [Forgot Pattern?]                        │  ← BodyMedium, centered
│                                                             │     24dp below grid
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  Navigation Bar (transparent, FLAG_SECURE on grid area)    │
└─────────────────────────────────────────────────────────────┘
```

#### Landscape (Phone)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Vaultaire                                              ○ ○ ○ ○ ○           │
│                                                                   ○ ○ ○ ○ ○ │
│                                                       ○ ○ ● ○ ○             │  Grid centered
│                                                       ○ ○ ○ ○ ○             │  vertically,
│                                                       ○ ○ ○ ○ ○             │  title left
│                                                                   [Forgot?] │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Tablet / Foldable (>600dp)
- Grid: 320×320dp (64dp cells)
- Title: HeadlineMedium
- More vertical padding

### Components
- **Toolbar**: None (immersive)
- **Title**: `Vaultaire` (HeadlineSmall, `onSurface`)
- **PatternGrid**: 5×5, centered
- **Error Message**: Below grid (BodyMedium, `error` color, animated in)
- **Forgot Pattern**: Text button (LabelLarge, `primary`)

### States

| State | Trigger | Visual |
|-------|---------|--------|
| Idle | Initial | Grid `Idle`, no error |
| Drawing | Finger down | Grid `Drawing`, line follows |
| Validating | Lift (valid pattern) | Grid `Success` (300ms) → navigate |
| Error | Lift (invalid) | Grid `Error` (500ms shake) → clear → error text |
| Lockout | 5 failures | Overlay: "Try again in 30s", countdown, grid disabled |
| Duress Trigger | Duress pattern matched | **No UI change** → silent wipe → decoy vault |

### Navigation
```kotlin
// PatternLockViewModel
sealed interface PatternLockResult {
    data class Unlock(val vaultId: String) : PatternLockResult
    data class Create(val vaultId: String) : PatternLockResult  // Onboarding
    data class Change(val vaultId: String) : PatternLockResult // Settings
    object ForgotPattern : PatternLockResult
    object DuressTriggered : PatternLockResult
}
```

### Compose Structure
```kotlin
@Composable
fun PatternLockScreen(
    viewModel: PatternLockViewModel = hiltViewModel(),
    mode: PatternLockMode = PatternLockMode.Unlock,
    onResult: (PatternLockResult) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    // WindowCompat.setDecorFitsSystemWindows(window, false)
    // WindowFlags.FLAG_SECURE on pattern grid area only
    
    Box(modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Vaultaire", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.padding(64.dp))
            PatternGrid(
                onPatternComplete = viewModel::submitPattern,
                state = viewModel.gridState,
                errorMessage = viewModel.errorMessage
            )
            Spacer(Modifier.padding(24.dp))
            if (viewModel.showError) {
                AnimatedVisibility(visible = viewModel.showError) {
                    Text(viewModel.errorMessage!!, 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.padding(16.dp))
            TextButton(onClick = { onResult(PatternLockResult.ForgotPattern) }) {
                Text("Forgot Pattern?", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
```

---

## 3. Vault Browser

### Purpose
Main unlocked view. Masonry grid of encrypted files. FAB for camera/import.

### Layout

#### Portrait (Phone <600dp)
```
┌─────────────────────────────────────────────────────────────┐
│  ←  My Vault                              [⋮]          │  ← TopAppBar
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐ ┌─────────┐                                     │
│  │  📷     │ │  📷     │  ← FileGrid (2 cols, 4:3)         │
│  │ Photo   │ │ Photo   │                                     │
│  │ 2.1 MB  │ │ 3.4 MB  │                                     │
│  │ 14:32   │ │ 14:30   │                                     │
│  └─────────┘ └─────────┘                                     │
│  ┌─────────┐ ┌─────────┐                                     │
│  │  📄     │ │  🎬     │                                     │
│  │ PDF     │ │ Video   │                                     │
│  │ 1.2 MB  │ │ 15 MB   │                                     │
│  │ 14:28   │ │ 14:25   │                                     │
│  └─────────┘ └─────────┘                                     │
│                                                             │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                    📷  +  📁          FAB   │  ← Bottom FAB
└─────────────────────────────────────────────────────────────┘
```

#### Landscape (Phone)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  ←  My Vault                                                      [⋮]  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐                           │
│  │  📷     │ │  📷     │ │  📄     │ │  🎬     │  ← 4 cols                 │
│  │ Photo   │ │ Photo   │ │ PDF     │ │ Video   │                           │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘                           │
│                                    FAB                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Tablet (600-840dp) — Two Pane
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  NavigationRail        ←  My Vault                              [⋮]       │
│  📁 Vaults             ┌───────────────────────────────────────────────┐   │
│  ☁️ Backup             │  ┌─────────┐ ┌─────────┐ ┌─────────┐        │   │
│  🔄 Sync               │  │  📷     │ │  📷     │ │  📄     │        │   │
│  🔗 Share              │  │ Photo   │ │ Photo   │ │ PDF     │        │   │
│  ⚙️ Settings           │  └─────────┘ └─────────┘ └─────────┘        │   │
│                        │  ┌─────────┐ ┌─────────┐ ┌─────────┐        │   │
│                        │  │  🎬     │ │  📷     │ │  📄     │        │   │
│                        │  │ Video   │ │ Photo   │ │ PDF     │        │   │
│                        │  └─────────┘ └─────────┘ └─────────┘        │   │
│                        │                                    FAB       │   │
│                        └───────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Desktop / Foldable (>840dp) — Three Pane
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│  NavRail    │  Vault List (VaultCard)  │  FileGrid (detail)                        │
│  📁 Vaults  │  ┌────────────────────┐  │  ┌─────────┐ ┌─────────┐ ┌─────────┐      │
│  ☁️ Backup  │  │ 🔒 Personal Docs  │  │  │  📷     │ │  📷     │ │  📄     │      │
│  🔄 Sync    │  │ 247 files · 1.2GB │  │  │ Photo   │ │ Photo   │ │ PDF     │      │
│  🔗 Share   │  │ ●●● Synced        │  │  └─────────┘ └─────────┘ └─────────┘      │
│  ⚙️ Settings│  ├────────────────────┤  │  ┌─────────┐ ┌─────────┐                  │
│             │  │ 🔒 Work Vault     │  │  │  🎬     │ │  📷     │                  │
│             │  │ 89 files · 456MB  │  │  │ Video   │ │ Photo   │                  │
│             │  │ ●○○ Offline       │  │  └─────────┘ └─────────┘                  │
│             │  └────────────────────┘  │                                         │
│             │                           │                                         │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### Components
- **TopAppBar**: Title (vault name or "My Vault"), Menu (Settings, Lock, Share)
- **FileGrid**: LazyVGrid, crossAxisCount by breakpoint
- **FAB**: Extended (Phone) / Standard (Tablet+), split: Camera / Import
- **Pull-to-refresh**: Sync trigger
- **Empty State**: Illustration + "No files" + [Add Files] [Add Samples]

### States
| State | Trigger | Visual |
|-------|---------|--------|
| Loading | Initial / pull refresh | Skeleton placeholders |
| Loaded | Files exist | FileGrid populated |
| Empty | No files | EmptyState illustration |
| Selection Mode | Long press | Checkboxes, bulk actions bar |
| Syncing | Background sync | Spinning cloud badge in toolbar |
| Error | Sync/backup failed | Snackbar + toolbar warning |

### Navigation
```kotlin
// VaultBrowserViewModel
sealed interface VaultBrowserAction {
    data class OpenFile(val fileId: String) : VaultBrowserAction
    data class OpenCamera(val mode: CameraMode) : VaultBrowserAction
    data class ImportFiles(val uris: List<Uri>) : VaultBrowserAction
    data class LockVault : VaultBrowserAction
    data class OpenSettings : VaultBrowserAction
    data class OpenShareCreate : VaultBrowserAction
    data class SelectFiles(val fileIds: Set<String>) : VaultBrowserAction
    data class DeleteFiles(val fileIds: Set<String>) : VaultBrowserAction
    data class ExportFiles(val fileIds: Set<String>) : VaultBrowserAction
}
```

### Compose Structure
```kotlin
@Composable
fun VaultBrowserScreen(
    viewModel: VaultBrowserViewModel = hiltViewModel(),
    vaultId: String,
    onAction: (VaultBrowserAction) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val scaffoldState = rememberScaffoldState()
    
    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { VaultTopAppBar(...) },
        floatingActionButton = { VaultFAB(...) },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Two-pane / three-pane logic via WindowSizeClass
            if (isTwoPane) {
                Row {
                    NavigationRail(...)
                    FileGrid(...)
                }
            } else if (isThreePane) {
                Row {
                    NavigationRail(...)
                    VaultList(...)
                    FileGrid(...)
                }
            } else {
                FileGrid(...)
            }
            
            PullToRefreshBox(
                state = viewModel.refreshState,
                onRefresh = viewModel::triggerSync
            ) { /* grid */ }
        }
    }
}
```

---

## 4. File Viewer

### Purpose
Full-screen view of decrypted photo/video/document. Zoom, share, delete, export.

### Layout

#### Portrait
```
┌─────────────────────────────────────────────────────────────┐
│  ←  photo.jpg                              [⋮]          │  ← TopAppBar
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                                                             │
│                    ████████████████████████████            │
│                    █                                         │  ← PhotoView/VideoView
│                    █        CONTENT                         █     Pinch zoom, pan
│                    █                                         █     Double-tap zoom
│                    ████████████████████████████            │
│                                                             │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  [ℹ️ Info]  [📤 Share]  [📥 Export]  [🗑️ Delete]       │  ← Bottom bar
└─────────────────────────────────────────────────────────────┘
```

#### Landscape
- Content fills width, bottom bar auto-hides after 3s

### Components
- **TopAppBar**: Back, filename, menu (Info, Rename, Move)
- **Content**: Coil AsyncImage (photo) / Media3 PlayerView (video) / PDF viewer
- **Bottom Bar**: Auto-hide, actions
- **Gesture**: Pinch zoom, double-tap, swipe down to dismiss (photo)

### States
| State | Visual |
|-------|--------|
| Loading | Blur placeholder + spinner |
| Loaded | Content visible |
| Error | Error illustration + "Cannot load" + Retry |
| Zoomed | Content scaled, pan enabled |
| Video Playing | Controls overlay, timer |

### Navigation
```kotlin
sealed interface FileViewerAction {
    data class Share(val file: EncryptedFile) : FileViewerAction
    data class Export(val file: EncryptedFile) : FileViewerAction
    data class Delete(val fileId: String) : FileViewerAction
    data class OpenInfo(val file: EncryptedFile) : FileViewerAction
    object Close : FileViewerAction
}
```

---

## 5. Secure Camera

### Purpose
Capture photos/videos directly into vault. FLAG_SECURE, no MediaStore.

### Layout (See Component Library for detailed anatomy)

#### Portrait
```
┌─────────────────────────────────────────────────────────────┐
│  ← Back          Secure Camera        [⚙️]               │  ← Toolbar (85% dark overlay)
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                                                             │
│        ██████████████████████████████████████████████████  │  ← PreviewView (CameraX)
│        █  LIVE PREVIEW    [▢] focus                      █  │
│        ██████████████████████████████████████████████████  │
│                                                             │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  [🔄]  [📷/📹]  [⚡/🔦]              ● SHUTTER ●        │  ← Controls (70% dark overlay)
│  Lens    Mode    Flash                Capture              │
└─────────────────────────────────────────────────────────────┘
```

### Components
- **Toolbar**: Back, Settings (grid lines, timestamp, etc.)
- **PreviewView**: CameraX, full bleed, FLAG_SECURE
- **Controls**: Lens, Mode, Flash/Torch, Shutter/Record
- **Focus Indicator**: Blue square at tap point

### States
| State | Shutter | Controls |
|-------|---------|----------|
| Photo Ready | 72dp white ring, 56dp dark inner | All enabled |
| Capturing | Spinner | Disabled |
| Video Ready | 72dp red ring | Torch (rear), Mode toggle |
| Recording | Red ring pulsing, timer | Stop button, torch |
| Processing | Spinner + "Encrypting..." | All disabled |

### Navigation
```kotlin
sealed interface CameraAction {
    data class CapturePhoto(val imageProxy: ImageProxy) : CameraAction
    data class CaptureVideo(val mediaStoreOutput: MediaStoreOutput) : CameraAction
    object Close : CameraAction
    object OpenSettings : CameraAction
}
```

---

## 6. Camera Review

### Purpose
Review capture before encryption. **Retake = discard, Keep = encrypt.**

### Layout — Photo
```
┌─────────────────────────────────────────────────────────────┐
│  ← Retake           Review          ✓ Keep               │  ← Top bar
├─────────────────────────────────────────────────────────────┤
│                                                             │
│        ██████████████████████████████████████████████████  │  ← Full-screen image
│        █  CAPTURED IMAGE                                    █
│        ██████████████████████████████████████████████████  │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  Photo will be encrypted into vault                        │  ← Hint
└─────────────────────────────────────────────────────────────┘
```

### Layout — Video
```
┌─────────────────────────────────────────────────────────────┐
│  ← Retake           Review          ✓ Keep               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│        ██████████████████████████████████████████████████  │
│        █  VIDEO PLAYBACK  [0:12 / 0:45]                  █  ← PlayerView
│        ██████████████████████████████████████████████████  │
│        █  ▶️ ⏸️ 🔊/🔇 🔁  📹/🎵                          █  ← Playback controls
│        ██████████████████████████████████████████████████  │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  Video will be encrypted into vault                        │
│  🔇 Audio off                          (if mic denied)    │
└─────────────────────────────────────────────────────────────┘
```

### Behavior
| Action | Transition |
|--------|------------|
| Retake | Slide down 200ms → CameraViewfinder |
| Keep (photo) | Encrypting overlay 300ms → VaultBrowser |
| Keep (video) | Finalizing overlay → VaultBrowser |
| Back gesture | Same as Retake |

---

## 7. Recovery Phrase Display

### Purpose
Show 9-word phrase at creation or in settings (re-auth). FLAG_SECURE.

### Layout
```
┌─────────────────────────────────────────────────────────────┐
│  Recovery Phrase                              (Dialog/Full) │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ⚠️  Your Recovery Phrase                                   │
│                                                             │
│  Write this down on paper.                                  │
│  Do not store digitally.                                    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ the  quiet  river  flows                            │   │  ← 3 words/line
│  │ beneath  ancient  stone                             │   │     Monospace 24sp
│  │ bridges  at  midnight                               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  [Copy]                                    [Done / Save]   │
│                                                             │
│  Auto-locks in 30s                                          │
└─────────────────────────────────────────────────────────────┘
```

### States
| Mode | Done Button | Timer |
|------|-------------|-------|
| Creation | Disabled 10s | 30s |
| Settings | Enabled | 30s |

---

## 8. Secret Phrase Entry

### Purpose
Enter recovery phrase (9 words generated, 6-24 custom) or custom phrase setup.

### Layout
```
┌─────────────────────────────────────────────────────────────┐
│  Enter Recovery Phrase                     (FLAG_SECURE)   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐  │
│  │word│ │word│ │word│ │word│ │word│ │word│ │word│ │word│  │  ← 3 cols
│  │ 1  │ │ 2  │ │ 3  │ │ 4  │ │ 5  │ │ 6  │ │ 7  │ │ 8  │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘  │
│  ┌────┐                                                     │
│  │word│                                                     │
│  │ 9  │                                                     │
│  └────┘                                                     │
│                                                             │
│  [Paste]                          [Validate / Continue]    │
│                                                             │
│  Error: "Phrase must contain exactly 9 words"              │
└─────────────────────────────────────────────────────────────┘
```

### Custom Phrase (≥6 words)
- Dynamic field count
- Add/remove field buttons
- Strength indicator

---

## 9. Duress Config (Pro)

### Purpose
Configure duress pattern + decoy vault.

### Layout
```
┌─────────────────────────────────────────────────────────────┐
│  ←  Duress Mode                      [PRO]               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Duress Pattern                                     │   │
│  │  Draw a pattern to trigger silent wipe              │   │
│  │                                                     │   │
│  │  [PatternGrid - 5x5]                                │   │
│  │                                                     │   │
│  │  [Confirm Pattern]  (enabled after draw)           │   │
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

---

## 10. App Icons (Pro)

### Purpose
Choose disguise icon.

### Layout
```
┌─────────────────────────────────────────────────────────────┐
│  ←  App Icon                                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Choose how Vaultaire appears on your Home Screen          │
│                                                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │  🔒     │ │  🧮     │ │  🖨️     │ │  📏     │  ← 2 cols phone
│  │ Vaultaire│ │Calculator│ │Printer  │ │ Units   │     4 cols tablet
│  │   ✓     │ │         │ │         │ │         │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘           │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  │  ℹ️     │ │  📐     │ │  📊     │ │  📶     │           │
│  │DeviceInfo│ │ Level  │ │Sound Mtr│ │ Network │           │
│  │         │ │         │ │         │ │         │           │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘           │
│                                                             │
│  [Pro badge on non-default]                                 │
└─────────────────────────────────────────────────────────────┘
```

### Confirmation Dialog
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

## 11. Backup Settings

### Purpose
Encrypted cloud backup toggle, status, manual trigger.

### Layout
```
┌─────────────────────────────────────────────────────────────┐
│  ←  Encrypted Cloud Backup                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Encrypted Cloud Backup                    [ON ▼]          │
│                                                             │
│  Last backup: Today, 14:32                                 │
│  Next backup: In 24h (Wi-Fi only)                          │
│  Backup size: 2.4 GB (47 files)                            │
│  Google Account: user@gmail.com              [Change]      │
│                                                             │
│  [Backup Now]          [View Backup Details]               │
│                                                             │
│  ────────────────────────────────────────                  │
│                                                             │
│  Recovery Phrase                                            │
│  ★★★★★★★★★★                 [Show Phrase]                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### States
- Enabled/Synced: Green check
- Enabled/Pending: Spinner
- Enabled/Error: Orange warning + retry
- Disabled: Gray, "Not backing up"

---

## 12. Sync Settings (Pro)

### Purpose
Cross-device sync status and control.

### Layout
```
┌─────────────────────────────────────────────────────────────┐
│  ←  Device Sync                         [PRO]            │
├─────────────────────────────────────────────────────────────┤
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

---

## 13. Share Create (Pro)

### Purpose
Create secure share with policy.

### Layout
```
┌─────────────────────────────────────────────────────────────┐
│  ←  Share Vault                      [PRO]               │
├─────────────────────────────────────────────────────────────┤
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

---

## 14. Share Open — Phrase Entry

### Purpose
Recipient enters sharing phrase.

### Layout
```
┌─────────────────────────────────────────────────────────────┐
│  ←  Open Shared Vault                     (FLAG_SECURE)   │
├─────────────────────────────────────────────────────────────┤
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

---

## 15. Share Open — Shared Vault View

### Purpose
Read-only view of shared vault.

### Layout
```
┌─────────────────────────────────────────────────────────────┐
│  ←  Shared Vault • Personal Docs      🔒 Read-only      │
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

---

## 16-21. Settings Screens

### Main Settings
```
┌─────────────────────────────────────────────────────────────┐
│  Settings                                                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ SECURITY                              [>]           │   │
│  │ Change Pattern, Recovery Phrase, Duress Mode...    │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ CLOUD                                  [>]           │   │
│  │ Encrypted Backup, Device Sync, Secure Sharing...   │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ APPEARANCE                            [>]           │   │
│  │ Theme, App Icon, Grid Density...                   │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ADVANCED                              [>]           │   │
│  │ Analytics, Cache, Diagnostics...                   │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ABOUT                                 [>]           │   │
│  │ Version, Licenses, Privacy, Security Whitepaper    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Each Section Screen
- Standard List/Navigation pattern
- Toggle items, Navigation items, Action items
- Pro badges on gated features

---

## 22. Onboarding Flow

### Step 1: Pattern Draw (Empty State)
```
┌─────────────────────────────────────────────────────────────┐
│  Draw your pattern                                          │
│                                                             │
│  [PatternGrid - 5x5]                                        │
│                                                             │
│  Minimum 6 dots, at least one turn                         │
└─────────────────────────────────────────────────────────────┘
```

### Step 2: Auto-create (No UI — Background)

### Step 3: Recovery Phrase Display (Creation Mode)
```
┌─────────────────────────────────────────────────────────────┐
│  ⚠️  Your Recovery Phrase                                   │
│                                                             │
│  Write this down on paper.                                  │
│  Do not store digitally.                                    │
│                                                             │
│  the  quiet  river  flows                                   │
│  beneath  ancient  stone                                    │
│  bridges  at  midnight                                      │
│                                                             │
│  [Copy]                    [I've Saved It]  (disabled 10s) │
│                                                             │
│  Auto-locks in 30s                                          │
└─────────────────────────────────────────────────────────────┘
```

### Step 4: Vault Home (Empty)
```
┌─────────────────────────────────────────────────────────────┐
│  No files in this vault                                     │
│                                                             │
│  [Add Files]    [Add Sample Files]                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 23. Error / Recovery Flows

### Wrong Pattern
- PatternGrid shake (500ms)
- Red flash on dots/lines
- Heavy haptic
- "Incorrect pattern" text appears
- Clear pattern, allow retry

### Pattern Lockout (5 failures)
```
┌─────────────────────────────────────────────────────────────┐
│  [PatternGrid - disabled, dimmed]                          │
│                                                             │
│  Too many attempts. Try again in 30s                       │
│                                                             │
│  ████████████░░░░░░░░░░  23s                              │  ← Progress bar
│                                                             │
│  [Forgot Pattern?]                                          │
└─────────────────────────────────────────────────────────────┘
```

### Forgot Pattern → Recovery Entry
- Navigate to SecretPhraseEntry (9 words)
- On success → Pattern Lock (Create New mode)
- New pattern → New recovery phrase display

### Backup Failed
- Snackbar: "Backup failed. Tap to retry." → Backup Settings
- Toolbar warning icon persistent until success

### Sync Conflict
- Banner: "Conflicts detected. Review?" → Conflict resolution screen
- Auto-merge for non-conflicting

### Share Expired/Revoked
- Full-screen error: "This share has expired" / "Access revoked by owner"
- [OK] → dismiss to vault browser or app entry

---

## Compose Navigation Graph

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "app_entry"
) {
    NavHost(navController, startDestination) {
        // Entry
        composable("app_entry") { AppEntryScreen() }
        
        // Onboarding
        composable("onboarding/pattern") { OnboardingPatternScreen() }
        composable("onboarding/phrase") { OnboardingPhraseScreen() }
        composable("onboarding/complete") { OnboardingCompleteScreen() }
        
        // Auth
        composable("lock") { PatternLockScreen() }
        composable("lock/create") { PatternLockScreen(mode = Create) }
        composable("lock/change") { PatternLockScreen(mode = Change) }
        
        // Vault
        composable("vault/{vaultId}") { backStackEntry ->
            VaultBrowserScreen(vaultId = backStackEntry.getString("vaultId")!!)
        }
        composable("vault/{vaultId}/file/{fileId}") { backStackEntry ->
            FileViewerScreen(
                vaultId = backStackEntry.getString("vaultId")!!,
                fileId = backStackEntry.getString("fileId")!!
            )
        }
        composable("vault/{vaultId}/camera") { backStackEntry ->
            CameraViewfinderScreen(vaultId = backStackEntry.getString("vaultId")!!)
        }
        composable("vault/{vaultId}/camera/review") { backStackEntry ->
            CameraReviewScreen(vaultId = backStackEntry.getString("vaultId")!!)
        }
        
        // Recovery
        composable("recovery/display") { RecoveryPhraseDisplayScreen() }
        composable("recovery/enter") { SecretPhraseEntryScreen() }
        
        // Settings
        composable("settings") { SettingsScreen() }
        composable("settings/security") { SettingsSecurityScreen() }
        composable("settings/security/duress") { DuressConfigScreen() }
        composable("settings/cloud/backup") { BackupSettingsScreen() }
        composable("settings/cloud/sync") { SyncSettingsScreen() }
        composable("settings/appearance") { AppearanceSettingsScreen() }
        composable("settings/appearance/icons") { DisguiseIconPickerScreen() }
        composable("settings/advanced") { AdvancedSettingsScreen() }
        composable("settings/about") { AboutSettingsScreen() }
        
        // Sharing
        composable("vault/{vaultId}/share/create") { backStackEntry ->
            ShareCreateScreen(vaultId = backStackEntry.getString("vaultId")!!)
        }
        composable("share/open") { ShareOpenEntryScreen() }
        composable("share/{shareId}") { backStackEntry ->
            SharedVaultScreen(shareId = backStackEntry.getString("shareId")!!)
        }
    }
}
```

---

## Window Size Class Breakpoints

```kotlin
// WindowSizeClass usage in screens
val windowSizeClass = remember { LocalWindowSizeClass.current }
val widthClass = windowSizeClass.windowWidthSizeClass
val heightClass = windowSizeClass.windowHeightSizeClass

// Breakpoints
// COMPACT (<600dp) → Phone
// MEDIUM (600-840dp) → Tablet/Foldable inner
// EXPANDED (>840dp) → Desktop/Foldable outer

// In VaultBrowser:
when (widthClass) {
    WindowWidthSizeClass.COMPACT -> SinglePaneFileGrid()
    WindowWidthSizeClass.MEDIUM -> TwoPaneWithNavRail()
    WindowWidthSizeClass.EXPANDED -> ThreePanePermanentNavRail()
}
```

---

## FLAG_SECURE Screens Summary

| Screen | FLAG_SECURE | Scope |
|--------|-------------|-------|
| Pattern Lock | ✅ | PatternGrid area only |
| Camera Viewfinder | ✅ | Full screen |
| Camera Review | ✅ | Full screen |
| Recovery Phrase Display | ✅ | Full screen/dialog |
| Secret Phrase Entry | ✅ | Input fields only |
| Shared Vault View | ✅ | Full screen |
| Share Open Entry | ✅ | Input field only |

**Implementation**: `window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)` on Activity/Dialog window.