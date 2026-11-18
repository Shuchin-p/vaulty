package com.vaultaire.android.vault

import kotlinx.serialization.Serializable

/**
 * Represents a single file record within a vault.
 */
@Serializable
data class FileRecord(
    val fileId: String,
    val encryptedPath: String,
    val plaintextSize: Long,
    val encryptedSize: Long,
    val createdAt: Long,
    val mimeType: String? = null,
    val checksum: String? = null
) {
    companion object {
        fun create(
            fileId: String,
            encryptedPath: String,
            plaintextSize: Long,
            encryptedSize: Long = 0L,
            mimeType: String? = null
        ): FileRecord {
            return FileRecord(
                fileId = fileId,
                encryptedPath = encryptedPath,
                plaintextSize = plaintextSize,
                encryptedSize = encryptedSize,
                createdAt = System.currentTimeMillis(),
                mimeType = mimeType,
                checksum = null
            )
        }
    }
}
