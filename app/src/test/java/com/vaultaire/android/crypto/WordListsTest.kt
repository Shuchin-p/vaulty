package com.vaultaire.android.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordListsTest {

    @Test
    fun `loadFromResources loads all word lists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wordLists = WordLists.loadFromResources(context)

        assertTrue(wordLists.articles.size >= 3)
        assertTrue(wordLists.adjectives.size >= 400)
        assertTrue(wordLists.nouns.size >= 800)
        assertTrue(wordLists.verbs.size >= 400)
        assertTrue(wordLists.prepositions.size >= 20)
        assertTrue(wordLists.adverbs.size >= 150)

        assertEquals(wordLists.articles.size, wordLists.articles.distinct().size)
        assertEquals(wordLists.adjectives.size, wordLists.adjectives.distinct().size)
        assertEquals(wordLists.nouns.size, wordLists.nouns.distinct().size)
        assertEquals(wordLists.verbs.size, wordLists.verbs.distinct().size)
        assertEquals(wordLists.prepositions.size, wordLists.prepositions.distinct().size)
        assertEquals(wordLists.adverbs.size, wordLists.adverbs.distinct().size)
    }

    @Test
    fun `word lists contain only lowercase ASCII words`() {
        val wordLists = WordLists.loadFromResources(ApplicationProvider.getApplicationContext())
        val allWords = wordLists.articles + wordLists.adjectives + wordLists.nouns +
                       wordLists.verbs + wordLists.prepositions + wordLists.adverbs

        allWords.forEach { word ->
            assertTrue(word.matches(Regex("^[a-z]+$")), "Invalid word: $word")
        }
    }
}
