# Device Sync — Test Specifications

**Perspective**: QA / Security Engineer  
**Feature**: Cross-Device Vault Synchronization  
**Focus**: Deterministic merge, Zero-knowledge security, Network resilience

---

## 1. Unit Tests: Sync & Conflict Resolution

Tests for `MergeEngine.kt` and `VectorClock.kt`. These must pass 100% before any transport integration.

### 1.1. Deterministic Merge Properties (Property-Based Testing)
| Test ID | Description | Expected Result |
|---------|-------------|-----------------|
| T-UN-01 | **Commutativity**: `merge(A, B) == merge(B, A)` | Order of receiving changes does not change final vault state. |
| T-UN-02 | **Associativity**: `merge(merge(A, B), C) == merge(A, merge(B, C))` | Convergence is guaranteed regardless of batching. |
| T-UN-03 | **Idempotency**: `merge(A, A) == A` | Re-running sync on the same data has no side effects. |
| T-UN-04 | **Convergence**: Start with state S, apply different changes on A and B, then merge. | Both devices reach identical state S'. |

### 1.2. Conflict Resolution Semantics
| Test ID | Scenario | Expected Outcome |
|---------|----------|------------------|
| T-UN-05 | **Add-Wins (vs unseen delete)**: Device A adds File 1. Device B deletes File 2 (which existed on both) AND adds File 3. | File 1 added, File 2 deleted, File 3 added. |
| T-UN-06 | **Add-Wins (vs unseen delete 2)**: Device A adds File 1. Device B (offline, hasn't seen File 1) deletes everything. | File 1 survives (Device B hadn't seen it to delete it). |
| T-UN-07 | **Deduplication**: Device A and B both add byte-identical Photo P (same content hash). | Merge results in exactly one `FileEntry` for Photo P; chunk references are shared. |
| T-UN-08 | **Tombstone Dominance**: Device A deletes File X. Device B (which had seen File X) merges. | File X is removed from Device B's local index. |
| T-UN-09 | **Incremental Metadata**: Update file metadata (name/tag) without changing content. | Only the `FileEntry` metadata updates; chunk references remain stable. |

---

## 2. Unit Tests: Incremental Sync Efficiency

Tests for `GarbageCollector.kt` and `SyncEncryptionEngine.kt`.

| Test ID | Description | Metric |
|---------|-------------|--------|
| T-EFF-01 | **Chunk Reuse**: Modify file metadata only. | Verify 0 new file chunks uploaded; only 1 metadata record uploaded. |
| T-EFF-02 | **Content Dedup**: Add same 5MB photo twice. | Verify only 5MB + overhead uploaded once, not twice. |
| T-EFF-03 | **Delta Metadata**: Sync a vault with 10k files after adding 1. | Verify metadata record size remains proportional to changes, not total file count (using delta-encoding). |
| T-EFF-04 | **Padding Buckets**: Encrypt 1KB metadata vs 9MB metadata. | Verify both result in identical 10MB padded record size on Drive. |

---

## 3. Integration Tests: Google Drive Transport

Using `DriveSyncTransport` with a Mock Drive API.

| Test ID | Scenario | Verification |
|---------|----------|--------------|
| T-INT-01 | **Background Sync (WorkManager)** | Trigger `SyncWorker` mock; verify it calls `changes.list` and downloads new records. |
| T-INT-02 | **Resumable Upload Interruption** | Simulate network drop at 50% of a 10MB record upload. | Resume upload; verify Drive receives complete file without re-uploading first 50%. |
| T-INT-03 | **Push Notification Trigger** | Simulate FCM message arrival. | Verify `SyncWorker` is immediately scheduled as an "Expedited" job. |
| T-INT-04 | **Auth Token Expiry** | Force 401 Unauthorized from Drive API. | Verify `DriveAuthManager` refreshes token and retries sync automatically. |
| T-INT-05 | **Quota Exceeded Handling** | Mock 403 Forbidden (User Rate Limit/Quota). | Verify sync enters exponential backoff and UI shows "Drive Storage Full". |

---

## 4. Security Tests: Zero-Knowledge Verification

Tests for `SyncEncryptionEngine.kt` and Drive metadata.

| Test ID | Requirement | Verification Method |
|---------|-------------|---------------------|
| T-SEC-01 | **No Plaintext in Cloud** | Intercept Drive API calls. Verify payload is high-entropy (random) ciphertext. |
| T-SEC-02 | **No Metadata Leak** | Inspect Drive file metadata (name, description, properties). Verify NO vaultIds, deviceIds, or filenames. |
| T-SEC-03 | **AAD Binding** | Attempt to "replay" a sync record from Vault A into Vault B. | Verify decryption fails due to GCM tag mismatch (VaultId is in AAD). |
| T-SEC-04 | **Device Impersonation** | Attempt to upload a record for Device B from Device A. | Verify AAD check fails (DeviceId binding). |
| T-SEC-05 | **Duress Isolation** | Trigger duress mode. Attempt to start sync. | Verify enrollment key is gone; `SyncEncryptionEngine` throws `IllegalStateException`. |
| T-SEC-06 | **MVK Protection** | Verify MVK never appears in logs, even during merge debug mode. | Scan logs/memory dump for known MVK bytes. |

---

## 5. Edge Cases & Resilience

| Test ID | Scenario | Expected Resilience |
|---------|----------|---------------------|
| T-EDGE-01 | **Network Partition** | Device A and B both make 10 changes offline. | On reconnect, both devices sync → merge → converge to identical state without data loss. |
| T-EDGE-02 | **Simultaneous Edit** | Device A adds File 1 and Device B adds File 2 at the exact same millisecond. | Both files survive; vector clocks handle concurrent updates correctly. |
| T-EDGE-03 | **Device Lost / Stolen** | User loses Device A, buys Device C. | User enters recovery phrase on Device C → Vault + Sync state reconstructed from Drive. |
| T-EDGE-04 | **App Version Skew** | Device A (v1.0) and Device B (v2.0 with new metadata fields). | v1.0 ignores unknown fields; v2.0 preserves v1.0 data. Sync continues (forward/backward compat). |
| T-EDGE-05 | **Storage Quota Hit** | Drive fills up during a multi-file sync. | Sync stops gracefully; partial uploads are discarded or resumed later; local data remains safe. |
| T-EDGE-06 | **Battery Saver Active** | Android enters Battery Saver / Doze mode. | `WorkManager` defers sync until charging/active unless user explicitly opens the app. |
| T-EDGE-07 | **Corrupt Remote Record** | A Drive record is truncated or has bit-flips. | AES-GCM tag validation fails; record is discarded; sync retries or alerts user (integrity check). |
| T-EDGE-08 | **Clock Drift** | Device A's clock is 1 year in the past. | Sync succeeds (Vector Clocks are logical, not wall-clock dependent). |

---

## 6. Performance Benchmarks

| Metric | Threshold | Environment |
|--------|-----------|-------------|
| **Initial Sync (100 files)** | < 30 seconds | Pixel 7, Wi-Fi 6 |
| **Incremental Sync (1 file)** | < 3 seconds | Pixel 7, 5G |
| **Merge Execution Time** | < 100 ms | 5000 file index |
| **Memory Overhead** | < 50 MB | During active sync cycle |
| **Battery Impact** | < 1% per 24h | Periodic 15m syncs |

---

## 7. Manual Testing Checklist (UAT)

- [ ] Enable sync on Device A (Pro user).
- [ ] Install app on Device B, enter recovery phrase.
- [ ] Add a photo on Device A → Check Device B (within 1 min).
- [ ] Turn off Wi-Fi on both → Add/Delete photos → Turn on Wi-Fi → Check convergence.
- [ ] Trigger Duress Mode on Device A → Verify Device B is unaffected.
- [ ] Verify "This device + 1 other" appears in settings.
- [ ] Disconnect Device B → Verify Device A continues syncing.
- [ ] Reconnect Device B → Verify it catches up to current state.