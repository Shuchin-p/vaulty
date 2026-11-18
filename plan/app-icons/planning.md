# App Icons - Product Planning Document

## Vision Statement
Enable Vaultaire Pro users to seamlessly blend into their device's existing app ecosystem by replacing the distinctive vault icon with familiar utility alternatives, while ensuring non-Pro users retain the ability to preview options before upgrading.

---

## User Stories

### Epic: App Icon Customization

| ID | User Story | Priority |
|---|---|---|
| US-01 | **As a** Vaultaire Pro user<br>**I want** to change my app icon to look like a standard utility app<br>**So that** casual observers won't recognize it as a vault app | P0 |
| US-02 | **As a** free user<br>**I want** to preview all alternate icons without switching them<br>**So that** I can visualize what my home screen would look like | P1 |
| US-03 | **As a** Pro user<br>**I want** to quickly switch between any icon and the default Vaultaire icon<br>**So that** I can temporarily hide my vault or experiment with different appearances | P0 |
| US-04 | **As a** Pro user (iOS 18+)<br>**I want** to create a custom-named tile via Shortcuts<br>**So that** I can give my vault a completely personalized name | P2 |
| US-05 | **As a** user who receives an app update from Play Store<br>**I want** my selected icon preference to persist<br>**So that** I don't have to re-apply my icon after an update | P0 |
| US-06 | **As a** user with a launcher that updates slowly<br>**I want** visual feedback that the icon change is in-progress<br>**So that** I know the action completed (or failed) | P1 |

---

## User Flows

### Flow 1: Preview Icons (Free User)

```
[Settings Screen] 
    ↓ tap "App Icon"
[App Icon Preview Screen] 
    ↓ (no action required)
[User views static grid of 8 icons]
    ↓
[User exits or taps "Upgrade to Pro to switch"]
```

### Flow 2: Switch Icon (Pro User)

```
[Settings Screen]
    ↓ tap "App Icon"
[App Icon Selection Screen]
    ↓ tap desired alternate icon
[Confirmation Dialog]
    "Change icon to [Icon Name]?" 
    "Vaultaire name will remain unchanged"
    ↓ [Cancel] | [Confirm]
[PackageManager.switchComponent()]
    ↓ (async, ~2-5s)
[Launcher refreshes icon]
[Toast: "Icon updated ✓"]
```

### Flow 3: Switch Back to Default (Pro User)

```
[Settings Screen] → [App Icon Selection Screen]
    ↓ tap Vaultaire icon (center/top)
[Confirmation Dialog with same flow]
```

---

## Acceptance Criteria

### AC-01: Icon Set Completeness
```
Given I am on the App Icon selection screen
When I view all available icons
Then I see exactly 8 icons: Vaultaire (default), Calculator, Printer, Units, Device Info, Level, Sound Meter, Network
And each icon has a matching drawable resource in the app
```

### AC-02: Pro Feature Gate
```
Given I am a free user
When I tap an alternate icon
Then I see a dialog: "Feature locked: App Icons is a Vaultaire Pro feature"
And the icon does not change
And I can preview all icons but cannot apply them
```

### AC-03: Confirmation Experience
```
Given I am a Pro user
When I tap an alternate icon to switch
Then a confirmation dialog appears
With title "Change App Icon?"
Message "The app icon will update on your Home Screen. Vaultaire name stays unchanged."
Buttons: [Cancel] [Confirm]
```

### AC-04: Icon Persistence
```
Given I have switched to an alternate icon
When I close and reopen the app
Then the selected icon remains active
And the Settings > App Icon screen shows the correct icon highlighted
```

### AC-05: App Update Reconciliation
```
Given Vaultaire receives a Play Store update
When the app launches for the first time after update
Then the previously selected icon (if any) is automatically re-applied
Via Application.onCreate() reconciliation logic
```

### AC-06: Launcher Refresh
```
Given I have successfully switched the icon
When I view my launcher (Pixel, Samsung, Nova, etc.)
Then the icon updates within 5 seconds
And the app opens to the pattern screen when tapped
```

---

## UX Design Specifications

### Icon Preview Grid
- **Layout**: Responsive 2-column grid (portrait), 4-column (landscape)
- **Item Size**: Minimum 48dp touch target
- **Icon Display**: Centered, with circular or adaptive background mask
- **Label Under Icon**: Utility name (e.g., "Calculator") — matches iOS behavior per website
- **Current Selection**: Small checkmark overlay in top-right

### Confirmation Dialog
- **Style**: Material3 AlertDialog
- **Title**: "Change App Icon?"
- **Message**: "This will update your Home Screen icon. The label will read Vaultaire."
- **Buttons**: Row layout - [Cancel] (text) | [Confirm] (filled button)
- **Visual**: Show the new icon inside the dialog body

### Settings Integration
- Screen title: "App Icon"
- Description text: "Choose how Vaultaire appears on your Home Screen. Pro feature."
- Toggle switch (optional): "Enable alternative icons" — hidden if Pro, informational if Free

### Name Under Icon Rationale
The Android manifest `android:label` on activity-alias allows customization. To match iOS behavior:
- Set alias `android:label` to the utility name (e.g., "Calculator")
- OR set to "Vaultaire" (more private, matches "stays Vaultaire" claim)
**Decision**: Set to utility name for brand recognition (matches current Android implementation)

---

## Pro Feature Gating

### Free User Experience
- Can access "App Icon" settings screen
- Sees preview grid with 8 icons
- Icons display the same as Pro would see
- **Cannot** tap icons to change them
- Tapping an icon shows: "App Icons is a Vaultaire Pro feature" toast/dialog

### Pro User Experience
- Full access to icon switching
- Can switch to any of the 8 icons
- Can switch back to default at any time
- Immediate confirmation feedback
- Icon state persists across app restarts and updates

### Licensing Check Implementation
- Utilize existing `Entitlement.CUSTOM_APP_ICONS`
- Check: `Entitlement.isEntitlementActive(Entitlement.CUSTOM_APPICONS)` or similar API
- If false, disable icon click handlers with tooltip

---

## Platform Differences to Document

| Aspect | iOS Behavior | Android Equivalent |
|---|---|---|
| System Alert | Non-dismissible system alert on icon change | No system alert; implement custom confirmation |
| Label Under Icon | Always "Vaultaire" | Can be customized via `android:label` |
| Tile with Custom Name | iOS 18+ Shortcuts integration | Not applicable; use custom launcher widget if needed |
| Icon Change Speed | Instant | 2-5 seconds, launcher-dependent |
| Permission | Automatic via App Store | No special permission; PackageManager API |

---

## Edge Cases & Considerations

1. **Launcher Variance**: Samsung OneUI, Nova, Pixel Launcher may update at different speeds
   - Solution: Toast notification after switch, no need to wait for launcher
   
2. **App Updated via Play Store**: Component states reset to `DISABLED` by default
   - Solution: Reconcile in `Application.onCreate()` using saved SharedPreferences state
   
3. **Rapid Switching**: User taps multiple icons quickly
   - Solution: Disable buttons during switch operation, queue requests
   
4. **Device without Play Store**: Amazon Appstore, etc.
   - Solution: If Pro, still allow icon switching (no billing dependency after purchase)
   
5. **Icon Resources Missing**: Drawable not found for an alias
   - Solution: Fallback to `ic_launcher` (default), log warning

---

## Success Metrics

| Metric | Target |
|---|---|
| Pro conversion rate from icon preview | 5% minimum |
| Post-update icon retention rate | 100% (via reconciliation) |
| Average time to icon refresh on launcher | < 5 seconds |
| Free user engagement with preview screen | 2+ taps per session |
| Support tickets about icon not changing | < 1% of Pro users |