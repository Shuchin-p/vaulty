# Test Specifications — Hidden Vaults

**Role**: QA / Test Lead  
**Scope**: Unit, UI, security, and integration test specifications for pattern-addressed vault resolution and hidden vault properties.

---

## 1. Unit Tests

### 1.1 Vault Resolution & Addressing

| Test ID | Scenario | Input | Expected Output |
|---------|----------|-------|-----------------|
| `UNIT-LOC-01` | Locator derivation is deterministic | Same `SecretKey` (VDK) | Same 32-char hex locator |
| `UNIT-LOC-02` | Different VDKs yield different locators | Two different `SecretKey`s | Two different 32-char hex locators |
| `UNIT-LOC-03` | Locator format validation | "a1b2...32chars" | `true` |
| `UNIT-LOC-04` | Locator format invalid (too short) | "a1b2" | `false` |
| `UNIT-LOC-05` | Locator format invalid (bad chars) | "z1x2..." | `false` |

### 1.2 Vault Index Manager (Encrypted Index)

| Test ID | Scenario | Setup | Expected Result |
|---------|----------|-------|-----------------|
| `UNIT-IDX-01` | Create new index | Valid VDK | Index file created in pool at correct locator |
| `UNIT-IDX-02` | Open index success | Valid VDK + existing index | `VaultIndex` object returned, MVK unwrapped |
| `UNIT-IDX-03` | Open index failure (wrong key) | Invalid VDK + existing index | `SecurityException` or `null` returned |
| `UNIT-IDX-04` | Open index failure (missing file) | Valid VDK + no file | `null` returned (no error listing other vaults) |
| `UNIT-IDX-05` | Index integrity failure | Valid VDK + corrupted index file | Auth failure detected, `null` returned |
| `UNIT-IDX-06` | **Zero-Config: openOrCreateVault creates when missing** | Valid VDK, no index file | New index created, `OpenVault` returned with empty fileMap |
| `UNIT-IDX-07` | **Zero-Config: openOrCreateVault opens existing** | Valid VDK, index exists | Existing index opened, `OpenVault` returned with fileMap |
| `UNIT-IDX-08` | **Zero-Config: wrong pattern throws SecurityException** | Invalid VDK, index exists | `SecurityException("Pattern authentication failed")` |

### 1.3 Storage Pool Manager (Padding & Bands)

| Test ID | Scenario | Setup | Expected Result |
|---------|----------|-------|-----------------|
| `UNIT-POOL-01` | Initial pool setup | Empty directory | 64 cover files created |
| `UNIT-POOL-02` | Reserve slot (1st vault) | Initial pool | 1 cover replaced by real index file |
| `UNIT-POOL-03` | Reserve slot (65th vault) | 64 existing vaults | New band added (128 total files) |
| `UNIT-POOL-04` | Delete vault | Existing vault | Index deleted, new cover file added to pool |
| `UNIT-POOL-05` | Pool file count invariant | Any CRUD ops | Total files in pool is always a multiple of 64 |

---

## 2. UI Tests (Jetpack Compose)

### 2.1 Pattern Unlock Screen

| Test ID | Scenario | Action | Expected Result |
|---------|----------|--------|-----------------|
| `UI-LOCK-01` | App launch directly to pattern | Start app | Pattern grid visible, no vault list |
| `UI-LOCK-02` | Correct pattern enters vault | Draw valid pattern | App navigates to `VaultView` |
| `UI-LOCK-03` | Incorrect pattern feedback | Draw invalid pattern | "Incorrect pattern" toast/overlay, grid clears |
| `UI-LOCK-04` | Cooldown after failure | Fail 3 times | Cooldown overlay active for 30s |
| `UI-LOCK-05` | Empty state for new pattern (Zero-Config) | Draw unused pattern | Vault auto-created, shows "No files in this vault" / "Add Files" (no "Create New" prompt) |
| `UI-LOCK-06` | No back navigation to list | Press back in vault | App minimizes/closes, does not show list |

### 2.2 Forensic Transparency

| Test ID | Scenario | Action | Expected Result |
|---------|----------|--------|-----------------|
| `UI-DOC-01` | Documentation visible | Open Help → Security | Honest limitations documented correctly |
| `UI-DOC-02` | No "Other Vaults" in UI | Open Settings in vault | No reference to total vault count or picker |

---

## 3. Security Tests

### 3.1 Plaintext Association Audit

| Test ID | Scenario | Tool | Expected Result |
|---------|----------|------|-----------------|
| `SEC-DISK-01` | No plaintext patterns on disk | `grep` / `strings` | No pattern dot sequences found in files |
| `SEC-DISK-02` | No plaintext vault labels | `grep` / `strings` | No user-entered vault names found in pool/storage |
| `SEC-DISK-03` | No plaintext filenames/types | Hex editor | Arena files contain no recognizable magic or strings |
| `SEC-DISK-04` | Index locator non-reversibility | Bruteforce test | Cannot derive VDK from locator |

### 3.2 Authentication & Integrity

| Test ID | Scenario | Tool | Expected Result |
|---------|----------|------|-----------------|
| `SEC-AUTH-01` | GCM Auth Tag failure | Modify 1 byte in index | App refuses to open, no partial data leak |
| `SEC-AUTH-02` | MVK wrap integrity | Modify 1 byte in envelope | MVK unwrap fails, vault does not open |
| `SEC-AUTH-03` | Arena chunk auth | Modify 1 byte in arena file | RandomAccessSource detects and rejects chunk |

---

## 4. Integration Tests

### 4.1 Multi-Vault Setup

| Test ID | Scenario | Steps | Expected Result |
|---------|----------|-------|-----------------|
| `INT-MULTI-01` | Independent vault state | Create Vault A + File X; Create Vault B + File Y | Vault A only shows X; Vault B only shows Y |
| `INT-MULTI-02` | Pattern collision avoidance | Create Vault A (Pattern P1); Try Create Vault B (P1) | App opens existing Vault A |
| `INT-MULTI-03` | Shared storage pool | Create 5 vaults | One shared `storage/` folder, random names |
| `INT-MULTI-04` | **Zero-Config: First launch creates first vault** | Fresh install, draw Pattern P1 | Pool has 1 real index + 63 covers; storage empty; VaultView shows empty vault |
| `INT-MULTI-05` | **Zero-Config: Second pattern creates second vault** | Existing vault, draw Pattern P2 | Pool has 2 real indexes + 62 covers; both patterns open independent vaults |

### 4.2 Duress Mode Flow

| Test ID | Scenario | Steps | Expected Result |
|---------|----------|-------|-----------------|
| `INT-DUR-01` | Duress pattern setup | Store duress pattern in Settings | Pattern exists in Keystore, encrypted |
| `INT-DUR-02` | Duress trigger (purge) | Trigger purge with duress pattern | All other indexes deleted, decoy remains |
| `INT-DUR-03` | Arena cleanup after purge | Run purge + Arena cleanup | Orphaned arena files removed from disk |

---

## 5. Performance Tests

### 5.1 KDF Overhead

| Test ID | Scenario | Requirement | Target |
|---------|----------|-------------|--------|
| `PERF-KDF-01` | Unlock latency (modern phone) | PBKDF2 600K rounds | < 1.0 seconds |
| `PERF-KDF-02` | Unlock latency (old phone) | PBKDF2 600K rounds | < 3.0 seconds |
| `PERF-KDF-03` | UI Responsiveness | Progress bar shown during KDF | 60fps UI while computing |

### 5.2 Storage Pool Scalability

| Test ID | Scenario | Requirement | Target |
|---------|----------|-------------|--------|
| `PERF-POOL-01` | Pool scan time (64 files) | Directory listing | < 10ms |
| `PERF-POOL-02` | Pool scan time (256 files) | Directory listing | < 25ms |
| `PERF-POOL-03` | Arena cleanup time | 1000 arena files | < 200ms |

---

## 6. Test Data Requirements

- **Test Patterns**:
  - Simple (3 dots)
  - Complex (9+ dots)
  - Overlapping
  - Duress pattern
- **Test Files**:
  - Small (1KB text)
  - Large (100MB video)
  - Zero-byte files
  - Random binary blobs
- **Test Device Classes**:
  - High-end (Pixel 8/9, Galaxy S24)
  - Entry-level (Moto G, Samsung A-series)
  - Emulator (Android 14/15)

---

## 7. Pass/Fail Criteria

1. **Pass**: All `UNIT` and `UI` tests pass on reference devices.
2. **Pass**: `SEC` tests confirm no plaintext leak of patterns, names, or file contents.
3. **Pass**: Pool file count is always `N * 64`.
4. **Pass**: Duress purge irrecoverably deletes non-decoy indexes.
5. **Pass**: **Zero-Config** — first pattern drawn on fresh install creates vault without any setup screen.
6. **Pass**: **Android-Native UX** — app launches directly to pattern grid; no RecyclerView, no vault list anywhere.
7. **Pass**: **Internal Storage Only** — all files in `context.filesDir`; no external storage used.
8. **Fail**: Any vault list picker, dialog, or reference to other vaults appears in UI.
9. **Fail**: Index locator can be mapped back to pattern without brute force.
10. **Fail**: Plaintext user data found in app container during audit.
11. **Fail**: "Create New Vault" button or prompt appears on first pattern draw.