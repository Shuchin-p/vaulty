package com.vaultaire.android.sync.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit

/**
 * Schedules sync work using WorkManager.
 *
 * - Periodic: every 15 min when online (NetworkType.CONNECTED), battery not low
 * - Expedited: OneTimeWorkRequest on local change/FCM (setExpedited=true)
 * - Tag: "sync-$vaultId"
 */
class SyncScheduler(
    private val context: Context
) {

    companion object {
        const val WORK_TAG_PREFIX = "sync-"
        const val PERIODIC_INTERVAL_MINUTES = 15
        const val EXPEDITED_BACKOFF_MINUTES = 5
    }

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedules periodic sync work for a vault.
     */
    fun schedulePeriodicSync(vaultId: String) {
        val tag = WORK_TAG_PREFIX + vaultId
        
        val restrictions = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val vaultIdInput = androidx.work.Data.Builder().putString("vaultId", vaultId).build()
        
        val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL_MINUTES.toLong(), TimeUnit.MINUTES)
            .setConstraints(restrictions)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, EXPEDITED_BACKOFF_MINUTES.toLong(), TimeUnit.MINUTES)
            .addTag(tag)
            .setInputData(vaultIdInput)
            .build()

        workManager.enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * Schedules an immediate (expedited) sync for a vault.
     * Used when local changes occur or FCM notification received.
     */
    fun scheduleImmediateSync(vaultId: String) {
        val tag = WORK_TAG_PREFIX + vaultId
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val vaultIdInput = androidx.work.Data.Builder().putString("vaultId", vaultId).build()
        
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(tag)
            .setInputData(vaultIdInput)
            .build()

        workManager.enqueue(request)
    }

    /**
     * Cancels all sync work for a vault.
     */
    fun cancelSync(vaultId: String) {
        val tag = WORK_TAG_PREFIX + vaultId
        workManager.cancelAllWorkByTag(tag)
    }

    /**
     * Cancels all sync work (all vaults).
     */
    fun cancelAllSync() {
        // WorkManager doesn't have a direct "cancel all by tag prefix" method
        // In practice, track vault IDs and cancel each
    }
}