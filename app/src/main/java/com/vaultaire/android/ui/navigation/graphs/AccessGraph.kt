package com.vaultaire.android.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.vaultaire.android.ui.navigation.Routes
import com.vaultaire.android.ui.screens.access.LockScreen
import com.vaultaire.android.ui.screens.access.OnboardingConfirmScreen
import com.vaultaire.android.ui.screens.access.OnboardingDoneScreen
import com.vaultaire.android.ui.screens.access.OnboardingPatternScreen
import com.vaultaire.android.ui.screens.access.OnboardingPhraseScreen
import com.vaultaire.android.ui.screens.access.RecoveryDisplayScreen
import com.vaultaire.android.ui.screens.access.RecoveryEnterScreen
import com.vaultaire.android.ui.screens.access.RecoveryResetPatternScreen
import com.vaultaire.android.ui.screens.access.RestoreProgressRoute
import com.vaultaire.android.ui.screens.access.SplashScreen
import com.vaultaire.android.ui.screens.access.WelcomeScreen

/** Access and recovery family: F1-F10. */
fun NavGraphBuilder.accessGraph(nav: NavHostController) {
    composable(Routes.Splash) {
        val context = androidx.compose.ui.platform.LocalContext.current
        SplashScreen(
            onDone = {
                val vaultManager = com.vaultaire.android.vault.VaultManager(context)
                val exists = vaultManager.listVaultFiles().isNotEmpty()
                com.vaultaire.android.ui.session.VaultaireSession.initialize(context)
                val dest = if (exists) Routes.Lock else Routes.Welcome
                nav.navigate(dest) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            },
        )
    }
    composable(Routes.Welcome) {
        WelcomeScreen(
            onCreateVault = { nav.navigate(Routes.OnboardingPattern) },
            onRestore = { nav.navigate(Routes.RecoveryEnter) },
        )
    }
    composable(Routes.OnboardingPattern) {
        OnboardingPatternScreen(
            onBack = { nav.popBackStack() },
            onContinue = { nav.navigate(Routes.OnboardingConfirm) },
        )
    }
    composable(Routes.OnboardingConfirm) {
        OnboardingConfirmScreen(
            onBack = { nav.popBackStack() },
            onContinue = { nav.navigate(Routes.OnboardingPhrase) },
        )
    }
    composable(Routes.OnboardingPhrase) {
        OnboardingPhraseScreen(
            onBack = { nav.popBackStack() },
            onContinue = { nav.navigate(Routes.OnboardingDone) },
        )
    }
    composable(Routes.OnboardingDone) {
        OnboardingDoneScreen(
            onContinue = {
                nav.navigate(Routes.Vault) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            },
        )
    }
    composable(Routes.Lock) {
        LockScreen(
            onUnlocked = {
                nav.navigate(Routes.Vault) {
                    popUpTo(Routes.Lock) { inclusive = true }
                }
            },
            onForgotPattern = { nav.navigate(Routes.RecoveryEnter) },
        )
    }
    composable(Routes.RecoveryEnter) {
        RecoveryEnterScreen(
            onBack = { nav.popBackStack() },
            onVerified = { nav.navigate(Routes.RecoveryDisplay) },
            onRestore = { nav.navigate(Routes.RestoreProgress) },
        )
    }
    composable(Routes.RecoveryDisplay) {
        RecoveryDisplayScreen(
            onBack = { nav.popBackStack() },
            onRegenerate = { nav.navigate(Routes.RecoveryResetPattern) },
        )
    }
    composable(Routes.RecoveryResetPattern) {
        RecoveryResetPatternScreen(
            onBack = { nav.popBackStack() },
            onDone = {
                nav.navigate(Routes.Lock) {
                    popUpTo(Routes.Lock) { inclusive = true }
                }
            },
        )
    }
    composable(Routes.RestoreProgress) {
        RestoreProgressRoute(
            onDone = {
                nav.navigate(Routes.Vault) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            },
        )
    }
}
