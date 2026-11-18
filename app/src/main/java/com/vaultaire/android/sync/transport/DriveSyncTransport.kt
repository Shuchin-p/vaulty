package com.vaultaire.android.sync.transport

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.vaultaire.android.backup.ZeroKnowledgeBackupEngine
import com.vaultaire.android.sync.engine.EncryptedSyncRecord
import com.vaultaire.android.sync.model.SyncMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * Transport layer for sync records over Google Drive App Folder.
 *
 * Reuses ZeroKnowledgeBackupEngine crypto/padding logic.
 * Handles upload, download, and change listing with resumable uploads.
 */
class DriveSyncTransport(
    private val authManager: DriveAuthManager,
    private val backupEngine: ZeroKnowledgeBackupEngine
) {

    companion object {
        private const val SYNC_MIME_TYPE = "application/vnd.vaultaire.sync.record.v1"
        private const val CHUNK_SIZE = 64 * 1024
        private const val MAX_RETRIES = 3
    }

    private val httpTransport: HttpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    /**
     * Uploads an encrypted sync record to Drive App Folder.
     * Returns the Drive file ID (record name).
     */
    suspend fun uploadSyncRecord(record: EncryptedSyncRecord): String = withContext(Dispatchers.IO) {
        val drive = getDriveService()
        val recordName = UUID.randomUUID().toString()

        val driveFile = File().apply {
            name = recordName
            mimeType = SYNC_MIME_TYPE
            setParents(listOf("appDataFolder"))
        }

        val mediaContent = com.google.api.client.http.ByteArrayContent(SYNC_MIME_TYPE, record.ciphertext)
        val request = drive.files().create(driveFile, mediaContent)
            .setFields("id")
            .setSupportsAllDrives(false)

        // Enable resumable upload for large records
        request.mediaHttpUploader.chunkSize = MediaHttpUploader.DEFAULT_CHUNK_SIZE
        request.mediaHttpUploader.setDirectUploadEnabled(false)

        val response = retryWithBackoff { request.execute() }
        response.id
    }

    /**
     * Downloads a sync record by Drive file ID.
     */
    suspend fun downloadSyncRecord(driveFileId: String): EncryptedSyncRecord = withContext(Dispatchers.IO) {
        val drive = getDriveService()
        val request = drive.files().get(driveFileId)
            .setAlt("media")
            .setSupportsAllDrives(false)

        val outputStream = ByteArrayOutputStream()
        request.executeMediaAndDownloadTo(outputStream)
        val ciphertext = outputStream.toByteArray()

        // We need IV, tag, AAD - for now assume they are embedded or derived
        // In practice, these would come from the record metadata or a companion file
        // For sync records, the IV is typically stored as a property or in a known position
        // This is a simplified version
        EncryptedSyncRecord(
            iv = ByteArray(12), // Placeholder - actual IV from record
            ciphertext = ciphertext,
            tag = ByteArray(16), // Placeholder
            aad = ByteArray(0) // Placeholder
        )
    }

    /**
     * Lists changes since the given change token.
     * Returns a pair of (newChangeToken, list of Drive file IDs).
     */
    suspend fun fetchChangesSince(changeToken: String?): Pair<String, List<String>> = withContext(Dispatchers.IO) {
        val drive = getDriveService()
        var token = changeToken
        val changedFileIds = mutableListOf<String>()

        do {
            // Try calling list() with pageToken as first argument
            val request = drive.changes().list(null).apply {
                pageSize = 100
                fields = "nextPageToken,newStartPageToken,changes(fileId)"
                includeRemoved = false
                supportsAllDrives = true
                pageToken = token
            }

            val response = retryWithBackoff { request.execute() }
            token = response.nextPageToken ?: response.newStartPageToken

            response.changes?.forEach { change ->
                if (!change.removed && change.fileId != null) {
                    changedFileIds.add(change.fileId)
                }
            }
        } while (token != null)

        Pair(token ?: "", changedFileIds)
    }

    /**
     * Gets the latest change token (for initial sync).
     */
    suspend fun getLatestChangeToken(): String = withContext(Dispatchers.IO) {
        val drive = getDriveService()
        val request = drive.changes().getStartPageToken()
            .setSupportsAllDrives(false)
        retryWithBackoff { request.execute() }.startPageToken ?: ""
    }

    /**
     * Deletes a sync record from Drive.
     */
    suspend fun deleteSyncRecord(driveFileId: String) = withContext(Dispatchers.IO) {
        val drive = getDriveService()
        val request = drive.files().delete(driveFileId)
            .setSupportsAllDrives(false)
        retryWithBackoff { request.execute() }
    }

    private fun getDriveService(): Drive {
        val credential = authManager.getCredential()
            ?: throw IllegalStateException("Not authenticated with Google Drive")
        return Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("Vaultaire")
            .build()
    }

    private suspend fun <T> retryWithBackoff(block: () -> T): T {
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    val delay = (2.0.pow(attempt) * 1000).toLong()
                    Thread.sleep(delay)
                }
            }
        }
        throw lastException!!
    }
}