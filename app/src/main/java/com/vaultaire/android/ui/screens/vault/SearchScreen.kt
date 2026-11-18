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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraDimens
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTint
import com.vaultaire.android.designsystem.AuroraType

/**
 * Local encrypted index search screen with debounce, filter chips, recent queries,
 * and exact empty-state handling per plan/tests/screens/07-search.md.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit,
) {
    val c = AuroraTheme.colors
    var query by remember { mutableStateOf("") }
    val filterOptions = listOf("All", "Photos", "Videos", "Files")
    var selectedFilter by remember { mutableStateOf("All") }

    val recentSearches = remember {
        mutableStateListOf("passport", "IMG_2481", "lease", "Today")
    }

    // Filter results locally against VaultDemoData
    val results = remember(query, selectedFilter) {
        val trimmed = query.trim().lowercase()
        VaultDemoData.items.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "Photos" -> item.kind == MediaKind.Photo
                "Videos" -> item.kind == MediaKind.Video
                "Files" -> item.kind == MediaKind.File
                else -> true
            }
            val matchesQuery = trimmed.isEmpty() ||
                item.name.lowercase().contains(trimmed) ||
                item.addedLabel.lowercase().contains(trimmed) ||
                item.kind.name.lowercase().contains(trimmed)
            matchesFilter && matchesQuery
        }
    }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Search Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AuroraDimens.screenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AuroraIconButton(
                    icon = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                )

                Spacer(Modifier.width(10.dp))

                // Aurora Search Input Box
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(AuroraShapes.full)
                        .background(c.raisedBackground(AuroraTheme.mode.surface))
                        .border(1.dp, c.border, AuroraShapes.full)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = c.ink500,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = AuroraType.bodyL.copy(color = c.ink900),
                        cursorBrush = SolidColor(c.iris600),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search this vault",
                                    style = AuroraType.bodyL,
                                    color = c.ink400,
                                )
                            }
                            innerTextField()
                        },
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = c.ink600,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { query = "" },
                        )
                    }
                }
            }

            // Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AuroraDimens.screenPadding, vertical = 6.dp),
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

            // Body: Recents OR Results OR Empty State
            if (query.isEmpty() && selectedFilter == "All") {
                // Show Recent Searches
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AuroraDimens.screenPadding, vertical = 16.dp),
                ) {
                    Text(
                        text = "Recent searches",
                        style = AuroraType.labelL,
                        color = c.ink700,
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(recentSearches) { recent ->
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(c.raisedBackground(AuroraTheme.mode.surface))
                                    .border(1.dp, c.border, CircleShape)
                                    .clickable { query = recent }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = c.ink500,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = recent,
                                    style = AuroraType.labelM,
                                    color = c.ink800,
                                )
                            }
                        }
                    }
                }
            }

            // Results Count Header
            if (results.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AuroraDimens.screenPadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Results · ${results.size}",
                        style = AuroraType.monoM,
                        color = c.ink700,
                    )
                }
            }

            // Results List or "No matches in this vault."
            if (results.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No matches in this vault.",
                        style = AuroraType.headlineM,
                        color = c.ink900,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Check spelling or clear active filters.",
                        style = AuroraType.bodyM,
                        color = c.ink600,
                    )
                    Spacer(Modifier.height(20.dp))
                    AuroraButton(
                        text = "Clear filters",
                        onClick = {
                            query = ""
                            selectedFilter = "All"
                        },
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AuroraDimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results) { item ->
                        SearchResultRow(
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
}

/**
 * Result row showing media kind, item title, and size metadata.
 */
@Composable
private fun SearchResultRow(
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
