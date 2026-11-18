package com.vaultaire.android.crypto.fixtures

import com.vaultaire.android.crypto.KeyDerivationEngine
import io.mockk.mockk
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Mock KeyDerivationEngine that derives keys deterministically from input bytes.
 * Used in tests where we need predictable key derivation without device salt.
 */
class MockKeyDerivationEngine : KeyDerivationEngine(mockk(relaxed = true)) {

    companion object {
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA512"
        private const val ITERATIONS = 600_000
        private const val KEY_LENGTH_BITS = 256
        private const val MOCK_SALT = "mock_salt_for_testing"
    }

    override fun deriveVaultKey(patternBytes: ByteArray): SecretKey {
        val salt = MOCK_SALT.toByteArray(Charsets.UTF_8)
        val chars = CharArray(patternBytes.size) { i -> patternBytes[i].toInt().toChar() }
        try {
            val spec = javax.crypto.spec.PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
            val skf = javax.crypto.SecretKeyFactory.getInstance(KDF_ALGORITHM)
            val keyBytes = skf.generateSecret(spec).encoded
            return SecretKeySpec(keyBytes, "AES")
        } finally {
            java.util.Arrays.fill(chars, 0.toChar())
        }
    }

    override fun deriveBackupKey(patternBytes: ByteArray): SecretKey {
        return deriveVaultKey(patternBytes)
    }
}
