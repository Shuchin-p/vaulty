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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraDimens
import com.vaultaire.android.designsystem.AuroraEmptyState
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTint
import com.vaultaire.android.designsystem.AuroraTopBar
import com.vaultaire.android.designsystem.AuroraType

/**
 * Vault collection detail screen with hero, media filter chips, item rows,
 * rename dialog, and delete collection confirmation.
 * Implemented against plan/tests/screens/06-collection.md.
 */
@Composable
fun CollectionScreen(
    collectionId: String,
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit,
) {
    val c = AuroraTheme.colors
    val collectionObj = VaultDemoData.collections.firstOrNull { it.id == collectionId }
    var collectionTitle by remember(collectionId) {
        mutableStateOf(collectionObj?.name ?: VaultDemoData.collectionName(collectionId))
    }
    val policy = collectionObj?.policy ?: "Backup on"
    val isReadOnly = policy.equals("Read-only", ignoreCase = true)

    // Items belonging to this collection (or demo items matching id)
    val collectionItems = remember(collectionId) {
        val matching = VaultDemoData.items.filter { it.collectionId == collectionId }
        mutableStateListOf<VaultItem>().apply {
            if (matching.isNotEmpty()) addAll(matching) else addAll(VaultDemoData.items.take(4))
        }
    }

    val filterOptions = listOf("All", "Photos", "Videos", "Files")
    var selectedFilter by remember { mutableStateOf("All") }

    val displayedItems = remember(selectedFilter, collectionItems.size) {
        when (selectedFilter) {
            "Photos" -> collectionItems.filter { it.kind == MediaKind.Photo }
            "Videos" -> collectionItems.filter { it.kind == MediaKind.Video }
            "Files" -> collectionItems.filter { it.kind == MediaKind.File }
            else -> collectionItems
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Screen Top Bar
            AuroraTopBar(
                title = collectionTitle,
                eyebrow = "Collection",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
                actions = {
                    if (!isReadOnly) {
                        AuroraIconButton(
                            icon = Icons.Filled.MoreVert,
                            contentDescription = "Collection actions",
                            onClick = { showMenu = !showMenu },
                        )
                    }
                },
            )

            // Context Action Row when menu is expanded
            if (showMenu && !isReadOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AuroraDimens.screenPadding, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AuroraButton(
                        text = "Rename",
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        },
                        leadingIcon = Icons.Filled.Edit,
                        variant = AuroraButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    AuroraButton(
                        text = "Delete",
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = Icons.Filled.Delete,
                        variant = AuroraButtonVariant.Danger,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Collection Hero Card (220dp approx)
            AuroraGlassCard(
                tint = AuroraTint.Lilac,
                raised = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AuroraDimens.screenPadding, vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = collectionTitle,
                            style = AuroraType.headlineL,
                            color = c.ink900,
                        )
                        // Read-only or Policy Badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(c.surfaceInk.copy(alpha = 0.08f))
                                .border(1.dp, c.border, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = c.ink700,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isReadOnly) "Read-only" else policy,
                                    style = AuroraType.labelM,
                                    color = c.ink700,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val totalBytes = collectionItems.sumOf { it.sizeBytes }
                    Text(
                        text = "${collectionItems.size} items · ${VaultDemoData.formatBytes(totalBytes)}",
                        style = AuroraType.monoM,
                        color = c.ink600,
                    )
                }
            }

            // Media Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AuroraDimens.screenPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filterOptions) { filter ->
                    AuroraChip(
                        label = filter,
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Items List or Empty State
            if (displayedItems.isEmpty()) {
                AuroraEmptyState(
                    title = "This collection is empty.",
                    body = if (selectedFilter != "All") "No items match filter '$selectedFilter'." else "Add encrypted items to this collection.",
                    primaryLabel = if (selectedFilter != "All") "Clear filters" else "Add items",
                    onPrimary = {
                        if (selectedFilter != "All") {
                            selectedFilter = "All"
                        } else {
                            collectionItems.addAll(VaultDemoData.items.take(2))
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AuroraDimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(displayedItems) { item ->
                        CollectionItemRow(
                            item = item,
                            onClick = { onOpenFile(item.id) },
                        )
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        var newName by remember { mutableStateOf(collectionTitle) }
        Dialog(onDismissRequest = { showRenameDialog = false }) {
            AuroraGlassCard(
                tint = AuroraTint.Sand,
                raised = true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Rename Collection",
                        style = AuroraType.headlineM,
                        color = c.ink900,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AuroraShapes.md)
                            .background(c.solid)
                            .border(1.dp, c.border, AuroraShapes.md)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        BasicTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            textStyle = AuroraType.bodyL.copy(color = c.ink900),
                            cursorBrush = SolidColor(c.iris600),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newName.isNotBlank()) {
                                    collectionTitle = newName.trim()
                                    showRenameDialog = false
                                }
                            }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        AuroraButton(
                            text = "Cancel",
                            variant = AuroraButtonVariant.Ghost,
                            onClick = { showRenameDialog = false },
                        )
                        Spacer(Modifier.width(8.dp))
                        AuroraButton(
                            text = "Save",
                            enabled = newName.isNotBlank(),
                            onClick = {
                                collectionTitle = newName.trim()
                                showRenameDialog = false
                            },
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog (Exact copy: "Delete this collection? The photos stay in your vault.")
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
                        text = "Delete collection?",
                        style = AuroraType.headlineM,
                        color = c.danger,
                    )
                    Text(
                        text = "Delete this collection? The photos stay in your vault.",
                        style = AuroraType.bodyM,
                        color = c.ink700,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        AuroraButton(
                            text = "Cancel",
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

/**
 * 64dp standard item row within a collection.
 */
@Composable
private fun CollectionItemRow(
    item: VaultItem,
    onClick: () -> Unit,
) {
    val c = AuroraTheme.colors
    AuroraGlassCard(
        tint = AuroraTint.None,
        raised = false,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.surfaceInk.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = VaultDemoData.kindIcon(item.kind),
                    contentDescription = item.kind.name,
                    tint = c.ink800,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = AuroraType.titleM,
                    color = c.ink900,
                )
                Text(
                    text = "${item.addedLabel} · ${VaultDemoData.formatBytes(item.sizeBytes)}",
                    style = AuroraType.bodyS,
                    color = c.ink600,
                )
            }
        }
    }
}
