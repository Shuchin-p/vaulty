package com.vaultaire.android.crypto.fixtures

import com.vaultaire.android.crypto.PhraseStrengthEstimator
import com.vaultaire.android.crypto.RecoveryPhraseEngine
import com.vaultaire.android.crypto.WordLists
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Mock RecoveryPhraseEngine for unit tests.
 * Uses a fixed phrase and simplified key derivation for deterministic tests.
 */
class MockRecoveryPhraseEngine(
    private val wordLists: WordLists = WordListsFixtures.minimal(),
    private val estimator: PhraseStrengthEstimator = MockPhraseStrengthEstimator()
) : RecoveryPhraseEngine(wordLists, estimator) {

    companion object {
        private const val MOCK_ITERATIONS = 1000
        private const val KEY_LENGTH_BITS = 256
        private const val MOCK_SALT = "mock_phrase_salt"
    }

    override fun deriveRecoveryKey(normalizedPhrase: String): SecretKey {
        if (normalizedPhrase.isBlank()) {
            throw IllegalArgumentException("Phrase cannot be blank")
        }
        val salt = MOCK_SALT.toByteArray(Charsets.UTF_8)
        val spec = javax.crypto.spec.PBEKeySpec(
            normalizedPhrase.toCharArray(),
            salt,
            MOCK_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val keyBytes = skf.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
