package com.vaultaire.android.vault

import com.vaultaire.android.crypto.EncryptedEnvelope
import kotlinx.serialization.Serializable

/**
 * Represents an unlocked vault with its master key and file index.
 */
@Serializable
data class OpenVault(
    val locator: String,
    val masterKeyEnvelope: EncryptedEnvelope,
    val fileMap: Map<String, FileRecord>,
    val createdAt: Long,
    val version: Int = CURRENT_VERSION,
    val isDuressDecoy: Boolean = false
) {
    companion object {
        const val CURRENT_VERSION = 1
        
        fun create(
            locator: String,
            masterKeyEnvelope: EncryptedEnvelope,
            createdAt: Long = System.currentTimeMillis()
        ): OpenVault {
            return OpenVault(
                locator = locator,
                masterKeyEnvelope = masterKeyEnvelope,
                fileMap = emptyMap(),
                createdAt = createdAt,
                version = CURRENT_VERSION,
                isDuressDecoy = false
            )
        }
    }
}
