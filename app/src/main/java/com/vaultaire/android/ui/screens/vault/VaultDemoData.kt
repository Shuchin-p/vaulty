package com.vaultaire.android.ui.screens.vault

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

enum class MediaKind { Photo, Video, File }

/** Demo item kinds used by the vault family. */
@Immutable
data class VaultItem(
    val id: String,
    val name: String,
    val kind: MediaKind,
    val sizeBytes: Long,
    val addedLabel: String,
    val durationSeconds: Int = 0,
    val collectionId: String = "camera",
    val hashPreview: String = "",
)

@Immutable
data class DemoCollection(
    val id: String,
    val name: String,
    val itemCount: Int,
    val policy: String,
)

object VaultDemoData {

    /**
     * Production lists are strictly empty on fresh launch per Requirement R3.
     * Mocks and dummy files are forbidden in the production app.
     */
    val items: List<VaultItem> = emptyList()

    val collections: List<DemoCollection> = emptyList()

    val events: List<Triple<String, String, String>> = emptyList()

    fun kindIcon(kind: MediaKind): ImageVector = when (kind) {
        MediaKind.Photo -> Icons.Filled.Image
        MediaKind.Video -> Icons.Filled.Videocam
        MediaKind.File -> Icons.Filled.Description
    }

    fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> String.format("%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.0f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }

    fun byId(id: String): VaultItem? = items.firstOrNull { it.id == id }

    fun collectionName(id: String): String = collections.firstOrNull { it.id == id }?.name ?: "Unassigned"
}