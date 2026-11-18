package com.vaultaire.android.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.vaultaire.android.ui.navigation.Routes
import com.vaultaire.android.ui.screens.settings.AboutScreen
import com.vaultaire.android.ui.screens.settings.AppearanceScreen
import com.vaultaire.android.ui.screens.settings.BackupSettingsScreen
import com.vaultaire.android.ui.screens.settings.CloudSyncScreen
import com.vaultaire.android.ui.screens.settings.DuressGuidanceScreen
import com.vaultaire.android.ui.screens.settings.DuressSettingsScreen
import com.vaultaire.android.ui.screens.settings.HiddenVaultsScreen
import com.vaultaire.android.ui.screens.settings.IconPackScreen
import com.vaultaire.android.ui.screens.settings.PrivacyScreen
import com.vaultaire.android.ui.screens.settings.ProScreen
import com.vaultaire.android.ui.screens.settings.SecurityInfoScreen
import com.vaultaire.android.ui.screens.settings.SecuritySettingsScreen
import com.vaultaire.android.ui.screens.settings.SettingsScreen
import com.vaultaire.android.ui.screens.settings.SyncLogScreen

/** Settings, security and monetization family: flows 04, 06 and 07. */
fun NavGraphBuilder.settingsGraph(nav: NavHostController) {
    composable(Routes.Settings) {
        SettingsScreen(
            onBack = { nav.popBackStack() },
            onOpenSecurity = { nav.navigate(Routes.SecuritySettings) },
            onOpenPrivacy = { nav.navigate(Routes.Privacy) },
            onOpenAppearance = { nav.navigate(Routes.Appearance) },
            onOpenBackup = { nav.navigate(Routes.BackupSettings) },
            onOpenSync = { nav.navigate(Routes.CloudSync) },
            onOpenAbout = { nav.navigate(Routes.About) },
            onOpenPro = { nav.navigate(Routes.Pro) },
        )
    }
    composable(Routes.SecuritySettings) {
        SecuritySettingsScreen(
            onBack = { nav.popBackStack() },
            onOpenDuress = { nav.navigate(Routes.DuressSettings) },
            onOpenHiddenVaults = { nav.navigate(Routes.HiddenVaults) },
            onOpenSecurityInfo = { nav.navigate(Routes.SecurityInfo) },
            onChangePattern = { nav.navigate(Routes.OnboardingPattern) },
            onRevealPhrase = { nav.navigate(Routes.RecoveryDisplay) },
        )
    }
    composable(Routes.DuressSettings) {
        DuressSettingsScreen(
            onBack = { nav.popBackStack() },
            onOpenGuidance = { nav.navigate(Routes.DuressGuidance) },
        )
    }
    composable(Routes.DuressGuidance) {
        DuressGuidanceScreen(onBack = { nav.popBackStack() })
    }
    composable(Routes.HiddenVaults) {
        HiddenVaultsScreen(onBack = { nav.popBackStack() })
    }
    composable(Routes.SecurityInfo) {
        SecurityInfoScreen(onBack = { nav.popBackStack() })
    }
    composable(Routes.Privacy) {
        PrivacyScreen(onBack = { nav.popBackStack() })
    }
    composable(Routes.About) {
        AboutScreen(onBack = { nav.popBackStack() })
    }
    composable(Routes.BackupSettings) {
        BackupSettingsScreen(
            onBack = { nav.popBackStack() },
            onRestore = { nav.navigate(Routes.RecoveryEnter) },
        )
    }
    composable(Routes.CloudSync) {
        CloudSyncScreen(
            onBack = { nav.popBackStack() },
            onOpenLog = { nav.navigate(Routes.SyncLog) },
        )
    }
    composable(Routes.SyncLog) {
        SyncLogScreen(onBack = { nav.popBackStack() })
    }
    composable(Routes.Appearance) {
        AppearanceScreen(
            onBack = { nav.popBackStack() },
            onOpenIcons = { nav.navigate(Routes.AppearanceIcons) },
        )
    }
    composable(Routes.AppearanceIcons) {
        IconPackScreen(onBack = { nav.popBackStack() })
    }
    composable(Routes.Pro) {
        ProScreen(onBack = { nav.popBackStack() })
    }
}
