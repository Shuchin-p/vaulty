package com.vaultaire.android.sync.model

import java.util.UUID

/**
 * Utility object for UUID ↔ String conversion.
 * Used throughout the sync module as IDs are stored as strings in JSON.
 */
object UUIDSerializer {
    fun toUUIDString(uuid: UUID): String = uuid.toString()
    fun fromUUIDString(str: String): UUID = UUID.fromString(str)
}