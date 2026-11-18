package com.vaultaire.android.sync.worker

import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Monitors network connectivity and metered status to optimize sync scheduling.
 */
class NetworkMonitor(
    private val context: Context,
    private val syncScheduler: SyncScheduler,
    private val scope: CoroutineScope
) {

    private var isConnected = false
    private var isMetered = false

    fun start() {
        scope.launch(Dispatchers.IO) {
            // Register network callback
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager

            val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    isConnected = true
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    isMetered = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true
                    onNetworkStateChanged()
                }

                override fun onLost(network: android.net.Network) {
                    isConnected = false
                    onNetworkStateChanged()
                }

                override fun onCapabilitiesChanged(network: android.net.Network, capabilities: android.net.NetworkCapabilities) {
                    val wasMetered = isMetered
                    isMetered = capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true
                    if (wasMetered != isMetered) {
                        onNetworkStateChanged()
                    }
                }
            }

            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    private fun onNetworkStateChanged() {
        // If we just got connected on unmetered network, trigger immediate sync for all vaults
        if (isConnected && !isMetered) {
            // In practice, iterate over all vault IDs and schedule immediate sync
            // syncScheduler.scheduleImmediateSync(vaultId)
        }
    }

    fun isNetworkAvailable(): Boolean = isConnected
    fun isNetworkMetered(): Boolean = isMetered
}
