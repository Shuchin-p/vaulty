package com.vaultaire.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vaultaire.android.designsystem.AuroraTheme
import com.vaultaire.android.ui.VaultaireApp
import com.vaultaire.android.ui.session.VaultaireSession
import com.vaultaire.android.vault.VaultKeyProvider

/**
 * Single-activity host for the whole app. All navigation happens inside
 * the Compose graph rooted at [VaultaireApp]. Enforces automatic locking
 * upon app backgrounding and screen-off events.
 */
class MainActivity : ComponentActivity() {

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                lockSession()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        VaultaireSession.initialize(this)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)

        setContent {
            AuroraTheme {
                VaultaireApp()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Auto-lock when app moves to background
        lockSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // Ignored if already unregistered
        }
    }

    private fun lockSession() {
        VaultaireSession.lock()
        VaultKeyProvider.getInstance().clearAll()
    }
}
