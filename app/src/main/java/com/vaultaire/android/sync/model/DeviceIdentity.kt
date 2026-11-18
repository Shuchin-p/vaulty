package com.vaultaire.android.sync.model

import kotlinx.serialization.Serializable

/**
 * Device identity for cross-device sync tracking.
 */
@Serializable
data class DeviceIdentity(
    val deviceId: String,
    val deviceName: String,
    val enrollmentKey: ByteArray,
    val createdAt: Long,
    val lastSyncAt: Long = 0L
)