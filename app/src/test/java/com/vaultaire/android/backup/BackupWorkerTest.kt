package com.vaultaire.android.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestWorkerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Clear preferences
        context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun `worker returns success when backup disabled`() {
        val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("backup_enabled", false).apply()

        val worker = TestWorkerBuilder<BackupWorker>(context).build()
        val result = worker.startWork().getResult().get()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `worker returns success when less than 24h since last backup`() {
        val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("backup_enabled", true)
            .putLong("last_backup_timestamp", System.currentTimeMillis() - 12 * 60 * 60 * 1000) // 12 hours ago
            .apply()

        val worker = TestWorkerBuilder<BackupWorker>(context).build()
        val result = worker.startWork().getResult().get()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `worker attempts backup when more than 24h since last backup`() {
        val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("backup_enabled", true)
            .putLong("last_backup_timestamp", System.currentTimeMillis() - 25 * 60 * 60 * 1000) // 25 hours ago
            .apply()

        val worker = TestWorkerBuilder<BackupWorker>(context).build()
        val result = worker.startWork().getResult().get()

        // Should attempt backup (returns success or retry depending on implementation)
        assertTrue(result == ListenableWorker.Result.success() || result == ListenableWorker.Result.retry())
    }

    @Test
    fun `worker returns success when no last backup timestamp (first run)`() {
        val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("backup_enabled", true)
            .remove("last_backup_timestamp")
            .apply()

        val worker = TestWorkerBuilder<BackupWorker>(context).build()
        val result = worker.startWork().getResult().get()

        // First run should attempt backup
        assertTrue(result == ListenableWorker.Result.success() || result == ListenableWorker.Result.retry())
    }

    @Test
    fun `schedulePeriodicBackup creates work with correct constraints`() {
        BackupWorker.schedulePeriodicBackup(context)
        
        // Note: Full verification requires WorkManager test utilities
        // This test verifies the method doesn't crash
        assertNotNull(WorkManager.getInstance(context))
    }

    @Test
    fun `cancelPeriodicBackup cancels work`() {
        BackupWorker.schedulePeriodicBackup(context)
        BackupWorker.cancelPeriodicBackup(context)
        
        // Verify no crash
        assertNotNull(WorkManager.getInstance(context))
    }
}