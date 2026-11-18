package com.vaultaire.android.sync.model

import kotlinx.serialization.Serializable

/**
 * A local change waiting to be uploaded to the sync transport.
 *
 * The vectorClock captures the state at the time of the change,
 * enabling correct merge ordering on other devices.
 */
@Serializable
data class PendingChange(
    val changeId: String,
    val type: ChangeType,
    val fileId: String?,
    val chunkRefs: List<ChunkRef>?,
    val vectorClock: Map<String, Long>,
    val timestamp: Long,
    val retryCount: Int = 0
)