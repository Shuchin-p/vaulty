package com.vaultaire.android.vault

import android.content.Context
import java.io.File
import java.util.UUID
import javax.crypto.SecretKey
import javax.inject.Inject

/**
 * Manages multiple vaults on disk with plausible deniability.
 * No vault selection UI is shown on launch. Each configured pattern addresses its own encrypted index.
 */
class VaultManager @Inject constructor(private val context: Context) {

    private val vaultsBaseDir: File
        get() = File(context.filesDir, "vaults").apply { mkdirs() }

    /**
     * Creates a new vault with a fresh Master Vault Key (MVK).
     * The MVK is wrapped with the pattern-derived Vault Derivation Key (VDK).
     */
    fun createVault(vaultId: String, vdk: SecretKey, masterVaultKey: SecretKey): File {
        val vaultFile = File(vaultsBaseDir, vaultId + ".vlt")
        val header = VaultHeader(
            vaultId = vaultId,
            envelope = com.vaultaire.android.crypto.EnvelopeEncryptionEngine().wrapMasterKey(masterVaultKey, vdk)
        )
        vaultFile.outputStream().use { stream ->
            header.serialize(stream)
        }
        return vaultFile
    }

    /**
     * Attempts to find and unwrap a vault matching the provided VDK.
     * Throws SecurityException if pattern is incorrect or vault not found.
     */
    fun unlockVault(vaultId: String, vdk: SecretKey): SecretKey {
        val vaultFile = File(vaultsBaseDir, vaultId + ".vlt")
        if (!vaultFile.exists()) throw SecurityException("Vault not found")

        val header = VaultHeader.deserialize(vaultFile.inputStream())
        val engine = com.vaultaire.android.crypto.EnvelopeEncryptionEngine()
        return engine.unwrapMasterKey(header.envelope, vdk)
    }

    /**
     * Lists all vault files on disk. Returns an empty list if no vaults exist.
     */
    fun listVaultFiles(): List<File> {
        return vaultsBaseDir.listFiles { _, name -> name.endsWith(".vlt") }?.toList() ?: emptyList()
    }

    /**
     * Generates a random Vault ID.
     */
    fun generateVaultId(): String = UUID.randomUUID().toString()

    /**
     * Deletes a vault file from local storage.
     */
    fun deleteVault(vaultId: String): Boolean {
        val vaultFile = File(vaultsBaseDir, vaultId + ".vlt")
        return vaultFile.delete()
    }

    /**
     * Returns the directory where encrypted media files (.vlt) for a vault are stored.
     * Creates the directory if it doesn't exist.
     */
    fun getVaultDir(vaultId: String): File {
        return File(vaultsBaseDir, vaultId).apply { mkdirs() }
    }

    /**
     * Purges all local vault indices and files for non-decoy vaults (Duress Mode).
     */
    fun purgeNonDecoyVaultFiles() {
        vaultsBaseDir.listFiles()?.forEach { file ->
            try {
                val header = VaultHeader.deserialize(file.inputStream())
                if (!header.isDuressDecoy) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Corrupted or unrecognized file, attempt direct deletion
                file.delete()
            }
        }
    }
}
