package com.vaultaire.android.sync.engine

/**
 * Vector-clock operations used by the merge engine and sync metadata.
 *
 * A vector clock is a map from String (device id) to Long (logical clock).
 * It is monotonic: a device only ever increments its own entry.
 *
 * Ordering:
 * - [dominates]: `vc1` dominates `vc2` iff for every device `d`, `vc1[d] >= vc2[d]`
 *   and there exists at least one device where `vc1[d] > vc2[d]`.
 * - [concurrent]: neither dominates the other.
 * - [merge]: element-wise maximum.
 */
object VectorClock {

    /** Returns `true` if [a] causally dominates [b]. */
    fun dominates(a: Map<String, Long>, b: Map<String, Long>): Boolean {
        if (a.isEmpty() || b.isEmpty()) return a != b
        val allKeys = a.keys union b.keys
        var aGreater = false
        for (key in allKeys) {
            val av = a[key] ?: 0L
            val bv = b[key] ?: 0L
            if (av < bv) return false
            if (av > bv) aGreater = true
        }
        return aGreater
    }

    /** Returns `true` when neither clock dominates the other. */
    fun concurrent(a: Map<String, Long>, b: Map<String, Long>): Boolean =
        !dominates(a, b) && !dominates(b, a)

    /** Returns the element-wise maximum of [a] and [b]. */
    fun merge(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val allKeys = a.keys union b.keys
        for (key in allKeys) {
            result[key] = maxOf(a[key] ?: 0L, b[key] ?: 0L)
        }
        return result
    }

    /** Returns `true` if both clocks are identical. */
    fun equal(a: Map<String, Long>, b: Map<String, Long>): Boolean = a == b
}