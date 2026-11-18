package com.vaultaire.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraProgressRing
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraType
import com.vaultaire.android.ui.screens.settings.components.HonestyCard
import com.vaultaire.android.ui.screens.settings.components.SegmentedOptions
import com.vaultaire.android.ui.screens.settings.components.SettingsPage
import com.vaultaire.android.ui.screens.settings.components.SettingsRow
import com.vaultaire.android.ui.screens.settings.components.SettingsSection
import com.vaultaire.android.ui.screens.settings.components.SettingsToggleRow
import com.vaultaire.android.ui.screens.settings.components.SettingsValueRow
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// 5.1 Encrypted backup
// ---------------------------------------------------------------------------

@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit,
    onRestore: () -> Unit,
) {
    val c = AuroraTheme.colors
    var enabled by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(true) }
    var includeVideos by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var lastRun by remember { mutableStateOf("2 hours ago · 1.8 GB") }
    var showBackups by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }
    var showDisable by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        if (running) {
            progress = 0f
            while (progress < 1f) {
                delay(220)
                progress = (progress + 0.12f).coerceAtMost(1f)
            }
            lastRun = "Just now · 1.9 GB"
            running = false
        }
    }

    SettingsPage(title = "Encrypted backup", onBack = onBack) {
        AuroraGlassCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AuroraProgressRing(
                    progress = if (running) progress else if (enabled) 1f else 0f,
                    size = 72.dp,
                    color = if (enabled) c.success else c.ink300,
                    center = {
                        Text(
                            if (running) "${(progress * 100).toInt()}%" else if (enabled) "Ready" else "Off",
                            style = AuroraType.monoM,
                            color = c.ink800,
                        )
                    },
                )
                Text(
                    if (running) "Encrypting ${(progress * 128).toInt()} of 128" else if (enabled) "Backup complete" else "Backup is off",
                    style = AuroraType.titleM,
                    color = c.ink900,
                )
                Text(
                    if (enabled) "Backed up $lastRun · Google Drive" else "Back up this vault",
                    style = AuroraType.bodyS,
                    color = c.ink500,
                )
            }
        }

        SettingsSection("Schedule") {
            SettingsToggleRow(
                "Encrypted backup",
                checked = enabled,
                onCheckedChange = { next -> if (next) enabled = true else showDisable = true },
            )
            SettingsToggleRow("Wi-Fi only", checked = wifiOnly, onCheckedChange = { wifiOnly = it }, enabled = enabled)
            SettingsToggleRow("Include videos", checked = includeVideos, onCheckedChange = { includeVideos = it }, enabled = enabled)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AuroraButton(
                if (running) "Backing up..." else "Backup now",
                onClick = { if (!running) running = true },
                enabled = enabled && !running,
                modifier = Modifier.weight(1f),
            )
            AuroraButton(
                "Restore on a new device",
                onClick = onRestore,
                variant = AuroraButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
        }

        SettingsSection("Backup details") {
            SettingsRow("Last 5 backups", subtitle = "Time, size and status", onClick = { showBackups = true })
            SettingsRow("Backup key", subtitle = "Requires biometric or pattern", onClick = { showKey = true })
            SettingsRow(
                "Delete cloud data",
                subtitle = "Local vault stays intact",
                onClick = { showDelete = true },
                icon = Icons.Filled.Delete,
            )
        }

        HonestyCard(
            "Google Drive sees ciphertext, padded sizes and timing only. Backup failures never block " +
                "unlocking or viewing your vault.",
        )
    }

    if (showBackups) {
        TextDocDialog(
            title = "Last 5 backups",
            body = "Just now - 1.9 GB - complete\n2 hours ago - 1.8 GB - complete\n" +
                "1 day ago - 1.8 GB - complete\n2 days ago - 1.7 GB - partial\n3 days ago - 1.6 GB - complete",
            onDismiss = { showBackups = false },
        )
    }
    if (showKey) {
        TextDocDialog(
            title = "Backup key",
            body = "The backup key is derived from your recovery phrase. It is shown only after " +
                "re-authentication and is never uploaded.",
            onDismiss = { showKey = false },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "Delete cloud data?",
            body = "Your vault stays on this device. This cannot be undone.",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = { enabled = false; showDelete = false; lastRun = "Off" },
            onDismiss = { showDelete = false },
        )
    }
    if (showDisable) {
        ConfirmDialog(
            title = "Turn off backup?",
            body = "Your encrypted copies stay in Google Drive until you delete them.",
            confirmLabel = "Turn off",
            danger = false,
            onConfirm = { enabled = false; showDisable = false; lastRun = "Off" },
            onDismiss = { showDisable = false },
        )
    }
}

// ---------------------------------------------------------------------------
// 5.2 Device sync
// ---------------------------------------------------------------------------

@Composable
fun CloudSyncScreen(
    onBack: () -> Unit,
    onOpenLog: () -> Unit,
) {
    val c = AuroraTheme.colors
    var syncEnabled by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(true) }
    var alias by remember { mutableStateOf("This phone") }
    var showAdd by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var showDisconnect by remember { mutableStateOf(false) }
    var conflicts by remember { mutableIntStateOf(2) }
    var rename by remember { mutableStateOf(false) }
    var aliasDraft by remember { mutableStateOf(alias) }

    SettingsPage(title = "Device sync", onBack = onBack) {
        AuroraGlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(c.iris100),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Devices, contentDescription = null, tint = c.iris700)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("2 devices", style = AuroraType.titleL, color = c.ink900)
                    Text("Last synced 4 minutes ago", style = AuroraType.bodyS, color = c.ink500)
                }
                AuroraChip(label = "Pro", selected = false, onClick = {})
            }
        }

        if (conflicts > 0) {
            SettingsRow(
                title = "$conflicts items need review",
                subtitle = "Merge resolved automatically where possible",
                value = "Review",
                onClick = { showReview = true },
                tint = c.tintButter,
                icon = Icons.Filled.Warning,
            )
        }

        SettingsSection("This device") {
            SettingsToggleRow("Device sync", checked = syncEnabled, onCheckedChange = { syncEnabled = it })
            SettingsToggleRow("Wi-Fi only", checked = wifiOnly, onCheckedChange = { wifiOnly = it }, enabled = syncEnabled)
            SettingsRow("Device name", value = alias, onClick = { rename = true })
            SettingsRow("Add a device", icon = Icons.Filled.Add, onClick = { showAdd = true })
            SettingsRow("Activity log", icon = Icons.Filled.Sync, onClick = onOpenLog)
        }

        SettingsSection("Danger zone") {
            SettingsRow(
                "Disconnect this device",
                subtitle = "Stops upload and download locally",
                icon = Icons.Filled.Delete,
                onClick = { showDisconnect = true },
            )
        }
        HonestyCard(
            "Only your own devices carry this vault. There is no recipient account and no " +
                "server-side vault catalog.",
        )
    }

    if (showAdd) {
        TextDocDialog(
            title = "Add a device",
            body = "Install Vaultaire on the other device, choose \"Restore from phrase\" and enter " +
                "your 12-word recovery phrase. The new device enrolls on first sync.",
            onDismiss = { showAdd = false },
        )
    }
    if (showReview) {
        TextDocDialog(
            title = "Review changes",
            body = "2 items changed on two devices.\n\nKeep added (1)\nKeep deleted (1)\n\n" +
                "Ignoring this list does not lose data; merge resolves on its own.",
            onDismiss = { showReview = false; conflicts = 0 },
        )
    }
    if (showDisconnect) {
        ConfirmDialog(
            title = "Disconnect this device?",
            body = "Sync stops immediately. Encrypted changes on other devices are unaffected.",
            confirmLabel = "Disconnect",
            danger = true,
            onConfirm = { syncEnabled = false; showDisconnect = false },
            onDismiss = { showDisconnect = false },
        )
    }
    if (rename) {
        Dialog(onDismissRequest = { rename = false }) {
            val cc = AuroraTheme.colors
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(AuroraShapes.xl)
                    .background(cc.solidRaised)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Device name", style = AuroraType.titleL, color = cc.ink900)
                BasicTextField(
                    value = aliasDraft,
                    onValueChange = { aliasDraft = it },
                    textStyle = AuroraType.bodyM.copy(color = cc.ink900),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AuroraShapes.sm)
                        .background(cc.ink050)
                        .border(1.dp, cc.border, AuroraShapes.sm)
                        .padding(14.dp),
                )
                AuroraButton("Save", {
                    alias = aliasDraft.ifBlank { "This phone" }
                    rename = false
                }, modifier = Modifier.fillMaxWidth())
                AuroraButton("Cancel", { rename = false }, variant = AuroraButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5.3 Sync activity log
// ---------------------------------------------------------------------------

private data class SyncEntry(val time: String, val device: String, val action: String, val detail: String, val kind: String)

@Composable
fun SyncLogScreen(onBack: () -> Unit) {
    val c = AuroraTheme.colors
    val entries = remember {
        listOf(
            SyncEntry("14:32", "This phone", "Uploaded", "4 changes", "Upload"),
            SyncEntry("14:28", "Pixel 8", "Downloaded", "2 changes", "Download"),
            SyncEntry("13:02", "This phone", "Conflict resolved", "1 item kept added", "Conflict"),
            SyncEntry("12:40", "Pixel 8", "Uploaded", "7 changes", "Upload"),
            SyncEntry("09:15", "Tablet", "Enrolled", "Device joined", "Device"),
        )
    }
    var filter by remember { mutableIntStateOf(0) }
    val filters = listOf("All", "Uploads", "Downloads", "Conflicts")
    val visible = entries.filter {
        when (filter) {
            1 -> it.kind == "Upload"
            2 -> it.kind == "Download"
            3 -> it.kind == "Conflict"
            else -> true
        }
    }

    SettingsPage(title = "Sync log", onBack = onBack) {
        SegmentedOptions(filters, filter, { filter = it })
        SettingsSection(null) {
            if (visible.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Sync, contentDescription = null, tint = c.ink300)
                    Spacer(Modifier.height(8.dp))
                    Text("No events in this filter", style = AuroraType.bodyM, color = c.ink500)
                }
            }
            visible.forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(entry.time, style = AuroraType.monoS, color = c.ink500, modifier = Modifier.width(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${entry.action} - ${entry.device}", style = AuroraType.bodyM, color = c.ink900)
                        Text(entry.detail, style = AuroraType.bodyS, color = c.ink500)
                    }
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (entry.kind == "Conflict") c.tintButter else c.ink050)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(entry.kind, style = AuroraType.monoS, color = c.ink700)
                    }
                }
            }
        }
        HonestyCard("The log stores opaque ids and counts only. File names, vault labels and record ids never appear.")
    }
}

// ---------------------------------------------------------------------------
// 6.1 Appearance
// ---------------------------------------------------------------------------

@Composable
fun AppearanceScreen(
    onBack: () -> Unit,
    onOpenIcons: () -> Unit,
) {
    val c = AuroraTheme.colors
    var theme by remember { mutableIntStateOf(2) }
    var surface by remember { mutableIntStateOf(0) }
    var reduceMotion by remember { mutableStateOf(false) }
    var reduceTransparency by remember { mutableStateOf(false) }
    var haptics by remember { mutableStateOf(true) }
    var hidePreviews by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(profileName) }
    var showTextSize by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }

    SettingsPage(title = "Appearance", onBack = onBack) {
        Text("Preview", style = AuroraType.overline, color = c.ink500, modifier = Modifier.padding(start = 4.dp))
        AuroraGlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (profileName.isBlank()) "Good evening" else "Good evening, $profileName",
                    style = AuroraType.headlineM,
                    color = c.ink900,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(AuroraShapes.md)
                                .background(if (index == 0) c.iris100 else c.ink100),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (index == 0) Icons.Filled.PhotoCamera else Icons.Filled.Image,
                                contentDescription = null,
                                tint = c.ink400,
                            )
                        }
                    }
                }
                Text(
                    if (surface == 1) "Solid surface - no blur, full contrast"
                    else "Glass surface - translucent panels over orbit",
                    style = AuroraType.bodyS,
                    color = c.ink500,
                )
            }
        }

        SettingsSection("Theme") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance mode", style = AuroraType.bodyM, color = c.ink900)
                SegmentedOptions(listOf("Light", "Night", "Auto"), theme, { theme = it })
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Surface", style = AuroraType.bodyM, color = c.ink900)
                SegmentedOptions(listOf("Glass", "Solid", "Auto"), surface, { surface = it })
            }
        }

        SettingsSection("Motion & transparency") {
            SettingsToggleRow("Reduce motion", checked = reduceMotion, onCheckedChange = { reduceMotion = it })
            SettingsToggleRow(
                "Reduce transparency",
                subtitle = "Promotes every glass fill to solid",
                checked = reduceTransparency,
                onCheckedChange = { reduceTransparency = it },
            )
            SettingsToggleRow("Haptics", checked = haptics, onCheckedChange = { haptics = it })
            SettingsToggleRow("Hide previews in recents", checked = hidePreviews, onCheckedChange = { hidePreviews = it })
        }

        SettingsSection("Device") {
            SettingsRow("Text size", value = "System", onClick = { showTextSize = true })
            SettingsRow("App language", value = "English", onClick = { showLanguage = true })
            SettingsRow("Profile name", value = profileName.ifBlank { "Not set" }, onClick = { nameDraft = profileName; editName = true })
            SettingsRow("App icon", subtitle = "Launcher alias only", badge = "PRO", onClick = onOpenIcons)
        }

        HonestyCard(
            "Theme and surface are cosmetic. No information, ordering or security affordance changes " +
                "between Glass, Solid and Night.",
        )
    }

    if (showTextSize) {
        TextDocDialog(
            title = "Text size",
            body = "Text size follows the system setting. Open Settings > Display > Font size on this device.",
            onDismiss = { showTextSize = false },
        )
    }
    if (showLanguage) {
        TextDocDialog(
            title = "App language",
            body = "Choose a language for the whole device. Vaultaire follows the system locale and " +
                "never syncs a language choice.",
            onDismiss = { showLanguage = false },
        )
    }
    if (editName) {
        Dialog(onDismissRequest = { editName = false }) {
            val cc = AuroraTheme.colors
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(AuroraShapes.xl)
                    .background(cc.solidRaised)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Profile name", style = AuroraType.titleL, color = cc.ink900)
                Text(
                    "Optional and local only. Never synced, never shown in notifications or recents.",
                    style = AuroraType.bodyS,
                    color = cc.ink500,
                )
                BasicTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    textStyle = AuroraType.bodyM.copy(color = cc.ink900),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AuroraShapes.sm)
                        .background(cc.ink050)
                        .border(1.dp, cc.border, AuroraShapes.sm)
                        .padding(14.dp),
                    decorationBox = { inner ->
                        if (nameDraft.isEmpty()) {
                            Text("Your name", style = AuroraType.bodyM, color = cc.ink400)
                        }
                        inner()
                    },
                )
                AuroraButton("Save", {
                    profileName = nameDraft.trim()
                    editName = false
                }, modifier = Modifier.fillMaxWidth())
                AuroraButton("Cancel", { editName = false }, variant = AuroraButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 6.2 App icons
// ---------------------------------------------------------------------------

private data class IconChoice(val label: String, val icon: ImageVector, val pro: Boolean)

@Composable
fun IconPackScreen(onBack: () -> Unit) {
    val c = AuroraTheme.colors
    val choices = remember {
        listOf(
            IconChoice("Vaultaire", Icons.Filled.Lock, false),
            IconChoice("Notes", Icons.Filled.Description, false),
            IconChoice("Photos", Icons.Filled.PhotoCamera, false),
            IconChoice("Calculator", Icons.Filled.Calculate, true),
            IconChoice("Wallet", Icons.Filled.Wallet, true),
            IconChoice("Weather", Icons.Filled.WbSunny, true),
        )
    }
    var selected by remember { mutableIntStateOf(0) }
    var pending by remember { mutableIntStateOf(0) }
    var applied by remember { mutableStateOf(false) }

    SettingsPage(title = "App icon", onBack = onBack) {
        Text(
            "The icon is a launcher alias. The app never shows the disguise inside its own UI.",
            style = AuroraType.bodyM,
            color = c.ink600,
        )
        SettingsSection(null) {
            choices.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { choice ->
                        val index = choices.indexOf(choice)
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(AuroraShapes.md)
                                .background(if (pending == index) c.iris050 else Color.Transparent)
                                .border(
                                    if (pending == index) 2.dp else 1.dp,
                                    if (pending == index) c.iris500 else c.border,
                                    AuroraShapes.md,
                                )
                                .clickable { pending = index }
                                .padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(48.dp).clip(AuroraShapes.sm).background(c.ink100),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(choice.icon, contentDescription = null, tint = c.ink700)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(choice.label, style = AuroraType.bodyS, color = c.ink700)
                            if (choice.pro) {
                                Text("PRO", style = AuroraType.monoS, color = c.iris600)
                            }
                        }
                    }
                }
            }
        }
        AuroraButton(
            if (applied) "Icon applied" else "Apply icon",
            onClick = { selected = pending; applied = true },
            enabled = pending != selected || !applied,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Current alias: ${choices[selected].label}",
            style = AuroraType.monoM,
            color = c.ink500,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        HonestyCard(
            "Changing the alias never flashes the Vaultaire brand. An already-changed icon keeps " +
                "working if Pro lapses.",
        )
    }
}

// ---------------------------------------------------------------------------
// 6.3 Paywall
// ---------------------------------------------------------------------------

@Composable
fun ProScreen(onBack: () -> Unit) {
    val c = AuroraTheme.colors
    var yearly by remember { mutableStateOf(true) }
    var subscribed by remember { mutableStateOf(false) }

    SettingsPage(title = "Vaultaire Pro", onBack = onBack) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(c.iris100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = c.iris600, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("Everything stays private. Pro adds safety and reach.", style = AuroraType.bodyM, color = c.ink600, textAlign = TextAlign.Center)
        }

        SettingsSection("Included") {
            ProFeature("Duress mode", "A decoy pattern that wipes the rest, silently.")
            ProFeature("Device sync", "Your own devices carry the same vault.")
            ProFeature("Every app icon", "Six launcher aliases, swap anytime.")
            ProFeature("Conflict review", "Resolve concurrent edits explicitly.")
        }

        SettingsSection("Plan") {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AuroraChip("Yearly - $24.99", selected = yearly, onClick = { yearly = true }, modifier = Modifier.weight(1f))
                AuroraChip("Monthly - $2.99", selected = !yearly, onClick = { yearly = false }, modifier = Modifier.weight(1f))
            }
            SettingsValueRow("Trial", if (yearly) "7 days free, then $24.99 / year" else "7 days free, then $2.99 / month")
        }

        AuroraButton(
            if (subscribed) "Pro active" else "Start 7-day trial",
            onClick = { subscribed = true },
            enabled = !subscribed,
            modifier = Modifier.fillMaxWidth(),
        )
        AuroraButton(
            "Restore purchase",
            onClick = { subscribed = true },
            variant = AuroraButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
        )

        if (subscribed) {
            AuroraGlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = c.success)
                    Spacer(Modifier.width(10.dp))
                    Text("Pro is active on this device.", style = AuroraType.bodyM, color = c.ink800)
                }
            }
        }

        Text(
            "No gate ever hides existing data. Backup is free for everyone. Duress and an " +
                "already-changed icon keep working if Pro lapses.",
            style = AuroraType.bodyS,
            color = c.ink400,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProFeature(title: String, body: String) {
    val c = AuroraTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = c.success, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = AuroraType.bodyM, color = c.ink900)
            Text(body, style = AuroraType.bodyS, color = c.ink500)
        }
    }
}

// ---------------------------------------------------------------------------
// Shared confirm dialog
// ---------------------------------------------------------------------------

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    danger: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = AuroraTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(AuroraShapes.xl)
                .background(c.solidRaised)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = AuroraType.titleL, color = c.ink900)
            Text(body, style = AuroraType.bodyM, color = c.ink600)
            AuroraButton(
                confirmLabel,
                onConfirm,
                variant = if (danger) AuroraButtonVariant.Danger else AuroraButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            AuroraButton("Cancel", onDismiss, variant = AuroraButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
        }
    }
}
