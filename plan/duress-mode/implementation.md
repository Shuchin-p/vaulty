# Duress Mode — Technical Implementation

## Android-Specific Architecture
- **Internal Storage**: `context.filesDir/vaults/*.dat` (vault indexes), `context.filesDir/storage/*.dat` (encrypted arenas/pages).
- **DuressWipeProtocol**: Performs DoD 5220.22-M 4-pass overwrite + fsync on ALL `.dat` files belonging to non-decoy vaults.
- **AttemptThrottler**: Uses `EncryptedSharedPreferences` to persist attempt counters and lockout times to survive app process death.
- **Sync Isolation**: Must invoke `WorkManager.cancelAllWork()` or equivalent to stop Google Drive sync tasks immediately upon duress pattern detection.

## Component Specifications

### AttemptThrottler (Android Lifecycle)
- Persistent: `recordAttempt` saves to `EncryptedSharedPreferences`.
- Thread-safe: `ReentrantLock` for access.
- Lockout: `System.currentTimeMillis()` comparison ensures it works across app restarts.

### DuressWipeProtocol (Secure Overwrite)
- Target: `/data/user/0/com.vaultaire.android/files/`
- Operation: `RandomAccessFile("rwd")` + `FileChannel.force(true)` (API 29+) or `FileDescriptor.sync()` (API <29).
- PASS 1-3: `0x00`, `0xFF`, `0x00`.
- PASS 4: `SecureRandom`.

### Sync Engine Isolation
```kotlin
fun isolateFromSync() {
    // Stop all background sync tasks
    WorkManager.getInstance(context).cancelAllWorkByTag("sync_tag")
    // Mark local storage state as "wiped" so no deletion requests are pushed
    preferences.edit().putBoolean("sync_wiped", true).apply()
}
```
