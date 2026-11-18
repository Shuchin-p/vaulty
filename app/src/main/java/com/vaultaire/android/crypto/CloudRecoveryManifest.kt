package com.vaultaire.android.crypto

import kotlinx.serialization.Serializable

/**
 * Metadata about the vault for display purposes.
 */
@Serializable
data class VaultMetadata(
    val name: String,
    val createdAt: Long,
    val itemCount: Int = 0
)

/**
 * Cloud recovery manifest (future feature).
 * Stores recovery envelope and backup envelope for cloud-based recovery.
 */
@Serializable
data class CloudRecoveryManifest(
    val locator: ByteArray,
    val recoveryEnvelope: EncryptedEnvelope,
    val backupEnvelope: EncryptedEnvelope,
    val vaultMetadata: VaultMetadata,
    val version: Int = 1
)