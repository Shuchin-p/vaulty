package com.vaultaire.android.sync.model

import kotlinx.serialization.Serializable

/**
 * Entry in the file index tracking a vault file across devices.
 *
 * The contentHash is SHA-256 of the plaintext (for deduplication).
 * chunkRefs point to encrypted chunks on Drive.
 * addedBy/version form a minimal vector clock for this file.
 */
@Serializable
data class FileEntry(
    val fileId: String,
    val contentHash: ByteArray,
    val chunkRefs: List<ChunkRef>,
    val size: Long,
    val mimeType: String,
    val addedBy: String,
    val addedAt: Long,
    val version: Long
)