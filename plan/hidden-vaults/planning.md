# Planning — Hidden Vaults Feature

**Role**: Product Owner  
**Scope**: Define what "Hidden Vaults" means for the Android app, user-facing behavior, and honest security limitations.

---

## 1. What "Hidden Vaults" Means in Vaultaire's Context

Hidden Vaults is an **interface property**, not a cryptographic invisibility guarantee. 

### Core Definition
- The app does **not** open to a catalog listing vault names.
- The user draws a pattern on a 5×5 lock grid.
- The pattern-derived key resolves to **one** encrypted vault index.
- No UI element advertises other vaults or their patterns.
- An observer who sees the unlocked interface knows only what that one vault contains.

### Zero-Config Onboarding
- **First launch**: App opens directly to the 5×5 pattern grid. No setup wizard, no vault creation screen.
- **First pattern drawn**: Automatically creates the first vault (encrypted index + master key) if no index exists for that pattern's derived locator.
- **No "Create New Vault" button on first run**: The act of drawing a pattern IS the vault creation.
- **Subsequent patterns**: Each new pattern = potential new vault (empty state until files added).

### What It Does NOT Mean
- It does **not** hide the fact that multiple vault indexes exist.
- It does **not** hide vault count from a forensic examiner with filesystem access.
- It does **not** make file allocation or modification timestamps invisible.
- It does **not** prevent offline key guessing against copied index files.

---

## 2. User Stories

### Epic: User Can Access Vaults Without Seeing a List

**US-1**: As a user, I want to draw a pattern and open one vault without seeing other vaults, so that casual observers cannot learn how many vaults I have.

**US-2**: As a user, I want the unlock screen to accept my pattern and immediately open the vault (or show empty state), so that there is no "vault picker" revealing other vaults.

**US-3**: As a user with multiple patterns, I want each pattern to open its own vault seamlessly, so that I can compartmentalize my data without extra UI steps.

### Epic: Plausible Deniability Support

**US-4**: As a user under coercion, I want to draw a decoy pattern that opens a vault I can safely show, so that I can provide a believable story to an examiner.

**US-5**: As a user in duress mode, I want the app to delete my real vault contents when triggered, so that I cannot be forced to reveal sensitive data.

**US-6**: As a user setting up plausible deniability, I want to understand that an examiner with filesystem access can estimate how many vaults I have, so that I make informed decisions about risk.

---

## 3. Acceptance Criteria

### AC-1: No Vault List on Launch
Given the app is unlocked or at the main lock screen, when the user starts the app, then it shows the pattern grid (5×5 dots), not a vault list, picker, or catalog.

### AC-2: Pattern Resolution Without Vault Metadata Exposure
Given a pattern is drawn, when the user enters it, then the app derives the key and attempts to open exactly one vault—the one whose index is named by the key-derived locator—without querying or displaying any other vault IDs or labels.

### AC-3: Empty State for Unknown Patterns
Given a pattern is drawn that does not match an existing vault, when the app attempts to resolve it, then the app automatically creates the vault index for that pattern and opens to an empty vault view (files list is empty). No "Create New Vault" prompt is shown — the vault is already created. The user simply starts adding files.

### AC-4: Duress Mode Integration
Given a user has configured a duress pattern and duress mode is triggered (via settings or special gesture), when triggered, then the app purges all non-duress vault files according to the `purgeNonDecoyVaultFiles()` implementation.

### AC-5: Forensic Limit Transparency
Given the user accesses settings or help, when they view information about vault security, then the app clearly states that a forensic examiner with filesystem access can estimate the number of vaults that hold files.

### AC-6: UI/UX Consistency
Given the app follows Material 3 and Android Design Guidelines, when the pattern grid is shown, then:
- The 5×5 dot grid uses accessible touch targets (minimum 48dp)
- Pattern feedback is visual (filled dots, connecting line)
- Pattern re-entry is allowed after 3 failed attempts with a cooldown
- "Forgot pattern?" navigates to recovery flow, not vault list

---

## 4. UX Design

### 4.1 Unlock Screen (Pattern Grid)

```
+-------------------------------------------------+
|                                                 |
|              Vaultaire                          |
|                                                 |
|         ○   ○   ○   ○   ○                      |
|                                                 |
|         ○   ○   ○   ○   ○                      |
|                                                 |
|         ○   ○   ●   ○   ○                      |
|                                                 |
|         ○   ○   ○   ○   ○                      |
|                                                 |
|         ○   ○   ○   ○   ○                      |
|                                                 |
|         [Forgot Pattern?]                        |
|                                                 |
+-------------------------------------------------+
```

- **Pattern Grid**: 5×5 dots, white/gray inactive, blue active
- **Visual Feedback**: Connecting line, filled dot animation
- **Error State**: After 3 failures, show "Incorrect pattern" with 30-second cooldown overlay
- **No "Create New Vault" button**: First pattern auto-creates vault
- **Forgot Pattern**: Navigates to recovery flow (biometric / recovery phrase)

### 4.2 Vault Open Success

```
+-------------------------------------------------+
| ←  My Vault                                   |
|                                                 |
|  [File List / Gallery View]                    |
|                                                 |
|                                                 |
|                                                 |
|                                                 |
|                                                 |
|  [Add File]  [Settings]                         |
|                                                 |
+-------------------------------------------------+
```

- No "Other Vaults", "Switch Vault", or "Vault List" visible
- No vault name shown in app bar (or shown as generic "Vault")
- Settings is accessed via three-dot menu, not a vault picker

### 4.3 Empty State (Pattern Not Used Before — First Time for This Pattern)

```
+-------------------------------------------------+
|                                                 |
|         No files in this vault                |
|                                                 |
|         [Add Files]                            |
|         [Add Sample Files]                     |
|                                                 |
+-------------------------------------------------+
```

- Vault is **already created** — no "Create New Vault" button
- User immediately sees "Add Files" or "Add Sample Files"
- No reference to other vaults or total vault count

---

## 5. Honest Limitations (Forensic Reality)

We must document **what the design cannot hide**:

### What a Forensic Examiner CAN Estimate

| Artifact | Reveals | Method |
|----------|---------|--------|
| Number of `vault_index_*.bin` files (excluding covers) | Approximate vault count | Count non-cover index files |
| Storage file count | Data volume estimate | Count random-named storage files |
| File sizes (total vs. individual) | Activity level | Compare against known app behavior |
| Modification timestamps | Recent vault usage | File system timestamps |
| Arena file count | Storage bands in use | Count arena files |

### What a Forensic Examiner CANNOT Learn

- Patterns (never stored, derived locally only)
- Vault labels or names (no metadata at rest)
- File original names or MIME types (encrypted in headers)
- File contents (AES-256-GCM encrypted)
- Which vault a storage file belongs to (random names, pooled storage)

### Limitations Document for Users

When users view "Security Information" or "Help", display:

> **Forensic Transparency**
> 
> Hidden Vaults protects against casual inspection. Someone looking at your screen during normal use will not see a list of your other vaults.
> 
> **However**, if an examiner obtains direct access to your device's storage (outside the running app), they can:
> - Estimate how many vaults contain files by examining storage files
> - See that Vaultaire is installed and how much space it uses
> - Copy encrypted files for offline pattern guessing
> 
> This is a limitation of the storage format, not a security vulnerability. The encryption protects content and metadata; it does not hide the existence or count of the encrypted containers.

---

## 6. Dependencies

| Dependency | Status | Required By |
|------------|--------|-------------|
| `KeyDerivationEngine` | ✅ Implemented | Pattern → VDK derivation |
| `VaultHeader` | ⚠️ Partial | Index metadata (needs encryption) |
| `VaultManager` | ⚠️ Needs refactoring | Vault CRUD operations |
| `EncryptedOutputStream` | ✅ Implemented | File writing with AES-GCM |
| `RandomAccessEncryptedSourceImpl` | ✅ Implemented | Random access reads |
| New: `VaultIndex` class | ❌ Needed | Encrypted index with file map |
| New: `VaultLocator` utility | ❌ Needed | Derive locator from key |
| New: `StoragePoolManager` | ❌ Needed | Manage padded root file pool |
| New: Pattern unlock screen | ❌ Needed | UI entry point |

---

## 7. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Off-the-shelf file manager can see vault files | Disclosure of storage files | Document limitation; advise Android security model |
| Users overestimate forensic protection | Legal/behavioral risk | Clear UI warnings and help text |
| Pattern guessing attacks | Unauthorized access | 600K PBKDF2 iterations + biometric fallback |
| Duress mode not discoverable | User safety risk | Visible configuration option, clear documentation |
| Storage pool implementation error | Data loss | Extensive testing, backup before pool changes |

---

## 8. Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| No vault list visible on any launch path | 100% | UI test coverage |
| Pattern resolution opens correct vault | ≥ 99.9% | Integration test with known patterns |
| Forensic limit transparency documented | 100% | Help screen content review |
| Duress mode purge works correctly | 100% | Manual and automated test |
| Storage file names are non-guessable | 100% | Audit storage directory after vault operations |