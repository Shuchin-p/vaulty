# Planning — Plausible Deniability & Hidden Vaults

**Role**: Product Owner  
**Scope**: Define the meaning, user stories, acceptance criteria, and honest limitations of Plausible Deniability for the Vaultaire Android app.

---

## 1. What Plausible Deniability Means in Vaultaire

Plausible Deniability is an **interface and storage property** designed to reduce disclosure under casual inspection and provide a believable "decoy" story under coercion.

### Core Principles
- **No Vault Catalog**: The app does not display a menu of vaults. You draw a pattern, and the app resolves exactly one vault (or shows an empty state).
- **Pattern-Addressed Routing**: The pattern itself (through a derived key and locator) names the encrypted file to be opened. 
- **Equal Validity**: Real vaults and decoy vaults use the same storage format, same encryption, and same UI. Nothing in the interface labels a vault as "decoy."
- **Cryptographic Separation**: Different patterns produce unrelated keys (via 600,000 PBKDF2 rounds). Access to one vault does not reveal the keys or contents of any other.
- **Interface Compartmentalization**: An observer looking over your shoulder sees only the files in the vault you unlocked. They see no list of other vaults to target.

### Honest Limitations (The "Forensic Boundary")
Per the official Vaultaire documentation, we must be honest about what we **cannot** hide from a forensic examiner with app-container access:
- **Vault Count Estimation**: An examiner can estimate the number of vaults by counting encrypted root files (even if padded with covers) and storage files.
- **Data Volume**: The total size of encrypted data is visible to the OS and forensic tools.
- **Activity Correlation**: File modification timestamps can suggest which vaults were recently used.
- **Offline Guessing**: An attacker who copies the device salt and index files can verify pattern guesses offline. The key-derived locator and AES-GCM tag allow the attacker to confirm if a guessed pattern matches a vault.

---

## 2. User Stories

### Epic: Multi-Vault Access
- **US-1**: As a user, I want to draw a pattern and have my vault open directly without seeing a list of other vaults, so that my privacy is maintained during casual use.
- **US-2**: As a user, I want to create multiple vaults with different patterns, so that I can compartmentalize my data (e.g., Personal, Work, Sensitive).
- **US-3**: As a user, I want the app to show an empty vault if I draw an unused pattern, so that it doesn't reveal whether that pattern is "wrong" or just "empty."

### Epic: Coercion Resistance
- **US-4**: As a user under pressure, I want to draw a "decoy" pattern that opens a believable vault, so that I can satisfy an examiner's demand without exposing my real data.
- **US-5**: As a user, I want to know that my sensitive vault is not listed anywhere in the interface, so that I am not forced to choose between vaults in front of an observer.
- **US-6**: As a user setting up a decoy, I want guidance on how to make it "believable" (e.g., adding common files), so that I am better protected under forensic scrutiny.

---

## 3. Acceptance Criteria

### AC-1: Zero-Disclosure Unlock
- Given the app is at the lock screen, when the user draws a pattern, then the app derives the key and opens the corresponding vault (or empty state) without ever displaying a vault selector, picker, or list.

### AC-2: Indistinguishable Vaults
- Given multiple vaults exist on disk, when a vault is opened, then there is no UI indicator, metadata field, or label (like "Real" or "Decoy") that distinguishes its purpose. All vaults share the same feature set.

### AC-3: Pattern-Based Discovery
- Given a valid pattern is entered, when the app attempts to unlock, then it must derive a **locator** from the pattern-derived key to find the index file. It must NOT iterate through a list of known vault IDs.

### AC-4: Silent Failure / Empty State
- Given a pattern is entered that does not correspond to an existing vault, when processed, then the app must transition to an empty vault view with a "Create New Vault" option, rather than showing a "Wrong Pattern" error that implies other patterns are valid.

### AC-5: Padded Storage Pool
- Given a new vault is created, when the root index is saved, then it must be placed into a pool of 64 slots padded with random "cover" files to obscure the exact count of active vaults from casual filesystem browsing.

---

## 4. User Flows

### 4.1 Setup: Creating Multiple Vaults
1. User opens App Settings → "Vaults".
2. User sees "Create New Vault" (no list of current vaults).
3. User chooses a new pattern.
4. User confirms pattern.
5. App derives VDK → Master Key → creates encrypted index at key-derived locator.
6. User can now switch between vaults by re-locking and drawing the different patterns.

### 4.2 Daily Use: Pattern-Addressed Access
1. App launches to 5x5 Pattern Grid.
2. User draws "Pattern A".
3. App derives "Key A" → "Locator A".
4. App finds `vault_index_A.bin` in the storage pool.
5. App opens "Vault A". UI shows "Vault" (generic title).

### 4.3 Coercion Scenario: Decoy Unlock
1. User is pressured to open the app.
2. User draws "Decoy Pattern B".
3. App derives "Key B" → "Locator B".
4. App opens "Vault B" (the decoy).
5. Examiner sees believable files (e.g., common photos, notes).
6. No "Switch Vault" or "Other Vaults" menu exists in the interface to discover "Vault A".

---

## 5. Summary of Forensic Limits (Documentation for Users)

We must include this information in the app's Help/Security section:

| Feature | Protection Provided | Forensic Limit |
|---------|---------------------|----------------|
| **No Vault List** | Hides other vaults from casual observers and casual app use. | Examiner with filesystem access can see all encrypted index files. |
| **Pattern Discovery** | No plaintext map between patterns and vaults. | Examiner can guess patterns offline and verify them against index tags. |
| **Random Filenames** | Hides which files belong to which vault. | Total volume of data and file count are still visible. |
| **Cover Files** | Obscures the exact number of active vaults (padded to 64). | Examiner can still estimate "active" vs "decoy" based on storage file activity. |
| **Duress Mode** | Allows silent destruction of sensitive data. | Wiped data may leave artifacts on flash storage or in backups. |

---

## 6. Integration with Duress Mode
Plausible Deniability is the *passive* defense (compartmentalization), while Duress Mode is the *active* defense (destruction).
- A **Duress Pattern** is special: when drawn, it triggers the wipe of all *non-duress* vaults before opening the decoy vault.
- The interface for both is identical: the pattern grid.
- The discovery logic must check for a Duress Pattern match **before** attempting normal vault discovery.
