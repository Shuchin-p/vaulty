package com.vaultaire.android.sync.engine

/**
 * Sync format versioning for forward compatibility.
 * Increment when changing SyncMetadata serialization in a breaking way.
 */
object SyncFormat {
    /** Current sync record format version. */
    const val CURRENT_VERSION = 1

    /** Oldest version this build can still read. */
    const val MIN_COMPATIBLE_VERSION = 1

    /**
     * Migrates a deserialized [SyncMetadata] to the current format.
     * Throws [IncompatibleSyncFormatException] when the version is too old.
     */
    fun migrateIfNeeded(metadata: com.vaultaire.android.sync.model.SyncMetadata):
            com.vaultaire.android.sync.model.SyncMetadata {
        return when (metadata.syncFormatVersion) {
            CURRENT_VERSION -> metadata
            else -> throw IncompatibleSyncFormatException(
                "Unsupported sync format version: ${metadata.syncFormatVersion}"
            )
        }
    }
}

/** Thrown when a sync record uses an unsupported format version. */
class IncompatibleSyncFormatException(message: String) : Exception(message)
