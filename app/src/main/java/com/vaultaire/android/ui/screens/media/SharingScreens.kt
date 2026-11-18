package com.vaultaire.android.ui.screens.media

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.window.Dialog
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraDimens
import com.vaultaire.android.designsystem.AuroraEmptyState
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraPhraseGrid
import com.vaultaire.android.designsystem.AuroraSecureSurface
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTint
import com.vaultaire.android.designsystem.AuroraTopBar
import com.vaultaire.android.designsystem.AuroraType
import com.vaultaire.android.ui.screens.vault.VaultDemoData

/**
 * Data structure representing an active or revoked share.
 */
data class DemoShare(
    val id: String,
    val title: String,
    val expiryLabel: String,
    val opensLabel: String,
    val allowExport: Boolean,
    var isRevoked: Boolean = false,
)

/**
 * Shares hub screen listing active outgoing shares with revoke actions
 * and create affordance per plan/tests/screens/28-my-shares.md.
 */
@Composable
fun SharesScreen(
    onBack: () -> Unit,
    onOpenShare: (String) -> Unit,
    onCreateShare: () -> Unit,
    onOpenSharedVault: () -> Unit = {},
) {
    val c = AuroraTheme.colors
    val sharesList = remember {
        mutableStateListOf(
            DemoShare("share-1", "Travel documents", "Expires in 6 days", "1 of 3 opens used", false),
            DemoShare("share-2", "Project blueprint", "Expires in 28 days", "Unlimited opens", true),
        )
    }
    var shareToRevoke by remember { mutableStateOf<DemoShare?>(null) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AuroraTopBar(
                title = "Shares",
                eyebrow = "Secure Sharing",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
                actions = {
                    AuroraIconButton(
                        icon = Icons.Filled.LockOpen,
                        contentDescription = "Open with phrase",
                        onClick = onOpenSharedVault,
                    )
                    AuroraIconButton(
                        icon = Icons.Filled.Add,
                        contentDescription = "New share",
                        onClick = onCreateShare,
                    )
                },
            )

            if (sharesList.isEmpty()) {
                AuroraEmptyState(
                    title = "No active shares",
                    body = "Shares are encrypted with a 12-word phrase and can be revoked anytime.",
                    primaryLabel = "Share a vault",
                    onPrimary = onCreateShare,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AuroraDimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = "Outgoing Shares",
                            style = AuroraType.labelL,
                            color = c.ink700,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    items(sharesList) { share ->
                        AuroraGlassCard(
                            tint = if (share.isRevoked) AuroraTint.Rose else AuroraTint.Lilac,
                            raised = true,
                            onClick = { if (!share.isRevoked) onOpenShare(share.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = share.title,
                                        style = AuroraType.titleM,
                                        color = c.ink900,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (share.isRevoked) c.dangerSoft else c.surfaceInk.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = if (share.isRevoked) "Revoked" else "Active",
                                            style = AuroraType.labelM,
                                            color = if (share.isRevoked) c.danger else c.ink700,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = "${share.expiryLabel} · ${share.opensLabel}",
                                    style = AuroraType.bodyS,
                                    color = c.ink600,
                                )

                                if (!share.isRevoked) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        AuroraButton(
                                            text = "Revoke",
                                            variant = AuroraButtonVariant.Danger,
                                            onClick = { shareToRevoke = share },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // Bottom CTA actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AuroraDimens.screenPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AuroraButton(
                    text = "Open phrase",
                    variant = AuroraButtonVariant.Ghost,
                    onClick = onOpenSharedVault,
                    modifier = Modifier.weight(1f),
                )
                AuroraButton(
                    text = "New share",
                    onClick = onCreateShare,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Revoke Confirmation Dialog per T28-31
    if (shareToRevoke != null) {
        Dialog(onDismissRequest = { shareToRevoke = null }) {
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
                        text = "Revoke this share?",
                        style = AuroraType.headlineM,
                        color = c.danger,
                    )
                    Text(
                        text = "The recipient loses access the next time they're online. Files already exported stay exported.",
                        style = AuroraType.bodyM,
                        color = c.ink700,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        AuroraButton(
                            text = "Keep share",
                            variant = AuroraButtonVariant.Ghost,
                            onClick = { shareToRevoke = null },
                        )
                        Spacer(Modifier.width(8.dp))
                        AuroraButton(
                            text = "Revoke access",
                            variant = AuroraButtonVariant.Danger,
                            onClick = {
                                val target = shareToRevoke
                                if (target != null) {
                                    val index = sharesList.indexOfFirst { it.id == target.id }
                                    if (index >= 0) {
                                        sharesList[index] = target.copy(isRevoked = true)
                                    }
                                }
                                shareToRevoke = null
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Create share configuration screen with expiry, max opens, and export policy.
 * Implemented against plan/tests/screens/16-secure-sharing.md.
 */
@Composable
fun ShareCreateScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
) {
    val c = AuroraTheme.colors
    val expiryOptions = listOf("Never", "24 hours", "7 days", "30 days")
    var selectedExpiry by remember { mutableStateOf("7 days") }

    val opensOptions = listOf("1", "3", "5", "Unlimited")
    var selectedOpens by remember { mutableStateOf("3") }

    var allowExport by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AuroraTopBar(
                title = "Create share",
                eyebrow = "Policy Options",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AuroraDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Scope Card
                item {
                    AuroraGlassCard(
                        tint = AuroraTint.Sky,
                        raised = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Text(
                                text = "Sharing selected items",
                                style = AuroraType.titleM,
                                color = c.ink900,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "3 items selected · About 8.1 MB encrypted payload",
                                style = AuroraType.bodyS,
                                color = c.ink600,
                            )
                        }
                    }
                }

                // Expiry Picker
                item {
                    Column {
                        Text(
                            text = "Expires after",
                            style = AuroraType.labelL,
                            color = c.ink700,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            expiryOptions.forEach { opt ->
                                AuroraChip(
                                    label = opt,
                                    selected = selectedExpiry == opt,
                                    onClick = { selectedExpiry = opt },
                                )
                            }
                        }
                    }
                }

                // Max Opens Picker
                item {
                    Column {
                        Text(
                            text = "Maximum opens",
                            style = AuroraType.labelL,
                            color = c.ink700,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            opensOptions.forEach { opt ->
                                AuroraChip(
                                    label = opt,
                                    selected = selectedOpens == opt,
                                    onClick = { selectedOpens = opt },
                                )
                            }
                        }
                    }
                }

                // Export Policy Switch (Default OFF per T16-09)
                item {
                    AuroraGlassCard(
                        tint = AuroraTint.None,
                        raised = false,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { allowExport = !allowExport },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allow download & export",
                                    style = AuroraType.titleM,
                                    color = c.ink900,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Exported copies leave the protection model.",
                                    style = AuroraType.bodyS,
                                    color = c.ink600,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (allowExport) c.surfaceInk else c.solid)
                                    .border(1.dp, if (allowExport) c.surfaceInk else c.border, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (allowExport) {
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
            }

            // Create Action Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AuroraDimens.screenPadding),
            ) {
                AuroraButton(
                    text = if (isCreating) "Generating phrase..." else "Create share",
                    loading = isCreating,
                    onClick = {
                        isCreating = true
                        onCreated("demo-share-1")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 12-word phrase display screen inside an AuroraSecureSurface (FLAG_SECURE).
 * Shown once per share creation.
 */
@Composable
fun SharePhraseScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val c = AuroraTheme.colors
    var hasSentCheckbox by remember { mutableStateOf(false) }
    var copiedNote by remember { mutableStateOf(false) }

    val sample12Words = listOf(
        "harbor", "willow", "cobalt", "meadow",
        "lantern", "copper", "thistle", "quartz",
        "ember", "beacon", "glacier", "falcon",
    )

    AuroraSecureSurface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AuroraTopBar(
                title = "Share phrase",
                eyebrow = "Shown Once",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        onInk = true,
                    )
                },
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AuroraDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Warning Banner
                item {
                    AuroraGlassCard(
                        tint = AuroraTint.Butter,
                        raised = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = c.ink800,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Anyone with this phrase can open the share. It is shown only once and cannot be recovered.",
                                style = AuroraType.bodyS,
                                color = c.ink800,
                            )
                        }
                    }
                }

                // 12-Word Grid
                item {
                    AuroraPhraseGrid(
                        words = sample12Words,
                        columns = 3,
                    )
                }

                // Copy Action
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        AuroraButton(
                            text = if (copiedNote) "Copied (clears in 60 s)" else "Copy phrase",
                            leadingIcon = Icons.Filled.ContentCopy,
                            variant = AuroraButtonVariant.Secondary,
                            onClick = { copiedNote = true },
                        )
                    }
                }

                // "I've sent it" confirmation checkbox
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AuroraShapes.md)
                            .clickable { hasSentCheckbox = !hasSentCheckbox }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (hasSentCheckbox) c.surfaceInk else c.solid)
                                .border(1.dp, if (hasSentCheckbox) c.surfaceInk else c.border, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (hasSentCheckbox) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Checked",
                                    tint = c.solid,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "I've sent the phrase to the recipient",
                            style = AuroraType.bodyM,
                            color = c.ink900,
                        )
                    }
                }
            }

            // Bottom Done Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AuroraDimens.screenPadding),
            ) {
                AuroraButton(
                    text = "Done",
                    enabled = hasSentCheckbox,
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Entry screen to open a shared vault using a 12-word phrase.
 */
@Composable
fun SharedOpenScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val c = AuroraTheme.colors
    var phraseText by remember { mutableStateOf("") }
    val enteredWords = remember(phraseText) {
        phraseText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    }
    val isValidCount = enteredWords.size == 12

    AuroraSecureSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AuroraTopBar(
                title = "Open shared vault",
                eyebrow = "Passphrase Intake",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AuroraDimens.screenPadding, vertical = 8.dp),
            ) {
                Text(
                    text = "Enter the 12-word sharing phrase provided by the vault owner.",
                    style = AuroraType.bodyM,
                    color = c.ink600,
                )

                Spacer(Modifier.height(16.dp))

                // BasicTextField inside Aurora container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AuroraShapes.md)
                        .background(c.solid)
                        .border(1.dp, c.border, AuroraShapes.md)
                        .padding(16.dp),
                ) {
                    BasicTextField(
                        value = phraseText,
                        onValueChange = { phraseText = it.lowercase() },
                        textStyle = AuroraType.monoM.copy(color = c.ink900),
                        cursorBrush = SolidColor(c.iris600),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        decorationBox = { inner ->
                            if (phraseText.isEmpty()) {
                                Text(
                                    text = "harbor willow cobalt meadow lantern copper thistle quartz ember beacon glacier falcon",
                                    style = AuroraType.monoM,
                                    color = c.ink300,
                                )
                            }
                            inner()
                        },
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${enteredWords.size} of 12 words",
                            style = AuroraType.monoS,
                            color = if (isValidCount) c.success else c.ink500,
                        )
                        if (!isValidCount && enteredWords.isNotEmpty()) {
                            Text(
                                text = "Need 12 words",
                                style = AuroraType.labelM,
                                color = c.danger,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Helper to populate sample phrase for testing
                AuroraButton(
                    text = "Paste sample phrase",
                    onClick = {
                        phraseText = "harbor willow cobalt meadow lantern copper thistle quartz ember beacon glacier falcon"
                    },
                    variant = AuroraButtonVariant.Ghost,
                )
            }

            // Bottom Open Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AuroraDimens.screenPadding),
            ) {
                AuroraButton(
                    text = "Open vault",
                    enabled = isValidCount,
                    onClick = { onOpen("demo-share-1") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Recipient view of a shared vault with read-only badges and policy constraints.
 */
@Composable
fun SharedVaultScreen(
    shareId: String,
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit,
) {
    val c = AuroraTheme.colors
    val sharedItems = VaultDemoData.items.take(3)

    AuroraSecureSurface(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            AuroraTopBar(
                title = "Shared vault",
                eyebrow = "Encrypted Share",
                leading = {
                    AuroraIconButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        onInk = true,
                    )
                },
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AuroraDimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Read-Only Status & Policy Card
                item {
                    AuroraGlassCard(
                        tint = AuroraTint.Lilac,
                        raised = true,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Read-Only Access",
                                    style = AuroraType.headlineM,
                                    color = c.ink900,
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(c.surfaceInk.copy(alpha = 0.08f))
                                        .border(1.dp, c.border, CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "Read-only",
                                        style = AuroraType.labelM,
                                        color = c.ink800,
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Expires in 3 days · 2 opens left · Export disabled",
                                style = AuroraType.bodyS,
                                color = c.ink700,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "A share is a phrase. There is no recipient account.",
                                style = AuroraType.labelM,
                                color = c.ink500,
                            )
                        }
                    }
                }

                // Section header
                item {
                    Text(
                        text = "Shared Items (${sharedItems.size})",
                        style = AuroraType.labelL,
                        color = c.ink700,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // Shared files
                items(sharedItems) { item ->
                    AuroraGlassCard(
                        tint = AuroraTint.None,
                        raised = false,
                        onClick = { onOpenFile(item.id) },
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
                                    text = "${item.kind.name} · ${VaultDemoData.formatBytes(item.sizeBytes)}",
                                    style = AuroraType.bodyS,
                                    color = c.ink600,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
