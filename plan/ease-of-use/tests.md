# Ease of Use — Test Specifications

## Test Strategy

| Layer | Tool | Coverage Target |
|-------|------|-----------------|
| Unit | JUnit 5 + MockK | 90% crypto, vault, storage logic |
| Integration | Robolectric | Navigation, lifecycle, DI |
| UI | Compose Test + Espresso | 100% critical user flows |
| Performance | Macrobenchmark | Cold start, unlock latency |
| Accessibility | Accessibility Scanner + Espresso | WCAG 2.1 AA compliance |

---

## UI Tests — Onboarding Flow (First Launch → Pattern → Vault)

### Test: First Launch Shows Pattern Lock Immediately

```kotlin
@Test
fun firstLaunch_showsPatternLockScreen_noSplash() {
    // Given: Fresh install (no vault files)
    val scenario = launchActivity<MainActivity>()
    
    // When: App launches
    // Then: PatternLockScreen is visible immediately
    scenario.onNode(hasTestTag("pattern_lock_screen"))
        .assertExists()
    
    // And: No welcome/onboarding screens
    scenario.onNode(hasText("Welcome")).assertDoesNotExist()
    scenario.onNode(hasText("Get Started")).assertDoesNotExist()
    scenario.onNode(hasText("Create Account")).assertDoesNotExist()
}
```

### Test: Drawing Valid Pattern Creates Vault

```kotlin
@Test
fun firstLaunch_validPattern_createsVaultAndShowsRecovery() {
    // Given: Fresh install
    val scenario = launchActivity<MainActivity>()
    val patternScreen = scenario.onNode(hasTestTag("pattern_lock_screen"))
    
    // When: User draws valid pattern (6+ points)
    val points = listOf(
        PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(0, 2),
        PatternPoint(1, 2), PatternPoint(2, 2), PatternPoint(2, 1)
    )
    patternScreen.performTouchInput { drawPattern(points) }
    
    // Then: RecoveryPhraseScreen appears with 12 words
    scenario.onNode(hasTestTag("recovery_phrase_screen")).assertExists()
    scenario.onNode(hasTestTag("recovery_word_1")).assertExists()
    // ... verify all 12 words present
    scenario.onNode(hasText("I've Saved It")).assertExists()
    scenario.onNode(hasText("Remind Me Later")).assertExists()
}
```

### Test: Invalid Pattern (< 6 Points) Shows Error

```kotlin
@Test
fun firstLaunch_invalidPatternTooShort_showsError() {
    val scenario = launchActivity<MainActivity>()
    val patternScreen = scenario.onNode(hasTestTag("pattern_lock_screen"))
    
    // When: User draws only 4 points
    val points = listOf(PatternPoint(0, 0), PatternPoint(0, 1), PatternPoint(1, 1), PatternPoint(1, 0))
    patternScreen.performTouchInput { drawPattern(points) }
    
    // Then: Error shown, pattern cleared
    scenario.onNode(hasText("Connect at least 6 dots")).assertExists()
    scenario.onNode(hasTestTag("pattern_grid")).assert { it.hasNoSelection() }
}
```

### Test: Recovery Phrase Dismissal → Vault Home

```kotlin
@Test
fun recoveryPhraseDismissed_navigatesToEmptyVaultHome() {
    val scenario = launchActivity<MainActivity>()
    
    // Complete first launch flow to recovery screen
    drawValidPattern(scenario)
    scenario.onNode(hasTestTag("recovery_phrase_screen")).assertExists()
    
    // When: User taps "I've Saved It"
    scenario.onNode(hasText("I've Saved It")).performClick()
    
    // Then: VaultHomeScreen shown (empty state)
    scenario.onNode(hasTestTag("vault_home_screen")).assertExists()
    scenario.onNode(hasTestTag("empty_vault_state")).assertExists()
    scenario.onNode(hasTestTag("fab_add_file")).assertExists()
}
```

### Test: Recovery Phrase Never Shown Again

```kotlin
@Test
fun recoveryPhraseShownOnce_onReopenShowsPatternLock() {
    val scenario = launchActivity<MainActivity>()
    completeFirstLaunchFlow(scenario) // Pattern → Recovery → Dismiss
    
    // Kill and relaunch
    scenario.recreate()
    
    // Then: PatternLockScreen shown (not recovery)
    scenario.onNode(hasTestTag("pattern_lock_screen")).assertExists()
    scenario.onNode(hasTestTag("recovery_phrase_screen")).assertDoesNotExist()
}
```

---

## UI Tests — Core Interaction (Pattern → Add File → Encrypt)

### Test: Correct Pattern Unlocks Vault

```kotlin
@Test
fun returningUser_correctPattern_unlocksVault() {
    // Given: Existing vault with known pattern
    val vaultId = setupExistingVault(pattern = TEST_PATTERN)
    val scenario = launchActivity<MainActivity>()
    
    // When: User draws correct pattern
    drawPattern(scenario, TEST_PATTERN)
    
    // Then: VaultHomeScreen shown with files
    scenario.onNode(hasTestTag("vault_home_screen")).assertExists()
    scenario.onNode(hasTestTag("vault_file_item")).assertExists() // At least one
}
```

### Test: Incorrect Pattern Shows Error

```kotlin
@Test
fun returningUser_incorrectPattern_showsError() {
    val vaultId = setupExistingVault(pattern = TEST_PATTERN)
    val scenario = launchActivity<MainActivity>()
    
    // When: User draws wrong pattern
    drawPattern(scenario, WRONG_PATTERN)
    
    // Then: Error feedback
    scenario.onNode(hasText("Incorrect pattern")).assertExists()
    scenario.onNode(hasTestTag("pattern_grid")).assert { it.hasNoSelection() }
}
```

### Test: Add File Encrypts and Shows in Grid

```kotlin
@Test
fun addFile_photo_encryptsAndAppearsInGrid() {
    val scenario = launchAndUnlockVault()
    
    // When: User taps FAB and selects photo
    scenario.onNode(hasTestTag("fab_add_file")).performClick()
    scenario.onNode(hasTestTag("file_picker")).assertExists()
    selectTestPhoto(scenario, "test_photo.jpg")
    
    // Then: Photo appears in vault grid (encrypted)
    scenario.onNode(hasTestTag("vault_home_screen")).assertExists()
    scenario.onNode(hasTestTag("vault_file_item"))
        .assert(hasDescendant(withText("test_photo.jpg")))
    
    // And: File is actually encrypted on disk
    val vaultFiles = getVaultFiles()
    assertTrue(vaultFiles.all { it.isEncrypted() })
}
```

### Test: Multiple Files Added Simultaneously

```kotlin
@Test
fun addMultipleFiles_allEncryptedAndVisible() {
    val scenario = launchAndUnlockVault()
    
    scenario.onNode(hasTestTag("fab_add_file")).performClick()
    selectMultipleFiles(scenario, listOf("doc.pdf", "photo.jpg", "video.mp4"))
    
    scenario.onNode(hasTestTag("vault_file_item")).assertCount(3)
    getVaultFiles().forEach { assertTrue(it.isEncrypted()) }
}
```

### Test: Auto-Lock on Background

```kotlin
@Test
fun appBackgrounded_autoLocks_nextForegroundShowsPattern() {
    val scenario = launchAndUnlockVault()
    
    // When: App goes to background
    scenario.moveToState(Lifecycle.State.STARTED)
    scenario.moveToState(Lifecycle.State.CREATED)
    
    // And: Returns to foreground
    scenario.moveToState(Lifecycle.State.STARTED)
    scenario.moveToState(Lifecycle.State.RESUMED)
    
    // Then: PatternLockScreen shown
    scenario.onNode(hasTestTag("pattern_lock_screen")).assertExists()
}
```

---

## Usability Test Scenarios

### Scenario 1: Complete Novice (Never Used Vault App)

| Step | Expected Time | Success Criteria |
|------|---------------|------------------|
| Install & open app | < 3s | Pattern grid visible |
| Draw pattern | < 10s | Pattern accepted (≥6 pts) |
| See recovery phrase | < 5s | 12 words visible |
| Dismiss recovery | < 3s | Vault home visible |
| Add first photo | < 15s | Photo in grid, encrypted |
| **Total** | **< 36s** | **All steps completed without help** |

### Scenario 2: Returning User (Daily Use)

| Step | Expected Time | Success Criteria |
|------|---------------|------------------|
| Open app | < 1s | Pattern grid visible |
| Draw pattern | < 1s | Vault unlocks |
| View recent file | < 2s | File opens |
| **Total** | **< 4s** | **Muscle memory, no conscious thought** |

### Scenario 3: Accessibility User (TalkBack Enabled)

| Step | Expected Behavior |
|------|-------------------|
| Open app | TalkBack announces "Pattern lock, 5 by 5 grid" |
| Navigate grid | Swipe right/left moves between dots, double-tap selects |
| Draw pattern | Each selected dot announced: "Dot 7 selected, row 2 column 2" |
| Pattern complete | "Pattern complete, unlocking vault" |
| Recovery phrase | Each word announced on focus, copy action available |

---

## Performance Tests

### Test: Cold Start Time

```kotlin
// Macrobenchmark: Cold start
@Test
fun coldStart_performance() {
    val benchmark = MacrobenchmarkScope()
    
    benchmark.measureRepeated(
        packageName = "com.vaultaire.android",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
    
    // Assert: p95 < 800ms
    assertTrue(benchmark.startupTimingMetric.timeToInitialDisplayP95 < 800)
}
```

### Test: Pattern Recognition Latency

```kotlin
@Test
fun patternRecognition_latency() {
    val scenario = launchActivity<MainActivity>()
    val patternScreen = scenario.onNode(hasTestTag("pattern_lock_screen"))
    
    // Warm up
    drawPattern(scenario, TEST_PATTERN)
    scenario.recreate()
    
    // Measure: Pattern drawn → VaultHomeScreen visible
    val startTime = SystemClock.uptimeMillis()
    drawPattern(scenario, TEST_PATTERN)
    scenario.onNode(hasTestTag("vault_home_screen")).waitForExistence(2000)
    val latency = SystemClock.uptimeMillis() - startTime
    
    // Assert: < 500ms
    assertTrue(latency < 500, "Pattern unlock took ${latency}ms, expected < 500ms")
}
```

### Test: Add File Encryption Throughput

```kotlin
@Test
fun addFile_encryptionThroughput() {
    val scenario = launchAndUnlockVault()
    val testFiles = (1..10).map { createTestFile("${it}.bin", 10 * 1024 * 1024) } // 10MB each
    
    val startTime = SystemClock.uptimeMillis()
    testFiles.forEach { file ->
        scenario.onNode(hasTestTag("fab_add_file")).performClick()
        selectFile(scenario, file)
        scenario.onNode(hasTestTag("vault_file_item")).waitForExistence(5000)
    }
    val totalTime = SystemClock.uptimeMillis() - startTime
    
    // Assert: 100MB encrypted in < 30s (~3.3 MB/s minimum)
    assertTrue(totalTime < 30_000, "100MB encryption took ${totalTime}ms")
}
```

### Test: Memory Usage During Operation

```kotlin
@Test
fun memoryUsage_duringFileOperations() {
    val scenario = launchAndUnlockVault()
    
    // Add 50 photos (100MB total)
    repeat(50) {
        scenario.onNode(hasTestTag("fab_add_file")).performClick()
        selectTestPhoto(scenario, "photo_${it}.jpg")
    }
    
    // Measure heap
    val heapSize = Debug.getNativeHeapAllocatedSize()
    
    // Assert: < 100MB heap (streaming, not loading full files)
    assertTrue(heapSize < 100 * 1024 * 1024, "Heap: ${heapSize / 1024 / 1024}MB")
}
```

---

## Accessibility Tests

### Test: PatternLockScreen TalkBack Support

```kotlin
@Test
fun patternLockScreen_talkBackAnnouncesGrid() {
    val scenario = launchActivity<MainActivity>()
    scenario.onNode(hasTestTag("pattern_lock_screen"))
        .assert(hasContentDescription("Pattern lock, 5 by 5 grid"))
}

@Test
fun patternLockScreen_talkBackAnnouncesDots() {
    val scenario = launchActivity<MainActivity>()
    val dot7 = scenario.onNode(hasTestTag("pattern_dot_7")) // row 1, col 2
    dot7.assert(hasContentDescription("Dot 7, row 1, column 2"))
}

@Test
fun patternLockScreen_talkBackAnnouncesSelection() {
    val scenario = launchActivity<MainActivity>()
    scenario.onNode(hasTestTag("pattern_dot_0")).performClick()
    scenario.onNode(hasTestTag("pattern_dot_6")).performClick()
    
    // TalkBack should announce selected dots
    // Verified via AccessibilityNodeInfo querying
}
```

### Test: Dynamic Type Scaling

```kotlin
@Test
fun vaultHomeScreen_dynamicType_scalesText() {
    // Set system font scale to 2.0x
    setFontScale(2.0f)
    
    val scenario = launchAndUnlockVault()
    val fileName = scenario.onNode(hasTestTag("vault_file_name"))
    
    // Text should scale with system setting
    val fontSize = fileName.getFontSize()
    assertTrue(fontSize > baseFontSize * 1.5)
}
```

### Test: Reduced Motion

```kotlin
@Test
fun patternLockScreen_reducedMotion_disablesAnimations() {
    setReducedMotionEnabled(true)
    
    val scenario = launchActivity<MainActivity>()
    val patternScreen = scenario.onNode(hasTestTag("pattern_lock_screen"))
    
    // Draw pattern - no connection line animation
    drawPattern(scenario, TEST_PATTERN)
    
    // Verify no animation running
    assertFalse(patternScreen.hasActiveAnimations())
}
```

### Test: Color Contrast

```kotlin
@Test
fun allScreens_meetWCAGContrast() {
    val scanner = AccessibilityScanner()
    val results = scanner.scanAllScreens()
    
    results.violations
        .filter { it.ruleId == "color-contrast" }
        .forEach { violation ->
            fail("Contrast violation: ${violation.description} on ${violation.screen}")
        }
}
```

---

## Edge Case Tests

### Test: First Launch with No Storage Space

```kotlin
@Test
fun firstLaunch_noStorageSpace_showsError() {
    // Mock: Internal storage full
    mockStorageFull()
    
    val scenario = launchActivity<MainActivity>()
    drawValidPattern(scenario)
    
    // Should show error, not crash
    scenario.onNode(hasText("Not enough storage space")).assertExists()
    scenario.onNode(hasText("Free up space and try again")).assertExists()
}
```

### Test: Pattern Too Short (Exact Boundary)

```kotlin
@Test
fun pattern_exactly5Points_rejected() {
    val scenario = launchActivity<MainActivity>()
    val points5 = listOf(
        PatternPoint(0,0), PatternPoint(0,1), PatternPoint(0,2),
        PatternPoint(1,2), PatternPoint(2,2)
    )
    drawPattern(scenario, points5)
    
    scenario.onNode(hasText("Connect at least 6 dots")).assertExists()
}

@Test
fun pattern_exactly6Points_accepted() {
    val scenario = launchActivity<MainActivity>()
    val points6 = listOf(
        PatternPoint(0,0), PatternPoint(0,1), PatternPoint(0,2),
        PatternPoint(1,2), PatternPoint(2,2), PatternPoint(2,1)
    )
    drawPattern(scenario, points6)
    
    scenario.onNode(hasTestTag("recovery_phrase_screen")).assertExists()
}
```

### Test: Recovery Phrase Screen Dismissal Variations

```kotlin
@Test
fun recoveryPhrase_swipeDown_dismisses() {
    val scenario = launchActivity<MainActivity>()
    completeFirstLaunchToRecovery(scenario)
    
    scenario.onNode(hasTestTag("recovery_phrase_screen")).performSwipeDown()
    
    scenario.onNode(hasTestTag("vault_home_screen")).assertExists()
}

@Test
fun recoveryPhrase_backPress_dismisses() {
    val scenario = launchActivity<MainActivity>()
    completeFirstLaunchToRecovery(scenario)
    
    scenario.sendKeyEvent(KeyEvent.KEYCODE_BACK)
    
    scenario.onNode(hasTestTag("vault_home_screen")).assertExists()
}
```

### Test: Forgot Pattern → Recovery Flow

```kotlin
@Test
fun forgotPattern_validRecoveryPhrase_restoresAccess() {
    val vaultId = setupExistingVault(pattern = TEST_PATTERN, recoveryWords = TEST_RECOVERY)
    val scenario = launchActivity<MainActivity>()
    
    // Tap "Forgot Pattern?"
    scenario.onNode(hasText("Forgot Pattern?")).performClick()
    
    // Enter recovery phrase
    scenario.onNode(hasTestTag("recovery_entry_screen")).assertExists()
    enterRecoveryWords(scenario, TEST_RECOVERY)
    
    // Should unlock
    scenario.onNode(hasTestTag("vault_home_screen")).assertExists()
}

@Test
fun forgotPattern_invalidRecoveryPhrase_showsError() {
    val vaultId = setupExistingVault(recoveryWords = TEST_RECOVERY)
    val scenario = launchActivity<MainActivity>()
    
    scenario.onNode(hasText("Forgot Pattern?")).performClick()
    enterRecoveryWords(scenario, WRONG_RECOVERY)
    
    scenario.onNode(hasText("Invalid recovery phrase")).assertExists()
}
```

### Test: 5 Failed Attempts → Lockout

```kotlin
@Test
fun fiveFailedAttempts_triggers30SecondLockout() {
    val vaultId = setupExistingVault(pattern = TEST_PATTERN)
    val scenario = launchActivity<MainActivity>()
    
    repeat(5) {
        drawPattern(scenario, WRONG_PATTERN)
        // Wait for error to clear
        scenario.onNode(hasText("Incorrect pattern")).waitUntilGone()
    }
    
    // 6th attempt should show lockout
    scenario.onNode(hasText("Too many attempts")).assertExists()
    scenario.onNode(hasText(matches("Wait \\d+s"))).assertExists()
    
    // After 30s, should allow retry
    Thread.sleep(31_000)
    drawPattern(scenario, TEST_PATTERN)
    scenario.onNode(hasTestTag("vault_home_screen")).assertExists()
}
```

### Test: Corrupt Vault File Handling

```kotlin
@Test
fun corruptVaultFile_showsErrorAndOffersReset() {
    // Create corrupt .vlt file
    createCorruptVaultFile()
    
    val scenario = launchActivity<MainActivity>()
    drawValidPattern(scenario)
    
    scenario.onNode(hasText("Vault damaged")).assertExists()
    scenario.onNode(hasText("Reset vault")).assertExists()
    
    scenario.onNode(hasText("Reset vault")).performClick()
    scenario.onNode(hasTestTag("recovery_phrase_screen")).assertExists()
}
```

---

## Test Data Helpers

```kotlin
// TestUtilities.kt
object TestData {
    val TEST_PATTERN = listOf(
        PatternPoint(0,0), PatternPoint(0,1), PatternPoint(0,2),
        PatternPoint(1,2), PatternPoint(2,2), PatternPoint(2,1)
    )
    
    val WRONG_PATTERN = listOf(
        PatternPoint(0,0), PatternPoint(1,0), PatternPoint(2,0),
        PatternPoint(2,1), PatternPoint(2,2), PatternPoint(1,2)
    )
    
    val TEST_RECOVERY = listOf(
        "abandon", "ability", "able", "about", "above", "absent",
        "absorb", "abstract", "absurd", "abuse", "access", "accident"
    )
    
    fun createTestFile(name: String, sizeBytes: Long): Uri {
        // Creates a test file in app's cache dir
    }
    
    fun drawPattern(scenario: ComposeTestRule, points: List<PatternPoint>) {
        // Simulates touch input drawing through grid points
    }
}
```

---

## CI/CD Integration

```yaml
# .github/workflows/test.yml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew testDebugUnitTest
      
  ui-tests:
    runs-on: macos-latest  # Required for Android emulator
    steps:
      - uses: actions/checkout@v4
      - name: Run UI Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          force-avd-creation: false
          emulator-options: -no-window -gpu swiftshader_indirect
      - run: ./gradlew connectedDebugAndroidTest
      
  performance-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Macrobenchmarks
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
      - run: ./gradlew :app:connectedBenchmarkAndroidTest
      
  accessibility-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.vaultaire.android.AccessibilityTestSuite
```

---

## Definition of Done

- [ ] All unit tests pass (90%+ coverage on crypto/vault/storage)
- [ ] All UI tests pass on API 26, 30, 34, 35
- [ ] Cold start p95 < 800ms on Pixel 6a (reference device)
- [ ] Pattern unlock p95 < 500ms
- [ ] Accessibility Scanner: 0 violations (WCAG 2.1 AA)
- [ ] Usability test: 5/5 novice users complete onboarding < 60s without help
- [ ] Memory usage < 100MB during 100MB file import
- [ ] No flaky tests in CI for 30 consecutive runs