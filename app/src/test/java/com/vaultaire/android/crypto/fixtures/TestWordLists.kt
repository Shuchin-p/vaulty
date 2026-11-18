package com.vaultaire.android.crypto.fixtures

import com.vaultaire.android.crypto.WordLists

object WordListsFixtures {

    fun minimal(): WordLists = WordLists(
        articles = listOf("the", "a", "an"),
        adjectives = listOf("quiet", "swift", "ancient", "golden", "silent", "dark", "bright"),
        nouns = listOf("river", "mountain", "bridge", "key", "shadow", "valley", "forest"),
        verbs = listOf("flows", "rests", "guards", "watches", "sleeps", "rises", "falls"),
        prepositions = listOf("beneath", "above", "within", "beyond", "across", "through"),
        adverbs = listOf("silently", "swiftly", "eternally", "quietly", "gently", "frequently")
    )

    fun loadCommonPhrases(): Set<String> = setOf(
        "to be or not to be",
        "the quick brown fox",
        "hello world",
        "once upon a time",
        "happily ever after"
    )

    fun loadDictionaryWords(): Set<String> = setOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "quiet", "swift", "ancient", "golden", "silent", "dark", "bright",
        "river", "mountain", "bridge", "key", "shadow", "valley", "forest",
        "flows", "rests", "guards", "watches", "sleeps", "rises", "falls",
        "beneath", "above", "within", "beyond", "across", "through",
        "silently", "swiftly", "eternally", "quietly", "gently", "frequently"
    )
}
