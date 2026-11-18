package com.vaultaire.android.crypto

import com.vaultaire.android.crypto.fixtures.WordListsFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseStrengthEstimatorTest {

    private val estimator = PhraseStrengthEstimator(
        commonPhrases = WordListsFixtures.loadCommonPhrases(),
        dictionaryWords = WordListsFixtures.loadDictionaryWords()
    )

    @Test
    fun `accepts 6-word custom phrase with good entropy`() {
        val words = listOf("correct", "horse", "battery", "staple", "apple", "banana")
        val result = estimator.estimate(words)
        assertTrue(result.isAcceptable)
        assertTrue(result.entropyBits >= 50)
    }

    @Test
    fun `rejects phrase with less than 6 words`() {
        val words = listOf("one", "two", "three", "four", "five")
        val result = estimator.estimate(words)
        assertFalse(result.isAcceptable)
        assertEquals("Phrase must contain at least 6 words", result.rejectionReason)
    }

    @Test
    fun `rejects phrase with more than 24 words`() {
        val words = List(25) { "word" }
        val result = estimator.estimate(words)
        assertFalse(result.isAcceptable)
        assertEquals("Phrase must contain at most 24 words", result.rejectionReason)
    }

    @Test
    fun `rejects repetitive pattern`() {
        val words = List(6) { "word" }
        val result = estimator.estimate(words)
        assertFalse(result.isAcceptable)
        assertEquals("Phrase contains repetitive pattern", result.rejectionReason)
    }

    @Test
    fun `rejects common phrase`() {
        val words = "to be or not to be".split(" ")
        val result = estimator.estimate(words)
        assertFalse(result.isAcceptable)
        assertEquals("Phrase is too common", result.rejectionReason)
    }

    @Test
    fun `rejects sequential dictionary words`() {
        val words = listOf("apple", "banana", "cherry", "date", "elder", "fig")
        val result = estimator.estimate(words)
        assertFalse(result.isAcceptable)
        assertEquals("Phrase uses sequential dictionary words", result.rejectionReason)
    }

    @Test
    fun `calculates entropy correctly for known phrases`() {
        val words = listOf("river", "mountain", "bridge", "shadow", "key", "silent")
        val result = estimator.estimate(words)
        assertTrue(result.entropyBits > 50)
        assertTrue(result.entropyBits < 80)
    }
}
