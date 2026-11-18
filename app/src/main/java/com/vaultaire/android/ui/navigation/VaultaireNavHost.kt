package com.vaultaire.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.vaultaire.android.ui.navigation.graphs.accessGraph
import com.vaultaire.android.ui.navigation.graphs.mediaGraph
import com.vaultaire.android.ui.navigation.graphs.settingsGraph
import com.vaultaire.android.ui.navigation.graphs.vaultGraph
import com.vaultaire.android.ui.session.VaultaireSession

/**
 * Root navigation graph.
 *
 * Each feature family owns one `*Graph.kt` file and its screen package, which
 * keeps parallel work on separate files. This file only composes the graphs.
 * Also monitors session locking to redirect to [Routes.Lock] upon lock events.
 */
@Composable
fun VaultaireNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val unlocked = VaultaireSession.unlocked
    val vaultExists = VaultaireSession.vaultExists

    LaunchedEffect(unlocked, vaultExists) {
        if (!unlocked && vaultExists) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            val isAccessRoute = currentRoute in listOf(
                Routes.Splash,
                Routes.Welcome,
                Routes.OnboardingPattern,
                Routes.OnboardingConfirm,
                Routes.OnboardingPhrase,
                Routes.OnboardingDone,
                Routes.Lock,
                Routes.RecoveryEnter,
                Routes.RecoveryDisplay,
                Routes.RecoveryResetPattern,
                Routes.RestoreProgress
            )
            if (!isAccessRoute) {
                navController.navigate(Routes.Lock) {
                    popUpTo(Routes.Lock) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
    ) {
        accessGraph(navController)
        vaultGraph(navController)
        mediaGraph(navController)
        settingsGraph(navController)
    }
}

