package com.vaultaire.android.crypto

import java.nio.charset.StandardCharsets
import java.util.Arrays
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupKeyDerivationEngine(private val saltManager: DeviceSaltManager) {

    companion object {
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA512"
        private const val BACKUP_ITERATIONS = 600_000
        private const val RECOVERY_ITERATIONS = 800_000
        private const val KEY_LENGTH_BITS = 256
        private const val BACKUP_SALT_CONTEXT = "VAULTAIRE_BACKUP_SALT_V1"
        private const val RECOVERY_SALT_CONTEXT = "VAULTAIRE_RECOVERY_SALT_V1"
    }

    fun deriveBackupKey(patternBytes: ByteArray): SecretKey {
        val deviceSalt = saltManager.getOrCreateDeviceSalt()
        val backupSalt = (BACKUP_SALT_CONTEXT + deviceSalt.hex()).toByteArray(StandardCharsets.UTF_8)
        val chars = CharArray(patternBytes.size) { i -> patternBytes[i].toInt().toChar() }

        try {
            val spec = PBEKeySpec(chars, backupSalt, BACKUP_ITERATIONS, KEY_LENGTH_BITS)
            val skf = SecretKeyFactory.getInstance(KDF_ALGORITHM)
            val keyBytes = skf.generateSecret(spec).encoded
            return SecretKeySpec(keyBytes, "AES")
        } finally {
            Arrays.fill(chars, '\u0000')
        }
    }

    fun deriveRecoveryKey(phrase: String): SecretKey {
        val normalized = normalizePhrase(phrase)
        val deviceSalt = saltManager.getOrCreateDeviceSalt()
        val recoverySalt = (RECOVERY_SALT_CONTEXT + deviceSalt.hex()).toByteArray(StandardCharsets.UTF_8)

        val spec = PBEKeySpec(
            normalized.toCharArray(),
            recoverySalt,
            RECOVERY_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val skf = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val keyBytes = skf.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun normalizePhrase(phrase: String): String {
        return phrase.trim().lowercase().replace(Regex("\\s+"), " ")
    }

    private fun ByteArray.hex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}