package com.vaultaire.android.crypto

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

/**
 * Manages attempt rate-limiting and progressive lockout delays for pattern unlock.
 * Persists failed attempt counts and lockout deadlines to survive app restarts.
 *
 * Progressive delay schedule:
 * - Attempts 1..4: 0s
 * - Attempt 5: 30s (Tier 1)
 * - Attempt 6: 120s / 2m (Tier 2)
 * - Attempt 7: 600s / 10m (Tier 3)
 * - Attempt 8+: 1800s / 30m (Tier 4)
 */
class AttemptThrottler(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "vlt_throttle_prefs"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_DEADLINE = "lockout_deadline_ms"
        private const val KEY_ELAPSED_REALTIME_AT_LOCKOUT = "elapsed_realtime_at_lockout"
        private const val KEY_LOCKOUT_DURATION_MS = "lockout_duration_ms"

        // Tier thresholds in seconds
        const val TIER_1_DELAY_SECONDS = 30
        const val TIER_2_DELAY_SECONDS = 120   // 2 minutes
        const val TIER_3_DELAY_SECONDS = 600   // 10 minutes
        const val TIER_4_DELAY_SECONDS = 1800  // 30 minutes

        /**
         * Pure function computing progressive delay seconds for a given count of failed attempts.
         */
        fun computeDelaySeconds(failedAttempts: Int): Int = when {
            failedAttempts < 5 -> 0
            failedAttempts == 5 -> TIER_1_DELAY_SECONDS
            failedAttempts == 6 -> TIER_2_DELAY_SECONDS
            failedAttempts == 7 -> TIER_3_DELAY_SECONDS
            else -> TIER_4_DELAY_SECONDS
        }
    }

    private val lock = ReentrantLock()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns remaining lockout seconds. Returns 0 if not locked out.
     * Incorporates monotonic floor to prevent clock rollback bypass.
     */
    fun getRemainingLockoutSeconds(): Int = lock.withLock {
        val deadline = prefs.getLong(KEY_LOCKOUT_DEADLINE, 0L)
        val lockoutDuration = prefs.getLong(KEY_LOCKOUT_DURATION_MS, 0L)
        val elapsedAtLockout = prefs.getLong(KEY_ELAPSED_REALTIME_AT_LOCKOUT, 0L)

        if (deadline == 0L || lockoutDuration == 0L) return 0

        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        // Check monotonic elapsed time if available from same boot
        val elapsedSinceLockout = nowElapsed - elapsedAtLockout
        val remainingByElapsed = if (elapsedSinceLockout in 0..lockoutDuration) {
            (lockoutDuration - elapsedSinceLockout) / 1000L
        } else {
            0L
        }

        // Remaining by wall clock
        val remainingByWall = (deadline - nowWall) / 1000L

        // Protection against clock rollback: choose the larger remaining time
        val remaining = max(remainingByWall, remainingByElapsed).toInt()
        return if (remaining > 0) remaining else 0
    }

    /**
     * Checks if user is currently locked out.
     */
    fun isLockedOut(): Boolean = getRemainingLockoutSeconds() > 0

    /**
     * Records a failed attempt and computes progressive lockout duration.
     * @return The lockout wait seconds (0 if below threshold)
     */
    fun recordFailedAttempt(): Int = lock.withLock {
        val currentAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val delaySeconds = computeDelaySeconds(currentAttempts)

        val now = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val durationMs = delaySeconds * 1000L
        val deadline = if (delaySeconds > 0) now + durationMs else 0L

        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, currentAttempts)
            .putLong(KEY_LOCKOUT_DEADLINE, deadline)
            .putLong(KEY_ELAPSED_REALTIME_AT_LOCKOUT, if (delaySeconds > 0) nowElapsed else 0L)
            .putLong(KEY_LOCKOUT_DURATION_MS, durationMs)
            .apply()

        return delaySeconds
    }

    /**
     * Records a successful attempt, resetting all failed attempt counters and deadlines.
     */
    fun recordSuccessfulAttempt(): Unit = lock.withLock {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_DEADLINE, 0L)
            .putLong(KEY_ELAPSED_REALTIME_AT_LOCKOUT, 0L)
            .putLong(KEY_LOCKOUT_DURATION_MS, 0L)
            .apply()
    }

    /**
     * Returns the current count of failed attempts.
     */
    fun getFailedAttempts(): Int = lock.withLock {
        prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    /**
     * Resets all throttle state (for tests or vault reset).
     */
    fun reset(): Unit = lock.withLock {
        prefs.edit().clear().apply()
    }
}
