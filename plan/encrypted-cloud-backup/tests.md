# Test Specifications — Encrypted Cloud Backup

## 1. Unit Tests (Cryptography & Local Engine)

### Backup Key Derivation (`BackupKeyDerivationEngineTest`)
- **Verify key separation**: Ensure `deriveBackupKey` and `deriveVaultKey` produce different keys for the same pattern.
- **Verify iterations**: Mock `SecretKeyFactory` to verify 600,000 iterations are used for backup and 800,000 for recovery.
- **Verify salt context**: Ensure the context string (`VLTBKP`) is prepended to the device salt before PBKDF2.
- **Verify normalization**: Test `normalizePhrase` with varying case, extra spaces, and trailing whitespace to ensure it produces a consistent deterministic string.

### Padding Scheme (`ZeroKnowledgeBackupEngineTest`)
- **Verify size obfuscation**:
  - Input: 100 KB -> Output: 10 MB
  - Input: 9.9 MB -> Output: 10 MB
  - Input: 10.1 MB -> Output: 20 MB
  - Input: 55 MB -> Output: 60 MB
- **Verify padding content**: Ensure padding bytes are zeros (to avoid introducing entropy that reveals true size) but the ciphertext itself looks random.

### Encrypt/Decrypt Round-Trip
- **Round-trip verification**: Encrypt a known plaintext file, decrypt it, and verify the hash matches original.
- **AAD Integrity**: Encrypt a file, modify one byte in the **header** (e.g., change `originalSize`), and verify `decryptBackup` throws a `AEADBadTagException` or equivalent.
- **Tag Integrity**: Encrypt a file, modify one byte in the **ciphertext**, and verify decryption fails immediately.
- **Large file handling**: Test with 50 MB file to ensure chunking and streaming logic works without OOM.

---

## 2. Integration Tests (Cloud & Background)

### Google Drive Integration (`GoogleDriveBackupServiceTest`)
- **Mock Drive API**: Use `Drive` mock to verify `files().create()` is called with `parents = ["appDataFolder"]`.
- **Metadata verification**: Verify file name starts with `vault_backup_` and ends with `.vltbk`.
- **Resumable upload**: Mock a network failure mid-upload and verify the service can resume using the same upload URL.
- **Account switching**: Test that changing the selected Google account correctly updates the `driveService` credentials.

### WorkManager Scheduling (`BackupWorkerTest`)
- **Eligibility check**: Verify worker returns `Result.success()` without uploading if < 24h since last backup.
- **Constraint validation**: Verify worker only runs when `NetworkType.UNMETERED` (Wi-Fi) is present.
- **Foreground service**: Verify `setForeground()` is called at the start of `doWork()`.
- **Battery exemption**: Test behavior when battery saver is on vs off.

---

## 3. Security Tests

### Data Leakage Audit
- **Plaintext upload check**: Use a mock proxy or interceptor to verify that NO unencrypted vault data is sent in the Google Drive request body.
- **Credential storage check**: Verify that the recovery phrase is NOT stored in `SharedPreferences` or `EncryptedSharedPreferences`. It must be transient.
- **Key persistence check**: Verify that the backup key is only stored in Android Keystore, not in plaintext files.

### Zero-Knowledge Verification
- **Search visibility**: Attempt to list files in the user's root Drive folder using a general scope and verify App Data folder files are NOT returned.
- **Metadata hiding**: Verify that the Drive file name does not contain the user's name, vault name, or any other identifiable information.

---

## 4. Edge Cases & Error Handling

### Network Failures
- **Interrupted upload**: Disconnect Wi-Fi during a 10 MB chunk upload. Verify WorkManager retries correctly with exponential backoff.
- **Timeout**: Simulate a slow network (GPRS speed). Verify the app handles timeouts gracefully without crashing.

### Storage & Quota
- **Quota exceeded**: Mock a Google Drive `403 User Rate Limit Exceeded` or `Storage Quota Exceeded` error. Verify the app notifies the user through a system notification.

### Data Integrity
- **Corrupt backup file**: Manually corrupt a backup record in the App Data folder. Verify the restore process detects the corruption, stops, and warns the user instead of producing garbage data.
- **Incomplete chunks**: Upload only 2 of 3 required chunks. Verify the restore process fails integrity check.

### Device State
- **Low disk space**: Simulate low device storage during backup staging (before upload). Verify the app cleans up temporary files and stops.
- **App termination**: Force-stop the app during a backup. Verify that upon next launch, WorkManager resumes the task or schedules a new one.

---

## 5. UI/UX Tests

### Settings Toggle
- **Toggle ON**: Verify `backup_enabled` preference is set and a sync is scheduled.
- **Toggle OFF**: Verify `backup_enabled` is false and verify a call to delete remote backups is queued.

### Restore Flow
- **Valid phrase**: User enters 12 correct words. Verify transition to "Restoring" then "Success".
- **Invalid phrase**: User enters 12 incorrect words. Verify error message: "Incorrect phrase. Please check your recovery card."
- **Wrong account**: User signs in with a different Google account. Verify error: "No backup found for this account."

---

## 6. Duress Mode Tests
- **Duress wipe**: Trigger duress pattern. Verify local vault is deleted but the Google Drive App Data folder remains intact.
- **Recovery after duress**: Verify the vault can be restored on the same device using the recovery phrase + original Google account.
- **Device salt persistence**: Verify that after a duress wipe, the device salt is still available in Keystore for backup key derivation.