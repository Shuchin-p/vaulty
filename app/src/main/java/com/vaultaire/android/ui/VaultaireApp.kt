package com.vaultaire.android.ui

import androidx.compose.runtime.Composable
import com.vaultaire.android.ui.navigation.VaultaireNavHost

/**
 * Root of the Compose navigation graph.
 *
 * The theme is applied by the activity ([com.vaultaire.android.MainActivity])
 * with the Aurora design-system theme; this composable only hosts navigation.
 */
@Composable
fun VaultaireApp() {
    VaultaireNavHost()
}
