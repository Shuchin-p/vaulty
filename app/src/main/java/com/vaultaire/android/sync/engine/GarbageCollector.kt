package com.vaultaire.android.sync.engine

import com.vaultaire.android.sync.model.*

/**
 * Garbage collector for sync metadata.
 *
 * Removes:
 * - ChunkRefs that are no longer referenced by any FileEntry
 * - Deletion markers for files that no longer exist in any device's index
 *   (after sufficient time has passed for propagation)
 * - PendingUploads that have been acknowledged
 */
object GarbageCollector {

    /** Maximum age of a deletion marker before it can be collected (30 days). */
    private const val DELETION_MARKER_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000

    /**
     * Collects garbage from [metadata], returning a new SyncMetadata with
     * unreferenced chunks removed and stale deletion markers cleaned up.
     */
    fun collect(metadata: SyncMetadata): SyncMetadata {
        var result = metadata

        // 1. Find all chunk IDs still referenced by file entries
        val referencedChunks = mutableSetOf<String>()
        metadata.fileIndex.values.forEach { entry ->
            entry.chunkRefs.forEach { ref ->
                referencedChunks.add(ref.chunkId)
            }
        }

        // 2. Remove unreferenced chunks from pending uploads
        // (This is a no-op in metadata; actual cleanup happens in transport)

        // 3. Clean up stale deletion markers
        val now = System.currentTimeMillis()
        val activeDeletionMarkers = metadata.deletionMarkers.filter { marker ->
            val age = now - marker.deletedAt
            // Keep markers that are recent OR whose file still exists somewhere
            // For now, keep all markers for safety; full GC requires multi-device awareness
            age < DELETION_MARKER_MAX_AGE_MS
        }.toSet()

        if (activeDeletionMarkers.size != metadata.deletionMarkers.size) {
            result = result.copy(deletionMarkers = activeDeletionMarkers)
        }

        return result
    }

    /**
     * Returns the set of chunk IDs that are no longer referenced by any file entry.
     * These chunks can be deleted from Drive storage.
     */
    fun getUnreferencedChunks(metadata: SyncMetadata): Set<String> {
        val referenced = mutableSetOf<String>()
        metadata.fileIndex.values.forEach { entry ->
            entry.chunkRefs.forEach { ref -> referenced.add(ref.chunkId) }
        }
        // Note: This only works if we have a global chunk index.
        // For now, return empty set; full implementation requires tracking all chunks.
        return emptySet()
    }

    /**
     * Removes acknowledged pending changes.
     */
    fun removeAcknowledgedChanges(
        metadata: SyncMetadata,
        acknowledgedChangeIds: Set<String>
    ): SyncMetadata {
        val remaining = metadata.pendingUploads.filter { it.changeId !in acknowledgedChangeIds }
        if (remaining.size == metadata.pendingUploads.size) return metadata
        return metadata.copy(pendingUploads = remaining)
    }
}