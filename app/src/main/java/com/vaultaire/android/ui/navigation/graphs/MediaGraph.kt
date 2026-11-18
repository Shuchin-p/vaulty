package com.vaultaire.android.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vaultaire.android.ui.navigation.Routes
import com.vaultaire.android.ui.screens.media.CameraCaptureScreen
import com.vaultaire.android.ui.screens.media.CameraPermissionScreen
import com.vaultaire.android.ui.screens.media.CameraReviewPhotoScreen
import com.vaultaire.android.ui.screens.media.CameraReviewVideoScreen
import com.vaultaire.android.ui.screens.media.ShareCreateScreen
import com.vaultaire.android.ui.screens.media.SharePhraseScreen
import com.vaultaire.android.ui.screens.media.SharedOpenScreen
import com.vaultaire.android.ui.screens.media.SharedVaultScreen
import com.vaultaire.android.ui.screens.media.SharesScreen

/** Camera and sharing family: flows 03 and 05. */
fun NavGraphBuilder.mediaGraph(nav: NavHostController) {
    composable(Routes.Camera) {
        CameraCaptureScreen(
            onBack = { nav.popBackStack() },
            onPhotoCaptured = { nav.navigate(Routes.CameraReview) },
            onVideoCaptured = { nav.navigate(Routes.CameraReviewVideo) },
            onOpenPermission = { nav.navigate(Routes.CameraPermission) },
        )
    }
    composable(Routes.CameraReview) {
        CameraReviewPhotoScreen(
            onBack = { nav.popBackStack() },
            onKeep = {
                nav.navigate(Routes.Vault) {
                    popUpTo(Routes.Vault) { inclusive = false }
                    launchSingleTop = true
                }
            },
        )
    }
    composable(Routes.CameraReviewVideo) {
        CameraReviewVideoScreen(
            onBack = { nav.popBackStack() },
            onKeep = {
                nav.navigate(Routes.Vault) {
                    popUpTo(Routes.Vault) { inclusive = false }
                    launchSingleTop = true
                }
            },
        )
    }
    composable(Routes.CameraPermission) {
        CameraPermissionScreen(
            onBack = { nav.popBackStack() },
            onGranted = {
                nav.navigate(Routes.Camera) {
                    popUpTo(Routes.Camera) { inclusive = true }
                }
            },
        )
    }
    composable(Routes.Shares) {
        SharesScreen(
            onBack = { nav.popBackStack() },
            onOpenShare = { id -> nav.navigate(Routes.sharedVault(id)) },
            onCreateShare = { nav.navigate(Routes.ShareCreate) },
            onOpenSharedVault = { nav.navigate(Routes.SharedOpen) },
        )
    }
    composable(Routes.ShareCreate) {
        ShareCreateScreen(
            onBack = { nav.popBackStack() },
            onCreated = { nav.navigate(Routes.SharePhrase) },
        )
    }
    composable(Routes.SharePhrase) {
        SharePhraseScreen(
            onBack = { nav.popBackStack() },
            onDone = { nav.popBackStack(Routes.Vault, inclusive = false) },
        )
    }
    composable(Routes.SharedOpen) {
        SharedOpenScreen(
            onBack = { nav.popBackStack() },
            onOpen = { id -> nav.navigate(Routes.sharedVault(id)) },
        )
    }
    composable(
        route = Routes.SharedVaultPattern,
        arguments = listOf(navArgument(Routes.ArgId) { type = NavType.StringType }),
    ) { entry ->
        SharedVaultScreen(
            shareId = entry.arguments?.getString(Routes.ArgId).orEmpty(),
            onBack = { nav.popBackStack() },
            onOpenFile = { nav.navigate(Routes.file(it)) },
        )
    }
}
