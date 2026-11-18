package com.vaultaire.android.backup

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Drive backup service using the Drive REST API v3.
 * Uploads to the AppData folder (drive.appdata scope) which is hidden from the user.
 */
open class GoogleDriveBackupService(
    private val context: Context,
    private val account: Account
) {

    private val driveService: Drive by lazy {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).setSelectedAccount(account)

        Drive.Builder(transport, jsonFactory, credential)
            .setApplicationName("Vaultaire Android")
            .build()
    }

    /**
     * Uploads an encrypted backup to Google Drive AppData folder.
     * Uses resumable upload with 1 MB chunks for progress tracking.
     * 
     * @param encryptedBackup The encrypted backup data
     * @return Result containing the file ID on success, or exception on failure
     */
    open suspend fun uploadBackup(encryptedBackup: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        val fileMetadata = File()
        fileMetadata.name = "vault_backup_\${System.currentTimeMillis()}.vltbk"
        fileMetadata.parents = listOf("appDataFolder")

        val mediaContent = InputStreamContent("application/octet-stream", ByteArrayInputStream(encryptedBackup))

        try {
            val request = driveService.files().create(fileMetadata, mediaContent)
            request.mediaHttpUploader.isDirectUploadEnabled = false
            request.mediaHttpUploader.chunkSize = 1 * 1024 * 1024 // 1 MB chunks
            val response = request.execute()
            Result.success(response.id!!)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a backup file from Google Drive AppData folder.
     * 
     * @param fileId The Drive file ID to download
     * @return Result containing the backup data on success, or exception on failure
     */
    open suspend fun downloadBackup(fileId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val request = driveService.files().get(fileId)
            request.alt = "media"
            val outputStream = ByteArrayOutputStream()
            request.executeMediaAndDownloadTo(outputStream)
            Result.success(outputStream.toByteArray())
        } catch (e: FileNotFoundException) {
            Result.failure(BackupNotFoundException("Backup not found: \$fileId", e))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lists all backup files in the Google Drive AppData folder.
     * 
     * @return Result containing list of backup files with metadata
     */
    open suspend fun listBackups(): Result<List<BackupFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val request = driveService.files().list()
            request.spaces = "appDataFolder"
            request.fields = "files(id, name, createdTime, size, modifiedTime)"
            request.q = "name contains 'vault_backup_' and name contains '.vltbk'"
            
            val fileList: FileList = request.execute()
            val files = fileList.files ?: emptyList()
            
            val backups = files.map { file ->
                BackupFileInfo(
                    id = file.id!!,
                    name = file.name!!,
                    size = file.size?.toLong() ?: 0,
                    createdTime = file.createdTime?.value ?: 0,
                    modifiedTime = file.modifiedTime?.value ?: 0
                )
            }.sortedByDescending { it.modifiedTime }
            
            Result.success(backups)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a backup file from Google Drive AppData folder.
     * 
     * @param fileId The Drive file ID to delete
     * @return Result.success on success, or exception on failure
     */
    open suspend fun deleteBackup(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            driveService.files().delete(fileId).execute()
            Result.success(Unit)
        } catch (e: FileNotFoundException) {
            Result.success(Unit) // Already deleted is fine
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes all backup files from Google Drive AppData folder.
     * Called when user disables backup in settings.
     */
    open suspend fun deleteAllBackups(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val listResult = listBackups()
            if (listResult.isSuccess) {
                listResult.getOrNull()?.forEach { backup ->
                    deleteBackup(backup.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the latest backup file ID.
     */
    open suspend fun getLatestBackupId(): Result<String?> = withContext(Dispatchers.IO) {
        val listResult = listBackups()
        if (listResult.isSuccess) {
            Result.success(listResult.getOrNull()?.firstOrNull()?.id)
        } else {
            Result.failure(listResult.exceptionOrNull()!!)
        }
    }
}

data class BackupFileInfo(
    val id: String,
    val name: String,
    val size: Long,
    val createdTime: Long,
    val modifiedTime: Long
)

class BackupNotFoundException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Mock implementation for testing.
 */
class MockDriveService {
    
    private var mockFiles = mutableMapOf<String, ByteArray>()
    private var shouldFail = false
    private var failException: Exception? = null

    suspend fun uploadBackup(encryptedBackup: ByteArray): Result<String> {
        if (shouldFail) return Result.failure(failException!!)
        val fileId = "mock_file_\${System.currentTimeMillis()}"
        mockFiles[fileId] = encryptedBackup
        return Result.success(fileId)
    }

    suspend fun downloadBackup(fileId: String): Result<ByteArray> {
        if (shouldFail) return Result.failure(failException!!)
        return mockFiles[fileId]?.let { Result.success(it) } ?: Result.failure(BackupNotFoundException("Not found: \$fileId"))
    }

    suspend fun listBackups(): Result<List<BackupFileInfo>> {
        if (shouldFail) return Result.failure(failException!!)
        val backups = mockFiles.map { (id, data) ->
            BackupFileInfo(id, "vault_backup_\${id}.vltbk", data.size.toLong(), System.currentTimeMillis(), System.currentTimeMillis())
        }.toList()
        return Result.success(backups)
    }

    suspend fun deleteBackup(fileId: String): Result<Unit> {
        if (shouldFail) return Result.failure(failException!!)
        mockFiles.remove(fileId)
        return Result.success(Unit)
    }

    fun setMockFiles(files: Map<String, ByteArray>) {
        mockFiles = files.toMutableMap()
    }

    fun setShouldFail(fail: Boolean, exception: Exception? = null) {
        shouldFail = fail
        failException = exception
    }

    fun clear() {
        mockFiles.clear()
        shouldFail = false
        failException = null
    }
}