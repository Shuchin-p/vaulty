package com.vaultaire.android.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraPatternGrid
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraType
import com.vaultaire.android.ui.screens.settings.components.HonestyCard
import com.vaultaire.android.ui.screens.settings.components.InfoCard
import com.vaultaire.android.ui.screens.settings.components.SettingsPage
import com.vaultaire.android.ui.screens.settings.components.SettingsRow
import com.vaultaire.android.ui.screens.settings.components.SettingsSection
import com.vaultaire.android.ui.screens.settings.components.SettingsToggleRow

// ---------------------------------------------------------------------------
// 4.3 Duress setup
// ---------------------------------------------------------------------------

@Composable
fun DuressSettingsScreen(
    onBack: () -> Unit,
    onOpenGuidance: () -> Unit,
) {
    val c = AuroraTheme.colors
    var stage by remember { mutableIntStateOf(0) }
    var enabled by remember { mutableStateOf(false) }

    SettingsPage(title = "Duress", onBack = onBack) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(AuroraShapes.lg)
                .background(c.tintRose)
                .border(1.dp, c.tintRoseDeep.copy(alpha = 0.3f), AuroraShapes.lg)
                .padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = c.ink800, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                if (enabled) {
                    "Duress is enabled. If you are forced to unlock, the duress pattern opens one " +
                        "vault and erases the others."
                } else {
                    "If you're forced to unlock, this pattern opens one vault and erases the rest."
                },
                style = AuroraType.bodyM,
                color = c.ink800,
            )
        }

        if (enabled) {
            AuroraGlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(28.dp))
                    Text("Duress is enabled. Hold to disable.", style = AuroraType.titleM, color = c.ink900)
                    Text(
                        "Disabling requires your pattern and biometrics. Erasure history cannot be undone.",
                        style = AuroraType.bodyS,
                        color = c.ink500,
                    )
                    HoldToEnable(label = "Hold to disable duress", onComplete = { enabled = false })
                }
            }
        } else if (stage == 0) {
            AuroraGlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Draw the decoy vault pattern", style = AuroraType.titleM, color = c.ink900)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "There is no vault list. The pattern itself selects the vault.",
                        style = AuroraType.bodyS,
                        color = c.ink500,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    AuroraPatternGrid(
                        modifier = Modifier.size(280.dp),
                        onPatternComplete = { dots ->
                            if (dots.size >= 4) stage = 1
                        },
                    )
                }
            }
        } else {
            AuroraGlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pattern recognized", style = AuroraType.titleM, color = c.ink900)
                    Text(
                        "Hold for two seconds to enable duress for this vault.",
                        style = AuroraType.bodyM,
                        color = c.ink600,
                    )
                    HoldToEnable(onComplete = { enabled = true })
                    AuroraButton(
                        "Draw again",
                        onClick = { stage = 0 },
                        variant = AuroraButtonVariant.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SettingsSection("Understand the limits") {
            SettingsRow(
                "Making it look lived-in",
                subtitle = "Honest guidance on decoys",
                onClick = onOpenGuidance,
            )
        }
        HonestyCard(
            "Duress setup is silent by design. A failed wipe is never surfaced inside the app, " +
                "because showing it would reveal the duress pattern.",
        )
    }
}

@Composable
private fun HoldToEnable(
    onComplete: () -> Unit,
    label: String = "Hold to enable duress",
) {
    val c = AuroraTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var completed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (pressed && !completed) 1f else 0f,
        animationSpec = tween(durationMillis = if (pressed && !completed) 2000 else 250),
        label = "hold",
    )
    LaunchedEffect(pressed) {
        if (pressed && !completed) {
            kotlinx.coroutines.delay(2000)
            if (!completed) {
                completed = true
                onComplete()
            }
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(c.iris600)
            .then(
                Modifier.clickableNoIndication(interaction),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(52.dp)
                .clip(CircleShape)
                .background(c.iris700),
        )
        Text(
            if (progress > 0f && progress < 1f) "Keep holding - ${(progress * 100).toInt()}%" else label,
            style = AuroraType.labelL,
            color = c.solid,
        )
    }
}

private fun Modifier.clickableNoIndication(interaction: MutableInteractionSource): Modifier =
    this.clickable(
        interactionSource = interaction,
        indication = null,
        onClick = {},
    )

// ---------------------------------------------------------------------------
// 4.4 Decoy guidance
// ---------------------------------------------------------------------------

@Composable
fun DuressGuidanceScreen(onBack: () -> Unit) {
    SettingsPage(title = "Guidance", onBack = onBack) {
        Text("Making it look lived-in", style = AuroraType.headlineM, color = AuroraTheme.colors.ink900)
        Text(
            "If someone forces you to unlock, the duress pattern opens a single vault. " +
                "That vault should look like the one you actually use.",
            style = AuroraType.bodyM,
            color = AuroraTheme.colors.ink600,
        )
        SettingsSection("Practical habits") {
            GuidanceRow("Add ordinary photos", "A decoy with three files invites questions. Keep a normal camera roll flowing.")
            GuidanceRow("Use a realistic amount", "Match the amount of storage a person like you would actually have.")
            GuidanceRow("Avoid 'fake' names", "Name collections the way you name real ones. Never label anything as a decoy.")
            GuidanceRow("Open it sometimes", "A vault that is never opened leaves its own pattern in recents and habits.")
        }
        HonestyCard(
            "If someone knows your normal vault exists, a perfect decoy may still not convince them. " +
                "Duress reduces coercion risk; it does not eliminate it.",
        )
    }
}

@Composable
private fun GuidanceRow(title: String, body: String) {
    val c = AuroraTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(c.iris100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.iris700, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = AuroraType.bodyM, color = c.ink900)
            Text(body, style = AuroraType.bodyS, color = c.ink500)
        }
    }
}

// ---------------------------------------------------------------------------
// 4.5 Hidden vaults explainer
// ---------------------------------------------------------------------------

@Composable
fun HiddenVaultsScreen(onBack: () -> Unit) {
    SettingsPage(title = "Hidden vaults", onBack = onBack) {
        Text("One app, many vaults", style = AuroraType.headlineM, color = AuroraTheme.colors.ink900)
        Text(
            "Different patterns resolve different vaults. No screen ever lists them, counts them, " +
                "or shows their names.",
            style = AuroraType.bodyM,
            color = AuroraTheme.colors.ink600,
        )
        SettingsSection("How it works") {
            GuidanceRow("Patterns, not profiles", "The pattern you draw selects the vault. There is no account chooser.")
            GuidanceRow("No catalog", "Vaults cannot be renamed, reordered, or browsed from inside the app.")
            GuidanceRow("Collections do the organizing", "Use collection names inside a vault instead of vault labels.")
        }
        HonestyCard(
            "A forensic examiner with file access may still estimate how many vaults exist. " +
                "Hidden vaults hide the catalog, not the storage.",
        )
    }
}

// ---------------------------------------------------------------------------
// 4.6 Security info
// ---------------------------------------------------------------------------

@Composable
fun SecurityInfoScreen(onBack: () -> Unit) {
    val c = AuroraTheme.colors
    var doc by remember { mutableStateOf<String?>(null) }
    SettingsPage(title = "Security info", onBack = onBack) {
        Text("What Vaultaire protects", style = AuroraType.headlineM, color = c.ink900)
        SettingsSection(null) {
            GuidanceRow("Coercion", "Duress opens a decoy and wipes the other vaults silently.")
            GuidanceRow("Device theft", "Files are encrypted at rest; the pattern derives the key.")
            GuidanceRow("Cloud leakage", "Drive stores ciphertext, padded sizes and timing only.")
        }
        Text("What it does not protect", style = AuroraType.headlineM, color = c.ink900)
        SettingsSection(null) {
            GuidanceRow("Vault-count estimation", "A forensic examiner may infer how many vaults exist.")
            GuidanceRow("Flash residue", "A wipe cannot guarantee every physical block is gone immediately.")
            GuidanceRow("Offline guessing", "A copied index can be attacked offline; use a strong pattern.")
            GuidanceRow("Lost credentials", "Pattern plus phrase are the only way back in. There is no reset.")
        }
        HonestyCard(
            "FLAG_SECURE blocks screenshots, screen recording and casting for secure screens. " +
                "It cannot stop a camera pointed at your screen.",
        )
        AuroraButton(
            "Read the full threat model",
            onClick = {
                doc = "Vaultaire threat model\n\n" +
                    "Protected: coercion, device theft, cloud leakage, shoulder-surfing of secure screens.\n\n" +
                    "Not protected: forensic vault-count estimation, flash residue after a wipe, " +
                    "offline pattern guessing, and permanent loss when both pattern and phrase are forgotten.\n\n" +
                    "No analytics. No account. No plaintext leaves this device."
            },
            variant = AuroraButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "No military-grade claims. These are the actual properties of the implementation.",
            style = AuroraType.bodyS,
            color = c.ink400,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    doc?.let { TextDocDialog(title = "Threat model", body = it, onDismiss = { doc = null }) }
}

// ---------------------------------------------------------------------------
// 4.7 Privacy
// ---------------------------------------------------------------------------

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    var crash by remember { mutableStateOf(false) }
    var diagnostics by remember { mutableStateOf(false) }
    var activityLog by remember { mutableStateOf(true) }
    var hidePreviews by remember { mutableStateOf(true) }
    var exportState by remember { mutableIntStateOf(0) } // 0 idle, 1 creating, 2 saved
    var clearLog by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }

    LaunchedEffect(exportState) {
        if (exportState == 1) {
            kotlinx.coroutines.delay(1200)
            exportState = 2
        }
    }

    SettingsPage(title = "Privacy", onBack = onBack) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
            Text(
                "Vaultaire has no server that can read your vault. There is no account to delete.",
                style = AuroraType.headlineM,
                color = AuroraTheme.colors.ink900,
            )
        }
        SettingsSection("Diagnostics") {
            SettingsToggleRow(
                "Crash reports",
                checked = crash,
                onCheckedChange = { crash = it },
                subtitle = "Off by default. Never contains vault content.",
            )
            SettingsToggleRow(
                "Diagnostics",
                checked = diagnostics,
                onCheckedChange = { diagnostics = it },
                subtitle = "Off by default. Build, vault size and sync state only.",
            )
            SettingsToggleRow(
                "Local activity log",
                checked = activityLog,
                onCheckedChange = { next ->
                    if (next) activityLog = true else clearLog = true
                },
                subtitle = "On by default. Stays on this device.",
            )
        }
        SettingsSection("On-screen privacy") {
            SettingsToggleRow(
                "Hide previews in recents",
                checked = hidePreviews,
                onCheckedChange = { hidePreviews = it },
                subtitle = "Recents snapshot is replaced with a solid surface.",
            )
            SettingsRow("Clipboard auto-clear", value = "30 seconds")
            SettingsRow("Screen capture", value = "Locked on for secure screens")
        }
        SettingsSection("Your data") {
            SettingsRow(
                "Export my data",
                subtitle = when (exportState) {
                    1 -> "Creating encrypted archive..."
                    2 -> "Archive saved to Files"
                    else -> "AES-256-GCM, sealed with your phrase"
                },
                onClick = { if (exportState == 0) showExport = true },
            )
            SettingsRow(
                "Clear local activity log",
                subtitle = "Removes on-device history only",
                onClick = { clearLog = true },
            )
            SettingsRow("Delete all local data", subtitle = "Requires pattern and confirmation")
        }
        HonestyCard(
            "There is no analytics SDK, no advertising ID and no third-party trackers. Export " +
                "produces an encrypted archive only; plaintext is never written to shared storage.",
        )
    }

    if (showExport) {
        ConfirmDialog2(
            title = "Export my data?",
            body = "Export creates an encrypted archive. Save to Files?",
            confirmLabel = "Export",
            onConfirm = { showExport = false; exportState = 1 },
            onDismiss = { showExport = false },
        )
    }
    if (clearLog) {
        ConfirmDialog2(
            title = "Clear local activity log?",
            body = "This can't be undone. Vault files and settings are not affected.",
            confirmLabel = "Clear",
            onConfirm = { clearLog = false; activityLog = false },
            onDismiss = { clearLog = false },
        )
    }
}

@Composable
private fun ConfirmDialog2(
    title: String,
    body: String,
    confirmLabel: String,
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
            AuroraButton(confirmLabel, onConfirm, modifier = Modifier.fillMaxWidth())
            AuroraButton("Cancel", onDismiss, variant = AuroraButtonVariant.Ghost, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------------------
// 4.8 About
// ---------------------------------------------------------------------------

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val c = AuroraTheme.colors
    var doc by remember { mutableStateOf<Pair<String, String>?>(null) }
    var diagnostics by remember { mutableStateOf(false) }
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableStateOf(0L) }
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsPage(title = "About", onBack = onBack) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(c.iris100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = c.iris700, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Vaultaire", style = AuroraType.headlineL, color = c.ink900)
            Text("Encrypted vault - 1.0.0 (1)", style = AuroraType.monoM, color = c.ink500)
        }
        SettingsSection("Documents") {
            SettingsRow("Open-source licenses", onClick = {
                doc = "Licenses" to "Vaultaire bundles AndroidX, Kotlin, CameraX, Media3, Coil, " +
                    "Room, Hilt, Google Drive API and Firebase Messaging under their respective " +
                    "open-source licenses. Full texts ship in the APK assets."
            })
            SettingsRow("Privacy policy", onClick = {
                doc = "Privacy policy" to "Vaultaire collects nothing. There is no account, no analytics " +
                    "and no telemetry. Cloud transport sees ciphertext, padded sizes and timing only."
            })
            SettingsRow("Security architecture", onClick = {
                doc = "Security architecture" to "Pattern -> Argon2id-derived master key -> envelope keys " +
                    "per vault. Files are chunked and encrypted with AES-256-GCM. Keys live in memory only " +
                    "and are wiped on lock."
            })
            SettingsRow("Threat model summary", onClick = {
                doc = "Threat model" to "Protects against coercion, device theft and cloud leakage. " +
                    "Does not protect against forensic vault-count estimation, flash residue after a wipe, " +
                    "offline guessing against a copied index, or forgotten credentials."
            })
        }
        SettingsSection("Build") {
            SettingsRow(
                "Version",
                value = "1.0.0 (1)",
                onClick = {
                    val now = System.currentTimeMillis()
                    taps = if (now - lastTap <= 1000) taps + 1 else 1
                    lastTap = now
                    if (taps >= 3) {
                        taps = 0
                        diagnostics = true
                        android.widget.Toast.makeText(
                            context,
                            "build=1.0.0-debug - vault=2.1 GB - sync=idle",
                            android.widget.Toast.LENGTH_LONG,
                        ).show()
                    }
                },
            )
            SettingsRow("Build type", value = "debug")
        }
        if (diagnostics) {
            AuroraGlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Local diagnostics", style = AuroraType.titleM, color = c.ink900)
                    Text("build=1.0.0-debug  vault=2.1 GB  sync=idle", style = AuroraType.monoS, color = c.ink600)
                    Text("Never includes keys, phrases, file names or pattern data.", style = AuroraType.bodyS, color = c.ink500)
                }
            }
        }
        Text(
            "Zero-knowledge by design. Your pattern is the only key.",
            style = AuroraType.bodyS,
            color = c.ink400,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    doc?.let { (title, body) ->
        TextDocDialog(title = title, body = body, onDismiss = { doc = null })
    }
}

@Composable
fun TextDocDialog(title: String, body: String, onDismiss: () -> Unit) {
    val c = AuroraTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(AuroraShapes.xl)
                .background(c.solidRaised)
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = AuroraType.titleL, color = c.ink900)
                Text(body, style = AuroraType.bodyM, color = c.ink600)
                AuroraButton("Close", onDismiss, variant = AuroraButtonVariant.Secondary, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
