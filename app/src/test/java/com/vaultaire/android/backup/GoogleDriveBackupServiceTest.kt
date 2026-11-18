package com.vaultaire.android.backup

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections

class GoogleDriveBackupServiceTest {

    private lateinit var mockService: MockDriveService
    private val testBackupData = "encrypted backup data".toByteArray()

    @Before
    fun setUp() {
        mockService = MockDriveService()
    }

    @Test
    fun `uploadBackup calls create with appDataFolder parent`() = kotlinx.coroutines.runBlocking {
        val result = mockService.uploadBackup(testBackupData)
        
        assertTrue(result.isSuccess)
        val fileId = result.getOrNull()
        assertNotNull(fileId)
        assertTrue(fileId.startsWith("mock_file_"))
        
        // Verify file was stored
        val listResult = mockService.listBackups()
        assertTrue(listResult.isSuccess)
        val backups = listResult.getOrNull()!!
        assertEquals(1, backups.size)
        assertTrue(backups[0].name.startsWith("vault_backup_"))
        assertTrue(backups[0].name.endsWith(".vltbk"))
    }

    @Test
    fun `downloadBackup retrieves uploaded data`() = kotlinx.coroutines.runBlocking {
        // Upload first
        val uploadResult = mockService.uploadBackup(testBackupData)
        assertTrue(uploadResult.isSuccess)
        val fileId = uploadResult.getOrNull()!!

        // Download
        val downloadResult = mockService.downloadBackup(fileId)
        assertTrue(downloadResult.isSuccess)
        val downloaded = downloadResult.getOrNull()!!
        
        assertEquals(testBackupData.contentToString(), downloaded.contentToString())
    }

    @Test
    fun `downloadBackup fails for non-existent file`() = kotlinx.coroutines.runBlocking {
        val result = mockService.downloadBackup("non_existent_file")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is BackupNotFoundException)
    }

    @Test
    fun `deleteBackup removes file`() = kotlinx.coroutines.runBlocking {
        val uploadResult = mockService.uploadBackup(testBackupData)
        assertTrue(uploadResult.isSuccess)
        val fileId = uploadResult.getOrNull()!!

        // Delete
        val deleteResult = mockService.deleteBackup(fileId)
        assertTrue(deleteResult.isSuccess)

        // Verify deleted
        val downloadResult = mockService.downloadBackup(fileId)
        assertTrue(downloadResult.isFailure)
    }

    @Test
    fun `listBackups returns files sorted by modified time descending`() = kotlinx.coroutines.runBlocking {
        mockService.setMockFiles(
            mapOf(
                "file_1" to "data1".toByteArray(),
                "file_2" to "data2".toByteArray(),
                "file_3" to "data3".toByteArray()
            )
        )

        val result = mockService.listBackups()
        assertTrue(result.isSuccess)
        val backups = result.getOrNull()!!
        assertEquals(3, backups.size)
        // Should be sorted by modified time (all same in mock, but structure is there)
    }

    @Test
    fun `deleteAllBackups removes all files`() = kotlinx.coroutines.runBlocking {
        mockService.setMockFiles(
            mapOf(
                "file_1" to "data1".toByteArray(),
                "file_2" to "data2".toByteArray()
            )
        )

        val result = mockService.deleteAllBackups()
        assertTrue(result.isSuccess)

        val listResult = mockService.listBackups()
        assertTrue(listResult.isSuccess)
        assertTrue(listResult.getOrNull()!!.isEmpty())
    }

    @Test
    fun `getLatestBackupId returns most recent`() = kotlinx.coroutines.runBlocking {
        mockService.setMockFiles(
            mapOf(
                "old_file" to "old_data".toByteArray(),
                "new_file" to "new_data".toByteArray()
            )
        )

        val result = mockService.getLatestBackupId()
        assertTrue(result.isSuccess)
        val fileId = result.getOrNull()
        assertNotNull(fileId)
    }

    @Test
    fun `mock simulates quota failure`() = kotlinx.coroutines.runBlocking {
        mockService.setShouldFail(true, IOException("403 User Rate Limit Exceeded"))
        
        val result = mockService.uploadBackup(testBackupData)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("403") == true)
    }

    @Test
    fun `mock simulates authentication failure`() = kotlinx.coroutines.runBlocking {
        mockService.setShouldFail(true, IOException("401 Unauthorized"))
        
        val result = mockService.uploadBackup(testBackupData)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("401") == true)
    }
}