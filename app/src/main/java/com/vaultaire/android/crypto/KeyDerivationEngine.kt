package com.vaultaire.android.crypto

import java.nio.charset.StandardCharsets
import java.util.Arrays
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

open class KeyDerivationEngine(private val saltManager: DeviceSaltManager) {

    companion object {
        const val KDF_ALGORITHM = "PBKDF2WithHmacSHA512"
        const val PATTERN_ITERATIONS = 600_000
        const val RECOVERY_ITERATIONS = 800_000
        const val SHARE_ITERATIONS = 250_000
        const val KEY_LENGTH_BITS = 256
        private const val BACKUP_SALT = "VAULTAIRE_BACKUP_SALT_V1"
    }

    open fun deriveVaultKey(patternBytes: ByteArray): SecretKey {
        return deriveKey(patternBytes, PATTERN_ITERATIONS)
    }

    open fun deriveKey(passwordBytes: ByteArray, iterations: Int): SecretKey {
        val salt = saltManager.getOrCreateDeviceSalt()
        val chars = CharArray(passwordBytes.size) { i -> (passwordBytes[i].toInt() and 0xFF).toChar() }

        var spec: PBEKeySpec? = null
        try {
            spec = PBEKeySpec(chars, salt, iterations, KEY_LENGTH_BITS)
            val skf = SecretKeyFactory.getInstance(KDF_ALGORITHM)
            val keyBytes = skf.generateSecret(spec).encoded

            val key = SecretKeySpec(keyBytes, "AES")
            Arrays.fill(keyBytes, 0.toByte())

            return key
        } finally {
            spec?.clearPassword()
            Arrays.fill(chars, '\u0000')
        }
    }

    open fun deriveBackupKey(patternBytes: ByteArray): SecretKey {
        val salt = BACKUP_SALT.toByteArray(StandardCharsets.UTF_8)
        val chars = CharArray(patternBytes.size) { i -> (patternBytes[i].toInt() and 0xFF).toChar() }

        var spec: PBEKeySpec? = null
        try {
            spec = PBEKeySpec(chars, salt, PATTERN_ITERATIONS, KEY_LENGTH_BITS)
            val skf = SecretKeyFactory.getInstance(KDF_ALGORITHM)
            val keyBytes = skf.generateSecret(spec).encoded

            val key = SecretKeySpec(keyBytes, "AES")
            Arrays.fill(keyBytes, 0.toByte())

            return key
        } finally {
            spec?.clearPassword()
            Arrays.fill(chars, '\u0000')
        }
    }
}
