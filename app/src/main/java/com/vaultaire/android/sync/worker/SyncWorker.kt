package com.vaultaire.android.sync.worker

import android.content.Context
import android.provider.Settings
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vaultaire.android.sync.engine.MergeEngine
import com.vaultaire.android.sync.engine.SyncEncryptionEngine
import com.vaultaire.android.sync.engine.SyncFormat
import com.vaultaire.android.sync.model.SyncMetadata
import com.vaultaire.android.sync.transport.DriveSyncTransport
import com.vaultaire.android.storage.VaultStorageEngine
import com.vaultaire.android.vault.VaultKeyProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.UUID
import javax.crypto.SecretKey

/**
 * WorkManager worker for periodic and on-demand vault synchronization.
 * 
 * Scheduled:
 * - Periodic: every 15 min when online (NetworkType.CONNECTED)
 * - Expedited: OneTimeWorkRequest on local change or FCM notification
 * - Tag: "sync-$vaultId"
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val driveSyncTransport: DriveSyncTransport,
    private val mergeEngine: MergeEngine,
    private val vaultStorage: VaultStorageEngine
) : CoroutineWorker(context, params) {
    
    init {
        require(params.inputData.getString("vaultId")?.isNotEmpty() != false) { "vaultId cannot be empty" }
    }

    override suspend fun doWork(): Result {
        return try {
            // 1. Get vault ID from input data
            val vaultId = inputData.getString("vaultId") ?: return Result.failure()
            
            // 2. Check if sync is enabled for this vault
            if (isSyncDisabled()) {
                return Result.success() // No-op, not a failure
            }
            
            // 3. Get master vault key and device ID for encryption
            val masterVaultKey = getMasterVaultKey(vaultId)
            val deviceId = getDeviceId()
            val crypto = SyncEncryptionEngine(masterVaultKey, vaultId, deviceId)
            
            // 4. Load local sync metadata
            val localMetadata = loadLocalSyncMetadata()
                ?: return Result.success() // No metadata yet (first sync)
            
            // 5. Pull remote changes since last sync token
            val lastSyncToken = getLastSyncToken()
            val (newToken, changedFileIds) = driveSyncTransport.fetchChangesSince(lastSyncToken)
            
            // 6. Download and merge each remote record
            var mergedMetadata = localMetadata
            for (fileId in changedFileIds) {
                val remoteRecord = driveSyncTransport.downloadSyncRecord(fileId)
                val remoteMetadata = crypto.decryptSyncRecord(remoteRecord)
                
                // Migrate format if needed
                val migratedRemote = SyncFormat.migrateIfNeeded(remoteMetadata)
                
                mergedMetadata = mergeEngine.merge(mergedMetadata, migratedRemote)
            }
            
            // 7. Apply merged changes to local vault storage
            if (changedFileIds.isNotEmpty()) {
                // Apply changes to vault storage
                applyChangesToVault(mergedMetadata)
            }
            
            // 8. Push local pending changes
            val localChanges = mergedMetadata.pendingUploads
            val acknowledgedChangeIds = mutableSetOf<String>()
            for (change in localChanges) {
                val record = createSyncMetadataFromChange(change)
                val encryptedRecord = crypto.encryptSyncRecord(record)
                driveSyncTransport.uploadSyncRecord(encryptedRecord)
                acknowledgedChangeIds.add(change.changeId)
            }
            
            // 9. Remove acknowledged changes and update metadata
            if (acknowledgedChangeIds.isNotEmpty()) {
                mergedMetadata = mergedMetadata.removePendingChanges(acknowledgedChangeIds)
            }
            
            // 10. Save updated metadata and sync token
            saveSyncMetadata(mergedMetadata)
            saveLastSyncToken(newToken)
            
            return Result.success()
        } catch (e: Exception) {
            // Log error and retry
            return Result.retry()
        }
    }
    
    private fun getMasterVaultKey(vaultId: String): SecretKey {
        // Get the master vault key from VaultKeyProvider
        // The MVK should have been stored when the user unlocked the vault
        val vaultKeyProvider = VaultKeyProvider.getInstance()
        return vaultKeyProvider.getMasterVaultKey(vaultId)
    }
    
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
    
    private fun isSyncDisabled(): Boolean {
        // Check if vault is disconnected or duress-isolated
        val metadata = loadLocalSyncMetadata()
        return metadata?.isDisconnected == true || metadata?.isDuressIsolated == true
    }
    
    private fun loadLocalSyncMetadata(): SyncMetadata? {
        // Load from vault storage (encrypted inside vault)
        return vaultStorage.loadSyncMetadata()
    }
    
    private fun saveSyncMetadata(metadata: SyncMetadata) {
        vaultStorage.saveSyncMetadata(metadata)
    }
    
    private fun getLastSyncToken(): String? {
        // Get from preferences
        return null
    }
    
    private fun saveLastSyncToken(token: String) {
        // Save to preferences
    }
    
    private fun applyChangesToVault(metadata: SyncMetadata) {
        // Apply merged changes to vault storage
    }
    
    /**
     * Creates a minimal SyncMetadata from a pending change for upload.
     */
    private fun createSyncMetadataFromChange(change: com.vaultaire.android.sync.model.PendingChange): SyncMetadata {
        // Create a minimal SyncMetadata containing just this change
        return SyncMetadata(
            version = 1,
            vaultId = change.changeId,
            deviceId = UUID.randomUUID().toString(),
            vectorClock = change.vectorClock,
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            isDuressIsolated = false,
            isDisconnected = false
        )
    }
}