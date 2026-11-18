package com.vaultaire.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraType
import com.vaultaire.android.ui.screens.settings.components.HonestyCard
import com.vaultaire.android.ui.screens.settings.components.SettingsPage
import com.vaultaire.android.ui.screens.settings.components.SettingsPageHeader
import com.vaultaire.android.ui.screens.settings.components.SettingsRow
import com.vaultaire.android.ui.screens.settings.components.SettingsSection
import com.vaultaire.android.ui.screens.settings.components.SettingsToggleRow
import com.vaultaire.android.ui.screens.settings.components.SettingsValueRow

// ---------------------------------------------------------------------------
// 4.1 Settings hub
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPro: () -> Unit,
) {
    val c = AuroraTheme.colors
    var backupOn by remember { mutableStateOf(true) }
    SettingsPage(title = "Settings", onBack = onBack) {
        AuroraGlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(c.iris100),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = c.iris700, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Vault", style = AuroraType.titleL, color = c.ink900)
                    Text("Pro · 2.1 GB", style = AuroraType.bodyS, color = c.ink500)
                }
                AuroraChip(label = "Pro", selected = false, onClick = onOpenPro, leadingIcon = Icons.Filled.Star)
            }
        }

        SettingsSection("Security") {
            SettingsRow("Security", subtitle = "Pattern, duress, hidden vaults", icon = Icons.Filled.Security, onClick = onOpenSecurity)
            SettingsRow("Recovery phrase", subtitle = "Reveal after re-authentication", icon = Icons.Filled.Visibility, onClick = onOpenSecurity)
        }

        SettingsSection("Cloud") {
            SettingsRow(
                "Encrypted backup",
                subtitle = if (backupOn) "2 hours ago · 1.8 GB" else "Off",
                icon = Icons.Filled.CloudUpload,
                onClick = onOpenBackup,
            )
            SettingsRow(
                "Device sync",
                subtitle = "3 devices · Synced 2m ago",
                badge = "PRO",
                icon = Icons.Filled.Devices,
                onClick = onOpenSync,
            )
        }

        SettingsSection("Appearance") {
            SettingsRow("Theme & surface", subtitle = "Auto · Glass", icon = Icons.Filled.Palette, onClick = onOpenAppearance)
        }

        SettingsSection("Legal & info") {
            SettingsRow("Privacy", icon = Icons.Filled.PrivacyTip, onClick = onOpenPrivacy)
            SettingsRow("About Vaultaire", icon = Icons.Filled.Info, onClick = onOpenAbout)
            com.vaultaire.android.ui.screens.settings.components.DiagnosticsVersionRow()
        }

        AuroraGlassCard(Modifier.fillMaxWidth(), onClick = onOpenPro) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = c.iris600)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Vaultaire Pro", style = AuroraType.titleM, color = c.ink900)
                    Text("Duress mode, device sync and every app icon", style = AuroraType.bodyS, color = c.ink500)
                }
                Text("View", style = AuroraType.labelM, color = c.iris600)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4.2 Security settings
// ---------------------------------------------------------------------------

@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    onOpenDuress: () -> Unit,
    onOpenHiddenVaults: () -> Unit,
    onOpenSecurityInfo: () -> Unit,
    onChangePattern: () -> Unit = {},
    onRevealPhrase: () -> Unit = {},
) {
    var autoLock by remember { mutableStateOf(1) }
    var clipboard by remember { mutableStateOf(1) }
    var biometric by remember { mutableStateOf(true) }
    var showAutoLock by remember { mutableStateOf(false) }
    var showClipboard by remember { mutableStateOf(false) }
    var showInterstitial by remember { mutableStateOf(false) }
    val autoLockLabels = listOf("Immediately", "30s", "1m", "5m")
    val clipboardLabels = listOf("30s", "60s", "Off")

    SettingsPage(title = "Security settings", onBack = onBack) {
        SettingsSection("Unlock") {
            SettingsRow("Change pattern", subtitle = "Creates a new recovery phrase", onClick = { showInterstitial = true })
            SettingsRow("Recovery phrase", subtitle = "Reveal requires re-authentication", onClick = onRevealPhrase)
            SettingsToggleRow(
                title = "Biometric unlock",
                subtitle = "Pattern always remains required",
                checked = biometric,
                onCheckedChange = { biometric = it },
            )
        }
        SettingsSection("Protection") {
            SettingsRow("Duress mode", subtitle = "Decoy pattern opens one vault, erases the rest", badge = "PRO", onClick = onOpenDuress)
            SettingsRow("Hidden vaults", subtitle = "No catalog. Patterns resolve vaults.", onClick = onOpenHiddenVaults)
            SettingsRow("Auto-lock", value = autoLockLabels[autoLock], onClick = { showAutoLock = true })
            SettingsRow("Attempt throttle", value = "Tier 0", onClick = null)
            SettingsRow("Screen capture", value = "Locked on for secure screens", onClick = null)
            SettingsRow("Clipboard auto-clear", value = clipboardLabels[clipboard], onClick = { showClipboard = true })
            SettingsRow("Security info", subtitle = "Threat model and honest limits", onClick = onOpenSecurityInfo)
        }
        HonestyCard(
            "Changing your pattern invalidates the old recovery phrase. Your files stay encrypted " +
                "and are never re-uploaded.",
        )
    }

    if (showAutoLock) {
        ChoiceDialog(
            title = "Auto-lock after",
            options = autoLockLabels,
            selected = autoLock,
            onSelect = { autoLock = it; showAutoLock = false },
            onDismiss = { showAutoLock = false },
        )
    }
    if (showClipboard) {
        ChoiceDialog(
            title = "Clear clipboard after",
            options = clipboardLabels,
            selected = clipboard,
            onSelect = { clipboard = it; showClipboard = false },
            onDismiss = { showClipboard = false },
        )
    }
    if (showInterstitial) {
        FullScreenInterstitial(
            title = "Change your pattern?",
            body = "Changing your pattern creates a new recovery phrase. Your files stay encrypted.",
            confirmLabel = "Continue",
            onConfirm = { showInterstitial = false; onChangePattern() },
            onDismiss = { showInterstitial = false },
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = AuroraTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(AuroraShapes.xl)
                .background(c.solidRaised)
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = AuroraType.titleL, color = c.ink900)
                options.forEachIndexed { index, label ->
                    AuroraChip(
                        label = label,
                        selected = index == selected,
                        onClick = { onSelect(index) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(4.dp))
                AuroraButton("Cancel", onDismiss, variant = AuroraButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FullScreenInterstitial(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = AuroraTheme.colors
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AuroraBackground {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = c.iris600, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(18.dp))
                Text(title, style = AuroraType.headlineL, color = c.ink900)
                Spacer(Modifier.height(10.dp))
                Text(body, style = AuroraType.bodyL, color = c.ink600)
                Spacer(Modifier.height(28.dp))
                AuroraButton(confirmLabel, onConfirm, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                AuroraButton("Cancel", onDismiss, variant = AuroraButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
