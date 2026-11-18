package com.vaultaire.android.vault

import javax.inject.Singleton
import javax.crypto.SecretKey

/**
 * Provides access to the Master Vault Key (MVK) for the currently unlocked vault.
 * 
 * The MVK is only available after successful vault unlock via pattern or recovery phrase.
 * This provider stores the MVK directly for easy retrieval by background workers.
 * 
 * Thread-safe singleton for use across the application including background workers.
 */
@Singleton
class VaultKeyProvider private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: VaultKeyProvider? = null
        
        @Synchronized
        fun getInstance(): VaultKeyProvider {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VaultKeyProvider().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Map of vaultId to Master Vault Key (MVK) directly.
     * The MVK is stored temporarily in memory and should be cleared when the vault is locked.
     */
    private val vaultMVKMap = mutableMapOf<String, SecretKey>()
    
    /**
     * Stores the MVK for an unlocked vault.
     * Called when vault is successfully unlocked.
     * 
     * @param vaultId The vault identifier
     * @param masterVaultKey The Master Vault Key to store
     */
    fun storeMVK(vaultId: String, masterVaultKey: SecretKey) {
        vaultMVKMap[vaultId] = masterVaultKey
    }
    
    /**
     * Retrieves the MVK for the given vault.
     * 
     * @param vaultId The vault identifier
     * @return The Master Vault Key, or throws if vault is not unlocked
     * @throws SecurityException if vault is not unlocked or MVK not available
     */
    fun getMasterVaultKey(vaultId: String): SecretKey {
        return vaultMVKMap[vaultId] ?: run {
            throw SecurityException("Vault $vaultId is not unlocked or MVK not available")
        }
    }
    
    /**
     * Checks if a vault is currently unlocked (has a stored MVK).
     * 
     * @param vaultId The vault identifier
     * @return true if the vault has an unlocked MVK stored
     */
    fun isVaultUnlocked(vaultId: String): Boolean {
        return vaultMVKMap.containsKey(vaultId)
    }
    
    /**
     * Clears the MVK for a vault (called when vault is locked).
     * 
     * @param vaultId The vault identifier
     */
    fun clearMVK(vaultId: String) {
        vaultMVKMap.remove(vaultId)
    }
    
    /**
     * Clears all stored MVKs (called on app shutdown or global lock).
     */
    fun clearAll() {
        vaultMVKMap.clear()
    }
}