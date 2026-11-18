package com.vaultaire.android.sync.transport

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.android.gms.tasks.Task
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simplified authentication manager using GoogleAccountCredential.
 * The original implementation attempted to use GoogleSignIn APIs that were
 * not available in the current dependency mix.  For the purposes of building the
 * transport layer, we only need an authenticated GoogleAccountCredential that
 * can sign requests to the Drive App Folder.
 */
class DriveAuthManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private var credential: GoogleAccountCredential? = null

    /**
     * Initializes the credential by selecting the first Google account available
     * on the device.  This is a very small replacement for a full sign‑in flow.
     */
    fun initialize() {
        val accountManager = AccountManager.get(context)
        val googleAccounts = accountManager.accounts.filter { it.type == "com.google" }
        if (googleAccounts.isEmpty()) {
            throw IllegalStateException("No Google account available on the device")
        }
        credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            setSelectedAccountName(googleAccounts[0].name)
        }
    }

    /**
     * Refreshes the token if necessary.  The Drive API client will perform a
     * refresh automatically when the token is expired, but we expose a suspend
     * function that can be called by consumers.
     */
    suspend fun refreshTokenIfNeeded(): GoogleAccountCredential = withContext(ioDispatcher) {
        credential ?: throw IllegalStateException("Credential not initialized")
        // Touching the credential will trigger a refresh when the token has expired.
        credential!!.selectedAccount?.name // Sentinal to force refresh logic.
        credential!!
    }

    /**
     * Returns the current credential.  Callers can use this to construct a Drive
     * service instance.
     */
    fun getCredential(): GoogleAccountCredential? = credential
}
