package com.vaultaire.android.vault

import java.io.File

/**
 * Manages vault index files for storing file records and vault metadata.
 */
class VaultIndexManager(
    private val vaultDir: File
) {
    
    companion object {
        const val INDEX_FILENAME = "index.bin"
        const val COVER_FILENAME = "cover.bin"
    }
    
    private val indexFile: File = File(vaultDir, INDEX_FILENAME)
    private val coverFile: File = File(vaultDir, COVER_FILENAME)
    
    /**
     * Loads the vault index from disk.
     */
    fun loadIndex(): OpenVault? {
        if (!indexFile.exists()) return null
        
        val bytes = indexFile.readBytes()
        return try {
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString(OpenVault.serializer(), String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Saves the vault index to disk.
     */
    fun saveIndex(vault: OpenVault) {
        val json = kotlinx.serialization.json.Json { prettyPrint = true }
        val bytes = json.encodeToString(OpenVault.serializer(), vault)
            .toByteArray(Charsets.UTF_8)
        indexFile.writeBytes(bytes)
    }
    
    /**
     * Creates or updates the cover file for plausible deniability.
     */
    fun updateCoverFile() {
        // Create a minimal cover file that looks like valid vault data
        val coverContent = ByteArray(4096) { 0 }
        coverFile.writeBytes(coverContent)
    }
    
    /**
     * Deletes the vault index.
     */
    fun deleteIndex() {
        indexFile.delete()
        coverFile.delete()
    }
    
    /**
     * Checks if index exists.
     */
    fun hasIndex(): Boolean {
        return indexFile.exists() && indexFile.length() > 0
    }
}
