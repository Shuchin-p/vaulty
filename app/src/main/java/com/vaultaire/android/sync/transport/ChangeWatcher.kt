package com.vaultaire.android.sync.transport

import android.content.Context
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.Channel
import com.vaultaire.android.sync.transport.DriveAuthManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Registers a push notification channel for Drive changes (changes.watch).
 *
 * Uses FCM as the webhook endpoint. When changes occur, FCM delivers
 * a notification to the app, which triggers an expedited sync worker.
 */
class ChangeWatcher(
    private val context: Context,
    private val authManager: DriveAuthManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val CHANNEL_ID_PREFIX = "vaultaire-sync-"
        private const val FCM_WEBHOOK_URL = "https://fcm.googleapis.com/v1/projects/%s/messages:send"
        // TTL: 1 hour (max for Drive push notifications)
        private const val CHANNEL_TTL_SECONDS = 3600
    }

    private var activeChannelId: String? = null
    private var activeResourceId: String? = null

    /**
     * Starts watching for changes on the given vault.
     * The vaultId is used to create a unique channel ID.
     */
    suspend fun startWatching(vaultId: String, fcmProjectId: String): Channel = withContext(ioDispatcher) {
        val credential = authManager.getCredential()
            ?: throw IllegalStateException("Not authenticated")

        val drive = Drive.Builder(
            com.google.api.client.http.javanet.NetHttpTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Vaultaire").build()

        val channelId = CHANNEL_ID_PREFIX + vaultId
        val webhookUrl = String.format(FCM_WEBHOOK_URL, fcmProjectId)

        val channel = Channel()
            channel.id = channelId
            channel.type = "web_hook"
            channel.address = webhookUrl
            channel.token = UUID.randomUUID().toString() // Channel token for verification
            channel.expiration = System.currentTimeMillis() + CHANNEL_TTL_SECONDS * 1000

        // Drive API changes().watch() expects channel as the request body
        val request = drive.changes().watch(channelId, channel)

        val response = request.execute()
        activeChannelId = response.id
        activeResourceId = response.resourceId
        response
    }

    /**
     * Stops the active change notification channel.
     */
    suspend fun stopWatching() = withContext(ioDispatcher) {
        activeChannelId?.let { channelId ->
            val credential = authManager.getCredential() ?: return@withContext
            val drive = Drive.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Vaultaire").build()

            val channel = Channel()
                channel.id = channelId
                channel.resourceId = activeResourceId

            drive.channels().stop(channel).execute()
            activeChannelId = null
            activeResourceId = null
        }
    }

    /**
     * Renews the channel before it expires (call periodically).
     */
    suspend fun renewIfNeeded(vaultId: String, fcmProjectId: String) = withContext(ioDispatcher) {
        // In practice, check expiration and renew if < 24h remaining
        stopWatching()
        startWatching(vaultId, fcmProjectId)
    }

    /**
     * Handles an incoming FCM push notification.
     * Returns true if the notification is for our sync channel.
     */
    fun handleFcmNotification(remoteMessage: com.google.firebase.messaging.RemoteMessage): Boolean {
        val data = remoteMessage.data
        // Verify the notification is for our channel
        return data["channelId"]?.startsWith(CHANNEL_ID_PREFIX) == true
    }
}
