package com.vaultaire.android.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import androidx.work.ForegroundInfo
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.pow
import java.io.IOException

/**
 * Worker for uploading encrypted backup to Google Drive with retry logic.
 * Uses exponential backoff and fails fast on quota errors.
 */
class UploadBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "upload_backup"
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "backup_upload_channel"
        const val MAX_RETRIES = 3
        private const val PREF_KEY_UPLOAD_RETRY_COUNT = "upload_retry_count"
        private const val ARG_BACKUP_DATA = "backup_data"
        private const val ARG_FILE_ID = "file_id"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForeground(createForegroundInfo())

        val backupData = inputData.getByteArray(ARG_BACKUP_DATA)
        if (backupData == null) {
            return@withContext Result.failure()
        }

        val prefs = applicationContext.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
        var retryCount = prefs.getInt(PREF_KEY_UPLOAD_RETRY_COUNT, 0)

        while (retryCount <= MAX_RETRIES) {
            try {
                // Get Google Drive service
                val driveService = getDriveService()
                
                // Upload backup
                val result = driveService.uploadBackup(backupData)
                
                if (result.isSuccess) {
                    // Success - clear retry count
                    prefs.edit().remove(PREF_KEY_UPLOAD_RETRY_COUNT).apply()
                    return@withContext Result.success()
                } else {
                    val exception = result.exceptionOrNull()
                    if (exception != null && isQuotaExceeded(exception)) {
                        // Quota exceeded - fail fast, notify user
                        prefs.edit().remove(PREF_KEY_UPLOAD_RETRY_COUNT).apply()
                        notifyQuotaExceeded()
                        return@withContext Result.failure()
                    }
                }
            } catch (e: Exception) {
                if (isQuotaExceeded(e)) {
                    notifyQuotaExceeded()
                    prefs.edit().remove(PREF_KEY_UPLOAD_RETRY_COUNT).apply()
                    return@withContext Result.failure()
                }
            }

            // Increment retry count
            retryCount++
            prefs.edit().putInt(PREF_KEY_UPLOAD_RETRY_COUNT, retryCount).apply()

            // Exponential backoff: 2^retryCount minutes
            val backoffMinutes = 2.0.pow(retryCount.toDouble()).toLong()
            delay(backoffMinutes * 60 * 1000)
        }

        // Max retries exceeded
        Result.failure()
    }

    private suspend fun getDriveService(): MockDriveService {
        // TODO: Get actual account and create service
        // For now, return mock
        return MockDriveService()
    }

    private fun isQuotaExceeded(e: Throwable): Boolean {
        val message = e.message ?: ""
        return message.contains("403") || 
               message.contains("quota") || 
               message.contains("storage") ||
               message.contains("Quota") ||
               message.contains("Storage")
    }

    private fun notifyQuotaExceeded() {
        // TODO: Show system notification about quota exceeded
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Uploading backup...")
            .setContentText("Encrypted backup upload in progress")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}