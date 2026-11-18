# Ease of Use — Product Planning

## Design Philosophy: Security Should Feel Effortless

> "The most secure app in the world is useless if nobody can figure out how to use it."

**Core Conviction**: Real security and usability are multipliers, not tradeoffs. An app that gets used protects people; an app abandoned after 10 minutes of frustration protects exactly zero files.

### Guiding Principles

1. **Zero Configuration** — The default experience IS the secure experience. No "advanced mode" to unlock.
2. **No Accounts** — No server, no email, no personal data stored anywhere. Account = liability.
3. **No Setup Wizards** — Open app → draw pattern → add files. Three seconds, three steps.
4. **Muscle Memory Over Memorization** — Patterns use procedural memory (like riding a bike), not declarative memory (passwords).
5. **Always-On Encryption** — No "encrypt button" to forget. Encryption is a system property, not a user action.
6. **Accessibility First** — Works for everyone: VoiceOver, Dynamic Type, reduced motion, high contrast.

---

## User Stories

### First-Time User
| ID | Story | Acceptance |
|----|-------|------------|
| US-01 | As a first-time user, I want to open the app and immediately see a pattern grid so I can start protecting files without any setup | App launches directly to pattern screen on first launch |
| US-02 | As a first-time user, I want to draw a pattern and have my vault created automatically so I don't need to configure anything | First valid pattern (≥6 points) triggers vault creation |
| US-03 | As a first-time user, I want to see a recovery phrase exactly once so I can back up my vault without being forced to write it down immediately | Recovery phrase shown once after first pattern, dismissible |
| US-04 | As a first-time user, I want to add a file immediately after setting my pattern so I experience instant protection | Tap "+" → select file → file is encrypted and visible |

### Returning User
| ID | Story | Acceptance |
|----|-------|------------|
| US-05 | As a returning user, I want to unlock my vault in under 1 second by drawing my pattern so I can access files instantly | Pattern recognition < 100ms, vault unlock < 500ms |
| US-06 | As a returning user, I want the app to lock automatically when I leave it so my files are always protected | App locks on background, home, or screen off |
| US-07 | As a returning user, I want to add photos/videos/documents using familiar system pickers so I don't learn a new UI | System share sheet / file picker used for import |

### Power User / Edge Cases
| ID | Story | Acceptance |
|----|-------|------------|
| US-08 | As a user who forgot my pattern, I want to use my recovery phrase to restore access so I don't lose my files | Recovery phrase screen accessible, 12-word mnemonic works |
| US-09 | As a user with accessibility needs, I want the pattern grid to work with TalkBack/switch access so I can use the app | Pattern grid fully accessible, high contrast, large targets |
| US-10 | As a user in duress, I want to draw a decoy pattern that opens a harmless vault so I protect my real data | Duress pattern creates separate decoy vault |

---

## Onboarding Flow: First Launch → Pattern → Vault Ready

### The "No Setup Wizard" Promise

```
┌─────────────────────────────────────────────────────────────────┐
│                    FIRST LAUNCH FLOW                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  App Launch                                                     │
│     │                                                           │
│     ▼                                                           │
│  ┌──────────────────┐     Draw Pattern (≥6 pts)               │
│  │  PatternLockScreen  │ ─────────────────────────►            │
│  │  (Empty state)    │                                        │
│  └──────────────────┘                                        │
│     │                                                           │
│     │  First valid pattern                                      │
│     ▼                                                           │
│  ┌──────────────────┐     Auto-create Vault                     │
│  │  Generate MVK     │ ─────────────────────────►                │
│  │  Wrap with VDK    │                                        │
│  │  Save .vlt file   │                                        │
│  └──────────────────┘                                        │
│     │                                                           │
│     ▼                                                           │
│  ┌──────────────────┐     Show Once                            │
│  │  RecoveryPhrase   │ ─────────────────────────►                │
│  │  (12-word mnemonic)│   "Write this down"                    │
│  └──────────────────┘                                        │
│     │                                                           │
│     │  User taps "I've Saved It" / Dismisses                   │
│     ▼                                                           │
│  ┌──────────────────┐                                          │
│  │  VaultHomeScreen  │  ◄── Ready to add files                 │
│  │  (Empty vault)    │                                          │
│  └──────────────────┘                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### What Happens on First Launch

1. **App launches directly to PatternLockScreen** — No splash, no welcome, no terms, no signup
2. **User draws pattern** — Minimum 6 points on 5×5 grid (interpolated internally)
3. **Vault auto-created** — 
   - Random 256-bit Master Vault Key (MVK) generated
   - Pattern bytes normalized → Vault Derivation Key (VDK) via PBKDF2 (600k iterations)
   - MVK wrapped with VDK → stored in `.vlt` header
3. **Recovery phrase shown ONCE** — 12-word mnemonic (BIP39-style), derived to Recovery Vault Key (RVK)
4. **User dismisses** → Lands in empty VaultHomeScreen, ready to add files

### No Config, No Choices

| Setting | Value | User Configurable? |
|---------|-------|-------------------|
| KDF Algorithm | PBKDF2WithHmacSHA512 | ❌ No |
| Iterations | 600,000 | ❌ No |
| Key Length | 256 bits | ❌ No |
| Encryption | AES-256-GCM | ❌ No |
| Auth Tag | 128 bits | ❌ No |
| IV Length | 12 bytes | ❌ No |
| Pattern Min Points | 6 | ❌ No |
| Grid Size | 5×5 | ❌ No |
| Backup | Encrypted (default) | ✅ Settings |
| Analytics | Anonymous (default) | ✅ Settings |

---

## Core Interaction Model

### Daily Use Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    DAILY UNLOCK FLOW                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  App Launch / Foreground                                        │
│       │                                                         │
│       ▼                                                         │
│  ┌──────────────────┐                                           │
│  │ PatternLockScreen │ ◄── Draw pattern (muscle memory)         │
│  └────────┬─────────┘                                           │
│           │ Correct pattern?                                     │
│      ┌────┴────┐                                                 │
│      ▼         ▼                                                 │
│   ✅ Yes    ❌ No  ──► Shake / "Try again"                       │
│      │                                                         │
│      ▼                                                         │
│  ┌──────────────────┐                                           │
│  │ VaultHomeScreen  │ ◄── Files visible, tap to view            │
│  │  (Grid/List)     │       + button to add                     │
│  └──────────────────┘                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Adding Files

```
┌─────────────────────────────────────────────────────────────────┐
│                      ADD FILE FLOW                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  VaultHomeScreen                                                │
│       │                                                         │
│       │ Tap FAB "+"                                             │
│       ▼                                                         │
│  ┌──────────────────┐   System Picker                          │
│  │ Intent.ACTION_    │ ─────────────────────────►                │
│  │ OPEN_DOCUMENT /   │     User selects file(s)                 │
│  │ GET_CONTENT       │                                        │
│  └────────┬─────────┘                                           │
│           │                                                     │
│           ▼                                                     │
│  ┌──────────────────┐                                           │
│  │ VaultStorageEngine│  Encrypts on write (AES-256-GCM)        │
│  │ .createEncrypted  │  No "encrypt button" — just happens      │
│  │ OutputStream()    │                                           │
│  └──────────────────┘                                           │
│           │                                                     │
│           ▼                                                     │
│  ┌──────────────────┐                                           │
│  │ VaultHomeScreen  │ ◄── New file appears in grid              │
│  │  (Updated)       │                                           │
│  └──────────────────┘                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Acceptance Criteria (Given/When/Then)

### First Launch
| Scenario | Given | When | Then |
|----------|-------|------|------|
| AC-01 | App is freshly installed | User launches app | PatternLockScreen shows immediately (no splash) |
| AC-02 | On PatternLockScreen (first launch) | User draws pattern with < 6 points | Error: "Pattern must connect at least 6 dots" |
| AC-03 | On PatternLockScreen (first launch) | User draws valid pattern (≥6 pts) | Vault created automatically, RecoveryPhraseScreen shows |
| AC-04 | RecoveryPhraseScreen shown | User taps "I've Saved It" | Navigates to VaultHomeScreen (empty state) |
| AC-05 | RecoveryPhraseScreen shown | User swipes down / taps dismiss | Navigates to VaultHomeScreen (empty state) |
| AC-06 | RecoveryPhraseScreen dismissed | User reopens app | PatternLockScreen shown (phrase never shown again) |

### Daily Unlock
| Scenario | Given | When | Then |
|----------|-------|------|------|
| AC-07 | Vault exists | User launches app | PatternLockScreen shows |
| AC-08 | On PatternLockScreen | User draws correct pattern | Vault unlocks < 500ms, navigates to VaultHomeScreen |
| AC-09 | On PatternLockScreen | User draws incorrect pattern | Haptic feedback, "Try again", pattern clears |
| AC-10 | On PatternLockScreen | User draws incorrect pattern 5× | 30-second lockout, then retry |
| AC-11 | VaultHomeScreen visible | User presses Home / switches app | App locks, next foreground → PatternLockScreen |
| AC-12 | VaultHomeScreen visible | Screen turns off | App locks, next wake → PatternLockScreen |

### Add Files
| Scenario | Given | When | Then |
|----------|-------|------|------|
| AC-13 | VaultHomeScreen (unlocked) | User taps "+" FAB | System file picker opens |
| AC-14 | File picker open | User selects photo | Photo encrypted with AES-256-GCM, appears in grid |
| AC-15 | File picker open | User selects multiple files | All files encrypted, all appear in grid |
| AC-16 | File picker open | User cancels | Returns to VaultHomeScreen, no changes |

### Recovery
| Scenario | Given | When | Then |
|----------|-------|------|------|
| AC-17 | PatternLockScreen | User taps "Forgot Pattern?" | RecoveryPhraseEntryScreen shown |
| AC-18 | RecoveryPhraseEntryScreen | User enters valid 12-word phrase | Vault unlocks, navigates to VaultHomeScreen |
| AC-19 | RecoveryPhraseEntryScreen | User enters invalid phrase | "Invalid recovery phrase" error |

---

## UX Principles

### 1. Zero Configuration
- No settings to configure before first use
- No "enable encryption" toggle
- No algorithm selection
- No backup destination to pick

### 2. No Account
- No email, no username, no password
- No server = no breach surface
- No account recovery flow needed

### 3. No Learning Curve
- Pattern grid = familiar Android pattern unlock
- File picker = system share sheet / DocumentsUI
- View file = tap to open (uses Media3/Coil)
- Delete = long press → delete

### 4. Muscle Memory First
- 5×5 grid, large touch targets (56dp minimum)
- No numbers/letters — just spatial pattern
- Works without looking after 3-4 uses

### 5. Encryption as Gravity
- No "lock" button, no "encrypt" button
- Files encrypted the moment they enter vault
- App drops keys on background — automatic

### 6. Minimalism by Decision
- Every element on screen serves a purpose
- No tutorials, no tips, no badges
- Settings only for: backup toggle, analytics toggle, disguise (Pro)

---

## Accessibility Considerations

### PatternLockScreen
- **TalkBack**: Announce each dot as "Dot 1, row 1, column 1" during draw
- **Switch Access**: Scan rows → columns, confirm to select
- **Voice Access**: "Tap dot 1, tap dot 6, tap dot 11"
- **Large touch targets**: 56dp minimum per dot
- **High contrast**: Dots 4.5:1 contrast minimum
- **Reduced motion**: Disable dot connection animation if enabled

### VaultHomeScreen
- **TalkBack**: File name, type, size, date announced
- **Dynamic Type**: All text scales with system font size
- **Grid/List toggle**: Both accessible
- **Pull-to-refresh**: Announced for screen readers

### RecoveryPhraseScreen
- **TalkBack**: Each word announced on focus
- **Copy to clipboard**: Accessible action
- **Screen reader only hint**: "Write these 12 words in order"

### General
- **Color not sole indicator**: Pattern dots use position + color
- **Focus order**: Logical left→right, top→bottom
- **Touch target size**: 48dp minimum everywhere
- **No time limits**: Except security lockout (30s after 5 failures)

---

## What "Feels Like Unlocking Your Phone" Means in Practice

| Aspect | Phone Unlock | Vaultaire Pattern |
|--------|--------------|-------------------|
| **Time to unlock** | < 1 second | < 1 second (target) |
| **Cognitive load** | Zero (muscle memory) | Zero after 3-4 uses |
| **Visual attention** | Optional (can do blind) | Optional after practice |
| **Failure feedback** | Shake / vibration | Haptic shake + subtle text |
| **Lock trigger** | Screen off / timeout | App background / screen off |
| **Recovery** | PIN / biometric fallback | 12-word phrase (shown once) |

### Performance Targets
- **Cold start → PatternLockScreen**: < 800ms
- **Pattern recognition**: < 100ms
- **Vault unlock (correct pattern)**: < 500ms
- **VaultHomeScreen render**: < 300ms
- **Add file → visible in grid**: < 2s for 10MB file

---

## Success Metrics

| Metric | Target |
|--------|--------|
| First-launch → first file added | < 30 seconds |
| Pattern recognition accuracy | > 99% on 2nd attempt |
| Day-1 retention | > 60% |
| Recovery phrase save rate | > 80% (of those who see it) |
| Accessibility audit score | 100% WCAG 2.1 AA |
| Cold start time (p95) | < 1.5s |