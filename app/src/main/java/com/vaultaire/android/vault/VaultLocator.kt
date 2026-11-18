package com.vaultaire.android.vault

import java.security.MessageDigest

/**
 * Locator derivation for vault identification.
 * Computes a deterministic identifier from the Vault Derivation Key (VDK).
 */
class VaultLocator {

    companion object {
        const val INDEX_PREFIX = "vault_index_"
        const val COVER_PREFIX = "vault_index_cover_"
        const val INDEX_SUFFIX = ".bin"
        
        /**
         * Derives a vault locator from the Vault Derivation Key (VDK).
         * Returns first 16 bytes (32 hex chars) of SHA-256(VDK).
         */
        fun deriveLocator(vdk: ByteArray): String {
            val sha256 = MessageDigest.getInstance("SHA-256")
            val hash = sha256.digest(vdk)
            val first16Bytes = hash.copyOf(16)
            return first16Bytes.joinToString("") { "%02x".format(it) }
        }
        
        /**
         * Gets the file path for a vault index.
         */
        fun indexPath(vaultId: String): String {
            return "$INDEX_PREFIX${vaultId}$INDEX_SUFFIX"
        }
        
        /**
         * Gets the file path for a cover file (plausible deniability).
         */
        fun coverPath(vaultId: String): String {
            return "$COVER_PREFIX${vaultId}$INDEX_SUFFIX"
        }
    }
}
