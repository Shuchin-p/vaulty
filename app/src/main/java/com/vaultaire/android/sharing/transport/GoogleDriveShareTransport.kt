package com.vaultaire.android.sharing.transport

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import com.google.api.services.drive.model.ChangeList
import com.google.common.io.BaseEncoding
import com.vaultaire.android.sharing.ShareMetadata
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException;
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Google Drive transport for secure sharing packages.
 *
 * Uses the Drive App Folder (DRIVE_APPDATA scope) for private, per-user storage.
 * File structure:
 *   App Folder/
 *   └── shares/
 *       └── {shareId}/
 *           ├── package.v{version}.bin    # EncryptedSharePackage
 *           └── metadata.json              # ShareMetadata
 *
 * Implements exponential backoff retry (1s, 2s, 4s, 8s, max 5 attempts).
 * Uses Drive Changes API for efficient polling.
 * Offline operations queued via WorkManager (handled by caller).
 */
class GoogleDriveShareTransport(
    private val context: Context,
    private val accountName: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val TAG = "DriveShareTransport"
    private val json = Json { ignoreUnknownKeys = true }

    private val driveService by lazy {
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
            .setSelectedAccountName(accountName)
        Drive(NetHttpTransport(), GsonFactory(), credential)
    }

    /**
     * Uploads an encrypted share package to Drive.
     *
     * @param shareId The share ID (folder name)
     * @param version Package version number
     * @param packageBytes The encrypted package bytes
     * @return Drive file ID of the uploaded package
     */
    suspend fun uploadPackage(
        shareId: String,
        version: Int,
        packageBytes: ByteArray,
    ): String = withContext(dispatcher) {
        val folderId = getOrCreateShareFolder(shareId)
        val fileName = "package.v$version.bin"

        // Check if file already exists (update)
        val existing = findFileInFolder(folderId, fileName)
        val mediaContent = com.google.api.client.http.ByteArrayContent("application/octet-stream", packageBytes)
        val driveFile = File().apply {
            name = fileName
            parents = listOf(folderId)
        }

        var attempt = 0
        val maxAttempts = 5
        while (attempt < maxAttempts) {
            try {
                val result = if (existing != null) {
                    driveService.files().update(existing.id!!, driveFile, mediaContent).execute()
                } else {
                    driveService.files().create(driveFile, mediaContent).execute()
                }
                Log.d(TAG, "Uploaded package $fileName for share $shareId (attempt ${attempt + 1})")
                return@withContext result.id!!
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) {
                    Log.e(TAG, "Failed to upload package after $maxAttempts attempts", e)
                    throw e
                }
                val delay = (1L shl (attempt - 1)) * 1000 // 1s, 2s, 4s, 8s
                Log.w(TAG, "Upload failed, retrying in ${delay}ms (attempt $attempt/$maxAttempts)", e)
                Thread.sleep(delay)
            }
        }
        throw IOException("Failed to upload package after $maxAttempts attempts")
    }

    /**
     * Downloads an encrypted share package from Drive.
     */
    suspend fun downloadPackage(
        shareId: String,
        version: Int,
    ): ByteArray = withContext(dispatcher) {
        val folderId = getOrCreateShareFolder(shareId)
        val fileName = "package.v$version.bin"
        val file = findFileInFolder(folderId, fileName)
            ?: throw IOException("Package $fileName not found for share $shareId")

        var attempt = 0
        val maxAttempts = 5
        while (attempt < maxAttempts) {
            try {
                val outputStream = ByteArrayOutputStream()
                driveService.files().get(file.id!!)
                    .setAlt("media")
                    .executeMediaAndDownloadTo(outputStream)
                Log.d(TAG, "Downloaded package $fileName for share $shareId (attempt ${attempt + 1})")
                return@withContext outputStream.toByteArray()
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) {
                    Log.e(TAG, "Failed to download package after $maxAttempts attempts", e)
                    throw e
                }
                val delay = (1L shl (attempt - 1)) * 1000
                Log.w(TAG, "Download failed, retrying in ${delay}ms (attempt $attempt/$maxAttempts)", e)
                Thread.sleep(delay)
            }
        }
        throw IOException("Failed to download package after $maxAttempts attempts")
    }

    /**
     * Uploads share metadata (metadata.json).
     */
    suspend fun uploadMetadata(metadata: ShareMetadata): String = withContext(dispatcher) {
        val folderId = getOrCreateShareFolder(metadata.shareId)
        val fileName = "metadata.json"
        val jsonString = json.encodeToString(ShareMetadata.serializer(), metadata)
        val mediaContent = com.google.api.client.http.ByteArrayContent(
            "application/json",
            jsonString.toByteArray(StandardCharsets.UTF_8),
        )
        val driveFile = File().apply {
            name = fileName
            parents = listOf(folderId)
        }

        var attempt = 0
        val maxAttempts = 5
        while (attempt < maxAttempts) {
            try {
                val existing = findFileInFolder(folderId, fileName)
                val result = if (existing != null) {
                    driveService.files().update(existing.id!!, driveFile, mediaContent).execute()
                } else {
                    driveService.files().create(driveFile, mediaContent).execute()
                }
                Log.d(TAG, "Uploaded metadata for share ${metadata.shareId} (attempt ${attempt + 1})")
                return@withContext result.id!!
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) {
                    Log.e(TAG, "Failed to upload metadata after $maxAttempts attempts", e)
                    throw e
                }
                val delay = (1L shl (attempt - 1)) * 1000
                Log.w(TAG, "Metadata upload failed, retrying in ${delay}ms (attempt $attempt/$maxAttempts)", e)
                Thread.sleep(delay)
            }
        }
        throw IOException("Failed to upload metadata after $maxAttempts attempts")
    }

    /**
     * Downloads share metadata.
     */
    suspend fun downloadMetadata(shareId: String): ShareMetadata = withContext(dispatcher) {
        val folderId = getOrCreateShareFolder(shareId)
        val fileName = "metadata.json"
        val file = findFileInFolder(folderId, fileName)
            ?: throw IOException("Metadata not found for share $shareId")

        var attempt = 0
        val maxAttempts = 5
        while (attempt < maxAttempts) {
            try {
                val outputStream = ByteArrayOutputStream()
                driveService.files().get(file.id!!)
                    .setAlt("media")
                    .executeMediaAndDownloadTo(outputStream)
                val jsonString = outputStream.toString("UTF-8")
                return@withContext json.decodeFromString(ShareMetadata.serializer(), jsonString)
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) {
                    Log.e(TAG, "Failed to download metadata after $maxAttempts attempts", e)
                    throw e
                }
                val delay = (1L shl (attempt - 1)) * 1000
                Log.w(TAG, "Metadata download failed, retrying in ${delay}ms (attempt $attempt/$maxAttempts)", e)
                Thread.sleep(delay)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse metadata", e)
                throw IOException("Corrupt metadata for share $shareId", e)
            }
        }
        throw IOException("Failed to download metadata after $maxAttempts attempts")
    }

    /**
     * Marks a phrase as spent (updates metadata.spent = true).
     */
    suspend fun markPhraseSpent(shareId: String) = withContext(dispatcher) {
        val metadata = downloadMetadata(shareId)
        if (!metadata.spent) {
            val updated = metadata.spent()
            uploadMetadata(updated)
        }
    }

    /**
     * Gets the Drive Changes API start page token.
     */
    suspend fun getChangesStartToken(): String = withContext(dispatcher) {
        var attempt = 0
        val maxAttempts = 5
        while (attempt < maxAttempts) {
            try {
                val response = driveService.changes().getStartPageToken().execute()
                return@withContext response.startPageToken!!
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) throw e
                Thread.sleep((1L shl (attempt - 1)) * 1000)
            }
        }
        throw IOException("Failed to get changes start page token after $maxAttempts attempts")
    }

    /**
     * Lists changes since the given page token.
     * Returns list of changed share IDs.
     */
    suspend fun listChanges(pageToken: String): Pair<List<String>, String> = withContext(dispatcher) {
        var attempt = 0
        val maxAttempts = 5
        while (attempt < maxAttempts) {
            try {
                val request = driveService.changes().list(pageToken)
                    .setSpaces("appDataFolder")
                    .setIncludeRemoved(true)
                val response = request.execute()
                val changedShareIds = mutableSetOf<String>()
                for (change in response.changes) {
                    if (change.fileId != null) {
                        // Get the file to check if it's in a shares/ folder
                        try {
                            val file = driveService.files().get(change.fileId!!)
                                .setFields("id,name,parents")
                                .setSupportsAllDrives(false)
                                .execute()
                            if (file.parents != null) {
                                for (parentId in file.parents!!) {
                                    val parent = driveService.files().get(parentId)
                                        .setFields("id,name,parents")
                                        .execute()
                                    if (parent.name == "shares") {
                                        // The share folder is the parent of the changed file
                                        // Actually, structure is shares/{shareId}/{file}
                                        // So we need to go up one more level
                                        val grandParent = driveService.files().get(parent.parents!![0])
                                            .setFields("id,name")
                                            .execute()
                                        if (grandParent.name == "shares" && parent.name != "shares") {
                                            changedShareIds.add(parent.name)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore individual file errors
                        }
                    }
                }
                return@withContext changedShareIds.toList() to response.newStartPageToken!!
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) throw e
                Thread.sleep((1L shl (attempt - 1)) * 1000)
            }
        }
        throw IOException("Failed to list changes after $maxAttempts attempts")
    }

    /**
     * Gets or creates the shares/{shareId} folder in the App Folder.
     */
    private suspend fun getOrCreateShareFolder(shareId: String): String = withContext(dispatcher) {
        val appFolderId = getOrCreateAppFolder()
        val sharesFolderId = getOrCreateFolder(appFolderId, "shares")
        getOrCreateFolder(sharesFolderId, shareId)
    }

    private suspend fun getOrCreateAppFolder(): String = withContext(dispatcher) {
        var attempt = 0
        val maxAttempts = 5
        var result: String? = null
        while (attempt < maxAttempts && result == null) {
            try {
                val response = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='vaultaire_shares' and trashed=false")
                    .setFields("files(id,name)")
                    .execute()
                if (response.files.isNotEmpty()) {
                    result = response.files[0].id!!
                } else {
                    // Create the root folder
                    val folder = File().apply {
                        name = "vaultaire_shares"
                        mimeType = "application/vnd.google-apps.folder"
                        parents = listOf("appDataFolder")
                    }
                    val created = driveService.files().create(folder).setFields("id").execute()
                    result = created.id!!
                }
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) throw e
                Thread.sleep((1L shl (attempt - 1)) * 1000)
            }
        }
        result ?: throw IOException("Failed to create app folder after $maxAttempts attempts")
    }

    private suspend fun getOrCreateFolder(parentId: String, folderName: String): String = withContext(dispatcher) {
        var attempt = 0
        val maxAttempts = 5
        var result: String? = null
        while (attempt < maxAttempts && result == null) {
            try {
                val response = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='$folderName' and '$parentId' in parents and trashed=false")
                    .setFields("files(id,name)")
                    .execute()
                if (response.files.isNotEmpty()) {
                    result = response.files[0].id!!
                } else {
                    val folder = File().apply {
                        name = folderName
                        mimeType = "application/vnd.google-apps.folder"
                        parents = listOf(parentId)
                    }
                    val created = driveService.files().create(folder).setFields("id").execute()
                    result = created.id!!
                }
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) throw e
                Thread.sleep((1L shl (attempt - 1)) * 1000)
            }
        }
        result ?: throw IOException("Failed to create folder '$folderName' after $maxAttempts attempts")
    }

    private fun findFileInFolder(folderId: String, name: String): File? {
        var attempt = 0
        val maxAttempts = 5
        while (attempt < maxAttempts) {
            try {
                val response = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ("name='$name' and '$folderId' in parents and trashed=false")
                    .setFields("files(id,name,parents)")
                    .execute()
                return if (response.files.isNotEmpty()) response.files[0] else null
            } catch (e: IOException) {
                attempt++
                if (attempt >= maxAttempts) throw e
                Thread.sleep((1L shl (attempt - 1)) * 1000)
            }
        }
        throw IOException("Failed to find file '$name' in folder after $maxAttempts attempts")
    }
}