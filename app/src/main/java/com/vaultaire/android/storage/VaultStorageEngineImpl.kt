package com.vaultaire.android.storage

import android.content.Context
import android.content.SharedPreferences
import com.vaultaire.android.sync.model.SyncMetadata
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.SecretKey
import javax.inject.Inject

/**
 * Implementation of VaultStorageEngine for encrypted file storage.
 * Uses 64KB chunk-based AES-GCM encryption for files.
 */
class VaultStorageEngineImpl @Inject constructor(
    private val context: Context
) : VaultStorageEngine {

    companion object {
        const val BAND_SIZE = 64  // 64KB chunks
        const val COVER_FILE_SIZE = 4096  // 4KB cover file size
    }

    private val vaultDir: File = File(context.filesDir, "vaults").apply { mkdirs() }
    private val prefs: SharedPreferences = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override fun createEncryptedOutputStream(targetFile: File, key: SecretKey): EncryptedOutputStream {
        return EncryptedOutputStream(
            outputStream = FileOutputStream(targetFile),
            key = key,
            chunkSize = BAND_SIZE * 1024
        )
    }

    override fun openEncryptedInputStream(sourceFile: File, key: SecretKey): java.io.InputStream {
        // Note: This is a simplified implementation
        // In production, would need proper streaming decryption
        throw NotImplementedError("Direct InputStream not fully implemented - use RandomAccessEncryptedSource")
    }

    override fun openRandomAccessSource(sourceFile: File, key: SecretKey): RandomAccessEncryptedSource {
        return RandomAccessEncryptedSourceImpl(sourceFile, key)
    }

    /**
     * Gets or creates the vault-specific directory.
     */
    fun getVaultDirectory(vaultId: String): File {
        val vaultDir = File(vaultDir, vaultId)
        if (!vaultDir.exists()) vaultDir.mkdirs()
        return vaultDir
    }

    /**
     * Lists all vault directories.
     */
    fun listVaults(): List<File> {
        return vaultDir.listFiles()?.toList() ?: emptyList()
    }

    /**
     * Deletes a vault and its contents.
     */
    fun deleteVault(vaultId: String): Boolean {
        val vaultDir = File(vaultDir, vaultId)
        return if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
            true
        } else {
            false
        }
    }

    override fun loadSyncMetadata(): SyncMetadata? {
        val jsonFile = File(vaultDir, "sync_metadata.json")
        if (!jsonFile.exists()) return null
        
        return try {
            val content = jsonFile.readText()
            json.decodeFromString(SyncMetadata.serializer(), content)
        } catch (e: Exception) {
            null
        }
    }

    override fun saveSyncMetadata(metadata: SyncMetadata) {
        val jsonFile = File(vaultDir, "sync_metadata.json")
        val content = json.encodeToString(SyncMetadata.serializer(), metadata)
        jsonFile.writeText(content)
    }

    override fun getLastSyncToken(): String? {
        return prefs.getString("last_sync_token", null)
    }

    override fun saveLastSyncToken(token: String) {
        prefs.edit().putString("last_sync_token", token).apply()
    }
}