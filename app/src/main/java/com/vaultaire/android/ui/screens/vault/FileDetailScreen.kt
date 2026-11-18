package com.vaultaire.android.ui.screens.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraDimens
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraSecureSurface
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTint
import com.vaultaire.android.designsystem.AuroraTopBar
import com.vaultaire.android.designsystem.AuroraType

/**
 * Full-bleed secure viewer for encrypted photos, videos, and documents.
 * Protected by FLAG_SECURE via AuroraSecureSurface.
 * Implemented against plan/tests/screens/08-file-viewer.md.
 */
@Composable
fun FileDetailScreen(
    fileId: String,
    onBack: () -> Unit,
) {
    val c = AuroraTheme.colors
    val sessionItem = com.vaultaire.android.ui.session.VaultaireSession.activeVault?.fileMap?.get(fileId)?.let { record ->
        val kind = when {
            record.mimeType?.startsWith("image/") == true -> MediaKind.Photo
            record.mimeType?.startsWith("video/") == true -> MediaKind.Video
            else -> MediaKind.File
        }
        VaultItem(
            id = record.fileId,
            name = java.io.File(record.encryptedPath).nameWithoutExtension.ifBlank { record.fileId },
            kind = kind,
            sizeBytes = record.plaintextSize,
            addedLabel = "Encrypted",
            durationSeconds = 0,
        )
    }
    val item = sessionItem ?: VaultDemoData.byId(fileId) ?: VaultItem(fileId, "File", MediaKind.File, 0L, "Encrypted")

    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableFloatStateOf(0.2f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    AuroraSecureSurface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Screen Top Bar
            AuroraTopBar(
                title = item.name,
                eyebrow = "${item.kind.name} · Encrypted",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        onInk = true,
                    )
                },
                actions = {
                    AuroraIconButton(
                        icon = Icons.Filled.Info,
                        contentDescription = "Metadata info",
                        onClick = { showInfoSheet = !showInfoSheet },
                        onInk = true,
                    )
                    AuroraIconButton(
                        icon = Icons.Filled.Share,
                        contentDescription = "Share",
                        onClick = { /* simulated share */ },
                        onInk = true,
                    )
                    AuroraIconButton(
                        icon = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        onClick = { showDeleteDialog = true },
                        onInk = true,
                    )
                },
            )

            // Security Badge: In-Memory Decryption
            Row(
                modifier = Modifier
                    .padding(horizontal = AuroraDimens.screenPadding, vertical = 4.dp)
                    .clip(CircleShape)
                    .background(c.surfaceInk.copy(alpha = 0.25f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = c.success,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Decrypted in memory · Zero plaintext at rest",
                    style = AuroraType.labelM,
                    color = c.solid,
                )
            }

            // Media Stage
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (item.kind) {
                    MediaKind.Photo -> {
                        // Photo Viewer with double-tap zoom simulation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.85f)
                                .clip(AuroraShapes.lg)
                                .background(c.surfaceInk.copy(alpha = 0.4f))
                                .border(1.dp, c.border.copy(alpha = 0.2f), AuroraShapes.lg)
                                .clickable {
                                    zoomScale = if (zoomScale == 1.0f) 2.5f else 1.0f
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = VaultDemoData.kindIcon(item.kind),
                                    contentDescription = null,
                                    tint = c.solid.copy(alpha = 0.85f),
                                    modifier = Modifier.size((72 * zoomScale).dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = item.name,
                                    style = AuroraType.titleM,
                                    color = c.solid,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Tap to zoom (${zoomScale}x)",
                                    style = AuroraType.labelM,
                                    color = c.solid.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }

                    MediaKind.Video -> {
                        // Video Player Surface with Play/Pause and Scrubber
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.85f)
                                .clip(AuroraShapes.lg)
                                .background(c.surfaceInk.copy(alpha = 0.5f))
                                .border(1.dp, c.border.copy(alpha = 0.2f), AuroraShapes.lg),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Center Play / Pause 44dp overlay button
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(c.solid.copy(alpha = 0.25f))
                                    .clickable { isPlaying = !isPlaying },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause video" else "Play video",
                                    tint = c.solid,
                                    modifier = Modifier.size(32.dp),
                                )
                            }

                            // Bottom Transport Bar
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(c.surfaceInk.copy(alpha = 0.7f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                val elapsedSec = (item.durationSeconds * videoProgress).toInt()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = VaultDemoData.formatDuration(elapsedSec),
                                        style = AuroraType.monoS,
                                        color = c.solid,
                                    )
                                    Text(
                                        text = VaultDemoData.formatDuration(item.durationSeconds),
                                        style = AuroraType.monoS,
                                        color = c.solid.copy(alpha = 0.7f),
                                    )
                                }
                                Slider(
                                    value = videoProgress,
                                    onValueChange = { videoProgress = it },
                                    colors = SliderDefaults.colors(
                                        thumbColor = c.solid,
                                        activeTrackColor = c.iris500,
                                        inactiveTrackColor = c.border.copy(alpha = 0.3f),
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }

                    MediaKind.File -> {
                        // Encrypted Document Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.9f)
                                .clip(AuroraShapes.lg)
                                .background(c.surfaceInk.copy(alpha = 0.4f))
                                .border(1.dp, c.border.copy(alpha = 0.2f), AuroraShapes.lg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(c.iris600.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = VaultDemoData.kindIcon(item.kind),
                                        contentDescription = null,
                                        tint = c.solid,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = item.name,
                                    style = AuroraType.titleM,
                                    color = c.solid,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = VaultDemoData.formatBytes(item.sizeBytes),
                                    style = AuroraType.monoM,
                                    color = c.solid.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }

            // Info Sheet Overlay (when toggled via Info button)
            if (showInfoSheet) {
                AuroraGlassCard(
                    tint = AuroraTint.Sand,
                    raised = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AuroraDimens.screenPadding, vertical = 8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Item Details",
                            style = AuroraType.headlineM,
                            color = c.ink900,
                        )
                        DetailRow("Format", item.kind.name)
                        DetailRow("Size", VaultDemoData.formatBytes(item.sizeBytes))
                        DetailRow("Added", item.addedLabel)
                        DetailRow("Cipher", "AES-256-GCM")
                        DetailRow("Hash SHA-256", item.hashPreview.ifEmpty { "e3b0c442" })
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        Dialog(onDismissRequest = { showDeleteDialog = false }) {
            AuroraGlassCard(
                tint = AuroraTint.Rose,
                raised = true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Delete this item?",
                        style = AuroraType.headlineM,
                        color = c.danger,
                    )
                    Text(
                        text = "This item will be permanently removed from your encrypted vault.",
                        style = AuroraType.bodyM,
                        color = c.ink700,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        AuroraButton(
                            text = "Keep",
                            variant = AuroraButtonVariant.Ghost,
                            onClick = { showDeleteDialog = false },
                        )
                        Spacer(Modifier.width(8.dp))
                        AuroraButton(
                            text = "Delete",
                            variant = AuroraButtonVariant.Danger,
                            onClick = {
                                showDeleteDialog = false
                                onBack()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val c = AuroraTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = AuroraType.labelM, color = c.ink600)
        Text(text = value, style = AuroraType.monoS, color = c.ink900)
    }
}
