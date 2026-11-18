package com.vaultaire.android.sync.model

import kotlinx.serialization.Serializable

/**
 * Change type for pending operations.
 */
@Serializable
enum class ChangeType {
    ADD, DELETE, METADATA_UPDATE
}