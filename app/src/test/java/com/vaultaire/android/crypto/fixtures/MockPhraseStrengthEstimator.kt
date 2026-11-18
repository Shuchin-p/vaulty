package com.vaultaire.android.crypto.fixtures

import com.vaultaire.android.crypto.PhraseStrengthEstimator

/**
 * Mock PhraseStrengthEstimator that always returns valid results.
 * Used in tests where phrase strength validation is not the focus.
 */
class MockPhraseStrengthEstimator : PhraseStrengthEstimator(emptySet(), emptySet()) {
    override fun estimate(words: List<String>): Result {
        return Result(
            entropyBits = words.size * 10.0,
            isAcceptable = true,
            rejectionReason = null
        )
    }
}
