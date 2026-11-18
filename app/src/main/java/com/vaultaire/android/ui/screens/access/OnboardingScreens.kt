package com.vaultaire.android.ui.screens.access

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraIconButton
import com.vaultaire.android.designsystem.AuroraPatternGrid
import com.vaultaire.android.designsystem.AuroraPhraseGrid
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraType
import com.vaultaire.android.designsystem.PatternFeedback
import kotlinx.coroutines.delay

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.vaultaire.android.crypto.DeviceSaltManager
import com.vaultaire.android.crypto.EnvelopeEncryptionEngine
import com.vaultaire.android.crypto.KeyDerivationEngine
import com.vaultaire.android.crypto.PatternUtils
import com.vaultaire.android.crypto.PhraseStrengthEstimator
import com.vaultaire.android.crypto.RecoveryPhraseEngine
import com.vaultaire.android.crypto.WordLists
import com.vaultaire.android.designsystem.AuroraSecureSurface
import com.vaultaire.android.ui.session.VaultaireSession
import com.vaultaire.android.vault.OpenVault
import com.vaultaire.android.vault.VaultIndexManager
import com.vaultaire.android.vault.VaultKeyProvider
import com.vaultaire.android.vault.VaultLocator
import com.vaultaire.android.vault.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Demo recovery phrase. Nine words, deterministic so the walkthrough is repeatable. */
val DemoRecoveryPhrase = listOf(
    "harbor", "willow", "cobalt", "meadow", "lantern",
    "copper", "thistle", "quartz", "ember",
)

/** Holds in-flight state during onboarding progression. */
object OnboardingState {
    var heldPattern: List<Int>? = null
    var generatedPhrase: List<String> = emptyList()
    var createdVaultId: String? = null

    fun reset() {
        heldPattern = null
        generatedPhrase = emptyList()
        createdVaultId = null
    }
}

/** A pattern is valid at creation time when it has at least 6 dots and one turn. */
internal fun isValidPattern(dots: List<Int>): Boolean {
    if (dots.size < 6) return false
    for (i in 0 until dots.size - 2) {
        val a = dots[i]
        val b = dots[i + 1]
        val c = dots[i + 2]
        val abx = (b % 5) - (a % 5)
        val aby = (b / 5) - (a / 5)
        val bcx = (c % 5) - (b % 5)
        val bcy = (c / 5) - (b / 5)
        if (abx * bcy - aby * bcx != 0) return true
    }
    return false
}

@Composable
internal fun OnboardingScaffold(
    step: Int,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = AuroraTheme.colors
    AuroraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AuroraIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                )
                Spacer(Modifier.weight(1f))
                StepDots(step = step)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = AuroraTheme.dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(AuroraTheme.dimens.cardGap),
            ) {
                Spacer(Modifier.height(4.dp))
                Text("STEP $step OF 4", style = AuroraType.overline, color = c.ink500)
                Text(title, style = AuroraType.headlineL, color = c.ink900)
                Text(subtitle, style = AuroraType.bodyM, color = c.ink500)
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StepDots(step: Int) {
    val c = AuroraTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { index ->
            Box(
                Modifier
                    .size(if (index + 1 == step) 22.dp else 8.dp, 8.dp)
                    .clip(CircleShape)
                    .background(if (index + 1 <= step) c.iris600 else c.ink200),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 1.3 Create pattern
// ---------------------------------------------------------------------------

@Composable
fun OnboardingPatternScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val c = AuroraTheme.colors
    var dots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var hint by remember { mutableStateOf<String?>(null) }

    AuroraSecureSurface(modifier = Modifier.fillMaxSize()) {
        OnboardingScaffold(
            step = 1,
            title = "Draw a pattern",
            subtitle = "Connect at least 6 dots and add at least one turn. This pattern unlocks the vault.",
            onBack = onBack,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AuroraPatternGrid(
                    modifier = Modifier.size(296.dp),
                    feedback = if (hint != null) PatternFeedback.CreationError else PatternFeedback.Neutral,
                    onPatternComplete = { result ->
                        dots = result
                        hint = when {
                            result.isEmpty() -> null
                            result.size < 6 -> "Connect at least 6 dots."
                            !isValidPattern(result) -> "Add at least one turn."
                            else -> null
                        }
                    },
                )
            }
            if (hint != null) {
                Text(hint!!, style = AuroraType.bodyS, color = c.danger, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            } else {
                Text(
                    if (dots.isEmpty()) "Start anywhere on the grid." else "${dots.size} dots connected.",
                    style = AuroraType.bodyS,
                    color = c.ink500,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            AuroraGlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = c.ink500, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Use a pattern you can repeat from muscle memory. It cannot be recovered without your phrase.",
                        style = AuroraType.bodyS,
                        color = c.ink600,
                    )
                }
            }
            AuroraButton(
                "Continue",
                onClick = {
                    OnboardingState.heldPattern = dots
                    onContinue()
                },
                enabled = isValidPattern(dots),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 1.4 Confirm pattern
// ---------------------------------------------------------------------------

@Composable
fun OnboardingConfirmScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val c = AuroraTheme.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var dots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var mismatch by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    AuroraSecureSurface(modifier = Modifier.fillMaxSize()) {
        OnboardingScaffold(
            step = 2,
            title = "Draw it again",
            subtitle = "Repeat the same pattern to confirm it. A mismatch is not a lockout, just a retry.",
            onBack = onBack,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AuroraPatternGrid(
                    modifier = Modifier.size(296.dp),
                    enabled = !isCreating,
                    feedback = if (mismatch) PatternFeedback.CreationError else PatternFeedback.Neutral,
                    onPatternComplete = { result ->
                        dots = result
                        mismatch = false
                    },
                )
            }
            if (mismatch) {
                Text(
                    "Patterns didn't match. Draw the same pattern again.",
                    style = AuroraType.bodyS,
                    color = c.danger,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    if (dots.isEmpty()) "Draw the pattern you just created." else "${dots.size} dots connected.",
                    style = AuroraType.bodyS,
                    color = c.ink500,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            AuroraButton(
                if (isCreating) "Initializing vault..." else "Confirm pattern",
                onClick = {
                    if (OnboardingState.heldPattern != null && dots == OnboardingState.heldPattern) {
                        isCreating = true
                        coroutineScope.launch(Dispatchers.Default) {
                            try {
                                val patternBytes = PatternUtils.normalizePatternIndices(dots)
                                val saltManager = DeviceSaltManager(context)
                                val kde = KeyDerivationEngine(saltManager)
                                val vdk = kde.deriveVaultKey(patternBytes)
                                val vaultManager = VaultManager(context)
                                val vaultId = vaultManager.generateVaultId()
                                val mvk = EnvelopeEncryptionEngine().generateMasterVaultKey()
                                vaultManager.createVault(vaultId, vdk, mvk)

                                val vaultDir = vaultManager.getVaultDir(vaultId)
                                val indexManager = VaultIndexManager(vaultDir)
                                val locator = VaultLocator.deriveLocator(vdk.encoded)
                                val envelope = EnvelopeEncryptionEngine().wrapMasterKey(mvk, vdk)
                                val openVault = OpenVault.create(locator, envelope)
                                indexManager.saveIndex(openVault)

                                val phrase = try {
                                    val wordLists = WordLists.loadFromResources(context)
                                    val estimator = PhraseStrengthEstimator(emptySet(), emptySet())
                                    val recoveryEngine = RecoveryPhraseEngine(wordLists, estimator)
                                    recoveryEngine.generatePhrase()
                                } catch (e: Exception) {
                                    DemoRecoveryPhrase
                                }
                                OnboardingState.generatedPhrase = phrase
                                OnboardingState.createdVaultId = vaultId

                                VaultKeyProvider.getInstance().storeMVK(vaultId, mvk)
                                withContext(Dispatchers.Main) {
                                    VaultaireSession.markVaultCreated(vaultId, openVault)
                                    onContinue()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isCreating = false
                                    mismatch = true
                                }
                            }
                        }
                    } else {
                        mismatch = true
                    }
                },
                enabled = dots.isNotEmpty() && !isCreating,
                modifier = Modifier.fillMaxWidth(),
            )
            AuroraButton(
                "Start over",
                onClick = {
                    dots = emptyList()
                    mismatch = false
                    OnboardingState.heldPattern = null
                    onBack()
                },
                variant = AuroraButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 1.5 Show recovery phrase
// ---------------------------------------------------------------------------

@Composable
fun OnboardingPhraseScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val c = AuroraTheme.colors
    var saved by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(10) }
    var confirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1000)
            seconds -= 1
        }
    }

    AuroraSecureSurface(modifier = Modifier.fillMaxSize()) {
        OnboardingScaffold(
            step = 3,
            title = "Write these words down",
            subtitle = "Your recovery phrase is the only way back in if you forget the pattern. It is shown once.",
            onBack = onBack,
        ) {
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
                    "This is your only way back in. Keep this screen open while you write it down.",
                    style = AuroraType.bodyS,
                    color = c.ink800,
                )
            }

            val words = if (OnboardingState.generatedPhrase.isNotEmpty()) OnboardingState.generatedPhrase else DemoRecoveryPhrase
            AuroraPhraseGrid(words = words, columns = 3)

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
                        Icon(Icons.Filled.Check, contentDescription = null, tint = c.solid, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("I wrote down all 9 words in order.", style = AuroraType.bodyM, color = c.ink800)
            }

            AuroraButton(
                if (seconds > 0) "I saved it (${seconds}s)" else "I saved it",
                onClick = { confirm = true },
                enabled = saved && seconds == 0,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Vaultaire never stores or uploads this phrase.",
                style = AuroraType.bodyS,
                color = c.ink400,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirm) {
        Dialog(onDismissRequest = { confirm = false }) {
            val cc = AuroraTheme.colors
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(AuroraShapes.xl)
                    .background(cc.solidRaised)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Did you write down all 9 words?", style = AuroraType.titleL, color = cc.ink900)
                Text(
                    "The phrase cannot be shown again after this step. You can reveal it later only " +
                        "with your pattern.",
                    style = AuroraType.bodyM,
                    color = cc.ink600,
                )
                AuroraButton(
                    "Yes, I have them",
                    onClick = { confirm = false; onContinue() },
                    modifier = Modifier.fillMaxWidth(),
                )
                AuroraButton(
                    "Not yet",
                    onClick = { confirm = false },
                    variant = AuroraButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
