package com.vaultaire.android.ui.screens.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraEmptyState
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraNavBar
import com.vaultaire.android.designsystem.AuroraNavItem
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTopBar
import com.vaultaire.android.designsystem.AuroraTint
import com.vaultaire.android.designsystem.AuroraType
import androidx.compose.foundation.layout.defaultMinSize
import com.vaultaire.android.ui.session.VaultaireSession
import java.io.File

private val filters = listOf("All", "Photos", "Videos", "Files")

@Composable
fun VaultHomeScreen(
    onOpenTimeline: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCollection: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenShares: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = AuroraTheme.colors
    var filterIndex by remember { mutableStateOf(0) }
    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }

    val sessionVault = VaultaireSession.activeVault
    val realItems: List<VaultItem> = remember(sessionVault) {
        sessionVault?.fileMap?.values?.map { record ->
            val kind = when {
                record.mimeType?.startsWith("image/") == true -> MediaKind.Photo
                record.mimeType?.startsWith("video/") == true -> MediaKind.Video
                else -> MediaKind.File
            }
            VaultItem(
                id = record.fileId,
                name = File(record.encryptedPath).nameWithoutExtension.ifBlank { record.fileId },
                kind = kind,
                sizeBytes = record.plaintextSize,
                addedLabel = "Just now",
                durationSeconds = 0,
                collectionId = "camera",
                hashPreview = record.checksum?.take(4) ?: "",
            )
        } ?: emptyList()
    }

    val items = when (filterIndex) {
        1 -> realItems.filter { it.kind == MediaKind.Photo }
        2 -> realItems.filter { it.kind == MediaKind.Video }
        3 -> realItems.filter { it.kind == MediaKind.File }
        else -> realItems
    }
    val totalBytes = realItems.sumOf { it.sizeBytes }
    val collectionsCount = if (realItems.isNotEmpty()) 1 else 0

    AuroraBackground {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                AuroraTopBar(
                    title = if (selecting) "${selected.size} selected" else "Vault",
                    actions = {
                        AuroraIconButton(Icons.Filled.Search, "Search", onOpenSearch)
                        AuroraIconButton(Icons.Filled.Settings, "Settings", onOpenSettings)
                    },
                )
                if (selecting) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AuroraButton(
                            "Delete",
                            onClick = { selected.clear(); selecting = false },
                            enabled = selected.isNotEmpty(),
                            variant = AuroraButtonVariant.Danger,
                            leadingIcon = Icons.Filled.Delete,
                            modifier = Modifier.weight(1f),
                        )
                        AuroraButton(
                            "Share",
                            onClick = { selected.clear(); selecting = false },
                            enabled = selected.isNotEmpty(),
                            variant = AuroraButtonVariant.Secondary,
                            leadingIcon = Icons.Filled.Share,
                            modifier = Modifier.weight(1f),
                        )
                        AuroraButton(
                            "Done",
                            onClick = { selecting = false; selected.clear() },
                            variant = AuroraButtonVariant.Ghost,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AuroraTheme.dimens.screenPadding),
                ) {
                    if (!selecting) {
                        AuroraGlassCard(Modifier.fillMaxWidth(), tint = AuroraTint.Lilac) {
                            Column(Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Storage,
                                        contentDescription = null,
                                        tint = c.iris600,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("This vault", style = AuroraType.titleM, color = c.ink900)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        VaultDemoData.formatBytes(totalBytes),
                                        style = AuroraType.monoM,
                                        color = c.ink500,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${realItems.size} items · $collectionsCount collections · ${if (totalBytes > 0) "backup available" else "no backup yet"}",
                                    style = AuroraType.bodyS,
                                    color = c.ink500,
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    AuroraButton(
                                        "Add media",
                                        onClick = onOpenCamera,
                                        modifier = Modifier.weight(1f),
                                        leadingIcon = Icons.Filled.Add,
                                    )
                                    AuroraButton(
                                        "Import",
                                        onClick = onOpenImport,
                                        variant = AuroraButtonVariant.Secondary,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(AuroraTheme.dimens.cardGap))
                        BannerRow(
                            itemCount = realItems.size,
                            totalBytes = totalBytes,
                            onOpenImport = onOpenImport,
                            onOpenTimeline = onOpenTimeline,
                        )

                        if (collectionsCount > 0) {
                            Spacer(Modifier.height(AuroraTheme.dimens.cardGap))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CollectionChip("All files", realItems.size) { onOpenCollection("all") }
                            }
                        }
                        Spacer(Modifier.height(AuroraTheme.dimens.cardGap))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            filters.forEachIndexed { index, label ->
                                AuroraChip(
                                    label = label,
                                    selected = filterIndex == index,
                                    onClick = { filterIndex = index },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(AuroraTheme.dimens.cardGap))
                    }

                    if (items.isEmpty()) {
                        AuroraEmptyState(
                            title = "No files in this vault — tap + to add",
                            body = "Encrypted photos, videos and files you add appear in this grid.",
                            primaryLabel = "Add media",
                            onPrimary = onOpenCamera,
                            secondaryLabel = "Import files",
                            onSecondary = onOpenImport,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (selecting) 520.dp else 620.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(items, key = { it.id }) { item ->
                                ItemTile(
                                    item = item,
                                    selecting = selecting,
                                    selected = selected.contains(item.id),
                                    onClick = {
                                        if (selecting) {
                                            if (!selected.remove(item.id)) selected.add(item.id)
                                        } else {
                                            onOpenFile(item.id)
                                        }
                                    },
                                    onLongPress = { selecting = true; if (!selected.contains(item.id)) selected.add(item.id) },
                                )
                            }
                        }
                    }


                    Spacer(Modifier.height(28.dp))
                }
            }

            if (!selecting) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = AuroraTheme.dimens.navBar + 20.dp),
                ) {
                    Box(
                        Modifier
                            .size(AuroraTheme.dimens.fab)
                            .clip(AuroraShapes.full)
                            .background(c.iris600)
                            .clickable(onClick = onOpenCamera),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Camera",
                            tint = c.solid,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }

            AuroraNavBar(
                items = listOf(
                    AuroraNavItem("Vault", Icons.Filled.Lock),
                    AuroraNavItem("Shares", Icons.Filled.Groups),
                    AuroraNavItem("Settings", Icons.Filled.Settings),
                ),
                selectedIndex = 0,
                onSelect = { index ->
                    when (index) {
                        1 -> onOpenShares()
                        2 -> onOpenSettings()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun BannerRow(
    itemCount: Int,
    totalBytes: Long,
    onOpenImport: () -> Unit,
    onOpenTimeline: () -> Unit,
) {
    val c = AuroraTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AuroraShapes.md)
            .background(c.tintButter)
            .border(1.dp, c.tintButterDeep.copy(alpha = 0.35f), AuroraShapes.md)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = c.ink800, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (itemCount > 0) "Backup pending" else "No backups yet",
                style = AuroraType.titleM,
                color = c.ink900,
            )
            Text(
                if (itemCount > 0) "${VaultDemoData.formatBytes(totalBytes)} encrypted and stored." else "0 B encrypted and stored.",
                style = AuroraType.bodyS,
                color = c.ink700,
            )
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(AuroraShapes.sm)
                .clickable(onClick = onOpenImport),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Manage",
                style = AuroraType.labelM,
                color = c.iris600,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(AuroraShapes.sm)
                .clickable(onClick = onOpenTimeline),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Timeline",
                style = AuroraType.labelM,
                color = c.iris600,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ItemTile(
    item: VaultItem,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val c = AuroraTheme.colors
    val tint = when (item.kind) {
        MediaKind.Photo -> c.iris100
        MediaKind.Video -> c.tintPeach
        MediaKind.File -> c.tintSand
    }
    Box(
        Modifier
            .aspectRatio(0.82f)
            .clip(AuroraShapes.md)
            .background(tint)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) c.iris600 else c.border,
                shape = AuroraShapes.md,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                VaultDemoData.kindIcon(item.kind),
                contentDescription = null,
                tint = c.ink500,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                item.name,
                style = AuroraType.monoS,
                color = c.ink700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            if (item.durationSeconds > 0) {
                Text(
                    VaultDemoData.formatDuration(item.durationSeconds),
                    style = AuroraType.monoS,
                    color = c.ink500,
                )
            }
        }
        if (selecting) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) c.iris600 else c.glass),
            )
        }
    }
}

@Composable
private fun CollectionChip(label: String, count: Int, onClick: () -> Unit) {
    val c = AuroraTheme.colors
    Row(
        Modifier
            .clip(AuroraShapes.md)
            .background(c.glass)
            .border(1.dp, c.border, AuroraShapes.md)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = c.iris600, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = AuroraType.bodyS, color = c.ink900)
            Text("$count items", style = AuroraType.monoS, color = c.ink500)
        }
    }
}

