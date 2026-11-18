package com.vaultaire.android.sync.model

import kotlinx.serialization.Serializable

/**
 * Reference to an encrypted chunk stored in Google Drive.
 *
 * The chunk ID is a random UUID (not content-addressed in cloud).
 * The Drive record name is the UUID used as the file name on Drive.
 * Offset/length allow multiple chunks per 10 MB padded record.
 * The encryptedHash is SHA-256 of the ciphertext for download verification.
 */
@Serializable
data class ChunkRef(
    val chunkId: String,
    val driveRecordName: String,
    val offset: Long,
    val length: Long,
    val encryptedHash: ByteArray
)