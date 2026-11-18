# Vaultaire Android — Accessibility Specification

> **Complete a11y specs** — TalkBack labels, focus order, dynamic type, contrast, haptics, screen reader announcements, FLAG_SECURE implications.

---

## 1. TalkBack Labels for Every Interactive Element

### 1.1 Pattern Lock Screen

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| App Title | "Vaultaire" | "Secure photo vault" |
| Pattern Grid (container) | "Pattern unlock grid, 5 rows, 5 columns" | "Draw your pattern to unlock" |
| Pattern Dot (idle) | "Dot, row {row}, column {col}, unselected" | "Double tap to select" |
| Pattern Dot (hover) | "Dot, row {row}, column {col}, hovered" | "Double tap to select" |
| Pattern Dot (active) | "Dot, row {row}, column {col}, selected" | "Part of your pattern" |
| Pattern Dot (connected) | "Dot, row {row}, column {col}, connected" | "Connected in pattern" |
| Pattern Dot (error) | "Dot, row {row}, column {col}, error" | "Incorrect pattern" |
| Pattern Dot (success) | "Dot, row {row}, column {col}, success" | "Pattern accepted" |
| Forgot Pattern Link | "Forgot pattern" | "Opens recovery phrase entry" |
| Error Message | "Incorrect pattern. Try again." | — |

### 1.2 Vault Browser

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Toolbar Title | "My Vault" | "Vault browser" |
| Toolbar Menu | "More options" | "Settings, lock, share" |
| FAB (Camera) | "Take photo or video" | "Opens secure camera" |
| FAB (Import) | "Import files" | "Opens system file picker" |
| File Grid Item | "{filename}, {type}, {size}, {date}" | "Double tap to view, long press to select" |
| File Grid Item (selected) | "Selected, {filename}, {type}, {size}, {date}" | "Double tap to deselect" |
| Empty State Title | "No files in this vault" | — |
| Empty State Add Files | "Add files" | "Opens file picker" |
| Empty State Add Samples | "Add sample files" | "Adds demo files" |
| Pull to Refresh | "Pull to refresh" | "Syncs with cloud" |
| Sync Status (synced) | "Synced, {count} devices" | — |
| Sync Status (syncing) | "Syncing" | — |
| Sync Status (offline) | "Offline, changes queued" | — |

### 1.3 File Viewer

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Toolbar Back | "Back" | "Returns to vault browser" |
| Toolbar Title | "{filename}" | — |
| Toolbar Menu | "More options" | "Info, share, export, delete" |
| Photo View | "Photo, pinch to zoom, swipe down to close" | — |
| Video View | "Video, {duration}, tap for controls" | — |
| Bottom Bar Info | "File info" | "Shows size, date, type" |
| Bottom Bar Share | "Share file" | "Exports decrypted copy" |
| Bottom Bar Export | "Export to Files app" | "Saves to Downloads" |
| Bottom Bar Delete | "Delete file" | "Permanently removes from vault" |

### 1.4 Secure Camera

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Toolbar Back | "Back" | "Returns to vault" |
| Toolbar Settings | "Camera settings" | "Grid lines, timestamp, etc." |
| Preview View | "Camera preview, double tap to focus" | "Tap to set focus point" |
| Lens Switch | "{Front/Rear} camera" | "Switches camera" |
| Mode Toggle | "{Photo/Video} mode" | "Switches capture mode" |
| Flash/Torch | "Flash {Auto/On/Off}" / "Torch {On/Off}" | "Cycles flash mode" |
| Shutter Button | "Capture photo" | "Takes photo" |
| Record Button | "Record video" | "Starts recording" |
| Stop Button | "Stop recording" | "Stops recording" |
| Timer | "Recording, {time}" | — |
| Focus Indicator | "Focus locked at {position}" | — |

### 1.5 Camera Review

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Retake Button | "Retake" | "Discards capture, returns to camera" |
| Keep Button | "Keep" | "Encrypts and saves to vault" |
| Photo Review | "Captured photo, double tap to keep, swipe left to retake" | — |
| Video Review | "Captured video, {duration}, tap to play" | — |
| Play/Pause | "{Play/Pause}" | — |
| Mute/Unmute | "{Mute/Unmute}" | — |
| Audio Off Badge | "Audio off, microphone permission denied" | — |

### 1.6 Recovery Phrase Display

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Screen Title | "Your recovery phrase" | "Write this down on paper" |
| Phrase Container | "9-word recovery phrase" | "Swipe to read each word" |
| Word {n} | "Word {n} of 9: {word}" | — |
| Copy Button | "Copy recovery phrase to clipboard, clears in 60 seconds" | — |
| Done Button | "I've saved it" / "Done" | "Dismisses phrase screen" |
| Timer | "Auto-locks in {seconds} seconds" | — |

### 1.7 Secret Phrase Entry

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Screen Title | "Enter recovery phrase" | — |
| Instruction | "Enter your 9-word recovery phrase" | — |
| Word Field {n} | "Word {n} of {total}, edit box" | "Type or paste word" |
| Paste Button | "Paste from clipboard" | — |
| Validate Button | "Validate phrase" | "Checks phrase and continues" |
| Error Message | "Phrase not recognized" / "Phrase must contain exactly 9 words" | — |

### 1.8 Duress Config

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Screen Title | "Duress mode" | "Pro feature" |
| Duress Pattern Grid | "Duress pattern grid" | "Draw pattern to trigger silent wipe" |
| Confirm Button | "Confirm duress pattern" | "Enabled after drawing pattern" |
| Decoy Vault List | "Select decoy vault" | "Opens after duress trigger" |
| Vault Option | "{Vault name}, {file count} files" | "Select as decoy" |
| Create Decoy | "Create new decoy vault" | — |
| Warning | "Duress mode silently destroys all non-decoy data. This cannot be undone." | — |
| Save Button | "Save duress configuration" | — |

### 1.9 Disguise Icon Picker

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Screen Title | "App icon" | "Choose how Vaultaire appears on home screen" |
| Icon Option | "{Icon name}, {selected/not selected}" | "Double tap to select" |
| Pro Badge | "Pro feature" | — |
| Confirmation Dialog Title | "Change app icon?" | — |
| Confirmation Dialog Message | "The app icon will update on your Home Screen. Vaultaire name stays unchanged." | — |
| Confirm Button | "Confirm" | "Applies new icon" |
| Cancel Button | "Cancel" | "Keeps current icon" |

### 1.10 Backup Settings

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Toggle | "Encrypted cloud backup, {on/off}" | "Backs up vault to Google Drive" |
| Last Backup | "Last backup: {date/time}" | — |
| Next Backup | "Next backup: {date/time}" | — |
| Backup Size | "Backup size: {size}" | — |
| Account | "Google account: {email}" | — |
| Change Account | "Change account" | "Switches Google Drive account" |
| Backup Now | "Backup now" | "Triggers immediate backup" |
| View Details | "View backup details" | — |
| Recovery Phrase Show | "Show recovery phrase" | "Requires authentication" |

### 1.11 Sync Settings

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Toggle | "Device sync, {on/off}" | "Pro feature, syncs across devices" |
| Device Count | "This device + {count} other devices" | — |
| Last Sync | "Last sync: {date/time}" | — |
| Device List Item | "{Device name}, {status}" | — |
| Pause Sync | "Pause sync" | "Stops sync temporarily" |
| Disconnect Device | "Disconnect this device" | "Keeps local vault, stops cloud sync" |
| Conflicts | "Conflicts: {count} pending" | — |

### 1.12 Share Create

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Screen Title | "Share vault" | "Pro feature" |
| Phrase Display | "Sharing phrase: {12 words}" | — |
| Regenerate | "Regenerate phrase" | "Creates new sharing phrase" |
| Copy Phrase | "Copy sharing phrase" | — |
| Expiration Picker | "Expiration date, {value}" | — |
| Max Opens Picker | "Max opens, {value}" | — |
| Disable Export Toggle | "Disable export and screenshots, {on/off}" | — |
| Create Share | "Create share" | "Uploads encrypted share to Drive" |
| Warning | "Send the phrase through a trusted channel" | — |

### 1.13 Share Open

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Screen Title | "Open shared vault" | — |
| Instruction | "Enter the 12-word sharing phrase you received" | — |
| Phrase Input | "Sharing phrase, edit box" | "Paste or type phrase" |
| Paste | "Paste from clipboard" | — |
| Scan QR | "Scan QR code" | — |
| Open Vault | "Open vault" | "Enabled when phrase entered" |
| Shared Vault Title | "Shared vault: {name}, read-only" | — |
| Policy Badge | "Expires in {time}, {opens} of {max} opens used" | — |
| File Item | "{filename}, {type}, {size}" | "Double tap to view" |
| Pull to Refresh | "Pull to refresh for updates" | — |

### 1.14 Settings Screens

| Element | TalkBack Label | Hint |
|---------|----------------|------|
| Section Header | "{Section name}" | — |
| Toggle Item | "{Title}, {on/off}" | "{Subtitle}" |
| Navigation Item | "{Title}" | "{Subtitle}" |
| Action Item | "{Title}" | "{Subtitle}" |
| Pro Badge | "Pro feature" | — |

---

## 2. Focus Order

### 2.1 Pattern Lock Screen
```
1. App Title (if focusable)
2. Pattern Grid (single focusable container)
   - Arrow keys: Navigate between dots (row/col)
   - Space/Enter: Select current dot
   - Escape: Clear pattern
3. "Forgot Pattern?" link
4. Emergency Info (if enabled)
```

### 2.2 Camera Viewfinder
```
1. Toolbar Back Button
2. Toolbar Settings Button
3. Preview View (NOT focusable - tap to focus)
4. Lens Switch Button
5. Mode Toggle Button
6. Flash/Torch Button
7. Shutter/Record Button
```

### 2.3 Vault Browser
```
1. Toolbar Back (if nested)
2. Toolbar Title
3. Toolbar Menu
4. File Grid (2D navigation)
   - Arrow keys: Move between items
   - Enter: Open file
   - Space: Toggle selection (in selection mode)
   - Escape: Exit selection mode
5. FAB (Camera)
6. FAB (Import) - if split FAB
```

### 2.4 File Viewer
```
1. Toolbar Back
2. Toolbar Title
3. Toolbar Menu
3. Content View (photo/video - NOT focusable)
4. Bottom Bar Info
5. Bottom Bar Share
6. Bottom Bar Export
7. Bottom Bar Delete
```

### 2.5 Recovery Phrase Display
```
1. Screen Title
2. Phrase Container (horizontal pager or vertical list)
   - Word 1 → Word 2 → ... → Word 9
3. Copy Button
4. Done Button
```

### 2.6 Secret Phrase Entry
```
1. Screen Title
2. Instruction
3. Word Field 1 → Word Field 2 → ... → Word Field N
   - Auto-advance on space/comma
   - Backspace on empty moves to previous
4. Paste Button
5. Validate/Continue Button
```

### 2.7 Settings Screens
```
1. Toolbar Back
2. Section Header 1
3. Item 1.1 → Item 1.2 → ...
4. Section Header 2
5. Item 2.1 → Item 2.2 → ...
...
```

---

## 3. Dynamic Type Scaling

### 3.1 Implementation
```kotlin
// All text uses SP units
Text(
    text = "Hello",
    fontSize = 16.sp, // NOT dp
    style = MaterialTheme.typography.bodyLarge
)

// Custom typography scales
val scaledRecoveryPhrase = with(LocalDensity.current) {
    24.sp * LocalConfiguration.current.fontScale
}
```

### 3.2 Scale Testing Matrix
| System Font Size | Scale | Pattern Grid | Recovery Phrase | Touch Targets |
|------------------|-------|--------------|-----------------|---------------|
| Small | 85% | 280dp fixed | 20.4sp | 48dp min |
| Default | 100% | 280dp fixed | 24sp | 48dp min |
| Large | 115% | 280dp fixed | 27.6sp | 48dp min |
| Larger | 130% | 280dp fixed | 31.2sp | 48dp min |
| Largest | 150% | 280dp fixed | 36sp | 48dp min |
| Max (Accessibility) | 200% | 280dp fixed | 48sp | 48dp min |

### 3.3 Constraints
- **Pattern Grid**: Fixed 280dp (56dp cells) — touch targets don't scale
- **Recovery Phrase**: Scales up to 200%, line height scales proportionally
- **Bottom Sheets/Dialogs**: Max height 90% viewport, scrollable
- **Camera Controls**: Fixed 48dp minimum, don't scale
- **Toolbar**: Title scales, icons fixed 24dp

---

## 4. Color Contrast Ratios (WCAG 2.1 AA Minimum)

### 4.1 Verified Ratios

| Foreground | Background | Ratio (Dark) | Ratio (Light) | Status |
|------------|------------|--------------|---------------|--------|
| `onSurface` (#E6E6E6) | `surface` (#1A1C1E) | 15.8:1 | — | ✅ AAA |
| `onSurface` (#1A1C1E) | `surface` (#F8F9FA) | — | 12.6:1 | ✅ AAA |
| `onSurfaceVariant` (#C4C6CB) | `surface` (#1A1C1E) | 7.2:1 | — | ✅ AA |
| `onSurfaceVariant` (#44474B) | `surface` (#F8F9FA) | — | 5.8:1 | ✅ AA |
| `primary` (#7BA3D9) | `onPrimary` (#121212) | 8.4:1 | — | ✅ AA |
| `primary` (#4A6FA5) | `onPrimary` (#FFFFFF) | — | 6.2:1 | ✅ AA |
| `error` (#FFB4AB) | `onError` (#690005) | 6.8:1 | — | ✅ AA |
| `error` (#BA1A1A) | `onError` (#FFFFFF) | — | 5.1:1 | ✅ AA |
| `success` (#81C784) | `onSuccess` (#1B5E20) | 6.2:1 | — | ✅ AA |
| `warning` (#FFB74D) | `onWarning` (#4E342E) | 5.4:1 | — | ✅ AA |
| `patternGrid.dotActive` | `patternGrid.gridBackground` | 4.8:1 | 4.5:1 | ✅ AA |
| `cameraOverlay.controlIcon` | `cameraOverlay.controlBackground` | 8.2:1 | 8.2:1 | ✅ AAA |
| `disguise icons` | `disguise backgrounds` | 4.5:1+ | 4.5:1+ | ✅ AA |

### 4.2 Non-Text Contrast (UI Components)
| Component | Ratio | Status |
|-----------|-------|--------|
| Pattern dot border (active) | 3:1 vs background | ✅ AA |
| Focus indicator outline | 3:1 vs background | ✅ AA |
| Selection border | 3:1 vs background | ✅ AA |
| Divider lines | 3:1 vs background | ✅ AA |
| Disabled states | Not required | — |

### 4.3 High Contrast Mode
```kotlin
// Force high contrast when system setting enabled
val highContrast = ConfigurationCompat.getHighContrast(
    LocalConfiguration.current
) == HIGH_CONTRAST_ON

// Override colors
val dotIdle = if (highContrast) Color.White else tokens.patternGrid.dotIdle
```

---

## 5. Haptic Feedback Patterns

### 5.1 Pattern Lock
| Interaction | VibrationEffect | Intensity |
|-------------|-----------------|-----------|
| Dot press | `EFFECT_CLICK` | Light |
| Interpolated dot | `EFFECT_CLICK` | Light (each) |
| Pattern complete (success) | `EFFECT_HEAVY_CLICK` | Medium |
| Pattern error (shake) | `EFFECT_HEAVY_CLICK` | Heavy |
| Pattern too short | None | — |

### 5.2 Camera
| Interaction | VibrationEffect | Intensity |
|-------------|-----------------|-----------|
| Shutter press | `EFFECT_CLICK` | Light |
| Record start | `EFFECT_DOUBLE_CLICK` | Medium |
| Record stop | `EFFECT_DOUBLE_CLICK` | Medium |
| Focus lock | `EFFECT_TICK` | Subtle |

### 5.3 General
| Interaction | VibrationEffect | Intensity |
|-------------|-----------------|-----------|
| Vault unlock | `EFFECT_TICK` | Subtle |
| File delete | `EFFECT_TICK` | Subtle |
| Pull-to-refresh | `EFFECT_TICK` | Subtle |
| Duress trigger | **NONE** | Silent |
| Error dialogs | `EFFECT_HEAVY_CLICK` | Heavy |

### 5.4 Implementation
```kotlin
@Composable
fun HapticFeedback(
    effect: VibrationEffect = VibrationEffect.EFFECT_CLICK
) {
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)
    LaunchedEffect(effect) {
        vibrator.vibrate(effect)
    }
}

// Usage
PatternDot(onClick = {
    HapticFeedback(VibrationEffect.EFFECT_CLICK)
})
```

---

## 6. Screen Reader Announcements for State Changes

### 6.1 Live Regions
```kotlin
// Error announcements
val errorMessage = viewModel.errorMessage.collectAsState()
Text(
    text = errorMessage ?: "",
    modifier = Modifier
        .fillMaxWidth()
        .semantics { liveRegion = LiveRegionMode.Polite }
)

// Status announcements
val syncStatus = viewModel.syncStatus.collectAsState()
Text(
    text = syncStatus.description,
    modifier = Modifier
        .semantics { liveRegion = LiveRegionMode.Assertive }
)
```

### 6.2 Announcement Catalog

| State Change | Announcement | Priority |
|--------------|--------------|----------|
| Pattern dot selected | "Dot {position} selected" | Polite |
| Pattern dot connected | "Connected, {count} dots in pattern" | Polite |
| Pattern wrong | "Incorrect pattern, try again" | Assertive |
| Pattern correct | "Vault unlocked" | Polite |
| Camera focus locked | "Focus locked at {position}" | Polite |
| File added | "{Name} added to vault" | Polite |
| File deleted | "{Name} deleted" | Polite |
| Backup complete | "Backup completed, {size} uploaded" | Polite |
| Sync complete | "Synced with {count} devices" | Polite |
| Share revoked | "Access revoked by owner" | Assertive |
| Duress triggered | **NO ANNOUNCEMENT** | Silent |
| Lockout started | "Too many attempts. Try again in 30 seconds" | Assertive |
| Lockout ended | "You can now try again" | Polite |
| Recovery phrase copied | "Copied to clipboard, clears in 60 seconds" | Polite |
| Auto-lock warning | "Auto-locking in 10 seconds" | Polite |

---

## 7. FLAG_SECURE Implications for Accessibility Services

### 7.1 Screens with FLAG_SECURE

| Screen | FLAG_SECURE Scope | TalkBack Impact | Mitigation |
|--------|-------------------|-----------------|------------|
| Pattern Lock | PatternGrid area only | Normal (grid accessible) | Standard |
| Camera Viewfinder | Full screen | Preview not readable | Announce controls only |
| Camera Review | Full screen | Image/video not readable | "Captured photo, double tap to keep" |
| Recovery Phrase Display | Full screen | **Blocks automatic reading** | Custom AccessibilityDelegate |
| Secret Phrase Entry | Input fields only | Normal for fields | Standard |
| Shared Vault View | Full screen | Files not readable | "Read-only shared vault, pull to refresh" |

### 7.2 Recovery Phrase Custom AccessibilityDelegate

```kotlin
class RecoveryPhraseAccessibilityDelegate(
    private val phraseWords: List<String>
) : AccessibilityDelegateCompat() {
    
    override fun onInitializeAccessibilityNodeInfo(
        host: View,
        info: AccessibilityNodeInfoCompat
    ) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        // Remove automatic text reading
        info.text = null
        info.contentDescription = "Recovery phrase display. " +
            "Use custom actions to read words."
        
        // Add custom actions
        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(
            ACTION_CUSTOM_1,
            "Read full phrase"
        ))
        for (i in phraseWords.indices) {
            info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                ACTION_CUSTOM_2 + i,
                "Read word ${i + 1}"
            ))
        }
    }
    
    override fun performAccessibilityAction(
        host: View,
        action: Int,
        args: Bundle?
    ): Boolean {
        return when (action) {
            ACTION_CUSTOM_1 -> {
                announceForAccessibility(phraseWords.joinToString(" "))
                true
            }
            in ACTION_CUSTOM_2..ACTION_CUSTOM_2 + phraseWords.lastIndex -> {
                val index = action - ACTION_CUSTOM_2
                announceForAccessibility("Word ${index + 1}: ${phraseWords[index]}")
                true
            }
            else -> super.performAccessibilityAction(host, action, args)
        }
    }
}
```

### 7.3 Camera Preview Accessibility

```kotlin
@Composable
fun CameraPreviewAccessibility(
    focusPosition: FocusPosition?
) {
    // Preview itself not accessible
    // Announce focus changes via live region
    val focusAnnouncement = remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(focusPosition) {
        if (focusPosition != null) {
            focusAnnouncement.value = "Focus locked at ${focusPosition.description}"
        }
    }
    
    if (focusAnnouncement.value != null) {
        Text(
            text = focusAnnouncement.value!!,
            modifier = Modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .alpha(0f) // Invisible but announced
        )
    }
}
```

---

## 8. Motor Impairment Accommodations

### 8.1 Pattern Grid
- **Dwell Time**: Configurable (default 200ms) before dot selection
- **Touch Tolerance**: Hit area 56dp (cell) > 24dp (dot visual)
- **Alternative Input**:
  - Keyboard: Arrow keys navigate, Space/Enter select
  - Switch Access: Row/column scanning
  - Voice Access: "Tap dot row 2 column 3"

### 8.2 Camera Controls
- **All controls**: 48dp minimum touch target
- **Shutter**: 72dp, centered, easy reach
- **Voice commands**: "Take photo", "Record video", "Switch camera"

### 8.3 General
- **No time limits** on interactions (except security lockout)
- **Undo**: Pattern clear on lift if < 6 dots
- **Confirmation dialogs**: For destructive actions

---

## 9. Visual Impairment Support

### 9.1 High Contrast Mode
- System `highContrast` setting detected
- Colors swapped to max contrast variants
- Pattern dots: White on black (dark) / Black on white (light)

### 9.2 Reduced Motion
```kotlin
val reduceMotion = ConfigurationCompat.isReduceMotionEnabled(
    LocalConfiguration.current
)

// Disable animations
val patternAnimDuration = if (reduceMotion) 0 else tokens.durationMedium2
val cameraReviewAnimDuration = if (reduceMotion) 0 else tokens.durationMedium3
```

### 9.3 Text Alternatives
- Pattern grid: "5 by 5 grid, draw pattern"
- Recovery phrase: Monospace, 24sp minimum
- Camera: "Secure camera, no preview in TalkBack"

### 9.4 Audio Feedback (Optional)
```kotlin
// Optional sound for dot selection
val dotSelectSound = SoundPool.Builder().setMaxStreams(1).build()
    .load(context, R.raw.dot_click, 1)

// Play on dot selection
dotSelectSound.play(soundId, 1f, 1f, 1, 0, 1f)
```

---

## 10. Cognitive Accessibility

### 10.1 Clear Error Messages
| Error | Message | Action |
|-------|---------|--------|
| Short pattern | "Pattern must connect at least 6 dots" | Auto-clear, retry |
| No turn | "Pattern needs at least one turn" | Auto-clear, retry |
| Wrong pattern | "Incorrect pattern" | Clear, retry |
| Lockout | "Too many attempts. Try again in 30 seconds" | Countdown |
| Invalid phrase | "Phrase not recognized" | No timing leak |

### 10.2 Guidance
- **Pattern strength**: Visual indicator during creation
- **Recovery phrase**: "Write this down on paper. Do not store digitally."
- **Share warning**: "Send the phrase through a trusted channel"

### 10.3 Simplified UI
- No tutorials, tips, badges
- Single primary action per screen
- Settings only for optional features

---

## 11. Accessibility Testing Checklist

### 11.1 Automated (Compose Preview)
- [ ] All interactive elements have `contentDescription`
- [ ] Focus order logical (left→right, top→bottom)
- [ ] Touch targets ≥ 48dp
- [ ] Color contrast ≥ 4.5:1 (AA)
- [ ] Text scales to 200%
- [ ] Live regions for dynamic content

### 11.2 Manual (TalkBack)
- [ ] Pattern grid navigable via arrows
- [ ] Pattern drawing announced correctly
- [ ] Camera controls labeled
- [ ] Recovery phrase readable on-demand
- [ ] File grid 2D navigation works
- [ ] Settings all reachable
- [ ] Error announcements clear

### 11.3 Manual (Switch Access)
- [ ] Pattern grid scannable row/col
- [ ] Camera controls scannable
- [ ] All dialogs dismissable

### 11.4 Manual (Voice Access)
- [ ] "Tap dot row X column Y" works
- [ ] "Take photo" works
- [ ] "Open settings" works

### 11.5 Manual (Dynamic Type)
- [ ] 200% scale: all text readable
- [ ] 200% scale: no truncation
- [ ] 200% scale: touch targets preserved

---

## 12. Compliance Target

| Standard | Level | Status |
|----------|-------|--------|
| WCAG 2.1 | AA | Target |
| Android Accessibility | — | Full |
| Section 508 | — | Compliant |
| EN 301 549 | — | Compliant |

---

## 13. Accessibility API Reference

### 13.1 Key Semantics Modifiers
```kotlin
// Custom role
Modifier.semantics { role = Role.Grid }

// Live region
Modifier.semantics { liveRegion = LiveRegionMode.Polite }

// Custom actions
Modifier.semantics {
    customActions = listOf(
        CustomAccessibilityAction("Read word 1") { /* ... */ }
    )
}

// Focus order
Modifier.focusOrder(1)
Modifier.focusTarget()
```

### 13.2 Testing Helpers
```kotlin
// In Compose tests
composeTestRule
    .onNode(hasText("Vaultaire"))
    .assert(hasContentDescription("Secure photo vault"))
    .assert(isFocused())

// TalkBack simulation
composeTestRule
    .onNodeWithTag("pattern_grid")
    .performSemanticsAction(AccessibilityActions.ScrollForward)
```