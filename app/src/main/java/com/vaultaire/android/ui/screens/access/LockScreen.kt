package com.vaultaire.android.ui.screens.access

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultaire.android.crypto.AttemptThrottler
import com.vaultaire.android.crypto.DeviceSaltManager
import com.vaultaire.android.crypto.EnvelopeEncryptionEngine
import com.vaultaire.android.crypto.KeyDerivationEngine
import com.vaultaire.android.crypto.PatternUtils
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraBackgroundVariant
import com.vaultaire.android.designsystem.AuroraPatternGrid
import com.vaultaire.android.designsystem.AuroraSecureSurface
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraType
import com.vaultaire.android.designsystem.PatternFeedback
import com.vaultaire.android.ui.session.VaultaireSession
import com.vaultaire.android.vault.OpenVault
import com.vaultaire.android.vault.VaultIndexManager
import com.vaultaire.android.vault.VaultKeyProvider
import com.vaultaire.android.vault.VaultLocator
import com.vaultaire.android.vault.VaultManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

/**
 * Pattern unlock screen.
 *
 * Every completed pattern resolves identically: there is no wrong-pattern
 * error message, no shake, and no red styling (neutral feedback to prevent
 * information disclosure). Progressive attempt throttling is enforced via
 * [AttemptThrottler]. Full screen is protected with FLAG_SECURE.
 */
@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    onForgotPattern: () -> Unit,
    onBiometric: (() -> Unit)? = null,
) {
    val c = AuroraTheme.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val throttler = remember(context) { AttemptThrottler(context) }

    var dots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var waitSeconds by remember { mutableIntStateOf(throttler.getRemainingLockoutSeconds()) }

    LaunchedEffect(waitSeconds) {
        if (waitSeconds > 0) {
            delay(1000)
            waitSeconds = throttler.getRemainingLockoutSeconds()
        }
    }

    AuroraSecureSurface(
        modifier = Modifier.fillMaxSize(),
    ) {
        AuroraBackground(variant = AuroraBackgroundVariant.Orbit) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = AuroraTheme.dimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(28.dp))
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(c.iris100),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = c.iris700, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("Welcome back", style = AuroraType.headlineL, color = c.ink900)
                Text("Draw your pattern", style = AuroraType.bodyM, color = c.ink500)
                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .clip(AuroraTheme.shapes.xl)
                        .padding(4.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AuroraPatternGrid(
                            modifier = Modifier.size(292.dp),
                            enabled = waitSeconds == 0,
                            feedback = PatternFeedback.Neutral,
                            onPatternComplete = { result ->
                                dots = result
                                if (waitSeconds > 0) return@AuroraPatternGrid

                                // T03-13: Patterns with fewer than 6 points are ignored silently
                                if (result.size < 6) return@AuroraPatternGrid

                                val validation = PatternUtils.validatePatternIndices(result)
                                if (!validation.isValid) {
                                    // Under-length or no-turn pattern entered: record failed attempt neutrally
                                    val delaySec = throttler.recordFailedAttempt()
                                    if (delaySec > 0) waitSeconds = delaySec
                                    return@AuroraPatternGrid
                                }

                                coroutineScope.launch(Dispatchers.Default) {
                                    try {
                                        val patternBytes = PatternUtils.normalizePatternIndices(result)
                                        val saltManager = DeviceSaltManager(context)
                                        val kde = KeyDerivationEngine(saltManager)
                                        val vdk = kde.deriveVaultKey(patternBytes)

                                        val vaultManager = VaultManager(context)
                                        val vaultFiles = vaultManager.listVaultFiles()

                                        var unlockedVaultId: String? = null
                                        var masterKey: SecretKey? = null

                                        for (file in vaultFiles) {
                                            val vaultId = file.nameWithoutExtension
                                            try {
                                                val key = vaultManager.unlockVault(vaultId, vdk)
                                                unlockedVaultId = vaultId
                                                masterKey = key
                                                break
                                            } catch (e: Exception) {
                                                // Continue checking other vault files
                                            }
                                        }

                                        if (unlockedVaultId != null && masterKey != null) {
                                            throttler.recordSuccessfulAttempt()
                                            VaultKeyProvider.getInstance().storeMVK(unlockedVaultId, masterKey)
                                            val vaultDir = vaultManager.getVaultDir(unlockedVaultId)
                                            val indexManager = VaultIndexManager(vaultDir)
                                            val openVault = indexManager.loadIndex() ?: OpenVault.create(
                                                locator = VaultLocator.deriveLocator(vdk.encoded),
                                                masterKeyEnvelope = EnvelopeEncryptionEngine().wrapMasterKey(masterKey, vdk)
                                            )
                                            withContext(Dispatchers.Main) {
                                                VaultaireSession.markUnlocked(unlockedVaultId, openVault)
                                                onUnlocked()
                                            }
                                        } else {
                                            // If no vaults existed on disk, auto-create initial vault cleanly
                                            if (vaultFiles.isEmpty()) {
                                                val newVaultId = vaultManager.generateVaultId()
                                                val mvk = EnvelopeEncryptionEngine().generateMasterVaultKey()
                                                vaultManager.createVault(newVaultId, vdk, mvk)
                                                val vaultDir = vaultManager.getVaultDir(newVaultId)
                                                val indexManager = VaultIndexManager(vaultDir)
                                                val locator = VaultLocator.deriveLocator(vdk.encoded)
                                                val envelope = EnvelopeEncryptionEngine().wrapMasterKey(mvk, vdk)
                                                val newVault = OpenVault.create(locator, envelope)
                                                indexManager.saveIndex(newVault)

                                                throttler.recordSuccessfulAttempt()
                                                VaultKeyProvider.getInstance().storeMVK(newVaultId, mvk)
                                                withContext(Dispatchers.Main) {
                                                    VaultaireSession.markVaultCreated(newVaultId, newVault)
                                                    onUnlocked()
                                                }
                                            } else {
                                                // Pattern does not match any configured vault
                                                val delaySec = throttler.recordFailedAttempt()
                                                withContext(Dispatchers.Main) {
                                                    if (delaySec > 0) waitSeconds = delaySec
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        val delaySec = throttler.recordFailedAttempt()
                                        withContext(Dispatchers.Main) {
                                            if (delaySec > 0) waitSeconds = delaySec
                                        }
                                    }
                                }
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            when {
                                waitSeconds > 0 -> "Please wait a moment. ${waitSeconds}s"
                                dots.isEmpty() -> "The grid accepts any pattern you set."
                                else -> "${dots.size} dots connected"
                            },
                            style = if (waitSeconds > 0) AuroraType.monoM else AuroraType.bodyS,
                            color = c.ink500,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                if (onBiometric != null && waitSeconds == 0) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(AuroraTheme.shapes.md)
                            .clickable(onClick = onBiometric)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = c.iris600, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Unlock with biometrics", style = AuroraType.labelM, color = c.iris600)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(AuroraTheme.shapes.sm)
                        .clickable(onClick = onForgotPattern),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Forgot pattern?",
                        style = AuroraType.labelM,
                        color = c.ink600,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                Spacer(Modifier.weight(1f))
                Text(
                    "Pattern mismatch never reveals whether a vault exists.",
                    style = AuroraType.bodyS,
                    color = c.ink400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

