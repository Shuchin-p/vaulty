package com.vaultaire.android.crypto

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [AttemptThrottler] verifying:
 * - Attempts 1..4 have 0s delay
 * - Attempt 5 has 30s delay (Tier 1)
 * - Attempt 6 has 120s delay (Tier 2)
 * - Attempt 7 has 600s delay (Tier 3)
 * - Attempt 8+ has 1800s delay (Tier 4)
 */
class AttemptThrottlerTest {

    @Test
    fun testAttemptsUnderFiveHaveZeroDelay() {
        assertEquals(0, AttemptThrottler.computeDelaySeconds(0))
        assertEquals(0, AttemptThrottler.computeDelaySeconds(1))
        assertEquals(0, AttemptThrottler.computeDelaySeconds(2))
        assertEquals(0, AttemptThrottler.computeDelaySeconds(3))
        assertEquals(0, AttemptThrottler.computeDelaySeconds(4))
    }

    @Test
    fun testAttemptFiveTierOneDelay() {
        assertEquals(30, AttemptThrottler.computeDelaySeconds(5))
    }

    @Test
    fun testAttemptSixTierTwoDelay() {
        assertEquals(120, AttemptThrottler.computeDelaySeconds(6))
    }

    @Test
    fun testAttemptSevenTierThreeDelay() {
        assertEquals(600, AttemptThrottler.computeDelaySeconds(7))
    }

    @Test
    fun testAttemptEightAndAboveTierFourDelay() {
        assertEquals(1800, AttemptThrottler.computeDelaySeconds(8))
        assertEquals(1800, AttemptThrottler.computeDelaySeconds(9))
        assertEquals(1800, AttemptThrottler.computeDelaySeconds(20))
    }
}
