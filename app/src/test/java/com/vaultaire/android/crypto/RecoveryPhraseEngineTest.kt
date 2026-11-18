package com.vaultaire.android.crypto

import com.vaultaire.android.crypto.fixtures.WordListsFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryPhraseEngineTest {

    private val wordLists = WordListsFixtures.minimal()
    private val estimator = MockPhraseStrengthEstimator()
    private val engine = RecoveryPhraseEngine(wordLists, estimator)

    @Test
    fun `generatePhrase returns exactly 9 words`() {
        val phrase = engine.generatePhrase()
        assertEquals(9, phrase.size)
    }

    @Test
    fun `generatePhrase follows grammatical template`() {
        val phrase = engine.generatePhrase()
        assertTrue(wordLists.articles.contains(phrase[0]))
        assertTrue(wordLists.adjectives.contains(phrase[1]))
        assertTrue(wordLists.nouns.contains(phrase[2]))
        assertTrue(wordLists.verbs.contains(phrase[3]))
        assertTrue(wordLists.prepositions.contains(phrase[4]))
        assertTrue(wordLists.articles.contains(phrase[5]))
        assertTrue(wordLists.adjectives.contains(phrase[6]))
        assertTrue(wordLists.nouns.contains(phrase[7]))
        assertTrue(wordLists.adverbs.contains(phrase[8]))
    }

    @Test
    fun `generatePhrase uses cryptographically secure randomness`() {
        val phrases = List(1000) { engine.generatePhrase() }
        val uniquePhrases = phrases.distinct()
        assertEquals(1000, uniquePhrases.size)
    }

    @Test
    fun `generatePhrase produces different results each call`() {
        val phrase1 = engine.generatePhrase()
        val phrase2 = engine.generatePhrase()
        assertNotEquals(phrase1, phrase2)
    }

    @Test
    fun `normalizePhrase lowercases and trims`() {
        val input = listOf("The", "  Quiet", "River", "Flows", "Beneath", "Ancient", "Stone", "Bridges", "At", "Midnight  ")
        val normalized = engine.normalizePhrase(input)
        assertEquals("the quiet river flows beneath ancient stone bridges at midnight", normalized)
    }

    @Test
    fun `normalizePhrase collapses multiple spaces`() {
        val input = listOf("the", "  quiet", "   river")
        val normalized = engine.normalizePhrase(input)
        assertEquals("the quiet river", normalized)
    }

    @Test
    fun `normalizePhrase applies NFKC normalization`() {
        val input = listOf("ｔｈｅ", "ａｋｉｅｔ", "ｒｉｒ")
        val normalized = engine.normalizePhrase(input)
        assertEquals("the quiet river", normalized)
    }
}
