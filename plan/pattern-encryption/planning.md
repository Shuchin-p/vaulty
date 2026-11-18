# Planning Document: Pattern Encryption
## Product Owner Perspective

---

## User Stories

### Epic: Pattern-Based Vault Access

#### US-001: As a user, I want to unlock my vault by drawing a pattern
**Given** I have created a vault with a pattern  
**When** I draw my pattern correctly on the 5×5 grid  
**Then** my vault unlocks and my files become accessible  
**Priority**: Critical

#### US-002: As a user, I want to lock my vault when not in use
**Given** my vault is unlocked  
**When** I press the lock button or the app goes to background for more than 5 minutes  
**Then** the active vault state is cleared and I must re-enter my pattern  
**Priority**: Critical

#### US-003: As a user, I want to create a new vault using a pattern
**Given** I am on the onboarding flow  
**When** I draw and confirm a valid pattern  
**Then** a new vault is created with a random master key wrapped by my pattern-derived vault key  
**Priority**: Critical

#### US-004: As a user, I want to recover my vault using a recovery phrase
**Given** I have created a vault with an associated recovery phrase  
**When** I have forgotten my pattern but remember my 12-word recovery phrase  
**Then** I can derive a recovery key to unlock my vault  
**Priority**: High

#### US-005: As a user, I want a secure pattern grid interaction
**Given** I am drawing a pattern on the 5×5 grid  
**When** I draw with my finger  
**Then** the interface provides haptic feedback, visual confirmation, and prevents accidental input  
**Priority**: High

#### US-006: As a user, I want to verify my pattern strength during setup
**Given** I am creating a new pattern  
**When** I draw a weak pattern (simple shapes, short)  
**Then** I receive guidance to choose a stronger pattern  
**Priority**: Medium

### Epic: Security Features

#### US-007: As a user, I want my pattern to be protected from shoulder surfing
**Given** I am entering my pattern in a public space  
**When** I draw my pattern  
**Then** the grid is obscured from the screen recording/screenshots and observers  
**Priority**: Medium

#### US-008: As a user, I want protection against brute-force attacks
**Given** I have entered an incorrect pattern multiple times  
**When** I exceed the allowed failure threshold  
**Then** the app temporarily locks and requires a delay before another attempt  
**Priority**: Medium

#### US-009: As a user, I want my salt to be device-bound
**Given** my pattern-derived keys are stored  
**When** someone extracts my device storage  
**Then** they cannot reuse my vault without my device-specific salt  
**Priority**: High

---

## User Flow Diagrams

### Flow 1: Pattern Unlock

```
mermaid
flowchart TD
    A[App Launch] --> B{Is Vault Configured?}
    B -->|No| C[Welcome Screen]
    B -->|Yes| D[Pattern Grid Screen]
    D --> E[User Draws Pattern]
    E --> F[Normalize Pattern]
    F --> G[Derive Vault Key via PBKDF2]
    G --> H[Retrieve Wrapped Master Key]
    H --> I{Unlock Successful?}
    I -->|Yes| J[Decrypt Index]
    I -->|No| K[Show Invalid Pattern]
    K --> D
    I -->|Success| L[Load Vault Contents]
```

### Flow 2: Vault Creation with Pattern

```
mermaid
flowchart TD
    A[Create New Vault] --> B[Show Pattern Grid]
    B --> C[Draw Pattern Once]
    C --> D[Confirm Pattern]
    D --> E{Patterns Match?}
    E -->|No| F[Show Mismatch Error]
    F --> B
    E -->|Yes| G[Validate Pattern Strength]
    G --> H{Valid?}
    H -->|No| I[Show Strength Guide]
    I --> B
    H -->|Yes| J[Generate Random Master Key]
    J --> K[Derive Vault Key from Pattern]
    K --> L[Wrap Master Key with Vault Key]
    L --> M[Create Vault File]
    M --> N[Store Device Salt]
    N --> O[Generate Recovery Phrase]
    O --> P[Store Recovery Envelope]
```

### Flow 3: Recovery with Phrase

```
mermaid
flowchart TD
    A[Forgot Pattern?] --> B[Recovery Screen]
    B --> C[Enter 12-Word Phrase]
    C --> D[Normalize Phrase]
    D --> E[Derive Recovery Key via PBKDF2 800k]
    E --> F[Decrypt Recovery Envelope]
    F --> G[Extract Vault Keys]
    G --> H[Unlock Vault]
```

### Flow 4: Pattern Grid UX Detail

```
mermaid
flowchart LR
    A[Touch Down on Dot] --> B[Highlight Dot]
    B --> C[Torch Haptic Feedback]
    C --> D[Draw Line to Next Dot]
    D --> E[Continue Until Lift]
    E --> F[Process Completed Pattern]
    
    subgraph "Grid Interaction"
        A --> B --> C --> D --> E --> F
    end
    
    style A fill:#e1f5fe
    style F fill:#c8e6c9
```

---

## Acceptance Criteria

### AC-001: Pattern Normalization
**Given** a user draws a pattern connecting dots  
**When** the pattern is normalized  
**Then** intermediate points on straight lines are interpolated according to 5×5 grid rules  
**And** the serialized pattern can be deterministically reconstructed  
**Test Cases**:
- Horizontal line: (0,0) → (0,4) includes (0,1), (0,2), (0,3)
- Vertical line: (0,0) → (4,0) includes (1,0), (2,0), (3,0)
- Diagonal: (0,0) → (4,4) includes (2,2) when both midpoints are integers
- Complex path: (0,0) → (0,4) → (4,4) does not interpolate (0,2) twice

### AC-002: Key Derivation Consistency
**Given** the same pattern bytes and device salt  
**When** deriving a vault key  
**Then** the output is identical across multiple derivations  
**And** the key is cryptographically random (no patterns in output)

### AC-003: Incorrect Pattern Rejection
**Given** an incorrect pattern is used  
**When** attempting to unlock the vault  
**Then** the unwrap operation fails with GCM authentication error  
**And** no vault contents are accessible

### AC-004: Vault Key Unwraps Master Key
**Given** a valid pattern-derived vault key  
**When** attempting to unwrap the master key from encrypted envelope  
**Then** the master key is successfully extracted  
**And** the master key is a new 256-bit random value

### AC-005: Minimum Pattern Requirements
**Given** a user is creating a new pattern  
**When** the pattern has fewer than 6 dots OR lacks at least one direction change  
**Then** the pattern is rejected with specific error message  
**Error Message**: "Pattern must connect at least 6 dots with at least one direction change"

### AC-006: Device Salt Uniqueness
**Given** a new device is created  
**When** the device salt is generated  
**Then** the salt is 32 bytes of cryptographically secure random data  
**And** the salt persists across app restarts  
**And** different devices have different salts

### AC-007: Memory Hygiene on Lock
**Given** the vault is locked or app terminates  
**When** checking memory state  
**Then** sensitive byte arrays are zeroed  
**And** char arrays used for pattern processing are cleared

---

## Edge Cases and Error Handling

### Edge Case Catalog

| Edge Case | Description | Handling Strategy |
|-----------|-------------|-------------------|
| **Empty Pattern** | User lifts finger after 1 dot | Reject: "Pattern must contain at least 6 dots" |
| **Same Dot Touched Twice** | User accidentally touches same dot twice | Normalize to remove duplicates |
| **Diagonal Skip** | Pattern jumps diagonally over intermediate dots | Interpolate intermediate points |
| **Pattern Too Short** | Pattern connects fewer than 6 unique dots | Reject with explanation |
| **Straight Line** | Pattern has no direction changes | Reject: "Pattern needs at least one turn" |
| **Device Clock Changed** | User changes device time during vault operations | No impact: all operations local |
| **App Crashed During Derivation** | PBKDF2 interrupted by crash | Safe: no state mutation until completion |
| **Low Memory** | Device has minimal RAM during encryption | Graceful: operations should work or fail safely |
| **Multiple Failed Attempts** | User enters wrong pattern repeatedly | Lockout after threshold (implementation-defined) |
| **Corrupted Salt File** | DeviceSaltManager returns invalid data | Regenerate salt (breaks existing vaults) |
| **Non-ASCII Pattern** | Pattern contains non-standard characters after serialization | Domain separator prevents collision |

### Error Handling Matrix

| Error Type | User Message | System Action | Logging |
|------------|--------------|---------------|---------|
| Invalid pattern length | "Pattern must connect at least 6 dots" | Return to grid, no state change | WARN |
| No direction change | "Pattern needs at least one turn" | Return to grid, no state change | WARN |
| GCM tag mismatch | "Incorrect pattern" | Clear active state, increment failed counter | INFO |
| Vault file not found | "Vault not found" | Error state, suggest recovery | ERROR |
| Salt generation failure | "Unable to initialize security" | Abort vault creation | ERROR |
| PBKDF2 timeout | "Operation taking too long" | Cancel and retry | WARN |
| Memory allocation failure | "Insufficient memory" | Abort, suggest restart | ERROR |

---

## UX Considerations for 5×5 Grid Interaction

### Grid Layout Specifications

**Visual Design:**
- 5×5 grid = 25 dots arranged in even spacing
- Cell size: minimum 48dp×48dp touch target
- Dot size: 24dp diameter with 12dp padding
- Spacing between dots: 56dp (accounting for dot diameter)
- Total grid size: ~248dp × 248dp (accounting for padding)

**Touch Interaction:**
```
┌──────────────────────────────────┐
│  •    •    •    •    •           │
│                                  │
│  •    •    •    •    •           │
│                                  │
│  •    •    •    •    •           │
│                                  │
│  •    •    •    •    •           │
│                                  │
│  •    •    •    •    •           │
└──────────────────────────────────┘
```

### Animation Specifications

| State | Visual Feedback | Duration | Haptic |
|-------|-----------------|----------|--------|
| Touch down | Dot fills + scales to 120% | 100ms | light impact |
| Drag to adjacent | Line draws smoothly | Continuous | None |
| Drag to diagonal/intermediate | Interpolated dots light up | 150ms | light impact per interpolated |
| Valid pattern complete | Checkmark animation | 300ms | medium impact |
| Invalid pattern | Shake animation | 500ms | None |
| Wrong pattern | Red flash + vibration | 300ms | heavy impact |

### Accessibility Considerations

**Visual:**
- High contrast colors (minimum 4.5:1)
- Alternative for colorblind users (shape differentiation)
- VoiceOver/TalkBack support: announce dot position
- Font size adjustable for instructional text

**Motor:**
- Larger touch targets (minimum 48dp)
- Allow pattern with single finger or stylus
- Support for switch access devices
- Adjustable gesture speed sensitivity

**Cognitive:**
- Clear error messages
- Pattern strength guidance
- Undo last draw gesture
- Show pattern as dots (not continuous line during draw)

---

## Accessibility Notes

### WCAG 2.1 Compliance Targets

| Criterion | Requirement | Implementation |
|-----------|-------------|----------------|
| 1.3.1 Info and Relationships | Grid structure programmatically determinable | Use semantic UI components |
| 2.1.1 Keyboard | Operable via keyboard | Arrow keys + Enter for dot selection |
| 2.4.7 Focus Visible | Visible focus indicator | Show selected dot with border |
| 3.2.1 On Focus | No context change on focus | No auto-submission on focus |
| 4.1.2 Name, Role, Value | ARIA labels for dots | "Dot row 1 column 1" etc. |

### Screen Reader Support

```
Grid: Pattern unlock grid, 5 rows, 5 columns
Row 1: Dot 1 of 25, Dot 2 of 25, Dot 3, Dot 4, Dot 5
User: Touch dot 3 for first pattern point
```

### Motor Impairment Accommodations

- **Dwell time**: Allow longer touch duration before registering selection
- **Tolerance**: Larger hit area around dots (extend touch target to 64dp)
- **Alternative input**: Support for:
  - Keyboard navigation (arrow keys + Space/Enter)
  - Switch scanning patterns
  - Voice commands to select dots by coordinate

### Visual Impairment Support

- **High contrast mode**: Black dots on white background, 7:1 contrast
- **Reduced motion**: Disable animation for vestibular disorders
- **Text alternatives**: Read out current pattern length during drawing
- **Audio feedback**: Optional sound for dot selection

---

## Performance Requirements

| Metric | Target | Measurement |
|--------|--------|-------------|
| Pattern normalization | < 5ms | CPU time on mid-tier device |
| Key derivation (PBKDF2) | < 2s | Wall clock time for 600k iterations |
| Vault unlock | < 500ms | Total time from lift to file access |
| Memory footprint | < 5MB | Peak during derivation |
| Battery impact | Normal | PBKDF2 should not drain battery excessively |

---

## Security Requirements

| Requirement | Specification |
|-------------|---------------|
| Minimum entropy | 13 bits (6 dots) to 16+ bits (recommended) |
| KDF iterations | 600,000 for pattern, 800,000 for recovery |
| Key length | 256 bits for all keys |
| Cipher | AES-256-GCM with 128-bit auth tag |
| Salt | 256-bit device-bound, stored in secure hardware |
| Nonce | 96-bit unique per encryption operation |
| Timing attacks | Constant-time comparison for pattern verification |
| Memory cleanup | Zero all sensitive buffers on lock/clear |