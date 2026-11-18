package com.vaultaire.android.ui.session

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaultaire.android.vault.OpenVault
import com.vaultaire.android.vault.VaultManager

/**
 * In-memory session state for Vaultaire.
 *
 * Tracks whether a vault exists on disk, whether it is unlocked, the active vault ID,
 * and the active OpenVault descriptor. Process death or locking clears sensitive keys.
 */
object VaultaireSession {

    /** True once onboarding completed (a vault exists on this device). */
    var vaultExists by mutableStateOf(false)
        private set

    /** True while the session key is in memory. */
    var unlocked by mutableStateOf(false)
        private set

    /** True while the S9 brand cover should be shown over the whole app. */
    var coverVisible by mutableStateOf(false)
        private set

    /** The active vault ID, if unlocked. */
    var activeVaultId: String? by mutableStateOf(null)
        private set

    /** The active OpenVault instance, if unlocked. */
    var activeVault: OpenVault? by mutableStateOf(null)
        private set

    fun initialize(context: Context) {
        val vaultManager = VaultManager(context)
        vaultExists = vaultManager.listVaultFiles().isNotEmpty()
    }

    fun markVaultCreated(vaultId: String? = null, vault: OpenVault? = null) {
        vaultExists = true
        unlocked = true
        coverVisible = false
        activeVaultId = vaultId
        activeVault = vault
    }

    fun markUnlocked(vaultId: String? = null, vault: OpenVault? = null) {
        unlocked = true
        coverVisible = false
        activeVaultId = vaultId
        activeVault = vault
    }

    fun updateActiveVault(vault: OpenVault?) {
        activeVault = vault
    }

    fun lock() {
        unlocked = false
        coverVisible = false
        activeVaultId = null
        activeVault = null
    }

    fun showCover() {
        unlocked = false
        coverVisible = true
    }

    fun reset() {
        vaultExists = false
        unlocked = false
        coverVisible = false
        activeVaultId = null
        activeVault = null
    }
}

/** Backgrounded moment of the app, or 0 while foregrounded. */
object BackgroundClock {
    var leftAt: Long by mutableLongStateOf(0)
        private set

    fun markLeft(at: Long = System.currentTimeMillis()) {
        leftAt = at
    }

    fun markReturned() {
        leftAt = 0
    }

    fun elapsed(): Long = if (leftAt == 0L) 0L else System.currentTimeMillis() - leftAt
}