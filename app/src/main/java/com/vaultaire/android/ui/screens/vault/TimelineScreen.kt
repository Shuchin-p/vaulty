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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vaultaire.android.designsystem.AuroraBackground
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
 * Vault timeline screen showing day-grouped activity events, offline sync badges,
 * and preview toggles per plan/design-system/flows/02-vault-core.md and
 * plan/tests/screens/05-timeline.md.
 */
@Composable
fun TimelineScreen(
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit,
) {
    val c = AuroraTheme.colors
    val days = listOf("All", "Today", "Yesterday", "3 days ago", "Last week")
    var selectedDay by remember { mutableStateOf("All") }
    var hidePreviews by remember { mutableStateOf(false) }

    // Filter events based on selected calendar bucket
    val filteredEvents = remember(selectedDay) {
        if (selectedDay == "All") {
            VaultDemoData.events
        } else {
            VaultDemoData.events.filter { it.second == selectedDay }
        }
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
                title = "Timeline",
                eyebrow = "Vault Activity",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
                actions = {
                    // Preview visibility toggle button
                    AuroraIconButton(
                        icon = if (hidePreviews) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (hidePreviews) "Show previews" else "Hide previews",
                        onClick = { hidePreviews = !hidePreviews },
                    )
                },
            )

            // Day Selector Calendar Strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AuroraDimens.screenPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(days) { day ->
                    AuroraChip(
                        label = day,
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Main Content: Event Rail or Empty State
            if (filteredEvents.isEmpty()) {
                AuroraEmptyState(
                    title = "No activity",
                    body = "There are no recorded vault events for this day.",
                    primaryLabel = "View all days",
                    onPrimary = { selectedDay = "All" },
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AuroraDimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredEvents) { event ->
                        val (action, dayLabel, fileId) = event
                        val item = VaultDemoData.byId(fileId)
                        val isQueued = dayLabel == "Today" && action == "Added"

                        TimelineEventCard(
                            action = action,
                            dayLabel = dayLabel,
                            item = item,
                            hidePreviews = hidePreviews,
                            isQueued = isQueued,
                            onClick = {
                                if (item != null) {
                                    onOpenFile(item.id)
                                }
                            },
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
 * Individual timeline event card with action icon, tint, and metadata.
 */
@Composable
private fun TimelineEventCard(
    action: String,
    dayLabel: String,
    item: VaultItem?,
    hidePreviews: Boolean,
    isQueued: Boolean,
    onClick: () -> Unit,
) {
    val c = AuroraTheme.colors
    val (tint, icon) = when (action) {
        "Added" -> Pair(AuroraTint.Mint, Icons.Filled.Add)
        "Imported" -> Pair(AuroraTint.Sky, Icons.Filled.Upload)
        "Shared" -> Pair(AuroraTint.Lilac, Icons.Filled.Share)
        "Deleted" -> Pair(AuroraTint.Rose, Icons.Filled.Delete)
        else -> Pair(AuroraTint.Sand, Icons.Filled.Add)
    }

    AuroraGlassCard(
        tint = tint,
        raised = true,
        onClick = if (item != null && action != "Deleted") onClick else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Action Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(c.surfaceInk.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = c.ink900,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            // Action Details & Item Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$action · $dayLabel",
                        style = AuroraType.labelM,
                        color = c.ink700,
                    )
                    if (isQueued) {
                        // Offline Sync Clock Badge (T05-18, T05-50)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(c.surfaceInk.copy(alpha = 0.06f))
                                .border(1.dp, c.border, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .semantics { contentDescription = "Waiting to sync" },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = c.ink500,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Queued",
                                style = AuroraType.labelM,
                                color = c.ink500,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                val displayName = when {
                    action == "Deleted" -> "Encrypted item deleted"
                    hidePreviews -> "1 encrypted item"
                    item != null -> item.name
                    else -> "Encrypted file"
                }

                Text(
                    text = displayName,
                    style = AuroraType.titleM,
                    color = c.ink900,
                )

                if (item != null && action != "Deleted" && !hidePreviews) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${item.kind.name} · ${VaultDemoData.formatBytes(item.sizeBytes)}",
                        style = AuroraType.bodyS,
                        color = c.ink600,
                    )
                }
            }
        }
    }
}
