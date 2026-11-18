package com.vaultaire.android.ui.screens.access

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaultaire.android.designsystem.AuroraBackground
import com.vaultaire.android.designsystem.AuroraBackgroundVariant
import com.vaultaire.android.designsystem.AuroraButton
import com.vaultaire.android.designsystem.AuroraButtonVariant
import com.vaultaire.android.designsystem.AuroraGlassCard
import com.vaultaire.android.designsystem.AuroraShapes
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.designsystem.AuroraType
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// 1.1 Splash
// ---------------------------------------------------------------------------

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val c = AuroraTheme.colors
    LaunchedEffect(Unit) {
        delay(900)
        onDone()
    }
    AuroraBackground(variant = AuroraBackgroundVariant.Bloom) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(c.iris100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = c.iris700, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Vaultaire", style = AuroraType.displayL, color = c.ink900)
            Spacer(Modifier.height(6.dp))
            Text("Encrypted, by design.", style = AuroraType.bodyM, color = c.ink500)
        }
    }
}

// ---------------------------------------------------------------------------
// 1.2 Welcome
// ---------------------------------------------------------------------------

@Composable
fun WelcomeScreen(
    onCreateVault: () -> Unit,
    onRestore: () -> Unit,
) {
    val c = AuroraTheme.colors
    AuroraBackground(variant = AuroraBackgroundVariant.Orbit) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AuroraTheme.dimens.screenPadding),
        ) {
            Spacer(Modifier.height(48.dp))
            Text("PRIVATE VAULT", style = AuroraType.overline, color = c.ink500)
            Spacer(Modifier.height(10.dp))
            Text(
                "Your vault.\nNobody else's.",
                style = AuroraType.displayM,
                color = c.ink900,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Files, photos and recordings are encrypted on this device. There is no account, " +
                    "no cloud identity, and nothing to sign in to.",
                style = AuroraType.bodyL,
                color = c.ink600,
            )
            Spacer(Modifier.weight(1f))
            AuroraGlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WelcomePoint("Pattern unlock only", "The pattern you draw derives the key. It never leaves the device.")
                    WelcomePoint("Recovery phrase", "Nine words are the only way back in. We never see them.")
                    WelcomePoint("No analytics", "No telemetry, no advertising ID, no third-party trackers.")
                }
            }
            Spacer(Modifier.height(20.dp))
            AuroraButton(
                "Create vault",
                onClick = onCreateVault,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            AuroraButton(
                "Restore from phrase",
                onClick = onRestore,
                variant = AuroraButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "By continuing you agree to keep your recovery phrase somewhere safe.",
                style = AuroraType.bodyS,
                color = c.ink400,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun WelcomePoint(title: String, body: String) {
    val c = AuroraTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(c.iris100),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = c.iris700, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = AuroraType.titleM, color = c.ink900)
            Text(body, style = AuroraType.bodyS, color = c.ink500)
        }
    }
}

// ---------------------------------------------------------------------------
// 1.6 Vault ready
// ---------------------------------------------------------------------------

@Composable
fun OnboardingDoneScreen(onContinue: () -> Unit) {
    val c = AuroraTheme.colors
    AuroraBackground(variant = AuroraBackgroundVariant.Bloom) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = AuroraTheme.dimens.screenPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(c.tintMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = c.tintMintDeep, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("Vault ready", style = AuroraType.headlineL, color = c.ink900, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your pattern is set and your recovery phrase is saved. Nothing was uploaded.",
                style = AuroraType.bodyM,
                color = c.ink600,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))
            AuroraGlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DonePoint("Pattern set", "Required every time the app is reopened.")
                    DonePoint("Phrase confirmed", "Nine words, stored offline by you.")
                    DonePoint("Vault empty", "Add photos or files whenever you're ready.")
                }
            }
            Spacer(Modifier.height(26.dp))
            AuroraButton(
                "Open vault",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Locking is automatic. There is no sign-out.",
                style = AuroraType.bodyS,
                color = c.ink400,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DonePoint(title: String, body: String) {
    val c = AuroraTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = c.success, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = AuroraType.titleM, color = c.ink900)
            Text(body, style = AuroraType.bodyS, color = c.ink500)
        }
    }
}
