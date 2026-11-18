package com.vaultaire.android.crypto

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.log2

/**
 * Handles all recovery phrase operations:
 * - Generation (grammatical 9-word sentences)
 * - Validation (custom phrases ≥6 words, strength estimation)
 * - Normalization
 * - Key derivation (PBKDF2-HMAC-SHA512, 800k, phrase-derived salt)
 */
open class RecoveryPhraseEngine(
    private val wordLists: WordLists,
    private val strengthEstimator: PhraseStrengthEstimator
) {

    companion object {
        const val GENERATED_WORD_COUNT = 9
        const val CUSTOM_MIN_WORDS = 6
        const val CUSTOM_MAX_WORDS = 24
        const val PBKDF2_ITERATIONS = 800_000
        const val KEY_LENGTH_BITS = 256
        const val PHRASE_SALT_LABEL = "VAULTAIRE_PHRASE_SALT_V1"
        const val KDF_ALGORITHM = "PBKDF2WithHmacSHA512"

        private val secureRandom = SecureRandom()
    }

    /**
     * Generates a cryptographically secure 9-word grammatical phrase.
     * @return List of 9 words in grammatical order
     */
    open fun generatePhrase(): List<String> {
        return wordLists.generateGrammaticalPhrase(secureRandom)
    }

    /**
     * Validates a custom phrase against security policy.
     * @param words List of words (6-24)
     * @return ValidationResult (valid, or error reason)
     */
    open fun validateCustomPhrase(words: List<String>): PhraseStrengthEstimator.Result {
        return strengthEstimator.estimate(words)
    }

    /**
     * Normalizes a phrase for deterministic key derivation.
     * Lowercase, trim, single spaces, NFKC normalization.
     */
    open fun normalizePhrase(words: List<String>): String {
        if (words.isEmpty()) {
            throw IllegalArgumentException("Phrase cannot be empty")
        }

        val joined = words.joinToString(" ")
            .trim()

        // NFKC normalization handles Unicode compatibility
        var normalized = Normalizer.normalize(joined, Normalizer.Form.NFKC)

        // Lowercase using default locale
        normalized = normalized.lowercase()

        // Collapse multiple spaces to single space
        return normalized.replace(Regex("""\s+"""), " ")
    }

    /**
     * Derives the 256-bit Recovery Key from a normalized phrase.
     * Uses deterministic phrase-derived salt (NOT device salt).
     * @param normalizedPhrase Output of normalizePhrase()
     * @return 256-bit AES SecretKey
     */
    open fun deriveRecoveryKey(normalizedPhrase: String): SecretKey {
        if (normalizedPhrase.isBlank()) {
            throw IllegalArgumentException("Phrase cannot be blank")
        }

        val salt = derivePhraseSalt(normalizedPhrase)
        val keyBytes = derivePbkdf2Key(normalizedPhrase.toByteArray(StandardCharsets.UTF_8), salt)

        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Derives the deterministic phrase salt for cross-platform compatibility.
     * Uses HKDF-SHA256 with label "VAULTAIRE_PHRASE_SALT_V1"
     */
    open fun derivePhraseSalt(normalizedPhrase: String): ByteArray {
        // HKDF-SHA256 implementation
        val ikm = normalizedPhrase.toByteArray(StandardCharsets.UTF_8)
        val salt = PHRASE_SALT_LABEL.toByteArray(StandardCharsets.UTF_8)
        val info = PHRASE_SALT_LABEL.toByteArray(StandardCharsets.UTF_8)

        // HKDF-Extract: HMAC-SHA256(salt, IKM)
        val prk = hmacSha256(salt, ikm)

        // HKDF-Expand: HMAC-SHA256(PRK, info || length)
        val length = 32 // 256 bits
        val expanded = ByteArray(length)
        var position = 0
        var t = ByteArray(0)
        var i = 1

        while (position < length) {
            val input = ByteArray(t.size + info.size + 1)
            var idx = 0
            System.arraycopy(t, 0, input, idx, t.size)
            idx += t.size
            System.arraycopy(info, 0, input, idx, info.size)
            input[input.size - 1] = i.toByte()

            t = hmacSha256(prk, input)
            val bytesToCopy = minOf(t.size, length - position)
            System.arraycopy(t, 0, expanded, position, bytesToCopy)
            position += bytesToCopy
            i++
        }

        return expanded
    }

    /**
     * Estimates entropy of a phrase (for custom phrase validation).
     */
    open fun estimateEntropy(words: List<String>): Double {
        if (words.isEmpty()) return 0.0
        return words.size * log2(2000.0) // Conservative estimate
    }

    private fun derivePbkdf2Key(password: ByteArray, salt: ByteArray): ByteArray {
        // Use password bytes directly for PBKDF2
        val spec = PBEKeySpec(
            String(password, StandardCharsets.UTF_8).toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val skf = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        return try {
            skf.generateSecret(spec).encoded
        } finally {
            // PBEKeySpec.clear() is not available in all API levels; zero out manually
            spec.clearPassword()
        }
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val keySpec = javax.crypto.spec.SecretKeySpec(key, "HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(data)
    }
}
