package com.vaultaire.android.ui.screens.access

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraChip
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraPatternGrid
import com.vaultaire.android.designsystem.AuroraPhraseGrid
import com.vaultaire.android.designsystem.AuroraProgressRing
import com.vaultaire.android.designsystem.AuroraSecureSurface
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraTopBar
import com.vaultaire.android.designsystem.AuroraType
import com.vaultaire.android.designsystem.PatternFeedback
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// 2.1 Enter recovery phrase
// ---------------------------------------------------------------------------

@Composable
fun RecoveryEnterScreen(
    onBack: () -> Unit,
    onVerified: () -> Unit,
    onRestore: () -> Unit,
) {
    val c = AuroraTheme.colors
    var text by remember { mutableStateOf("") }
    var wordCount by remember { mutableIntStateOf(9) }
    var error by remember { mutableStateOf<String?>(null) }
    var failures by remember { mutableIntStateOf(0) }
    var waitSeconds by remember { mutableIntStateOf(0) }

    val words = remember(text) { text.trim().split(Regex("\\s+")).filter { it.isNotBlank() } }

    LaunchedEffect(waitSeconds) {
        if (waitSeconds > 0) {
            delay(1000)
            waitSeconds -= 1
        }
    }

    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AuroraTopBar(
                title = "Recovery phrase",
                leading = {
                    AuroraIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = AuroraTheme.dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AuroraTheme.dimens.cardGap),
            ) {
                Spacer(Modifier.height(4.dp))
                Text("Type the words in order", style = AuroraType.headlineM, color = c.ink900)
                Text(
                    "The phrase was shown once when this vault was created. Words are lowercase and separated by spaces.",
                    style = AuroraType.bodyM,
                    color = c.ink500,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(9, 12).forEach { count ->
                        AuroraChip(
                            label = "$count words",
                            selected = wordCount == count,
                            onClick = { wordCount = count; error = null },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(AuroraShapes.md)
                        .background(c.solid)
                        .border(1.dp, if (error != null) c.danger else c.border, AuroraShapes.md)
                        .padding(16.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.lowercase(); error = null },
                        textStyle = AuroraType.monoM.copy(color = c.ink900),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(c.iris600),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        decorationBox = { inner ->
                            if (text.isEmpty()) {
                                Text(
                                    "harbor willow cobalt meadow lantern copper thistle quartz ember",
                                    style = AuroraType.monoM,
                                    color = c.ink300,
                                )
                            }
                            inner()
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${words.size} of $wordCount words",
                            style = AuroraType.monoS,
                            color = if (words.size == wordCount) c.success else c.ink500,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Paste",
                            style = AuroraType.labelM,
                            color = c.iris600,
                            modifier = Modifier
                                .clip(AuroraShapes.sm)
                                .clickable {
                                    text = DemoRecoveryPhrase.take(wordCount).joinToString(" ")
                                    error = null
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                if (error != null) {
                    Text(error!!, style = AuroraType.bodyS, color = c.danger)
                }
                if (waitSeconds > 0) {
                    Text(
                        "Please wait a moment. ${waitSeconds}s",
                        style = AuroraType.monoM,
                        color = c.ink600,
                    )
                }

                AuroraButton(
                    "Verify phrase",
                    onClick = {
                        if (words.size != wordCount) {
                            error = "Enter all $wordCount words."
                        } else if (words.any { word -> word.any { !it.isLetter() } }) {
                            error = "That phrase doesn't look right. Check the words and order."
                        } else {
                            failures += 1
                            if (failures >= 3 && failures < 5) {
                                waitSeconds = 30
                                error = "That phrase doesn't look right. Check the words and order."
                            } else {
                                onVerified()
                            }
                        }
                    },
                    enabled = waitSeconds == 0 && words.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
                AuroraButton(
                    "Restore this device",
                    onClick = onRestore,
                    variant = AuroraButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Vaultaire cannot reset your phrase. If it is lost and the pattern is forgotten, the vault is unrecoverable.",
                    style = AuroraType.bodyS,
                    color = c.ink400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2.2 Display recovery phrase
// ---------------------------------------------------------------------------

@Composable
fun RecoveryDisplayScreen(
    onBack: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val c = AuroraTheme.colors
    var stage by remember { mutableIntStateOf(0) } // 0 re-auth, 1 phrase
    var pattern by remember { mutableStateOf<List<Int>>(emptyList()) }

    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AuroraTopBar(
                title = if (stage == 0) "Confirm it's you" else "Recovery phrase",
                leading = {
                    AuroraIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                    )
                },
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AuroraTheme.dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AuroraTheme.dimens.cardGap),
            ) {
                if (stage == 0) {
                    Spacer(Modifier.height(12.dp))
                    Text("Draw your pattern", style = AuroraType.headlineM, color = c.ink900)
                    Text(
                        "Re-authentication is required before a phrase can be revealed.",
                        style = AuroraType.bodyM,
                        color = c.ink500,
                        textAlign = TextAlign.Center,
                    )
                    AuroraPatternGrid(
                        modifier = Modifier.size(280.dp),
                        onPatternComplete = { result ->
                            pattern = result
                            if (result.size >= 4) stage = 1
                        },
                    )
                    Text(
                        if (pattern.isEmpty()) "Draw any part of your pattern to continue." else "${pattern.size} dots connected",
                        style = AuroraType.bodyS,
                        color = c.ink400,
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(AuroraShapes.md)
                            .background(c.tintButter)
                            .border(1.dp, c.tintButterDeep.copy(alpha = 0.35f), AuroraShapes.md)
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = c.ink700, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Recovery phrase shown. Screen capture is blocked. Write it down offline.",
                            style = AuroraType.bodyS,
                            color = c.ink800,
                        )
                    }
                    AuroraPhraseGrid(words = DemoRecoveryPhrase, columns = 3)
                    AuroraButton(
                        "Regenerate phrase",
                        onClick = onRegenerate,
                        variant = AuroraButtonVariant.Secondary,
                        leadingIcon = Icons.Filled.Refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Regenerating invalidates the current phrase. The new one must be saved before leaving.",
                        style = AuroraType.bodyS,
                        color = c.ink500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2.3 Reset pattern
// ---------------------------------------------------------------------------

@Composable
fun RecoveryResetPatternScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val c = AuroraTheme.colors
    var stage by remember { mutableIntStateOf(0) }
    var dots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var first by remember { mutableStateOf<List<Int>>(emptyList()) }
    var mismatch by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }

    if (stage == 0) {
        AuroraBackground(variant = com.vaultaire.android.designsystem.AuroraBackgroundVariant.Bloom) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = c.iris600, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(18.dp))
                Text("Reset your pattern?", style = AuroraType.headlineL, color = c.ink900)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your files stay encrypted and are never re-uploaded. You will draw a new pattern " +
                        "and must save a new recovery phrase.",
                    style = AuroraType.bodyL,
                    color = c.ink600,
                )
                Spacer(Modifier.height(26.dp))
                AuroraButton("Start reset", onClick = { stage = 1 }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                AuroraButton(
                    "Cancel",
                    onClick = onBack,
                    variant = AuroraButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }

    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AuroraTopBar(
                title = when (stage) {
                    1 -> "New pattern"
                    2 -> "Confirm pattern"
                    else -> "New recovery phrase"
                },
                leading = {
                    AuroraIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = {
                            if (stage <= 1) onBack() else confirmExit = true
                        },
                    )
                },
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = AuroraTheme.dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AuroraTheme.dimens.cardGap),
            ) {
                when (stage) {
                    1 -> {
                        Text("Draw a new pattern", style = AuroraType.headlineM, color = c.ink900)
                        Text(
                            "At least 6 dots and at least one turn.",
                            style = AuroraType.bodyM,
                            color = c.ink500,
                        )
                        AuroraPatternGrid(
                            modifier = Modifier.size(296.dp),
                            onPatternComplete = { dots = it },
                        )
                        AuroraButton(
                            "Continue",
                            onClick = { first = dots; dots = emptyList(); stage = 2 },
                            enabled = isValidPattern(dots),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    2 -> {
                        Text("Draw it again", style = AuroraType.headlineM, color = c.ink900)
                        Text(
                            "A mismatch only retries; your current pattern is still active until you finish.",
                            style = AuroraType.bodyM,
                            color = c.ink500,
                            textAlign = TextAlign.Center,
                        )
                        AuroraPatternGrid(
                            modifier = Modifier.size(296.dp),
                            feedback = if (mismatch) PatternFeedback.CreationError else PatternFeedback.Neutral,
                            onPatternComplete = { dots = it; mismatch = false },
                        )
                        if (mismatch) {
                            Text("Patterns didn't match. Try again.", style = AuroraType.bodyS, color = c.danger)
                        }
                        AuroraButton(
                            "Confirm new pattern",
                            onClick = {
                                if (dots == first) stage = 3 else mismatch = true
                            },
                            enabled = dots.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AuroraButton(
                            "Start over",
                            onClick = { dots = emptyList(); first = emptyList(); mismatch = false; stage = 1 },
                            variant = AuroraButtonVariant.Ghost,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(AuroraShapes.md)
                                .background(c.tintButter)
                                .border(1.dp, c.tintButterDeep.copy(alpha = 0.35f), AuroraShapes.md)
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = c.ink700, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Your previous phrase no longer works. Save these 9 words before continuing.",
                                style = AuroraType.bodyS,
                                color = c.ink800,
                            )
                        }
                        AuroraPhraseGrid(words = DemoRecoveryPhrase, columns = 3)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(AuroraShapes.md)
                                .clickable { saved = !saved }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .clip(AuroraShapes.sm)
                                    .background(if (saved) c.iris600 else c.ink050)
                                    .border(1.dp, if (saved) c.iris600 else c.ink300, AuroraShapes.sm),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (saved) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.solid, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("I saved all 9 new words.", style = AuroraType.bodyM, color = c.ink800)
                        }
                        AuroraButton(
                            "Finish reset",
                            onClick = onDone,
                            enabled = saved,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmExit) {
        Dialog(onDismissRequest = { confirmExit = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(AuroraShapes.xl)
                    .background(c.solidRaised)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Leave without finishing?", style = AuroraType.titleL, color = c.ink900)
                Text(
                    "Your old pattern and phrase stay valid. Nothing has changed yet.",
                    style = AuroraType.bodyM,
                    color = c.ink600,
                )
                AuroraButton("Keep editing", { confirmExit = false }, modifier = Modifier.fillMaxWidth())
                AuroraButton(
                    "Leave",
                    { confirmExit = false; onBack() },
                    variant = AuroraButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2.4 Restore progress
// ---------------------------------------------------------------------------

@Composable
fun RestoreProgressRoute(onDone: () -> Unit) {
    val c = AuroraTheme.colors
    var progress by remember { mutableFloatStateOf(0f) }
    var step by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    val stages = listOf(
        "Verifying phrase",
        "Downloading 128 records",
        "Decrypting index",
        "Rebuilding thumbnails",
    )

    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(420)
            progress = (progress + 0.04f).coerceAtMost(1f)
            step = ((progress * stages.size).toInt()).coerceAtMost(stages.lastIndex)
            if (progress >= 1f) {
                finished = true
                delay(600)
                onDone()
            }
        }
    }

    AuroraBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AuroraTheme.dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AuroraProgressRing(
                progress = progress,
                size = 132.dp,
                color = c.iris600,
                center = {
                    Text("${(progress * 100).toInt()}%", style = AuroraType.monoL, color = c.ink900)
                },
            )
            Spacer(Modifier.height(24.dp))
            Text(if (finished) "Vault restored" else "Restoring vault", style = AuroraType.headlineM, color = c.ink900)
            Spacer(Modifier.height(8.dp))
            Text(
                if (finished) "Your encrypted files are back on this device." else stages[step],
                style = AuroraType.bodyM,
                color = c.ink500,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            AuroraGlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stages.forEachIndexed { index, label ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (index < step || finished) Icons.Filled.CheckCircle else Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = if (index <= step || finished) c.success else c.ink300,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                label,
                                style = AuroraType.bodyS,
                                color = if (index <= step || finished) c.ink800 else c.ink400,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "You can leave this screen. The restore continues in the background.",
                style = AuroraType.bodyS,
                color = c.ink400,
                textAlign = TextAlign.Center,
            )
        }
    }
}
