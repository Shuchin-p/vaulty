# Device Sync — Technical Implementation

**Perspective**: Tech Lead / Software Architect  
**Feature**: Cross-Device Vault Synchronization  
**Entitlement**: `CROSS_DEVICE_SYNC` (Pro)

---

## Architecture Overview

```mermaid
graph TB
    subgraph DeviceA [Device A - Phone]
        VA[Vault Manager] --> MVK_A[Master Vault Key]
        VA --> SyncEngineA[Sync Engine]
        SyncEngineA --> CryptoA[Encryption Layer]
        CryptoA --> EncryptedVaultA[(Encrypted Vault\n+ Sync Metadata)]
        SyncEngineA --> UploaderA[Google Drive Uploader]
    end

    subgraph Cloud [Google Drive - Private App Folder]
        Drive[(Encrypted Records\nRandom UUID names\nPadded 10MB chunks)]
        Drive --> ChangeLog[Change Notifications\nPush / Polling]
    end

    subgraph DeviceB [Device B - Tablet]
        VB[Vault Manager] --> MVK_B[Master Vault Key]
        VB --> SyncEngineB[Sync Engine]
        SyncEngineB --> CryptoB[Encryption Layer]
        CryptoB --> EncryptedVaultB[(Encrypted Vault\n+ Sync Metadata)]
        SyncEngineB --> DownloaderB[Google Drive Downloader]
    end

    MVK_A == "Same key via\nrecovery phrase\nor enrollment" == MVK_B
    UploaderA --> Drive
    DownloaderB <-- ChangeLog
    DownloaderB --> Drive
    SyncEngineA -.-> MergeEngine[Merge Engine\nOrder-Independent\nAdd-Wins\nDedup]
    SyncEngineB -.-> MergeEngine
```

### Data Flow: Write Path (Device A adds photo)

```
User adds photo
       │
       ▼
VaultStorageEngine.createEncryptedOutputStream() ──▶ Encrypted chunk (64KB, AES-GCM)
       │
       ▼
SyncEngine.stageChange(ChangeType.ADD, fileId, chunkRefs, metadata)
       │
       ▼
SyncMetadata.updateVectorClock(deviceId, fileId, version)
       │
       ▼
SyncMetadata.serialize() ──▶ Encrypted with MVK ──▶ Padded to 10MB
       │
       ▼
GoogleDriveUploader.upload(recordName: UUID, payload: ByteArray)
       │
       ▼
Google Drive (Private App Folder) ──▶ Change notification to Device B
```

### Data Flow: Read/Merge Path (Device B receives)

```
Change notification received (WorkManager / FCM / Polling)
       │
       ▼
GoogleDriveDownloader.download(recordName)
       │
       ▼
Decrypt with MVK (AES-GCM verify)
       │
       ▼
SyncMetadata.deserialize() ──▶ Vector clocks, file membership, deletion markers
       │
       ▼
MergeEngine.merge(localMetadata, remoteMetadata)
       │
       ├── Adds win over unseen deletes
       ├── Identical content (same chunk hashes) → collapse
       └── Order-independent result
       │
       ▼
VaultStorageEngine.applyMergedChanges()
       │
       ▼
Local vault updated ──▶ UI refresh
```

---

## Transport: Encrypted Google Drive Backup as Sync Mechanism

### Why Google Drive (App Folder)?
- **Same zero-knowledge model** as iOS iCloud backup
- **Private App Folder** (`https://www.googleapis.com/auth/drive.appdata`) — not user-visible, not in "My Drive"
- **Per-app quota** — doesn't count against user's visible Drive storage (but does count against total)
- **Change notifications** — Push via Google Drive API v3 `changes.watch` + FCM
- **Android-native** — Google Play Services integration, no third-party deps

### Record Format on Drive

```
Record Name: UUID v4 (random, no correlation to vault/file)
Record Type: application/vnd.vaultaire.sync.record.v1
Size: Padded to 10 MB bucket (same as backup)
Content: AES-256-GCM encrypted payload
  │
  ├── Header (authenticated via AAD)
  │   ├── Version: u8
  │   ├── Record Type: u8 (SYNC_METADATA / FILE_CHUNK / RECOVERY_ENVELOPE)
  │   ├── Vault ID: UUID (encrypted inside payload, not in metadata)
  │   └── Device ID: UUID (local device identifier, encrypted)
  │
  ├── Payload (encrypted)
  │   ├── SyncMetadata (vector clocks, file index, deletion markers)
  │   └── File Chunks (content-addressed by SHA-256 of plaintext)
  │
  └── GCM Tag (16 bytes)
```

### Drive API Usage

| Operation | API Call | Frequency |
|-----------|----------|-----------|
| Upload new record | `files.create` (media upload, resumable) | Per change batch |
| Download record | `files.get` (alt=media) | On change notification |
| List changes | `changes.list` (pageToken) | Startup + periodic |
| Watch for changes | `changes.watch` (webhook → FCM) | Continuous |
| Delete obsolete | `files.delete` | Cleanup cycle |
| Get storage quota | `about.get` (fields=storageQuota) | Periodic |

### Push Notifications (FCM + Drive Changes Watch)

```kotlin
// Register watch channel on app start / vault unlock
val channel = Channel.newBuilder()
    .setId("vaultaire-sync-${vaultId}")
    .setType("web_hook")
    .setAddress("https://fcm.googleapis.com/v1/projects/${projectId}/messages:send")
    .setToken("device-fcm-token")
    .build()

driveService.changes().watch(channel).execute()
```

---

## Sync Bookkeeping: ALL Metadata INSIDE Encrypted Vault

### What Lives in Sync Metadata (Encrypted)

```kotlin
@Serializable
data class SyncMetadata(
    val version: Int = 1,
    val vaultId: UUID,                           // Encrypted - not in Drive metadata
    val deviceId: UUID,                          // This device's stable ID
    val vectorClock: Map<UUID, Long>,            // deviceId → logical clock
    val fileIndex: Map<FileId, FileEntry>,       // All files known to this device
    val deletionMarkers: Set<DeletionMarker>,    // Tombstones with vector clock
    val pendingUploads: List<PendingUpload>,     // Local changes not yet acknowledged
    val lastSyncedTimestamp: Long,               // Wall clock for debugging
    val syncFormatVersion: Int                   // For migration
)

@Serializable
data class FileEntry(
    val fileId: UUID,
    val contentHash: ByteArray,      // SHA-256 of plaintext (for dedup)
    val chunkRefs: List<ChunkRef>,   // Encrypted chunk references
    val size: Long,
    val mimeType: String,
    val addedBy: UUID,               // Device that first added
    val addedAt: Long,               // Logical timestamp
    val version: Long                // Vector clock entry for this file
)

@Serializable
data class ChunkRef(
    val chunkId: UUID,               // Random, not content-addressed in cloud
    val driveRecordName: String,     // UUID on Drive
    val offset: Long,
    val length: Long,
    val encryptedHash: ByteArray     // SHA-256 of ciphertext (verify download)
)

@Serializable
data class DeletionMarker(
    val fileId: UUID,
    val deletedBy: UUID,
    val deletedAt: Long,
    val vectorClockSnapshot: Map<UUID, Long>  // What deleter had seen
)
```

### Vector Clock Design

- **Per-device logical clock** — increments on each local change
- **No wall-clock dependency** — works offline, no NTP needed
- **Merge comparison**: `vc1 dominates vc2` iff ∀d: vc1[d] ≥ vc2[d] ∧ ∃d: vc1[d] > vc2[d]
- **Concurrent** if neither dominates → merge rules apply

### Why Inside Encrypted Vault?
- Google sees: random UUID record names, padded 10MB sizes, timing
- Google does NOT see: vault IDs, file IDs, file counts, device IDs, membership, which vault a record belongs to
- Recovery phrase unlocks everything — same key decrypts vault + sync metadata

---

## Conflict Resolution: Merge Engine

### Merge Algorithm (Order-Independent, Add-Wins)

```kotlin
class MergeEngine {
    fun merge(local: SyncMetadata, remote: SyncMetadata): SyncMetadata {
        val merged = local.copy()
        
        // 1. Merge vector clocks (max per device)
        merged.vectorClock = mergeVectorClocks(local.vectorClock, remote.vectorClock)
        
        // 2. Process file entries from remote
        remote.fileIndex.forEach { (fileId, remoteEntry) ->
            val localEntry = local.fileIndex[fileId]
            
            when {
                localEntry == null -> {
                    // New file from remote - ADD
                    merged.fileIndex[fileId] = remoteEntry
                }
                remoteEntry.contentHash.contentEquals(localEntry.contentHash) -> {
                    // Identical content - COLLAPSE (dedup)
                    merged.fileIndex[fileId] = mergeIdentical(localEntry, remoteEntry)
                }
                isConcurrent(localEntry, remoteEntry) -> {
                    // Concurrent modification - ADD WINS (photo vault semantics)
                    // Keep both versions? No - same fileId means same logical file
                    // For photo vault: newer version wins, but preserve if both added
                    merged.fileIndex[fileId] = resolveConcurrentModification(localEntry, remoteEntry)
                }
                remoteEntry.version > localEntry.version -> {
                    // Remote is causally newer - ACCEPT
                    merged.fileIndex[fileId] = remoteEntry
                }
            }
        }
        
        // 3. Process deletion markers
        remote.deletionMarkers.forEach { marker ->
            val localEntry = merged.fileIndex[marker.fileId]
            if (localEntry != null && marker.dominates(localEntry.version)) {
                // Deleter had seen this version - APPLY DELETE
                merged.fileIndex.remove(marker.fileId)
                merged.deletionMarkers.add(marker)
            } else {
                // Deleter had NOT seen this version - ADD WINS, ignore delete
                // File stays (or gets re-added by other device)
            }
        }
        
        // 4. Garbage collect obsolete chunks (identical content dedup)
        merged.garbageCollectChunks()
        
        return merged
    }
    
    private fun isConcurrent(a: FileEntry, b: FileEntry): Boolean {
        val vcA = mapOf(a.addedBy to a.version)
        val vcB = mapOf(b.addedBy to b.version)
        return !vcA.dominates(vcB) && !vcB.dominates(vcA)
    }
}
```

### Key Properties (Verified by Tests)

| Property | Test |
|----------|------|
| **Commutative** | `merge(A, B) == merge(B, A)` |
| **Associative** | `merge(merge(A, B), C) == merge(A, merge(B, C))` |
| **Idempotent** | `merge(A, A) == A` |
| **Add-wins** | Concurrent add + delete of unseen file → add preserved |
| **Dedup** | Same content added twice → single entry |

---

## Encryption Model

### Key Hierarchy (Same as Backup)

```
User Pattern (drawing)
       │
       ▼
Device Salt (per-device, never leaves device)
       │
       ▼
PBKDF2-HMAC-SHA512 (600,000 iters)
       │
       ▼
Vault Derivation Key (VDK) ──────▶ Wraps MVK in local vault header
       │
       ▼
Master Vault Key (MVK) ─────────▶ Encrypts/Decrypts:
                                  ├── Vault file chunks (storage)
                                  ├── Backup records (backup)
                                  └── Sync records (SYNC) ◄── NEW
       │
       ▼
Recovery Phrase (12 words)
       │
       ▼
PBKDF2-HMAC-SHA512 (300,000 iters) + Device Salt
       │
       ▼
Recovery Vault Key (RVK) ───────▶ Wraps MVK in recovery envelope
```

### Sync Record Encryption

```kotlin
class SyncEncryptionEngine(private val masterVaultKey: SecretKey) {
    
    // Same AES-256-GCM as backup/storage
    private const val ALGO = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 16
    
    fun encryptSyncRecord(metadata: SyncMetadata): EncryptedSyncRecord {
        val plaintext = Json.encodeToByteArray(metadata)
        val iv = SecureRandom().nextBytes(IV_SIZE)
        val cipher = Cipher.getInstance(ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, masterVaultKey, GCMParameterSpec(128, iv))
        
        // AAD binds record to vault + device (prevents record swap attacks)
        val aad = buildAAD()
        cipher.updateAAD(aad)
        
        val ciphertext = cipher.doFinal(plaintext)
        val padded = padToBucket(ciphertext)  // 10 MB buckets
        
        return EncryptedSyncRecord(
            iv = iv,
            ciphertext = padded,
            tag = extractTag(ciphertext),  // Last 16 bytes
            aad = aad
        )
    }
    
    fun decryptSyncRecord(record: EncryptedSyncRecord): SyncMetadata {
        val cipher = Cipher.getInstance(ALGO)
        cipher.init(Cipher.DECRYPT_MODE, masterVaultKey, GCMParameterSpec(128, record.iv))
        cipher.updateAAD(record.aad)
        
        val plaintext = cipher.doFinal(record.ciphertext)
        return Json.decodeFromByteArray(plaintext)
    }
    
    private fun buildAAD(): ByteArray {
        // Binds record to: vault ID + device ID + format version
        // Prevents: record replay across vaults, device impersonation
        return ByteArrayOutputStream().apply {
            write(vaultId.toByteArray())
            write(deviceId.toByteArray())
            writeInt(SYNC_FORMAT_VERSION)
        }.toByteArray()
    }
}
```

---

## Key Management

### Same Vault Keys on All Devices

| Scenario | How MVK Reaches Device |
|----------|------------------------|
| **Original device** | Generated locally, wrapped by VDK (pattern + device salt) |
| **New device (recovery phrase)** | Recovery phrase + device salt → RVK → unwrap recovery envelope → MVK |
| **New device (Move to new device)** | Enrollment key transferred via secure QR/nearby (out of scope for v1) |

### Critical: Enrollment Key Never Leaves Device

- **V3 Pattern Restore**: Only works on device that made backup + holds enrollment key
- **Recovery Phrase**: Works on ANY device with backup records
- **Sync**: Requires MVK → obtained via recovery phrase on new devices

### Device Identity

```kotlin
data class DeviceIdentity(
    val deviceId: UUID,              // Stable, generated on first vault create
    val deviceName: String,          // Local only (e.g., "Pixel 9 Pro")
    val enrollmentKey: ByteArray,    // 32 bytes, NEVER synced, NEVER in cloud
    val createdAt: Long,
    val lastSyncAt: Long
)
```

- Stored in `EncryptedSharedPreferences` (AndroidX Security)
- Used to sign sync metadata (vector clock ownership)
- Duress wipe → destroys enrollment key → device can never rejoin sync

---

## Offline Support

### Local Change Queue

```kotlin
@Serializable
data class PendingChange(
    val changeId: UUID,
    val type: ChangeType,            // ADD, DELETE, METADATA_UPDATE
    val fileId: UUID?,
    val chunkRefs: List<ChunkRef>?,  // For ADD
    val vectorClock: Map<UUID, Long>, // Clock at time of change
    val timestamp: Long,
    val retryCount: Int = 0
)

enum class ChangeType { ADD, DELETE, METADATA_UPDATE }
```

### Sync Scheduler (WorkManager)

```kotlin
// Periodic sync: every 15 min when online, exponential backoff when offline
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build())
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
    .build()

// Immediate sync: triggered by local change
val immediateRequest = OneTimeWorkRequestBuilder<SyncWorker>()
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build())
    .setExpedited(true)  // Android 12+ expedited jobs
    .build()
```

### SyncWorker Logic

```kotlin
class SyncWorker @AssistedInject constructor(
    @Assisted vaultId: String,
    private val driveSyncTransport: DriveSyncTransport,
    private val mergeEngine: MergeEngine,
    private val vaultStorage: VaultStorageEngine,
    private val crypto: SyncEncryptionEngine
) : CoroutineWorker() {

    override suspend fun doWork(): Result {
        return try {
            // 1. Pull remote changes since last sync token
            val remoteRecords = driveSyncTransport.fetchChangesSince(lastSyncToken)
            
            // 2. Decrypt and merge each
            var mergedMetadata = loadLocalSyncMetadata()
            for (record in remoteRecords) {
                val remoteMetadata = crypto.decryptSyncRecord(record)
                mergedMetadata = mergeEngine.merge(mergedMetadata, remoteMetadata)
            }
            
            // 3. Apply merged changes to local vault storage
            vaultStorage.applySyncChanges(mergedMetadata)
            
            // 4. Push local pending changes
            val localChanges = getPendingChanges()
            for (change in localChanges) {
                val record = crypto.encryptSyncRecord(change.toSyncRecord())
                driveSyncTransport.upload(record)
                markChangeAcknowledged(change.changeId)
            }
            
            // 5. Update sync token, save metadata
            saveSyncMetadata(mergedMetadata)
            saveLastSyncToken(driveSyncTransport.getLatestChangeToken())
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
```

---

## Platform Adaptations: Android Specifics

### Google Drive API Integration

```kotlin
// Dependencies to add (app/build.gradle.kts)
implementation("com.google.android.gms:play-services-auth:21.0.0")
implementation("com.google.api-client:google-api-client-android:2.3.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20240000-2.0.0")
implementation("com.google.http-client:google-http-client-android:1.43.0")

// Auth: Google Sign-In for Drive scope
val signInOptions = GoogleSignInOptionsExtension.Builder()
    .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
    .requestEmail()
    .build()
```

### Battery Optimization Exemption

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Runtime request (Settings → Apps → Vaultaire → Battery → Unrestricted) -->
val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
intent.data = Uri.parse("package:$packageName")
startActivity(intent)
```

### Foreground Service for Large Uploads

```kotlin
// For vaults > 100 MB, use foreground service with progress notification
class SyncForegroundService : Service() {
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notification = buildProgressNotification("Syncing vault…")
        startForeground(SYNC_NOTIFICATION_ID, notification)
        // Run sync in background thread
        return START_STICKY
    }
}
```

### Storage Access Framework (Fallback)

If Drive API unavailable, fallback to Storage Access Framework:
- User picks "Vaultaire Sync" folder once
- App gets persistent URI permission
- Less efficient (no push notifications, polling only)

---

## Integration Points

### 1. Encrypted Cloud Backup (Transport Reuse)

```kotlin
// Sync uses SAME backup infrastructure
class DriveSyncTransport(private val backupEngine: ZeroKnowledgeBackupEngine) {
    
    // Reuse backup's chunking, padding, encryption
    fun uploadSyncRecord(record: EncryptedSyncRecord): String {
        // Upload to Drive App Folder with same padded 10MB format
        return driveUploader.upload(
            fileName = record.recordName,  // UUID
            content = record.ciphertext,
            mimeType = "application/vnd.vaultaire.sync.record.v1"
        )
    }
    
    // Backup runs every 24h; Sync runs every 15m + on change
    // They share: encryption, chunking, Drive auth, quota handling
}
```

| Shared | Backup | Sync |
|--------|--------|------|
| Encryption | AES-256-GCM + padding | ✅ Same |
| Chunk size | 64 KB → 10 MB padded | ✅ Same |
| Drive folder | App Folder | ✅ Same |
| Auth | Google Sign-In | ✅ Same |
| Quota handling | Yes | ✅ Same |
| Recovery envelope | Yes | ✅ Same |
| Schedule | 24h / Wi-Fi | 15m / any network + on-change |

### 2. Duress Mode Isolation

```kotlin
class DuressModeManager {
    fun executeDuressWipe(vaultId: String) {
        // 1. Destroy local vault files
        vaultManager.purgeNonDecoyVaultFiles()
        
        // 2. Destroy enrollment key (device can NEVER rejoin sync)
        deviceSaltManager.destroyEnrollmentKey()
        
        // 3. Destroy recovery data
        recoveryPhraseEngine.clearRecoveryData()
        
        // 4. Mark sync metadata as "duress-isolated" (local only)
        syncMetadata.markDuressIsolated()
        saveSyncMetadata(syncMetadata)
        
        // 5. Stop all sync workers
        workManager.cancelAllWorkByTag("sync-$vaultId")
        
        // 6. Cloud records REMAIN (peers unaffected)
        // 7. Device will NOT auto-rejoin on next unlock
    }
}
```

**Guarantee**: Duress wipe on Device B → Device A and C continue syncing normally. Device B's deletion markers never uploaded (enrollment key destroyed → can't encrypt new sync records).

### 3. Disconnect This Device (Local Only)

```kotlin
fun disconnectThisDevice(vaultId: String) {
    // 1. Cancel sync workers
    workManager.cancelAllWorkByTag("sync-$vaultId")
    
    // 2. Mark metadata as disconnected (local flag)
    syncMetadata.setDisconnected(true)
    saveSyncMetadata(syncMetadata)
    
    // 3. Keep local vault intact
    // 4. Cloud records untouched
    // 5. Peers not notified
    
    // Reconnect: clear flag, reschedule workers, full re-sync
}
```

---

## Performance: Incremental Sync

### Change Detection

```kotlin
// VaultStorageEngine already provides random access to encrypted chunks
// Sync metadata tracks: fileId → (chunkRefs, contentHash, version)

// On local change:
fun onFileAdded(fileId: UUID, content: ByteArray) {
    // 1. Encrypt to chunks (storage engine)
    val chunkRefs = storageEngine.encryptAndStore(fileId, content)
    
    // 2. Compute content hash (SHA-256 of plaintext)
    val contentHash = SHA256(content)
    
    // 3. Check dedup: any existing file with same hash?
    val existing = syncMetadata.fileIndex.values.find { it.contentHash == contentHash }
    if (existing != null) {
        // Dedup! Reuse existing chunk refs
        syncMetadata.fileIndex[fileId] = FileEntry(
            fileId = fileId,
            contentHash = contentHash,
            chunkRefs = existing.chunkRefs,  // REUSE
            ...
        )
    } else {
        // New content: upload new chunks
        syncMetadata.fileIndex[fileId] = FileEntry(
            fileId = fileId,
            contentHash = contentHash,
            chunkRefs = chunkRefs,
            ...
        )
        queueChunksForUpload(chunkRefs)
    }
    
    // 4. Increment vector clock
    syncMetadata.vectorClock[deviceId] = syncMetadata.vectorClock[deviceId]!! + 1
    syncMetadata.fileIndex[fileId]!!.version = syncMetadata.vectorClock[deviceId]!!
}
```

### Bandwidth Optimization

| Technique | Savings |
|-----------|---------|
| **Content-addressed dedup** | Same photo on 2 devices → 1 upload |
| **Chunk reuse** | Modified metadata only → no file re-upload |
| **10 MB padding** | Fixed bucket sizes hide file size |
| **Delta metadata** | Only changed vector clock entries transmitted |
| **Compression** | Sync metadata JSON compresses ~80% (gzip) |

### Sync Cycle Metrics (Targets)

| Metric | Target |
|--------|--------|
| Time to first byte (local change → Drive) | < 2 s |
| Time to merge (remote change → local UI) | < 500 ms |
| Metadata size per 1000 files | < 200 KB |
| Bandwidth per photo (5 MB) | ~5.2 MB (padding + overhead) |
| Battery per sync cycle | < 0.5% |
| Background sync frequency | 15 min (configurable) |

---

## Data Models (New Files)

### Package: `com.vaultaire.android.sync`

```
sync/
├── model/
│   ├── SyncMetadata.kt           # Core sync state (serializable)
│   ├── FileEntry.kt              # File index entry
│   ├── ChunkRef.kt               # Encrypted chunk reference
│   ├── DeletionMarker.kt         # Tombstone with vector clock
│   ├── PendingChange.kt          # Local change queue
│   ├── DeviceIdentity.kt         # Local device ID + enrollment key
│   └── VectorClock.kt            # Vector clock operations
├── engine/
│   ├── MergeEngine.kt            # Order-independent merge logic
│   ├── SyncEncryptionEngine.kt   # AES-GCM for sync records
│   ├── ConflictResolver.kt       # Add-wins, dedup logic
│   └── GarbageCollector.kt       # Chunk dedup + obsolete cleanup
├── transport/
│   ├── DriveSyncTransport.kt     # Google Drive API wrapper
│   ├── DriveAuthManager.kt       # Google Sign-In + token refresh
│   ├── ChangeWatcher.kt          # changes.watch + FCM
│   └── ResumableUploader.kt      # Large file upload with retry
├── worker/
│   ├── SyncWorker.kt             # WorkManager periodic + expedited
│   ├── SyncScheduler.kt          # Scheduling policy
│   └── NetworkMonitor.kt         # Connectivity + metered detection
├── ui/
│   ├── SyncStatusViewModel.kt    # StateFlow for sync status
│   ├── SyncSettingsScreen.kt     # Compose UI for sync settings
│   └── DeviceCountIndicator.kt   # "This device + N others"
└── SyncModule.kt                 # Hilt/Koin module for DI
```

---

## Migration & Versioning

### Sync Format Versioning

```kotlin
object SyncFormat {
    const val CURRENT_VERSION = 1
    const val MIN_COMPATIBLE_VERSION = 1
    
    fun migrateIfNeeded(metadata: SyncMetadata): SyncMetadata {
        return when (metadata.syncFormatVersion) {
            1 -> metadata
            // Future: add migration functions
            else -> throw IncompatibleSyncFormatException()
        }
    }
}
```

### App Version Compatibility

- **Same major version**: Full sync compatibility
- **Minor version diff**: Backward compatible (older app ignores unknown fields)
- **Major version diff**: Block sync until all devices update (per website: "Versions without this check can keep separate local and old iCloud copies")

---

## Security Considerations

### Threat Model

| Threat | Mitigation |
|--------|------------|
| Google reads vault content | Client-side AES-256-GCM, keys never leave device |
| Google swaps sync records between vaults | AAD binds record to vaultId + deviceId |
| Google replays old sync record | Vector clocks + monotonic clocks |
| Malicious app impersonates device | Enrollment key never in cloud; deviceId in AAD |
| Network attacker modifies records | AES-GCM authentication tag verified on decrypt |
| Duress wipe propagates | Enrollment key destroyed → cannot sign new records |
| Lost device → attacker syncs | Recovery phrase required; enrollment key on lost device only |
| Sync metadata leak via record sizes | 10 MB padded buckets (same as backup) |

### Audit Checklist

- [ ] All sync records encrypted with MVK (AES-256-GCM)
- [ ] AAD includes vaultId + deviceId + format version
- [ ] Vector clocks prevent replay/reordering
- [ ] Enrollment key never written to Drive
- [ ] Recovery phrase derives RVK → unwraps MVK (same as backup)
- [ ] Duress destroys enrollment key + stops sync workers
- [ ] Device count UI shows only count, no identifiers
- [ ] No plaintext file names, vault names, device names in Drive metadata
- [ ] Padding to 10 MB buckets for all records
- [ ] GCM tag verified on every decrypt

---

## Dependencies to Add

```kotlin
// app/build.gradle.kts additions

// Google Drive API
implementation("com.google.android.gms:play-services-auth:21.0.0")
implementation("com.google.api-client:google-api-client-android:2.3.0")
implementation("com.google.apis:google-api-services-drive:v3-rev20241111-2.0.0")
implementation("com.google.http-client:google-http-client-android:1.43.0")
implementation("com.google.http-client:google-http-client-gson:1.43.0")

// FCM for push notifications
implementation("com.google.firebase:firebase-messaging-ktx:24.1.0")

// WorkManager (already in deps via androidx.lifecycle.runtime.ktx? verify)
// implementation("androidx.work:work-runtime-ktx:2.9.0")

// Kotlinx Serialization (for sync metadata)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
ksp("org.jetbrains.kotlinx:kotlinx-serialization:1.6.3")

// OkHttp for resumable uploads (if not using Google HTTP client)
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Kotlinx Coroutines (already in deps)
```

---

## Implementation Phases

### Phase 1: Foundation (Week 1-2)
- [ ] Sync data models (`model/` package)
- [ ] Vector clock implementation + tests
- [ ] Merge engine + property-based tests (commutative, associative, idempotent)
- [ ] Sync encryption engine (reuse backup crypto)

### Phase 2: Transport (Week 2-3)
- [ ] Google Drive auth (Sign-In + DriveScopes.DRIVE_APPDATA)
- [ ] DriveSyncTransport: upload, download, list changes
- [ ] Change watcher (changes.watch + FCM)
- [ ] Resumable upload for large vaults

### Phase 3: Sync Engine (Week 3-4)
- [ ] SyncWorker (WorkManager periodic + expedited)
- [ ] Local change queue + pending uploads
- [ ] Merge integration with VaultStorageEngine
- [ ] Garbage collection for deduped chunks

### Phase 4: Duress & Disconnect (Week 4)
- [ ] Duress isolation integration
- [ ] Disconnect This Device logic
- [ ] Sync metadata persistence (encrypted, inside vault)

### Phase 5: UI & Polish (Week 5)
- [ ] Sync settings screen (Compose)
- [ ] Sync status indicators
- [ ] Device count display
- [ ] Error states + recovery flows
- [ ] Pro entitlement gating

### Phase 6: Testing & Hardening (Week 6)
- [ ] Unit tests (merge, vector clock, encryption)
- [ ] Integration tests (mock Drive transport)
- [ ] Security tests (no plaintext in cloud)
- [ ] Edge case tests (offline, concurrent, quota, duress)
- [ ] Performance benchmarks
- [ ] Battery profiling

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Drive API quota limits | Medium | High | Exponential backoff, batch uploads, user-facing quota warning |
| Android background execution limits | High | Medium | WorkManager + expedited jobs + foreground service for large syncs |
| FCM push unreliable in China/restricted | Medium | Medium | Fallback to polling (changes.list every 15 min) |
| Merge bugs causing data loss | Low | Critical | Property-based testing, formal verification of merge properties |
| Recovery phrase UX confusion | Medium | High | Clear distinction: "Pattern = this device only, Phrase = any device" |
| Drive App Folder not visible to user | Low | Medium | Document: "Sync uses hidden app folder, not your Drive files" |
| Large vault initial sync timeout | Medium | Medium | Chunked resumable upload, progress notification, foreground service |