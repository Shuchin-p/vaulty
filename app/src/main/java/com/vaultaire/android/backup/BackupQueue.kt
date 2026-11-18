package com.vaultaire.android.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages a queue of backup requests for offline/failed upload handling.
 * Uses WorkManager for persistent scheduling.
 */
class BackupQueue(private val context: Context) {

    private val pendingBackups = ConcurrentLinkedQueue<BackupRequest>()
    private val workManager = WorkManager.getInstance(context)

    data class BackupRequest(
        val vaultChecksum: String,
        val timestamp: Long = System.currentTimeMillis(),
        val retryCount: Int = 0,
        val priority: Priority = Priority.NORMAL
    )

    enum class Priority { LOW, NORMAL, HIGH }

    /**
     * Adds a backup request to the queue.
     * If network is available, schedules immediately. Otherwise queues for later.
     */
    fun enqueue(vaultChecksum: String, priority: Priority = Priority.NORMAL) {
        val request = BackupRequest(vaultChecksum, priority = priority)
        pendingBackups.add(request)
        scheduleUpload()
    }

    /**
     * Schedules the upload worker with network constraints.
     */
    private fun scheduleUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = OneTimeWorkRequestBuilder<UploadBackupWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            UploadBackupWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            work
        )
    }

    /**
     * Processes the queue - called when network becomes available.
     */
    fun processQueue() {
        if (pendingBackups.isNotEmpty()) {
            scheduleUpload()
        }
    }

    /**
     * Clears the pending queue (e.g., after successful upload).
     */
    fun clearQueue() {
        pendingBackups.clear()
    }

    /**
     * Gets the number of pending backup requests.
     */
    fun pendingCount(): Int = pendingBackups.size

    /**
     * Checks if there are pending backups.
     */
    fun hasPendingBackups(): Boolean = pendingBackups.isNotEmpty()

    /**
     * Gets all pending requests (for debugging/inspection).
     */
    fun getPendingRequests(): List<BackupRequest> = pendingBackups.toList()
}
