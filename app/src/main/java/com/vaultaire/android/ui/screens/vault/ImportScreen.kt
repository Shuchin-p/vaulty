package com.vaultaire.android.ui.screens.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraDimens
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraProgressRing
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTint
import com.vaultaire.android.designsystem.AuroraTopBar
import com.vaultaire.android.designsystem.AuroraType

/**
 * Queue item for simulated system import.
 */
data class ImportQueueItem(
    val name: String,
    val sizeLabel: String,
    val isDuplicate: Boolean = false,
    var skipDuplicate: Boolean = true,
)

/**
 * Gallery and file system import screen with duplicate detection,
 * delete originals toggle, and encrypt-in-place progress.
 * Implemented against plan/tests/screens/27-restore-import.md.
 */
@Composable
fun ImportScreen(
    onBack: () -> Unit,
) {
    val c = AuroraTheme.colors
    var deleteOriginals by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importFinished by remember { mutableStateOf(false) }
    var progressStep by remember { mutableIntStateOf(1) }

    val queueItems = remember {
        mutableStateListOf(
            ImportQueueItem("IMG_4091.jpg", "3.4 MB", isDuplicate = false),
            ImportQueueItem("IMG_2481.jpg", "3.8 MB", isDuplicate = true, skipDuplicate = true),
            ImportQueueItem("Contract_Scan.pdf", "1.1 MB", isDuplicate = false),
            ImportQueueItem("VID_3002.mp4", "45.0 MB", isDuplicate = false),
        )
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Screen Top Bar
            AuroraTopBar(
                title = "Import",
                eyebrow = "Vault Intake",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
            )

            if (isImporting) {
                // In-Progress Encrypt-In-Place Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AuroraProgressRing(
                        progress = progressStep / 4f,
                        size = 140.dp,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = c.iris600,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Encrypting into vault...",
                        style = AuroraType.headlineM,
                        color = c.ink900,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "$progressStep of 4 items encrypted",
                        style = AuroraType.monoM,
                        color = c.ink600,
                    )
                    Spacer(Modifier.height(32.dp))
                    AuroraButton(
                        text = "Complete",
                        onClick = {
                            isImporting = false
                            importFinished = true
                        },
                        variant = AuroraButtonVariant.Secondary,
                    )
                }
            } else if (importFinished) {
                // Import Success Summary Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(c.tintMint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = c.success,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Import complete",
                        style = AuroraType.headlineM,
                        color = c.ink900,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "3 new encrypted items saved to vault. 1 duplicate skipped.",
                        style = AuroraType.bodyM,
                        color = c.ink600,
                    )
                    Spacer(Modifier.height(28.dp))
                    AuroraButton(
                        text = "Open vault",
                        onClick = onBack,
                    )
                }
            } else {
                // Queue Preview & Options List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AuroraDimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        // Source Indicator Card
                        AuroraGlassCard(
                            tint = AuroraTint.Sky,
                            raised = false,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(c.iris600.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = null,
                                        tint = c.iris600,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "4 files selected for import",
                                        style = AuroraType.titleM,
                                        color = c.ink900,
                                    )
                                    Text(
                                        text = "Direct encrypt-in-place · No plaintext temp copy",
                                        style = AuroraType.bodyS,
                                        color = c.ink600,
                                    )
                                }
                            }
                        }
                    }

                    // Delete Originals Toggle Card (Off by default per T27-36)
                    item {
                        AuroraGlassCard(
                            tint = AuroraTint.None,
                            raised = false,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { deleteOriginals = !deleteOriginals },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Delete originals from Photos",
                                        style = AuroraType.titleM,
                                        color = c.ink900,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Only deletes from system gallery after encryption completes.",
                                        style = AuroraType.bodyS,
                                        color = c.ink600,
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (deleteOriginals) c.surfaceInk else c.solid)
                                        .border(1.dp, if (deleteOriginals) c.surfaceInk else c.border, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (deleteOriginals) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Enabled",
                                            tint = c.solid,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section Heading
                    item {
                        Text(
                            text = "Import Queue",
                            style = AuroraType.labelL,
                            color = c.ink700,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    // Queue items
                    items(queueItems) { item ->
                        ImportItemRow(
                            item = item,
                            onToggleDuplicateSkip = {
                                item.skipDuplicate = !item.skipDuplicate
                            },
                        )
                    }
                }

                // Bottom Action Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AuroraDimens.screenPadding),
                ) {
                    AuroraButton(
                        text = "Import ${queueItems.size} items",
                        onClick = {
                            isImporting = true
                            progressStep = 2
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportItemRow(
    item: ImportQueueItem,
    onToggleDuplicateSkip: () -> Unit,
) {
    val c = AuroraTheme.colors
    AuroraGlassCard(
        tint = if (item.isDuplicate) AuroraTint.Butter else AuroraTint.None,
        raised = false,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c.surfaceInk.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (item.name.endsWith(".pdf")) Icons.Filled.Description else Icons.Filled.Image,
                        contentDescription = null,
                        tint = c.ink800,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = AuroraType.titleM,
                        color = c.ink900,
                    )
                    Text(
                        text = item.sizeLabel,
                        style = AuroraType.bodyS,
                        color = c.ink600,
                    )
                }
                if (item.isDuplicate) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(c.tintButter)
                            .clickable(onClick = onToggleDuplicateSkip)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = c.ink800,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (item.skipDuplicate) "Skip duplicate" else "Add anyway",
                            style = AuroraType.labelM,
                            color = c.ink900,
                        )
                    }
                }
            }
        }
    }
}
