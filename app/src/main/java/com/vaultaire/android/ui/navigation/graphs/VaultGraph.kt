package com.vaultaire.android.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vaultaire.android.ui.navigation.Routes
import com.vaultaire.android.ui.screens.vault.CollectionScreen
import com.vaultaire.android.ui.screens.vault.FileDetailScreen
import com.vaultaire.android.ui.screens.vault.ImportScreen
import com.vaultaire.android.ui.screens.vault.SearchScreen
import com.vaultaire.android.ui.screens.vault.TimelineScreen
import com.vaultaire.android.ui.screens.vault.VaultHomeScreen

/** Vault core family: flows 02-vault-core. */
fun NavGraphBuilder.vaultGraph(nav: NavHostController) {
    composable(Routes.Vault) {
        VaultHomeScreen(
            onOpenTimeline = { nav.navigate(Routes.VaultTimeline) },
            onOpenSearch = { nav.navigate(Routes.VaultSearch) },
            onOpenCollection = { id -> nav.navigate(Routes.collection(id)) },
            onOpenFile = { id -> nav.navigate(Routes.file(id)) },
            onOpenCamera = { nav.navigate(Routes.Camera) },
            onOpenImport = { nav.navigate(Routes.VaultImport) },
            onOpenShares = { nav.navigate(Routes.Shares) },
            onOpenSettings = { nav.navigate(Routes.Settings) },
        )
    }
    composable(Routes.VaultTimeline) {
        TimelineScreen(
            onBack = { nav.popBackStack() },
            onOpenFile = { id -> nav.navigate(Routes.file(id)) },
        )
    }
    composable(
        route = Routes.VaultCollectionPattern,
        arguments = listOf(navArgument(Routes.ArgId) { type = NavType.StringType }),
    ) { entry ->
        CollectionScreen(
            collectionId = entry.arguments?.getString(Routes.ArgId).orEmpty(),
            onBack = { nav.popBackStack() },
            onOpenFile = { id -> nav.navigate(Routes.file(id)) },
        )
    }
    composable(Routes.VaultSearch) {
        SearchScreen(
            onBack = { nav.popBackStack() },
            onOpenFile = { id -> nav.navigate(Routes.file(id)) },
        )
    }
    composable(
        route = Routes.VaultFilePattern,
        arguments = listOf(navArgument(Routes.ArgId) { type = NavType.StringType }),
    ) { entry ->
        FileDetailScreen(
            fileId = entry.arguments?.getString(Routes.ArgId).orEmpty(),
            onBack = { nav.popBackStack() },
        )
    }
    composable(Routes.VaultImport) {
        ImportScreen(onBack = { nav.popBackStack() })
    }
}
