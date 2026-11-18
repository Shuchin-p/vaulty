package com.vaultaire.android.storage

import com.vaultaire.android.sync.model.SyncMetadata
import java.io.File
import javax.crypto.SecretKey

interface VaultStorageEngine {
    fun createEncryptedOutputStream(targetFile: File, key: SecretKey): EncryptedOutputStream
    fun openEncryptedInputStream(sourceFile: File, key: SecretKey): java.io.InputStream
    fun openRandomAccessSource(sourceFile: File, key: SecretKey): RandomAccessEncryptedSource
    
    /** Load sync metadata for a vault */
    fun loadSyncMetadata(): SyncMetadata?
    
    /** Save sync metadata for a vault */
    fun saveSyncMetadata(metadata: SyncMetadata)
    
    /** Get the last sync token for a vault */
    fun getLastSyncToken(): String?
    
    /** Save the last sync token for a vault */
    fun saveLastSyncToken(token: String)
}

interface RandomAccessEncryptedSource : AutoCloseable {
    val totalPlaintextLength: Long
    val chunkSize: Int
    
    /**
     * Reads decrypted bytes into [dest] starting from absolute [plaintextOffset].
     * Returns total bytes read into [dest].
     */
    fun readAt(plaintextOffset: Long, dest: ByteArray, destOffset: Int, length: Int): Int
}