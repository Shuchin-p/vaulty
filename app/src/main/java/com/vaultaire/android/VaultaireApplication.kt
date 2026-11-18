package com.vaultaire.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Vaultaire application entry point.
 *
 * Feature containers and engines are wired in feature implementations
 * (security architecture, duress mode, sync, etc.). This class stays
 * dependency-free on purpose so the security-critical lifecycle remains
 * simple and testable.
 */
@HiltAndroidApp
class VaultaireApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        lateinit var instance: VaultaireApplication
            private set
    }
}
