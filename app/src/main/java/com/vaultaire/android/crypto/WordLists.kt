package com.vaultaire.android.crypto

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Immutable word lists for grammatical phrase generation.
 * Must match iOS word lists exactly for cross-platform compatibility.
 * Loaded from embedded JSON resource res/raw/word_lists_en.json
 */
data class WordLists(
    val articles: List<String>,       // 3 words
    val adjectives: List<String>,     // ~100 words
    val nouns: List<String>,          // ~100 words
    val verbs: List<String>,          // ~50 words
    val prepositions: List<String>,   // ~30 words
    val adverbs: List<String>         // ~50 words
) {
    companion object {
        private const val RESOURCE_NAME = "word_lists_en"
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Loads word lists from the embedded JSON resource.
         */
        fun loadFromResources(context: Context): WordLists {
            val resourceId = context.resources.getIdentifier(
                RESOURCE_NAME, "raw", context.packageName
            )
            val jsonString = context.resources.openRawResource(resourceId).use { 
                it.bufferedReader().readText() 
            }
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject

            return WordLists(
                articles = jsonObject["articles"]!!.jsonArray.map { it.jsonPrimitive.content },
                adjectives = jsonObject["adjectives"]!!.jsonArray.map { it.jsonPrimitive.content },
                nouns = jsonObject["nouns"]!!.jsonArray.map { it.jsonPrimitive.content },
                verbs = jsonObject["verbs"]!!.jsonArray.map { it.jsonPrimitive.content },
                prepositions = jsonObject["prepositions"]!!.jsonArray.map { it.jsonPrimitive.content },
                adverbs = jsonObject["adverbs"]!!.jsonArray.map { it.jsonPrimitive.content }
            )
        }
    }

    /**
     * Pick a random word from each list following the grammatical template:
     * [Article] [Adjective] [Noun] [Verb] [Preposition] [Article] [Adjective] [Noun] [Adverb]
     */
    fun generateGrammaticalPhrase(random: java.security.SecureRandom = java.security.SecureRandom()): List<String> {
        return listOf(
            articles[random.nextInt(articles.size)],
            adjectives[random.nextInt(adjectives.size)],
            nouns[random.nextInt(nouns.size)],
            verbs[random.nextInt(verbs.size)],
            prepositions[random.nextInt(prepositions.size)],
            articles[random.nextInt(articles.size)],
            adjectives[random.nextInt(adjectives.size)],
            nouns[random.nextInt(nouns.size)],
            adverbs[random.nextInt(adverbs.size)]
        )
    }
}
