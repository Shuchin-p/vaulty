# App Icons - Test Specifications

## Test Plan Overview

| Test Category | Focus Area | Priority |
|---|---|---|
| Unit Tests | AppDisguiseManager logic, enum mapping, SharedPreferences | P0 |
| Integration Tests | PackageManager interactions, component state verification | P0 |
| UI Tests | Settings flow, confirmation dialog, icon selection | P1 |
| Security Tests | Pro enforcement, icon switching restrictions | P0 |
| Edge Case Tests | App update, launcher failure, rapid switching | P1 |

---

## Unit Tests

### AppDisguiseManager - setDisguiseIcon()

**Test File**: `test/com/vaultaire/android/disguise/AppDisguiseManagerTest.kt`

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| UT-01 | Switch to Calculator icon successfully | Pro user, all aliases disabled | `setDisguiseIcon(CALCULATOR)` | Returns `true`, only Calculator enabled, stored in prefs |
| UT-02 | Switch to Calculator for free user returns false | Free user | `setDisguiseIcon(CALCULATOR)` | Returns `false`, no component changes |
| UT-03 | Switching to DEFAULT always succeeds | Any user, any icon selected | `setDisguiseIcon(DEFAULT)` | Returns `true`, MainActivity enabled, all others disabled |
| UT-04 | PackageManager exception is caught | PackageManager throws SecurityException | `setDisguiseIcon(CALCULATOR)` | Returns `false`, error logged, no state change |
| UT-05 | Correct component names passed | N/A | Call `setDisguiseIcon` with any icon | ComponentName has correct package and class |
| UT-06 | DONT_KILL_APP flag is used | N/A | Call `setDisguiseIcon` | `PackageManager.DONT_KILL_APP` passed to API |

### AppDisguiseManager - getCurrentDisguiseIcon()

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| UT-10 | Returns DEFAULT when no pref saved | Fresh install, no pref | `getCurrentDisguiseIcon()` | Returns `DisguiseIcon.DEFAULT` |
| UT-11 | Returns saved icon from SharedPreferences | PREFS has `selected_alias_class=CalculatorAlias` | `getCurrentDisguiseIcon()` | Returns `DisguiseIcon.CALCULATOR` |
| UT-12 | Handles corrupted/null pref gracefully | PREFS has invalid class name | `getCurrentDisguiseIcon()` | Returns `DisguiseIcon.DEFAULT` |
| UT-13 | Handles null SharedPreferences | SharedPreferences returns null | `getCurrentDisguiseIcon()` | Returns `DisguiseIcon.DEFAULT` |

### AppDisguiseManager - reconcileDisguiseState()

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| UT-20 | Reconciles to DEFAULT for free user | Free user, prefs has CalculatorAlias | `reconcileDisguiseState()` | All aliases disabled, DEFAULT enabled |
| UT-21 | Reconciles to saved icon for Pro user | Pro user, prefs has UnitsAlias | `reconcileDisguiseState()` | Only UnitsAlias enabled |
| UT-22 | Reconciles to DEFAULT when pref shows DEFAULT | Any user, prefs has DEFAULT | `reconcileDisguiseState()` | MainActivity enabled, all aliases disabled |

### DisguiseIcon Enum

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| UT-30 | `fromClassName()` returns correct enum | Class name = "CalculatorAlias" | `DisguiseIcon.fromClassName("CalculatorAlias")` | Returns `DisguiseIcon.CALCULATOR` |
| UT-31 | `fromClassName()` returns DEFAULT for unknown | Class name = "UnknownAlias" | `DisguiseIcon.fromClassName("UnknownAlias")` | Returns `DisguiseIcon.DEFAULT` |
| UT-32 | `fromClassName()` handles MainActivity | Class name = "MainActivity" | `DisguiseIcon.fromClassName("MainActivity")` | Returns `DisguiseIcon.DEFAULT` |

### Test Code Skeleton

```kotlin
class AppDisguiseManagerTest {

    private lateinit var context: Context
    private lateinit var mockIsPro: () -> Boolean
    private lateinit var disguiseManager: AppDisguiseManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockIsPro = { false }
        disguiseManager = AppDisguiseManagerImpl(context, mockIsPro)
    }

    @Test
    fun `setDisguiseIcon Calculator for Pro user returns true`() {
        mockIsPro = { true }
        disguiseManager = AppDisguiseManagerImpl(context, mockIsPro)
        
        val result = disguiseManager.setDisguiseIcon(DisguiseIcon.CALCULATOR)
        
        assertTrue(result)
        assertEquals(DisguiseIcon.CALCULATOR, disguiseManager.getCurrentDisguiseIcon())
        
        // Verify preferences
        val prefs = context.getSharedPreferences("vaultaire_disguise_prefs", Context.MODE_PRIVATE)
        assertEquals("com.vaultaire.android.alias.CalculatorAlias", prefs.getString("selected_alias_class", null))
    }
}
```

---

## Integration Tests

### Icon Switching Verification

**Test File**: `test/com/vaultaire/android/disguise/AppDisguiseIntegrationTest.kt`

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| IT-01 | Verify only one alias is enabled | Empty state | Switch to Calculator | Calculator enabled, all others disabled |
| IT-02 | Verify MainActivity always enabled | N/A | Any icon switch | MainActivity component state = ENABLED |
| IT-03 | Verify icon persists after PackageManager call | Calculator selected | Query PackageManager component state | CalculatorAlias state = ENABLED |
| IT-04 | Multiple rapid switches are serialized | N/A | Switch: Calculator → Printer → Units quickly | Final state = Units, no exceptions thrown |

### Component State Verification Helper

```kotlin
fun getComponentState(packageName: String, className: String): Int {
    return try {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        val component = ComponentName(packageName, className)
        pm.getComponentEnabledSetting(component)
    } catch (e: Exception) {
        -1 // ERROR
    }
}
```

---

## UI Tests

### Settings Flow Tests

**Test File**: `test/com/vaultaire/android/ui/settings/AppIconSettingsTest.kt`

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| UI-01 | App Icon screen displays for all users | User opens Settings | Navigate to App Icon tab | Grid of 8 icons displayed |
| UI-02 | Free user cannot switch icons | Free user on App Icon screen | Tap Calculator icon | Pro unlock dialog shown, icon unchanged |
| UI-03 | Pro user can switch icons | Pro user on App Icon screen | Tap Calculator → Confirm | Toast "Icon updated", icon visible in launcher |
| UI-04 | Switch back to default works | Pro user with alternate icon | Tap Vaultaire icon → Confirm | Toast shows, default icon visible |
| UI-05 | Preview grid shows correct labels | Any user | View preview grid | All labels match: Calculator, Printer, Units, Device Info, Level, Sound Meter, Network |

### Confirmation Dialog Tests

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| UI-10 | Dialog appears on icon tap (Pro) | Pro user, not on default icon | Tap alternate icon | Material dialog with "Change App Icon?" |
| UI-11 | Dialog can be cancelled | Pro user, dialog shown | Tap Cancel | Icon does not change, dialog dismisses |
| UI-12 | Dialog confirms switch | Pro user, dialog shown | Tap Confirm | Icon changes within 5 seconds |
| UI-13 | No dialog for free user | Free user | Tap alternate icon | Pro unlock dialog shown instead |

### Compose UI Test Example

```kotlin
@get:Rule
val composeTestRule = createAndroidComposeRule<MainActivity>()

@Test
fun appIconGrid_displaysAllIcons() {
    composeTestRule.onNodeWithTag("app_icon_grid").assertIsDisplayed()
    
    val expectedIcons = listOf(
        "Vaultaire", "Calculator", "Printer", "Units",
        "Device Info", "Level", "Sound Meter", "Network"
    )
    
    expectedIcons.forEach { iconName ->
        composeTestRule.onNodeWithTag("icon_label_$iconName").assertIsDisplayed()
    }
}
```

---

## Security Tests

### Pro Enforcement Tests

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| SEC-01 | Non-Pro user cannot switch via API | Free user, `isProUserProvider = { false }` | Call `setDisguiseIcon(CALCULATOR)` directly | Returns `false`, no PackageManager calls made |
| SEC-02 | Pro flag check cannot be bypassed | Free user | Attempt to bypass Pro check in UI | No way to call setDisguiseIcon without checking entitlement |
| SEC-03 | Entitlement check is authoritative | User with revoked Pro (in-app) | Open App Icon settings | Still shows lock icon / disabled state |

### Mock Pro Status Tests

```kotlin
@Test
fun `free user cannot switch via API`() {
    val freeManager = AppDisguiseManagerImpl(context) { false }
    
    val result = freeManager.setDisguiseIcon(DisguiseIcon.CALCULATOR)
    
    assertFalse(result)
    // Verify prefs not changed
    val prefs = context.getSharedPreferences("vaultaire_disguise_prefs", Context.MODE_PRIVATE)
    assertNull(prefs.getString("selected_alias_class", null))
}
```

---

## Edge Case Tests

### App Update Flow Tests

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| EC-01 | Icon persists after version change | Pro user, UnitsIcon active | Simulate version code increment, app relaunch | Units remains active, no re-selection needed |
| EC-02 | Free user defaults to DEFAULT after update | Free user, pre-update prefs had icon | Simulate version code increment, app relaunch | DEFAULT active immediately |
| EC-03 | Corrupted pref after update handled | Pref has invalid class from old version | App launch after update | Falls back to DEFAULT gracefully |

### Update Simulation Helper

```kotlin
@Test
fun `icon persists after simulated update`() {
    // Pre-setup: Pro user with Calculator selected
    val manager = AppDisguiseManagerImpl(context) { true }
    manager.setDisguiseIcon(DisguiseIcon.CALCULATOR)
    
    // Simulate update: version pref change
    // (App would normally do this on first launch after update)
    recreateApplication()
    
    // Verify
    assertEquals(DisguiseIcon.CALCULATOR, manager.getCurrentDisguiseIcon())
}
```

### Launcher Failure Tests

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| EC-10 | App handles launcher not refreshing | Icon switched, launcher ignores broadcast | App remains functional, shows Toast | No crash, user can retry or wait |
| EC-11 | Rapid switching handled gracefully | User taps 3 icons quickly | All switches complete | Final icon is last selected, no duplicates enabled |

### Error Handling Tests

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| EC-20 | PackageManager throws during switch | Mock PackageManager security exception | `setDisguiseIcon(CALCULATOR)` | Returns `false`, logs error, user notified via Toast |
| EC-21 | Icon resource missing | Drawable doesn't exist | Attempt to switch to that icon | Uses fallback icon, shows warning in log |

---

## Instrumentation / Instrumentation-Extra Tests

### Real Device Launch Testing

These tests require a physical device/emulator with a real launcher:

| Test ID | Description | Given | When | Then |
|---|---|---|---|---|
| ITX-01 | Icon visible in Pixel Launcher | Pro user, switched to Calculator | Open Pixel Launcher | Calculator icon visible on Home Screen |
| ITX-02 | Icon visible in Samsung OneUI | Pro user, switched to Units | Open Samsung Home | Units icon visible (may take up to 5s) |
| ITX-03 | Pattern screen still shows | Any icon active | Tap the disguised icon | Goes to pattern lock screen |

### Manual Validation Checklist

- [ ] Icon changes are visible on Pixel Launcher within 3 seconds
- [ ] Icon changes are visible on Samsung OneUI within 5 seconds
- [ ] Icon changes are visible on Nova Launcher within 2 seconds
- [ ] Tapping alternate icon still shows pattern screen
- [ ] Switching back to default works
- [ ] App update scenario tested on device

---

## Test Coverage Matrix

| Area | Unit Tests | Integration Tests | UI Tests | Security Tests |
|---|---|---|---|---|
| setDisguiseIcon() | 5 tests | 4 tests | - | 1 test |
| getCurrentDisguiseIcon() | 4 tests | - | - | - |
| reconcileDisguiseState() | 3 tests | - | - | - |
| DisguiseIcon enum | 3 tests | - | - | - |
| Settings UI | - | - | 5 tests | - |
| Pro Enforcement | - | - | - | 3 tests |
| Update Handling | - | - | - | 3 tests |
| Edge Cases | - | - | - | 4 tests |

**Target**: 100% coverage of `AppDisguiseManager.kt`, 90%+ coverage of Settings UI

---

## Running Tests

### JUnit 5 (Unit)
```bash
./gradlew testAppDisguiseManagerTest
```

### Android Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Specific Test Class
```bash
./gradlew connectedAndroidTest --tests "com.vaultaire.android.ui.settings.AppIconSettingsTest"
```

### Coverage Report
```bash
./gradlew buildCoverageReport
```