# Vaultaire Android — Responsive Breakpoints & Adaptive Layouts

> **Navigation3 Scenes-based adaptive layouts** for Phone, Tablet, Foldable, and Desktop. Implementation-ready with Compose APIs.

---

## 1. Breakpoint Definitions

### 1.1 Window Size Classes (Material 3)

```kotlin
// WindowSizeClass from androidx.compose.material3:material3-window-size-class
// COMPACT    < 600dp  → Phone (portrait), Foldable inner (portrait)
// MEDIUM     600–840dp → Tablet, Foldable inner (landscape), Foldable outer (portrait)
// EXPANDED   > 840dp  → Desktop, Foldable outer (landscape), Large tablets

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val windowMetrics = LocalWindowMetrics.current
    val widthDp = with(LocalDensity.current) { windowMetrics.bounds.width().toDp() }
    val heightDp = with(LocalDensity.current) { windowMetrics.bounds.height().toDp() }
    
    val widthSizeClass = when {
        widthDp < 600.dp -> WindowWidthSizeClass.COMPACT
        widthDp < 840.dp -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }
    
    val heightSizeClass = when {
        heightDp < 480.dp -> WindowHeightSizeClass.COMPACT
        heightDp < 900.dp -> WindowHeightSizeClass.MEDIUM
        else -> WindowHeightSizeClass.EXPANDED
    }
    
    return WindowSizeClass(widthSizeClass, heightSizeClass)
}
```

### 1.2 Breakpoint Tokens (from design-tokens.json)

| Breakpoint | Width Range | Layout Archetype | Navigation |
|------------|-------------|------------------|------------|
| **Phone** | < 600dp | Single column | Bottom Navigation Bar |
| **Tablet** | 600–840dp | Two-pane | Navigation Rail (collapsible) |
| **Desktop/Foldable** | > 840dp | Three-pane | Permanent Navigation Rail |

### 1.3 Device Form Factors

| Form Factor | Typical Width | Breakpoint | Special Handling |
|-------------|---------------|------------|------------------|
| Phone (portrait) | 360–430dp | COMPACT | — |
| Phone (landscape) | 640–800dp | MEDIUM | Two-pane if >600dp |
| Foldable inner (portrait) | 500–650dp | COMPACT/MEDIUM | Hinge awareness |
| Foldable inner (landscape) | 800–900dp | MEDIUM/EXPANDED | Two/three-pane |
| Foldable outer (portrait) | 360–430dp | COMPACT | Quick access |
| Foldable outer (landscape) | 700–900dp | MEDIUM/EXPANDED | — |
| Tablet (portrait) | 600–840dp | MEDIUM | Two-pane |
| Tablet (landscape) | 840–1200dp | EXPANDED | Three-pane |
| Desktop/ChromeOS | 1024dp+ | EXPANDED | Three-pane, windowed |

---

## 2. Navigation3 Scenes Architecture

### 2.1 Scene Graph Structure

```mermaid
flowchart TD
    subgraph Phone["📱 Phone (COMPACT)"]
        P1[Scene: PatternLock] --> P2[Scene: VaultBrowser]
        P2 --> P3[Scene: FileViewer]
        P2 --> P4[Scene: CameraViewfinder]
        P2 --> P5[Scene: Settings]
    end
    
    subgraph Tablet["📱 Tablet (MEDIUM) - TwoPane"]
        T1[Scene: PatternLock] --> T2[Scene: TwoPaneScaffold]
        T2 --> T3[NavRail: Vaults | Backup | Sync | Share | Settings]
        T2 --> T4[DetailPane: VaultBrowser | FileViewer | Camera | SettingsSection]
        T4 --> T5[Scene: CameraViewfinder (full)]
        T4 --> T6[Scene: FileViewer (full)]
    end
    
    subgraph Desktop["💻 Desktop (EXPANDED) - ThreePane"]
        D1[Scene: PatternLock] --> D2[Scene: ThreePaneScaffold]
        D2 --> D3[NavRail: Vaults | Backup | Sync | Share | Settings]
        D2 --> D4[ListPane: VaultList | ShareList | DeviceList]
        D2 --> D5[DetailPane: VaultBrowser | FileViewer | Camera | SettingsSection]
        D5 --> D6[Scene: CameraViewfinder (full)]
        D5 --> D7[Scene: FileViewer (full)]
    end
```

### 2.2 Scene Definitions

```kotlin
// Scene destinations for Navigation3
sealed interface VaultaireScene {
    // Authentication
    @Serializable object PatternLock : VaultaireScene
    @Serializable data class RecoveryDisplay(val mode: DisplayMode) : VaultaireScene
    @Serializable data class SecretPhraseEntry(val mode: EntryMode) : VaultaireScene
    
    // Main vault
    @Serializable data class VaultBrowser(val vaultId: String) : VaultaireScene
    @Serializable data class FileViewer(val vaultId: String, val fileId: String) : VaultaireScene
    @Serializable data class CameraViewfinder(val vaultId: String) : VaultaireScene
    @Serializable data class CameraReview(val vaultId: String) : VaultaireScene
    
    // Two/Three pane list-detail
    @Serializable data class VaultList(
        val selectedVaultId: String? = null,
        val onVaultSelected: (String) -> Unit = {}
    ) : VaultaireScene
    
    @Serializable data class SettingsList(
        val selectedSection: SettingsSection? = null,
        val onSectionSelected: (SettingsSection) -> Unit = {}
    ) : VaultaireScene
    
    @Serializable data class ShareList(
        val selectedShareId: String? = null,
        val onShareSelected: (String) -> Unit = {}
    ) : VaultaireScene
    
    @Serializable data class DeviceList(
        val selectedDeviceId: String? = null,
        val onDeviceSelected: (String) -> Unit = {}
    ) : VaultaireScene
    
    // Settings detail
    @Serializable data class SettingsSection(
        val section: SettingsSectionType,
        val parentScene: VaultaireScene
    ) : VaultaireScene
    
    // Sharing
    @Serializable data class ShareCreate(val vaultId: String) : VaultaireScene
    @Serializable object ShareOpenEntry : VaultaireScene
    @Serializable data class SharedVault(val shareId: String) : VaultaireScene
    
    // Pro settings
    @Serializable object DuressConfig : VaultaireScene
    @Serializable object DisguiseIconPicker : VaultaireScene
}

enum class DisplayMode { Creation, Settings }
enum class EntryMode { Recovery, CustomPhrase }
enum class SettingsSectionType { Security, Cloud, Appearance, Advanced, About }
```

---

## 3. Scaffold Implementations per Breakpoint

### 3.1 Phone Scaffold (COMPACT) — Single Pane + Bottom Nav

```kotlin
@Composable
fun PhoneScaffold(
    currentScene: VaultaireScene,
    onSceneChange: (VaultaireScene) -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        bottomBar = { PhoneBottomNavBar(currentScene, onSceneChange) },
        floatingActionButton = { 
            if (currentScene is VaultBrowser) VaultFAB(onCameraClick, onImportClick) 
        },
        floatingActionButtonPosition = FabPosition.End,
        isFloatingActionButtonDocked = false
    ) { innerPadding ->
        NavHost(navController, startDestination = "pattern_lock") {
            composable("pattern_lock") { PatternLockScreen(onResult = { /* ... */ }) }
            composable("vault/{vaultId}") { backStackEntry ->
                VaultBrowserScreen(
                    vaultId = backStackEntry.getString("vaultId")!!,
                    onAction = { action -> /* navigate */ }
                )
            }
            composable("vault/{vaultId}/file/{fileId}") { backStackEntry ->
                FileViewerScreen(/* ... */)
            }
            composable("vault/{vaultId}/camera") { backStackEntry ->
                CameraViewfinderScreen(vaultId = backStackEntry.getString("vaultId")!!)
            }
            composable("vault/{vaultId}/camera/review") { backStackEntry ->
                CameraReviewScreen(vaultId = backStackEntry.getString("vaultId")!!)
            }
            // ... other composable routes
        }
    }
}

@Composable
fun PhoneBottomNavBar(
    currentScene: VaultaireScene,
    onSceneChange: (VaultaireScene) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = "Vault") },
            label = { Text("Vault") },
            selected = currentScene is VaultBrowser,
            onClick = { onSceneChange(VaultBrowser(defaultVaultId)) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Cloud, contentDescription = "Backup") },
            label = { Text("Backup") },
            selected = currentScene is BackupSettingsScreen,
            onClick = { onSceneChange(BackupSettingsScreen()) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Sync, contentDescription = "Sync") },
            label = { Text("Sync") },
            selected = currentScene is SyncSettingsScreen,
            onClick = { onSceneChange(SyncSettingsScreen()) },
            badge = { if (isPro) { Text("PRO", style = MaterialTheme.typography.labelSmall) } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
            label = { Text("Share") },
            selected = currentScene is ShareCreate,
            onClick = { onSceneChange(ShareCreate(defaultVaultId)) },
            badge = { if (isPro) { Text("PRO", style = MaterialTheme.typography.labelSmall) } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentScene is SettingsScreen,
            onClick = { onSceneChange(SettingsScreen()) }
        )
    }
}
```

### 3.2 Tablet Scaffold (MEDIUM) — Two Pane + Collapsible Nav Rail

```kotlin
@Composable
fun TabletScaffold(
    currentScene: VaultaireScene,
    onSceneChange: (VaultaireScene) -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val railState = rememberNavigationRailState()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    
    // Two-pane: NavRail (280dp) | DetailPane (flex)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldState = scaffoldState
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            // Navigation Rail
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                railState = railState,
                singleLineLabel = false
            ) {
                NavigationRailItem(
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Vaults") },
                    label = { Text("Vaults") },
                    selected = currentScene is VaultBrowser,
                    onClick = { 
                        scope.launch { railState.animateTo(0) }
                        onSceneChange(VaultList(selectedVaultId = currentVaultId) { id ->
                            onSceneChange(VaultBrowser(id))
                        })
                    }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Default.Cloud, contentDescription = "Backup") },
                    label = { Text("Backup") },
                    selected = currentScene is BackupSettingsScreen,
                    onClick = { 
                        scope.launch { railState.animateTo(1) }
                        onSceneChange(BackupSettingsScreen()) 
                    }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Default.Sync, contentDescription = "Sync") },
                    label = { Text("Sync") },
                    selected = currentScene is SyncSettingsScreen,
                    onClick = { 
                        scope.launch { railState.animateTo(2) }
                        onSceneChange(SyncSettingsScreen()) 
                    },
                    badge = { if (isPro) Badge { Text("PRO", style = MaterialTheme.typography.labelSmall) } }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                    label = { Text("Share") },
                    selected = currentScene is ShareCreate || currentScene is ShareList,
                    onClick = { 
                        scope.launch { railState.animateTo(3) }
                        onSceneChange(ShareList()) 
                    },
                    badge = { if (isPro) Badge { Text("PRO", style = MaterialTheme.typography.labelSmall) } }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentScene is SettingsScreen || currentScene is SettingsSection,
                    onClick = { 
                        scope.launch { railState.animateTo(4) }
                        onSceneChange(SettingsList()) 
                    }
                )
            }
            
            // Detail Pane
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScene) {
                    is VaultBrowser -> VaultBrowserScreen(/* ... */)
                    is FileViewer -> FileViewerScreen(/* ... */)
                    is CameraViewfinder -> CameraViewfinderScreen(/* ... */)
                    is CameraReview -> CameraReviewScreen(/* ... */)
                    is SettingsSection -> SettingsSectionScreen(/* ... */)
                    is ShareList -> ShareListScreen(onShareSelected = { id -> onSceneChange(SharedVault(id)) })
                    is DeviceList -> DeviceListScreen(onDeviceSelected = { id -> /* ... */ })
                    is VaultList -> VaultListScreen(onVaultSelected = { id -> onSceneChange(VaultBrowser(id)) })
                    is SettingsList -> SettingsListScreen(onSectionSelected = { section -> onSceneChange(SettingsSection(section, currentScene)) })
                    else -> VaultBrowserScreen(/* default */)
                }
            }
        }
    }
}
```

### 3.3 Desktop Scaffold (EXPANDED) — Three Pane + Permanent Nav Rail

```kotlin
@Composable
fun DesktopScaffold(
    currentScene: VaultaireScene,
    onSceneChange: (VaultaireScene) -> Unit,
    modifier: Modifier = Modifier
) {
    val railState = rememberNavigationRailState()
    val scope = rememberCoroutineScope()
    
    // Three-pane: NavRail (280dp) | ListPane (320dp) | DetailPane (flex)
    Row(modifier = modifier.fillMaxSize()) {
        // Navigation Rail (Permanent)
        NavigationRail(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            railState = railState,
            singleLineLabel = false
        ) {
            NavigationRailItem(
                icon = { Icon(Icons.Default.Folder, contentDescription = "Vaults") },
                label = { Text("Vaults") },
                selected = currentScene is VaultList || currentScene is VaultBrowser,
                onClick = { scope.launch { railState.animateTo(0) }; onSceneChange(VaultList()) }
            )
            NavigationRailItem(
                icon = { Icon(Icons.Default.Cloud, contentDescription = "Backup") },
                label = { Text("Backup") },
                selected = currentScene is BackupSettingsScreen,
                onClick = { scope.launch { railState.animateTo(1) }; onSceneChange(BackupSettingsScreen()) }
            )
            NavigationRailItem(
                icon = { Icon(Icons.Default.Sync, contentDescription = "Sync") },
                label = { Text("Sync") },
                selected = currentScene is SyncSettingsScreen || currentScene is DeviceList,
                onClick = { scope.launch { railState.animateTo(2) }; onSceneChange(SyncSettingsScreen()) },
                badge = { if (isPro) Badge { Text("PRO", style = MaterialTheme.typography.labelSmall) } }
            )
            NavigationRailItem(
                icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                label = { Text("Share") },
                selected = currentScene is ShareCreate || currentScene is ShareList || currentScene is SharedVault,
                onClick = { scope.launch { railState.animateTo(3) }; onSceneChange(ShareList()) },
                badge = { if (isPro) Badge { Text("PRO", style = MaterialTheme.typography.labelSmall) } }
            )
            NavigationRailItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = currentScene is SettingsList || currentScene is SettingsSection,
                onClick = { scope.launch { railState.animateTo(4) }; onSceneChange(SettingsList()) }
            )
        }
        
        // Vertical divider
        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        
        // List Pane (320dp fixed)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(320.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.TopStart
        ) {
            when (currentScene) {
                is VaultList -> VaultListScreen(
                    onVaultSelected = { id -> onSceneChange(VaultBrowser(id)) }
                )
                is ShareList -> ShareListScreen(
                    onShareSelected = { id -> onSceneChange(SharedVault(id)) }
                )
                is DeviceList -> DeviceListScreen(
                    onDeviceSelected = { id -> /* handle device selection */ }
                )
                is SettingsList -> SettingsListScreen(
                    onSectionSelected = { section -> onSceneChange(SettingsSection(section, currentScene)) }
                )
                else -> {
                    // Default to vault list when no list scene active
                    VaultListScreen(onVaultSelected = { id -> onSceneChange(VaultBrowser(id)) })
                }
            }
        }
        
        // Vertical divider
        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        
        // Detail Pane (flexible)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.TopStart
        ) {
            when (currentScene) {
                is VaultBrowser -> VaultBrowserScreen(/* ... */)
                is FileViewer -> FileViewerScreen(/* ... */)
                is CameraViewfinder -> CameraViewfinderScreen(/* ... */)
                is CameraReview -> CameraReviewScreen(/* ... */)
                is SettingsSection -> SettingsSectionScreen(/* ... */)
                is SharedVault -> SharedVaultScreen(/* ... */)
                is ShareCreate -> ShareCreateScreen(/* ... */)
                is DuressConfig -> DuressConfigScreen(/* ... */)
                is DisguiseIconPicker -> DisguiseIconPickerScreen(/* ... */)
                is BackupSettingsScreen -> BackupSettingsScreen(/* ... */)
                is SyncSettingsScreen -> SyncSettingsScreen(/* ... */)
                is SettingsScreen -> SettingsScreen(/* ... */)
                else -> {
                    // Default empty state
                    EmptyDetailPlaceholder()
                }
            }
        }
    }
}

@Composable
fun EmptyDetailPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.FolderOpen, contentDescription = "", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
            Spacer(Modifier.padding(16.dp))
            Text(
                "Select a vault or item from the list",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## 4. Adaptive Component Specifications

### 4.1 PatternGrid — Hero Interaction Scaling

| Breakpoint | Grid Size | Dot Size | Cell Size | Line Width | Shadow |
|------------|-----------|----------|-----------|------------|--------|
| Phone (COMPACT) | 280×280dp | 24dp | 56dp | 3dp | elevation3 |
| Tablet (MEDIUM) | 320×320dp | 28dp | 64dp | 3.5dp | elevation3 |
| Desktop (EXPANDED) | 360×360dp | 32dp | 72dp | 4dp | elevation4 |

```kotlin
@Composable
fun AdaptivePatternGrid(
    onPatternComplete: (List<PatternPoint>) -> Unit,
    state: PatternGridState = PatternGridState.Idle,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    showNumbers: Boolean = false
) {
    val widthSizeClass = LocalWindowSizeClass.current.windowWidthSizeClass
    
    val (gridSize, dotSize, cellSize, lineWidth) = when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> 280.dp to 24.dp to 56.dp to 3.dp
        WindowWidthSizeClass.MEDIUM -> 320.dp to 28.dp to 64.dp to 3.5.dp
        WindowWidthSizeClass.EXPANDED -> 360.dp to 32.dp to 72.dp to 4.dp
    }
    
    PatternGrid(
        gridSize = gridSize,
        dotSize = dotSize,
        cellSize = cellSize,
        lineWidth = lineWidth,
        onPatternComplete = onPatternComplete,
        state = state,
        errorMessage = errorMessage,
        modifier = modifier
            .size(gridSize)
            .shadow(tokens.shadow.patternGrid),
        showNumbers = showNumbers
    )
}
```

### 4.2 FileGrid — Column Count & Aspect Ratio

| Breakpoint | Columns | Aspect Ratio | Gutter | Item Min Width |
|------------|---------|--------------|--------|----------------|
| Phone (COMPACT) | 2 | 4:3 | 12dp | 140dp |
| Tablet (MEDIUM) | 3 | 4:3 | 16dp | 160dp |
| Desktop (EXPANDED) | 4–5 | 4:3 | 20dp | 180dp |

```kotlin
@Composable
fun AdaptiveFileGrid(
    files: List<EncryptedFile>,
    onFileClick: (EncryptedFile) -> Unit,
    onFileLongClick: (EncryptedFile) -> Unit,
    selectedFiles: Set<String> = emptySet(),
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val widthSizeClass = LocalWindowSizeClass.current.windowWidthSizeClass
    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> 2
        WindowWidthSizeClass.MEDIUM -> 3
        WindowWidthSizeClass.EXPANDED -> 4 // 5 if width > 1200dp
    }
    val gutter = when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> 12.dp
        WindowWidthSizeClass.MEDIUM -> 16.dp
        WindowWidthSizeClass.EXPANDED -> 20.dp
    }
    
    LazyVGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(horizontal = gutter, vertical = gutter),
        horizontalArrangement = Arrangement.spacedBy(gutter),
        verticalArrangement = Arrangement.spacedBy(gutter),
        modifier = modifier.fillMaxSize()
    ) {
        items(files) { file ->
            FileGridItem(
                file = file,
                isSelected = file.id in selectedFiles,
                onClick = { onFileClick(file) },
                onLongClick = { onFileLongClick(file) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            )
        }
    }
}
```

### 4.3 CameraViewfinder — Control Layout

| Breakpoint | Control Layout | Shutter Position | Control Size |
|------------|----------------|------------------|--------------|
| Phone (COMPACT) | Horizontal bar bottom | Center | 48dp |
| Tablet (MEDIUM) | Horizontal bar bottom | Center | 56dp |
| Desktop (EXPANDED) | Horizontal bar bottom | Center | 56dp |
| Landscape (all) | Vertical bar right | Bottom-right | 56dp |

```kotlin
@Composable
fun AdaptiveCameraControls(
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
) {
    val widthSizeClass = LocalWindowSizeClass.current.windowWidthSizeClass
    val heightSizeClass = LocalWindowSizeClass.current.windowHeightSizeClass
    val isLandscape = heightSizeClass == WindowHeightSizeClass.COMPACT
    
    val controlSize = when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> 48.dp
        else -> 56.dp
    }
    
    if (isLandscape) {
        // Vertical layout on right side
        Column(
            modifier = modifier
                .fillMaxHeight()
                .width(controlSize * 1.5)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CameraControlButton(icon = Icons.Default.CameraRear, onClick = onLensToggle, size = controlSize)
            Spacer(Modifier.padding(16.dp))
            CameraControlButton(icon = modeIcon(cameraMode), onClick = onModeToggle, size = controlSize)
            Spacer(Modifier.padding(16.dp))
            CameraControlButton(icon = flashIcon(flashMode), onClick = onFlashCycle, size = controlSize)
            if (cameraMode == CameraMode.Video) {
                Spacer(Modifier.padding(16.dp))
                CameraControlButton(icon = torchIcon(torchEnabled), onClick = onTorchToggle, size = controlSize)
            }
            Spacer(Modifier.padding(24.dp))
            ShutterButton(
                mode = cameraMode,
                isRecording = isRecording,
                onClick = if (cameraMode == CameraMode.Photo) onShutterClick else onRecordClick,
                size = 72.dp
            )
        }
    } else {
        // Horizontal layout at bottom
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(controlSize * 2)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraControlButton(icon = Icons.Default.CameraRear, onClick = onLensToggle, size = controlSize)
            CameraControlButton(icon = modeIcon(cameraMode), onClick = onModeToggle, size = controlSize)
            CameraControlButton(icon = flashIcon(flashMode), onClick = onFlashCycle, size = controlSize)
            if (cameraMode == CameraMode.Video) {
                CameraControlButton(icon = torchIcon(torchEnabled), onClick = onTorchToggle, size = controlSize)
            }
            ShutterButton(
                mode = cameraMode,
                isRecording = isRecording,
                onClick = if (cameraMode == CameraMode.Photo) onShutterClick else onRecordClick,
                size = 72.dp
            )
        }
    }
}
```

### 4.4 RecoveryPhraseDisplay — Word Layout

| Breakpoint | Words per Line | Font Size | Container Width |
|------------|----------------|-----------|-----------------|
| Phone (COMPACT) | 3 | 24sp | 90% screen width |
| Tablet (MEDIUM) | 3 | 24sp | 600dp max |
| Desktop (EXPANDED) | 3 | 24sp | 600dp max |

```kotlin
@Composable
fun AdaptiveRecoveryPhraseDisplay(
    phrase: String,
    mode: DisplayMode,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    onAutoLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val widthSizeClass = LocalWindowSizeClass.current.windowWidthSizeClass
    
    val containerWidth = when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> Modifier.fillMaxWidth(0.9f)
        else -> Modifier.width(600.dp)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        Card(
            modifier = containerWidth,
            elevation = tokens.shadow.dialog,
            shape = tokens.shape.dialog
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your Recovery Phrase", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.padding(8.dp))
                Text(
                    "Write this down on paper. Do not store digitally.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.padding(24.dp))
                
                // 3 words per line, monospace
                RecoveryPhraseGrid(
                    phrase = phrase,
                    wordsPerLine = 3,
                    style = tokens.typography.recoveryPhraseWord
                )
                
                Spacer(Modifier.padding(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = onCopy) {
                        Text("Copy")
                    }
                    Button(
                        onClick = onDismiss,
                        enabled = mode != DisplayMode.Creation || canDismiss
                    ) {
                        Text(mode == DisplayMode.Creation ? "I've Saved It" : "Done")
                    }
                }
            }
        }
    }
}
```

### 4.5 Settings — Section Navigation

| Breakpoint | Layout | Navigation |
|------------|--------|------------|
| Phone (COMPACT) | Flat list in SettingsScreen | Navigate to section screen |
| Tablet (MEDIUM) | Two-pane: NavRail sections | Section list in NavRail, detail in pane |
| Desktop (EXPANDED) | Three-pane: NavRail → Section List → Detail | Permanent rail, section list, detail |

```kotlin
@Composable
fun AdaptiveSettingsScreen(
    currentScene: VaultaireScene,
    onSceneChange: (VaultaireScene) -> Unit,
    modifier: Modifier = Modifier
) {
    val widthSizeClass = LocalWindowSizeClass.current.windowWidthSizeClass
    
    when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            // Single screen with list navigation
            SettingsScreen(onSectionClick = { section ->
                onSceneChange(SettingsSection(section, currentScene))
            })
        }
        WindowWidthSizeClass.MEDIUM -> {
            // Two-pane: NavRail has section items
            TabletScaffold(currentScene, onSceneChange, modifier)
        }
        WindowWidthSizeClass.EXPANDED -> {
            // Three-pane: NavRail → Section List → Detail
            DesktopScaffold(currentScene, onSceneChange, modifier)
        }
    }
}
```

---

## 5. Foldable & Hinge Awareness

### 5.1 Foldable States

```kotlin
@Composable
fun rememberFoldableState(): FoldableState {
    val windowLayoutInfo = LocalWindowLayoutInfo.current
    val foldingFeature = windowLayoutInfo.displayFeatures
        .firstOrNull { it is FoldingFeature }
        as? FoldingFeature
    
    return when {
        foldingFeature == null -> FoldableState.FLAT
        foldingFeature.state == FoldingFeature.State.HALF_OPENED -> 
            if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL)
                FoldableState.HALF_OPENED_VERTICAL
            else
                FoldableState.HALF_OPENED_HORIZONTAL
        else -> FoldableState.FLAT
    }
}

enum class FoldableState {
    FLAT,
    HALF_OPENED_VERTICAL,   // Book mode, hinge vertical
    HALF_OPENED_HORIZONTAL  // Tabletop mode, hinge horizontal
}
```

### 5.2 Layout Adjustments for Foldables

| Foldable State | Phone Inner | Layout Adjustment |
|----------------|-------------|-------------------|
| FLAT | COMPACT/MEDIUM | Standard breakpoint logic |
| HALF_OPENED_VERTICAL | Two panes split by hinge | **Avoid content under hinge** — offset grid/panes |
| HALF_OPENED_HORIZONTAL | Two panes stacked | Top pane: preview, Bottom pane: controls |

```kotlin
@Composable
fun HingeAwarePatternGrid(
    onPatternComplete: (List<PatternPoint>) -> Unit,
    modifier: Modifier = Modifier
) {
    val foldableState = rememberFoldableState()
    val windowMetrics = LocalWindowMetrics.current
    val displayFeatures = LocalWindowLayoutInfo.current.displayFeatures
    
    val hingeOffset = displayFeatures
        .firstOrNull { it is FoldingFeature }
        ?.let { it.bounds.centerX().toDp() }
    
    val gridModifier = when (foldableState) {
        FoldableState.FLAT -> modifier
        FoldableState.HALF_OPENED_VERTICAL -> {
            // Shift grid away from hinge
            val shift = if ((hingeOffset ?: 0.dp) < (windowMetrics.bounds.width().toDp() / 2)) {
                // Hinge on left, shift right
                Modifier.padding(start = 80.dp)
            } else {
                // Hinge on right, shift left
                Modifier.padding(end = 80.dp)
            }
            modifier.then(shift)
        }
        FoldableState.HALF_OPENED_HORIZONTAL -> {
            // Center vertically in top half
            modifier.padding(top = windowMetrics.bounds.height().toDp() / 4)
        }
    }
    
    AdaptivePatternGrid(
        onPatternComplete = onPatternComplete,
        modifier = gridModifier
    )
}
```

---

## 6. Navigation3 Scene Transitions

### 6.1 Transition Specs

| Transition | Phone | Tablet | Desktop |
|------------|-------|--------|---------|
| PatternLock → VaultBrowser | Fade + slide up (450ms) | Fade + slide up (450ms) | Fade + slide up (450ms) |
| VaultBrowser → FileViewer | Slide left (300ms) | Detail pane fade (200ms) | Detail pane fade (200ms) |
| VaultBrowser → Camera | Slide up (350ms) | Full screen slide up (350ms) | Full screen slide up (350ms) |
| Settings list → Detail | Push (300ms) | Pane cross-fade (200ms) | Pane cross-fade (200ms) |
| List item select (3-pane) | N/A | N/A | Detail pane cross-fade (150ms) |

```kotlin
// Navigation3 scene transitions
val enterTransition = EnterTransition(
    animationSpec = tween(
        durationMillis = tokens.durationLong1,
        easing = tokens.easingVaultUnlock
    ) { fraction ->
        TranslationY(-100.dp * (1 - fraction)) + Fade(1f - fraction)
    }
)

val exitTransition = ExitTransition(
    animationSpec = tween(
        durationMillis = tokens.durationMedium1,
        easing = tokens.easingStandard
    ) { fraction ->
        TranslationY(50.dp * fraction) + Fade(fraction)
    }
)

val detailPaneTransition = EnterTransition(
    animationSpec = tween(
        durationMillis = tokens.durationShort4,
        easing = tokens.easingStandard
    ) { fraction ->
        Fade(fraction) + TranslationX(20.dp * (1 - fraction))
    }
)
```

### 6.2 Deep Link Handling

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = "app_entry"
) {
    val widthSizeClass = LocalWindowSizeClass.current.windowWidthSizeClass
    
    NavHost(navController, startDestination) {
        // Authentication
        composable("lock") { PatternLockScreen() }
        composable("recovery/display") { RecoveryPhraseDisplayScreen() }
        composable("recovery/enter") { SecretPhraseEntryScreen() }
        
        // Main vault - adaptive based on breakpoint
        if (widthSizeClass == WindowWidthSizeClass.COMPACT) {
            // Phone: Single pane navigation
            composable("vault/{vaultId}") { backStackEntry ->
                VaultBrowserScreen(vaultId = backStackEntry.getString("vaultId")!!)
            }
            composable("vault/{vaultId}/file/{fileId}") { backStackEntry ->
                FileViewerScreen(/* ... */)
            }
        } else {
            // Tablet/Desktop: Two/Three pane with scenes
            navigation(startDestination = "vault_list", route = "vaults") {
                composable("vault_list") { 
                    VaultListScreen(onVaultSelected = { id ->
                        navController.navigate("vault_detail/$id")
                    }) 
                }
                composable(
                    route = "vault_detail/{vaultId}",
                    arguments = listOf(navArgument("vaultId") { type = NavType.StringType })
                ) { backStackEntry ->
                    VaultBrowserScreen(vaultId = backStackEntry.getString("vaultId")!!)
                }
                composable(
                    route = "vault_detail/{vaultId}/file/{fileId}",
                    arguments = listOf(
                        navArgument("vaultId") { type = NavType.StringType },
                        navArgument("fileId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    FileViewerScreen(/* ... */)
                }
            }
        }
        
        // Camera (always full screen on all breakpoints)
        composable("vault/{vaultId}/camera") { backStackEntry ->
            CameraViewfinderScreen(vaultId = backStackEntry.getString("vaultId")!!)
        }
        composable("vault/{vaultId}/camera/review") { backStackEntry ->
            CameraReviewScreen(vaultId = backStackEntry.getString("vaultId")!!)
        }
        
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
        
        // Onboarding
        composable("onboarding/pattern") { OnboardingPatternScreen() }
        composable("onboarding/phrase") { OnboardingPhraseScreen() }
        composable("onboarding/complete") { OnboardingCompleteScreen() }
    }
}
```

---

## 7. Window Insets & Edge-to-Edge

### 7.1 Insets Handling per Breakpoint

```kotlin
@Composable
fun provideWindowInsets() {
    // Enable edge-to-edge
    WindowCompat.setDecorFitsSystemWindows(window, false)
    
    // Status bar & navigation bar colors
    WindowCompat.getInsetsController(window, window.decorView)
        .apply {
            appearanceLightStatusBars = isSystemInDarkTheme()
            appearanceLightNavigationBars = isSystemInDarkTheme()
        }
    
    // Set system bar colors transparent
    window.statusBarColor = Color.TRANSPARENT.toArgb()
    window.navigationBarColor = Color.TRANSPARENT.toArgb()
}

@Composable
fun EdgeToEdgeBox(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    val windowInsets = rememberWindowInsets()
    val systemBars = windowInsets.getInsets(WindowInsets.Type.systemBars())
    val ime = windowInsets.getInsets(WindowInsets.Type.ime())
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        content(PaddingValues(
            start = systemBars.left,
            top = systemBars.top,
            end = systemBars.right,
            bottom = systemBars.bottom + ime.bottom
        ))
    }
}
```

### 7.2 Component Insets Requirements

| Component | Top Inset | Bottom Inset | Notes |
|-----------|-----------|--------------|-------|
| PatternLock | Status bar height | Navigation bar height | Grid centered in safe area |
| VaultBrowser | Status bar + TopAppBar | FAB + Navigation bar | Pull-to-refresh in safe area |
| CameraViewfinder | Status bar | Controls + Navigation bar | FLAG_SECURE full screen |
| CameraReview | Status bar | Actions + Navigation bar | FLAG_SECURE full screen |
| RecoveryPhrase | Status bar | Actions + Navigation bar | FLAG_SECURE, centered dialog |
| Settings | Status bar + TopAppBar | Navigation bar | Standard scaffold |

---

## 8. Orientation Handling

### 8.1 Phone Landscape (MEDIUM breakpoint)

```kotlin
@Composable
fun PhoneLandscapeScaffold(
    currentScene: VaultaireScene,
    onSceneChange: (VaultaireScene) -> Unit
) {
    // In landscape, phone becomes MEDIUM width → Two-pane capable
    // But height is COMPACT → Use horizontal split
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left: List (300dp)
        if (currentScene is VaultBrowser || currentScene is VaultList) {
            VaultListScreen(
                modifier = Modifier.width(300.dp),
                onVaultSelected = { id -> onSceneChange(VaultBrowser(id)) }
            )
        } else if (currentScene is SettingsScreen || currentScene is SettingsList) {
            SettingsListScreen(
                modifier = Modifier.width(300.dp),
                onSectionSelected = { section -> onSceneChange(SettingsSection(section, currentScene)) }
            )
        }
        
        Divider(modifier = Modifier.fillMaxHeight())
        
        // Right: Detail (flex)
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (currentScene) {
                is VaultBrowser -> VaultBrowserScreen(/* ... */)
                is FileViewer -> FileViewerScreen(/* ... */)
                is SettingsSection -> SettingsSectionScreen(/* ... */)
                else -> VaultBrowserScreen(/* default */)
            }
        }
    }
}
```

### 8.2 Configuration Change Handling

```kotlin
// In AndroidManifest.xml for activities with complex state
<activity
    android:name=".MainActivity"
    android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|density|fontScale|keyboardHidden|navigation"
    android:windowSoftInputMode="adjustResize">
</activity>

// In Compose - remember state across config changes
@Composable
fun rememberAdaptiveState(): AdaptiveState {
    val widthSizeClass = LocalWindowSizeClass.current.windowWidthSizeClass
    val heightSizeClass = LocalWindowSizeClass.current.windowHeightSizeClass
    val orientation = LocalConfiguration.current.orientation
    
    return remember(widthSizeClass, heightSizeClass, orientation) {
        AdaptiveState(
            widthSizeClass = widthSizeClass,
            heightSizeClass = heightSizeClass,
            orientation = orientation
        )
    }
}

data class AdaptiveState(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
    val orientation: Int
) {
    val isPhonePortrait: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.COMPACT && orientation == Configuration.ORIENTATION_PORTRAIT
    
    val isPhoneLandscape: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.MEDIUM && orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val isTablet: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.MEDIUM && orientation == Configuration.ORIENTATION_PORTRAIT
    
    val isDesktop: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.EXPANDED
}
```

---

## 9. Dynamic Type & Accessibility at Scale

### 9.1 Font Scale Impact on Layout

| System Font Scale | Phone Grid | Tablet Grid | Recovery Phrase | Touch Targets |
|-------------------|------------|-------------|-----------------|---------------|
| 85% (Small) | 280dp | 320dp | 20.4sp | 48dp |
| 100% (Default) | 280dp | 320dp | 24sp | 48dp |
| 115% (Large) | 280dp | 320dp | 27.6sp | 48dp |
| 130% (Larger) | 280dp | 320dp | 31.2sp | 48dp |
| 150% (Largest) | 280dp | 320dp | 36sp | 48dp |
| 200% (Max) | 280dp | 320dp | 48sp | 48dp |

**Key Rule**: Pattern grid size NEVER scales — touch targets remain 56dp minimum. Only typography scales.

### 9.2 Reduced Motion

```kotlin
@Composable
fun AdaptiveAnimationSpec(): AnimationSpec<Float> {
    val reduceMotion = ConfigurationCompat.isReduceMotionEnabled(LocalConfiguration.current)
    
    return if (reduceMotion) {
        SnapSpringSpec
    } else {
        SpringSpec(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    }
}
```

---

## 10. Testing Matrix

### 10.1 Device Testing Checklist

| Device Category | Test Scenarios |
|-----------------|----------------|
| **Phone (360dp)** | Pattern lock, vault browser, camera, settings, onboarding |
| **Phone Landscape (720dp)** | Two-pane vault browser, settings split |
| **Foldable Inner (580dp)** | Hinge avoidance, half-open layouts |
| **Foldable Outer (360dp)** | Quick actions, notifications |
| **Tablet Portrait (720dp)** | Two-pane with nav rail |
| **Tablet Landscape (1024dp)** | Three-pane |
| **Desktop (1280dp+)** | Three-pane, window resize, multi-window |
| **ChromeOS** | Windowed mode, keyboard/mouse, trackpad gestures |

### 10.2 Compose Preview Annotations

```kotlin
@Preview(
    name = "Phone Portrait",
    device = Devices.PIXEL_6_PRO,
    uiMode = UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Composable
fun PhonePortraitPreview() {
    VaultaireTheme(darkTheme = true) {
        PhoneScaffold(
            currentScene = VaultBrowser("default"),
            onSceneChange = {}
        )
    }
}

@Preview(
    name = "Tablet Two-Pane",
    device = Devices.PIXEL_TABLET,
    uiMode = UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Composable
fun TabletPreview() {
    VaultaireTheme(darkTheme = true) {
        TabletScaffold(
            currentScene = VaultBrowser("default"),
            onSceneChange = {}
        )
    }
}

@Preview(
    name = "Desktop Three-Pane",
    device = "1280x800",
    uiMode = UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Composable
fun DesktopPreview() {
    VaultaireTheme(darkTheme = true) {
        DesktopScaffold(
            currentScene = VaultBrowser("default"),
            onSceneChange = {}
        )
    }
}

@Preview(
    name = "Foldable Half-Open",
    device = "foldable_inner",
    uiMode = UI_MODE_NIGHT_YES,
    showSystemUi = true
)
@Composable
fun FoldablePreview() {
    VaultaireTheme(darkTheme = true) {
        // Simulate hinge in middle
        HingeAwarePatternGrid(onPatternComplete = {})
    }
}
```

---

## 11. Implementation Priority

### Phase 1: Core Breakpoints (Week 1)
- [ ] WindowSizeClass detection
- [ ] Phone scaffold (COMPACT)
- [ ] PatternGrid adaptive sizing
- [ ] FileGrid column count

### Phase 2: Tablet Two-Pane (Week 2)
- [ ] NavigationRail implementation
- [ ] Two-pane VaultBrowser
- [ ] Settings two-pane
- [ ] Landscape phone → two-pane

### Phase 3: Desktop Three-Pane (Week 3)
- [ ] Permanent NavigationRail
- [ ] ListPane + DetailPane
- [ ] VaultList, ShareList, DeviceList
- [ ] Window resize handling

### Phase 4: Foldable & Polish (Week 4)
- [ ] Hinge awareness
- [ ] Half-open layouts
- [ ] Outer display quick actions
- [ ] Dynamic type testing at 200%
- [ ] Reduced motion verification
- [ ] Accessibility testing per breakpoint

---

## 12. Design Token References

All adaptive values reference design-tokens.json:

```json
{
  "spacing": {
    "patternGridCell": { "phone": 56, "tablet": 64, "desktop": 72 },
    "patternGridTotal": { "phone": 280, "tablet": 320, "desktop": 360 }
  },
  "typography": {
    "recoveryPhrase": { "base": 24, "scale": true }
  },
  "motion": {
    "transition": { "phone": 300, "tablet": 200, "desktop": 150 }
  }
}
```

---

## 13. Migration Notes for Compose Developers

### 13.1 From View System
- Replace `Configuration.orientation` with `LocalWindowSizeClass.current`
- Use `rememberNavController()` with scene-based routes
- Migrate `DrawerLayout` → `NavigationRail` / `ModalNavigationDrawer`

### 13.2 New Dependencies
```kotlin
// build.gradle.kts
implementation("androidx.compose.material3:material3:1.3.0")
implementation("androidx.compose.material3:material3-window-size-class:1.3.0")
implementation("androidx.navigation:navigation-compose:2.8.0")
implementation("androidx.window:window-layout:1.2.0") // For foldable features
```

### 13.3 Key APIs
- `LocalWindowSizeClass.current` — Breakpoint detection
- `LocalWindowLayoutInfo.current` — Foldable hinge info
- `LocalWindowMetrics.current` — Raw window bounds
- `rememberNavigationRailState()` — Rail animation state
- `WindowSizeClass` — COMPACT/MEDIUM/EXPANDED

---

*End of Responsive Breakpoints Specification*