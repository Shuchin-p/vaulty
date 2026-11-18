package com.vaultaire.android.crypto

import kotlin.math.log2

/**
 * Estimates phrase entropy and validates against security policy.
 * Used for custom phrase validation during recovery phrase setup.
 */
open class PhraseStrengthEstimator(
    private val commonPhraseList: Set<String>,
    private val dictionaryWords: Set<String>
) {

    companion object {
        const val MIN_WORDS = 6
        const val MAX_WORDS = 24
        const val MIN_ENTROPY_BITS = 50.0
    }

    data class Result(
        val entropyBits: Double,
        val isAcceptable: Boolean,
        val rejectionReason: String? = null
    )

    /**
     * Estimates entropy and validates against policy.
     * @param words Normalized word list (lowercase, trimmed, single-spaced)
     * @return Result with entropy and accept/reject decision
     */
    open fun estimate(words: List<String>): Result {
        // Validate word count bounds
        if (words.size < MIN_WORDS) {
            return Result(0.0, false, "Phrase must contain at least $MIN_WORDS words")
        }
        if (words.size > MAX_WORDS) {
            return Result(0.0, false, "Phrase must contain at most $MAX_WORDS words")
        }

        // Check for repetitive patterns
        if (hasRepetitivePattern(words)) {
            return Result(0.0, false, "Phrase contains repetitive pattern")
        }

        // Check if phrase is too common
        val phrase = words.joinToString(" ")
        if (isCommonPhrase(phrase)) {
            return Result(0.0, false, "Phrase is too common")
        }

        // Check for sequential dictionary words
        if (hasSequentialDictionaryWords(words)) {
            return Result(0.0, false, "Phrase uses sequential dictionary words")
        }

        // Calculate entropy
        val entropy = calculateEntropy(words)

        // Check minimum entropy
        if (entropy < MIN_ENTROPY_BITS) {
            return Result(entropy, false, "Phrase does not have sufficient entropy")
        }

        return Result(entropy, true, null)
    }

    private fun hasRepetitivePattern(words: List<String>): Boolean {
        if (words.size < 2) return false

        // Check for repeated words (more than 50% same word = repetitive)
        val distinctCount = words.distinct().size
        if (distinctCount == 1) return true
        if (distinctCount.toDouble() / words.size < 0.5) return true

        // Check for simple patterns like "word word word word word word"
        val firstWord = words.first()
        val allSame = words.all { it == firstWord }
        if (allSame) return true

        // Check for 2-word repeating patterns
        if (words.size >= 4) {
            val pattern = words.take(2)
            val isPattern = words.chunked(2).all { chunk ->
                chunk.size == 2 && chunk[0] == pattern[0] && chunk[1] == pattern[1]
            }
            if (isPattern) return true
        }

        return false
    }

    private fun isCommonPhrase(phrase: String): Boolean {
        return commonPhraseList.contains(phrase)
    }

    private fun hasSequentialDictionaryWords(words: List<String>): Boolean {
        // Check if words appear in alphabetical/sequential order in dictionary
        if (words.size > dictionaryWords.size) return false

        // Normalize dictionary words to lowercase for comparison
        val normalizedDictionary = dictionaryWords.map { it.lowercase() }.toSet()

        // Check for sequential appearance
        val lowerWords = words.map { it.lowercase() }
        if (lowerWords.none { normalizedDictionary.contains(it) }) {
            return false // No dictionary words, not sequential
        }

        // Check if words are in sequence relative to dictionary
        val indices = lowerWords.mapNotNull { word ->
            normalizedDictionary.toList().indexOf(word)
        }

        if (indices.size < 2) return false

        // Check if indices are sequential (consecutive numbers)
        val isSequential = indices.zipWithNext().all { (a, b) ->
            kotlin.math.abs(a - b) <= 2  // Allow small gap for natural words
        }

        // Require at least 3 sequential dictionary words
        return isSequential && indices.size >= 3
    }

    private fun calculateEntropy(words: List<String>): Double {
        if (words.isEmpty()) return 0.0

        // Pool sizes for each word position (based on grammatical template)
        // For custom phrases, we use approximate pool sizes
        // Articles: 3, Adjectives: ~500+, Nouns: ~1000+, Verbs: ~500+, Prepositions: ~30, Adverbs: ~200+

        // For entropy calculation, we assume a reasonable pool size per word position
        // Based on the grammatical template, each position has different pool sizes
        val poolSizes = listOf(
            3.0,    // Articles (position 0, 5)
            500.0,  // Adjectives (position 1, 6)
            1000.0, // Nouns (position 2, 7)
            500.0,  // Verbs (position 3)
            30.0,   // Prepositions (position 4)
            200.0   // Adverbs (position 8)
        )

        // For custom phrases, estimate based on common word pool
        val effectivePoolSize = 2000.0 // Reasonable estimate for custom word pools

        // Simple entropy calculation: log2(pool_size) per word
        // For a phrase of N random words from a pool of size P: entropy = N * log2(P)
        return words.size * log2(effectivePoolSize)
    }
}
