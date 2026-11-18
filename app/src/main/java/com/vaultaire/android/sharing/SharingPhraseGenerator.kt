package com.vaultaire.android.sharing

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Generator and validator for BIP39 12-word sharing phrases with checksum.
 *
 * Uses 128 bits of entropy + 4-bit checksum = 132 bits = 12 words (11 bits each).
 * The checksum is the first 4 bits of SHA256(entropy).
 */
object SharingPhraseGenerator {
    private const val ENTROPY_BITS = 128
    private const val ENTROPY_BYTES = ENTROPY_BITS / 8 // 16 bytes
    private const val CHECKSUM_BITS = 4
    private const val WORD_COUNT = 12
    private val WORDLIST = Bip39Wordlist.WORDS

    /**
     * Generates a cryptographically secure 12-word BIP39 sharing phrase.
     *
     * @return A 12-word phrase with valid checksum.
     */
    fun generateShareSecretPhrase(): String {
        // Generate 128 bits of entropy
        val entropy = ByteArray(ENTROPY_BYTES)
        SecureRandom().nextBytes(entropy)

        // Calculate checksum (first 4 bits of SHA256(entropy))
        val digest = MessageDigest.getInstance("SHA-256").digest(entropy)
        val checksumBits = (digest[0].toInt() shr 4) and 0xF // First 4 bits

        // Convert entropy to 11-bit indices (11 indices = 121 bits, we need 128)
        // Actually we need 12 * 11 = 132 bits total (128 entropy + 4 checksum)
        // So we use the full 128 bits of entropy + 4 bits of checksum = 132 bits
        // Split into 12 groups of 11 bits each
        val bits = ByteArray(17) // 132 bits = 16.5 bytes, round up to 17
        System.arraycopy(entropy, 0, bits, 0, ENTROPY_BYTES)
        // Add checksum bits to the end
        bits[16] = (checksumBits shl 4).toByte() // checksum in upper 4 bits of last byte

        // Extract 11-bit indices
        val indices = IntArray(WORD_COUNT)
        var bitIndex = 0
        for (i in 0 until WORD_COUNT) {
            var value = 0
            for (b in 0 until 11) {
                val byteIndex = bitIndex / 8
                val bitOffset = bitIndex % 8
                val bit = (bits[byteIndex].toInt() shr (7 - bitOffset)) and 1
                value = (value shl 1) or bit
                bitIndex++
            }
            indices[i] = value
        }

        // Map to words
        val words = indices.map { WORDLIST[it] }
        return words.joinToString(" ")
    }

    /**
     * Validates a sharing phrase.
     *
     * Checks:
     * - Exactly 12 words
     * - All words in BIP39 wordlist
     * - Valid checksum
     *
     * @param phrase The phrase to validate (words separated by spaces).
     * @return true if valid, false otherwise.
     */
    fun validatePhrase(phrase: String): Boolean {
        val words = phrase.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (words.size != WORD_COUNT) return false

        // Convert words to indices
        val indices = IntArray(WORD_COUNT)
        for (i in 0 until WORD_COUNT) {
            val idx = Bip39Wordlist.indexOf(words[i].lowercase())
            if (idx < 0) return false
            indices[i] = idx
        }

        // Reconstruct 132 bits from indices
        val bits = ByteArray(17)
        var bitIndex = 0
        for (i in 0 until WORD_COUNT) {
            var value = indices[i]
            for (b in 10 downTo 0) {
                val bit = (value shr b) and 1
                val byteIndex = bitIndex / 8
                val bitOffset = bitIndex % 8
                if (bit == 1) {
                    bits[byteIndex] = (bits[byteIndex].toInt() or (1 shl (7 - bitOffset))).toByte()
                }
                bitIndex++
            }
        }

        // Extract entropy (first 128 bits = 16 bytes)
        val entropy = ByteArray(ENTROPY_BYTES)
        System.arraycopy(bits, 0, entropy, 0, ENTROPY_BYTES)

        // Extract checksum from bits (4 bits at position 128-131)
        val extractedChecksum = (bits[16].toInt() shr 4) and 0xF

        // Compute expected checksum
        val digest = MessageDigest.getInstance("SHA-256").digest(entropy)
        val expectedChecksum = (digest[0].toInt() shr 4) and 0xF

        return extractedChecksum == expectedChecksum
    }

    /**
     * Normalizes a phrase for use in key derivation.
     * Lowercases and trims extra whitespace.
     */
    fun normalizePhrase(phrase: String): String {
        return phrase.trim().lowercase().replace("\\s+".toRegex(), " ")
    }
}
