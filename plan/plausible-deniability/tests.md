# Test Specifications — Plausible Deniability

**Role**: Test Lead / QA Engineer  
**Scope**: Verification of cryptographic integrity, interface compartmentalization, and forensic resistance claims.

---

## 1. Unit Tests

### 1.1 Vault Envelope Serialization
- **Test**: Verify `VaultEnvelopeHeader` is always exactly 96 bytes.
- **Test**: Verify `VLTI` magic is present and correct.
- **Test**: Verify no plaintext `vaultId` or `isDuressDecoy` flag exists in the byte stream.
- **Test**: Verify `serialize()` and `deserialize()` are symmetric.

### 1.2 Vault Discovery Engine
- **Test**: `deriveLocator(vdk)` produces deterministic 32-byte hash.
- **Test**: `deriveLocator(vdk)` produces unrelated hashes for different patterns (avoids collisions).
- **Test**: `discoverVault()` returns `null` for a random 96-byte "cover" file (simulates no match).
- **Test**: `discoverVault()` returns `null` if GCM tag is manipulated by 1 bit.
- **Test**: `discoverVault()` returns valid `VaultSession` only if VDK matches the envelope.

### 1.3 Storage Pool Manager
- **Test**: `ensurePoolSize()` creates exactly 64 files in an empty directory.
- **Test**: `ensurePoolSize()` adds 64 more files if the count reaches 65.
- **Test**: `isCoverFile()` correctly identifies random noise vs a valid header (by magic/canary).

---

## 2. Security Tests

### 2.1 Metadata Leak Audit
- **Requirement**: No plaintext metadata at rest.
- **Test**: Use a hex editor/dump tool to scan a newly created vault file. Search for:
    - The `vaultId` string.
    - The `0x01` byte at the old duress offset.
    - Any recognizable user filenames.
- **Expected Result**: 0 matches. The file should appear as high-entropy random noise except for the 4-byte magic.

### 2.2 Uniformity Test (GCM Failure)
- **Requirement**: Decryption failure for a wrong pattern must be indistinguishable from decryption failure for a cover file.
- **Test**: Measure time taken to process a "Wrong VDK" vs a "Cover File".
- **Expected Result**: Constant-time response (within jitter limits) to prevent timing-based vault enumeration.

### 2.3 Shared Storage Test
- **Requirement**: Storage files do not reveal vault ownership.
- **Test**: Create 3 vaults with different patterns. Add 5 files to each.
- **Test**: Inspect the `data/` directory.
- **Expected Result**: 15 files with random names (e.g., `uuid.dat`) in a single flat directory. No subfolders per vault.

---

## 3. Integration Tests

### 3.1 Multi-Vault Setup & Switching
1. Create "Vault A" (Pattern: L-shape).
2. Create "Vault B" (Pattern: Square).
3. Draw "Pattern A": Verify Vault A files are visible, B is hidden.
4. Draw "Pattern B": Verify Vault B files are visible, A is hidden.
5. Draw "Pattern C" (Unused): Verify empty state with "Create New" prompt.

### 3.2 Duress Trigger Integration
1. Create a normal vault (Vault A).
2. Create a duress vault (Vault B) with a specific duress pattern.
3. Draw the duress pattern.
4. **Verify**:
    - Vault B opens successfully.
    - Vault A's index file and storage files are deleted from disk (verified via filesystem check).
    - No UI alert or delay occurred during the wipe.

---

## 4. Performance Tests

### 4.1 Discovery Scaling
- **Test**: Measure time to unlock with 1, 10, and 50 vaults in the pool.
- **Expected Result**: $O(1)$ lookup time. Since we use a locator-named file, lookup should be near-instant regardless of pool size.

### 4.2 Pool Generation
- **Test**: Measure time to generate 64 cover files (96 bytes each).
- **Expected Result**: < 100ms. Secure random generation of small buffers is fast.

---

## 5. Forensic Resistance Audit (Manual)

1. **Step**: Perform a full backup of the device app container.
2. **Step**: Use a forensics tool (e.g., `Autopsy` or `Adb shell`) to list the `vaults/` directory.
3. **Audit**: Can the auditor tell if there is 1 vault or 2?
    - *Answer*: If count is < 64, it should look like 64 files.
4. **Audit**: Can the auditor tell which file is the "Real" one?
    - *Answer*: Only by attempting to brute-force the pattern offline.
5. **Audit**: Are there any leaked patterns in logs?
    - *Answer*: Scan `logcat` during unlock.
