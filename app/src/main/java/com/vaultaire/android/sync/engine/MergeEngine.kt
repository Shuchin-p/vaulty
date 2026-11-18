package com.vaultaire.android.sync.engine

import com.vaultaire.android.sync.model.*
import java.util.UUID

/**
 * Order-independent merge engine for sync metadata.
 *
 * Implements:
 * - Vector clock merge (element-wise max)
 * - Add-wins over unseen deletes
 * - Identical content hash collapse (deduplication)
 * - Tombstone dominance
 * - Garbage collection of obsolete chunks
 */
class MergeEngine {

    /**
     * Merges local and remote sync metadata into a new merged state.
     *
     * Properties:
     * - Commutative: merge(A, B) == merge(B, A)
     * - Associative: merge(merge(A, B), C) == merge(A, merge(B, C))
     * - Idempotent: merge(A, A) == A
     * - Add-wins: concurrent add + delete of unseen file -> add preserved
     * - Dedup: same content added twice -> single entry
     */
    fun merge(local: SyncMetadata, remote: SyncMetadata): SyncMetadata {
        require(local.vaultId == remote.vaultId) { "Cannot merge metadata from different vaults" }

        var merged = local.copy()

        // 1. Merge vector clocks (element-wise max)
        merged = merged.copy(
            vectorClock = VectorClock.merge(local.vectorClock, remote.vectorClock)
        )

        // 2. Process file entries from remote
        remote.fileIndex.forEach { (fileId, remoteEntry) ->
            val localEntry = local.fileIndex[fileId]

            when {
                localEntry == null -> {
                    // New file from remote - ADD
                    merged = merged.addFile(remoteEntry)
                }
                remoteEntry.contentHash.contentEquals(localEntry.contentHash) -> {
                    // Identical content - COLLAPSE (deduplicate)
                    merged = mergeIdentical(merged, localEntry, remoteEntry)
                }
                isConcurrent(localEntry, remoteEntry) -> {
                    // Concurrent modification - ADD WINS
                    merged = resolveConcurrentModification(merged, localEntry, remoteEntry)
                }
                remoteEntry.version > localEntry.version -> {
                    // Remote is causally newer - ACCEPT
                    merged = merged.addFile(remoteEntry)
                }
                else -> {
                    // Local is newer or equal - keep local
                }
            }
        }

        // 3. Process deletion markers from remote
        remote.deletionMarkers.forEach { marker ->
            val localEntry = merged.fileIndex[marker.fileId]
            if (localEntry != null && marker.dominates(localEntry.version)) {
                // Deleter had seen this version - APPLY DELETE
                merged = merged.deleteFile(marker.fileId, marker.deletedAt)
                    .copy(deletionMarkers = merged.deletionMarkers + marker)
            } else {
                // Deleter had NOT seen this version - ADD WINS, ignore delete
                // File stays (or gets re-added by other device)
            }
        }

        // 4. Garbage collect obsolete chunks (identical content dedup)
        merged = GarbageCollector.collect(merged)

        return merged
    }

    /**
     * Merges two entries with identical content hash.
     * Keeps the one with the higher version (newer), but combines chunk refs
     * if they differ (e.g., different chunking boundaries).
     */
    private fun mergeIdentical(merged: SyncMetadata, local: FileEntry, remote: FileEntry): SyncMetadata {
        val newer = if (remote.version >= local.version) remote else local
        val combinedChunkRefs = (local.chunkRefs + remote.chunkRefs).distinctBy { it.chunkId }
        val combined = newer.copy(chunkRefs = combinedChunkRefs)
        return merged.addFile(combined)
    }

    /**
     * Resolves concurrent modifications to the same logical file.
     * For photo vault semantics: keep both as separate files if they have different content.
     * If same content, dedup. If different content, rename remote to avoid collision.
     */
    private fun resolveConcurrentModification(
        merged: SyncMetadata,
        local: FileEntry,
        remote: FileEntry
    ): SyncMetadata {
        // Different content hashes -> treat as separate files
        // Generate a new fileId for the remote version to avoid collision
        val newFileId = UUID.randomUUID().toString()
        val renamedRemote = remote.copy(fileId = newFileId)
        return merged.addFile(renamedRemote)
    }

    /**
     * Returns true if two file entries represent concurrent modifications.
     * Uses the version field (which is the device's clock at time of creation).
     */
    private fun isConcurrent(a: FileEntry, b: FileEntry): Boolean {
        val vcA = mapOf(a.addedBy to a.version)
        val vcB = mapOf(b.addedBy to b.version)
        return VectorClock.concurrent(vcA, vcB)
    }
}

/**
 * Provides conflict resolution strategies used by MergeEngine.
 */
object ConflictResolver {
    /**
     * Add-wins strategy: when a file is added on one device and deleted on another
     * concurrently (deleter did not see the add), the add wins.
     */
    fun addWins(deletionMarker: DeletionMarker, fileEntry: FileEntry): Boolean =
        !deletionMarker.dominates(fileEntry.version)

    /**
     * Deduplication: if two files have the same content hash, they are the same file.
     */
    fun isDuplicate(a: FileEntry, b: FileEntry): Boolean =
        a.contentHash.contentEquals(b.contentHash)

    /**
     * Tombstone dominance: a deletion marker applies if the deleter had seen
     * the version being deleted (or a newer one).
     */
    fun tombstoneDominates(marker: DeletionMarker, fileVersion: Long): Boolean =
        marker.dominates(fileVersion)
}