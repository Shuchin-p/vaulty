package com.vaultaire.android.sync.model

import kotlinx.serialization.Serializable

/**
 * Tombstone marking a file as deleted.
 *
 * A deletion marker carries a vector clock snapshot showing what the
 * deleting device had seen at the time of deletion. This allows the
 * merge engine to implement "add wins over unseen delete": if another
 * device concurrently added the same file (content hash) without the
 * deleter having seen it, the add wins.
 */
@Serializable
data class DeletionMarker(
    val fileId: String,
    val deletedBy: String,
    val deletedAt: Long,
    val vectorClockSnapshot: Map<String, Long>
) {
    /**
     * Returns true if this deletion marker dominates the given file version.
     * i.e., the deleter had seen this version (or a newer one) of the file.
     */
    fun dominates(fileVersion: Long, deleterDeviceId: String = deletedBy): Boolean {
        val deleterClock = vectorClockSnapshot[deleterDeviceId] ?: 0L
        return deleterClock >= fileVersion
    }
}