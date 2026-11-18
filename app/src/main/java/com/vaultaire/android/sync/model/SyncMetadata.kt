package com.vaultaire.android.sync.model

import kotlinx.serialization.Serializable

/**
 * Sync metadata for vault synchronization.
 */
@Serializable
data class SyncMetadata(
    val version: Int = 1,
    val vaultId: String,
    val deviceId: String,
    val vectorClock: Map<String, Long>,
    val fileIndex: Map<String, FileEntry>,
    val deletionMarkers: Set<DeletionMarker>,
    val pendingUploads: List<PendingChange>,
    val lastSyncedTimestamp: Long,
    val syncFormatVersion: Int = com.vaultaire.android.sync.engine.SyncFormat.CURRENT_VERSION,
    val isDuressIsolated: Boolean = false,
    val isDisconnected: Boolean = false
) {
    /**
     * Adds a file entry to the index.
     */
    fun addFile(file: FileEntry): SyncMetadata {
        return copy(
            fileIndex = fileIndex + (file.fileId to file),
            vectorClock = vectorClock + (deviceId to (vectorClock[deviceId] ?: 0L) + 1)
        )
    }
    
    /**
     * Deletes a file entry from the index.
     */
    fun deleteFile(fileId: String, deletedAt: Long): SyncMetadata {
        return copy(
            fileIndex = fileIndex - fileId,
            vectorClock = vectorClock + (deviceId to (vectorClock[deviceId] ?: 0L) + 1)
        )
    }
    
    /**
     * Removes acknowledged pending changes from this metadata.
     */
    fun removePendingChanges(acknowledgedIds: Set<String>): SyncMetadata {
        return copy(
            pendingUploads = pendingUploads.filterNot { it.changeId in acknowledgedIds }
        )
    }
}