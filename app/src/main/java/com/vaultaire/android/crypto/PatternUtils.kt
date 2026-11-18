package com.vaultaire.android.crypto

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.math.abs

/**
 * Utility functions for 5x5 pattern grid validation, straight-line interpolation,
 * and canonical deterministic serialization for key derivation.
 */
object PatternUtils {
    /** Domain separator for pattern normalization - public for testing */
    const val DOMAIN_SEPARATOR = "VAULTAIRE_PATTERN_V1"

    /**
     * Converts a raw list of touched grid points into a canonical byte array.
     * Applies intermediate point interpolation on a 5x5 grid.
     */
    fun normalizePattern(points: List<PatternPoint>): ByteArray {
        val interpolated = interpolatePattern(points)
        val validation = validatePattern(interpolated)
        require(validation.isValid) { validation.errorMessage ?: "Invalid pattern" }

        val domainBytes = DOMAIN_SEPARATOR.toByteArray(StandardCharsets.UTF_8)
        val buffer = ByteBuffer.allocate(domainBytes.size + (interpolated.size * 2))
        buffer.put(domainBytes)
        for (pt in interpolated) {
            buffer.putShort(pt.index.toShort())
        }
        return buffer.array()
    }

    /**
     * Converts raw indices (0..24) into a canonical byte array.
     */
    fun normalizePatternIndices(indices: List<Int>): ByteArray {
        val points = indices.map { PatternPoint(row = it / 5, col = it % 5) }
        return normalizePattern(points)
    }

    /**
     * Interpolates straight lines between points on a 5x5 grid.
     */
    fun interpolatePattern(points: List<PatternPoint>): List<PatternPoint> {
        if (points.isEmpty()) return emptyList()

        val interpolated = mutableListOf<PatternPoint>()
        interpolated.add(points.first())

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val intermediates = findIntermediatePoints(p1, p2)
            for (inter in intermediates) {
                if (!interpolated.contains(inter)) {
                    interpolated.add(inter)
                }
            }

            if (interpolated.last() != p2) {
                interpolated.add(p2)
            }
        }
        return interpolated
    }

    /**
     * Interpolates straight lines between integer indices (0..24).
     */
    fun interpolateIndices(indices: List<Int>): List<Int> {
        val points = indices.map { PatternPoint(row = it / 5, col = it % 5) }
        return interpolatePattern(points).map { it.index }
    }

    /**
     * Validates a pattern meets all security and grid requirements:
     * - Minimum 6 points
     * - Maximum 25 points
     * - Coordinates in 0..4
     * - No consecutive duplicate points
     * - At least one direction change
     *
     * @return ValidationResult with isValid flag and error message if invalid
     */
    fun validatePattern(points: List<PatternPoint>): ValidationResult {
        if (points.isEmpty()) {
            return ValidationResult(false, "Pattern must contain at least 6 points")
        }

        // All points must be within 5x5 grid bounds
        for (pt in points) {
            if (pt.row !in 0..4 || pt.col !in 0..4) {
                return ValidationResult(false, "Pattern point out of bounds: row=${pt.row}, col=${pt.col}")
            }
        }

        // Check for duplicate consecutive points
        for (i in 0 until points.size - 1) {
            if (points[i] == points[i + 1]) {
                return ValidationResult(false, "Pattern contains consecutive duplicate points")
            }
        }

        // Interpolate straight lines between points before checking length and turns
        val interpolated = interpolatePattern(points)

        if (interpolated.size < 6) {
            return ValidationResult(false, "Pattern must contain at least 6 points")
        }

        if (interpolated.size > 25) {
            return ValidationResult(false, "Pattern cannot exceed 25 points (5x5 grid)")
        }

        // Enforce at least one direction change
        if (!hasDirectionChange(interpolated)) {
            return ValidationResult(false, "Pattern must connect at least 6 dots with at least one direction change")
        }

        return ValidationResult(true, null)
    }

    /**
     * Validates a pattern given as integer indices (0..24).
     */
    fun validatePatternIndices(indices: List<Int>): ValidationResult {
        val points = indices.map { PatternPoint(row = it / 5, col = it % 5) }
        return validatePattern(points)
    }

    /**
     * Checks whether the sequence of points contains at least one direction change.
     * Evaluates the 2D cross product between successive vectors AB and BC.
     */
    fun hasDirectionChange(points: List<PatternPoint>): Boolean {
        if (points.size < 3) return false
        for (i in 0 until points.size - 2) {
            val a = points[i]
            val b = points[i + 1]
            val c = points[i + 2]
            val abx = b.col - a.col
            val aby = b.row - a.row
            val bcx = c.col - b.col
            val bcy = c.row - b.row
            if (abx * bcy - aby * bcx != 0) {
                return true
            }
        }
        return false
    }

    /**
     * Calculates the strength of a pattern in bits of entropy.
     */
    fun calculatePatternStrength(points: List<PatternPoint>): Double {
        val interpolated = interpolatePattern(points)
        val validation = validatePattern(interpolated)
        if (!validation.isValid) return 0.0

        val n = interpolated.size
        // Entropy ≈ log2(25) + (n-1) * log2(24)
        return kotlin.math.log2(25.0) + (n - 1) * kotlin.math.log2(24.0)
    }

    /**
     * Finds all intermediate points along the straight line between p1 and p2.
     * Restricts interpolation to horizontal, vertical, and 45-degree diagonal lines.
     */
    fun findIntermediatePoints(p1: PatternPoint, p2: PatternPoint): List<PatternPoint> {
        val dr = p2.row - p1.row
        val dc = p2.col - p1.col
        val steps = gcd(abs(dr), abs(dc))

        if (steps <= 1) return emptyList()
        // Straight line check: must be horizontal (dr == 0), vertical (dc == 0), or 45-degree diagonal (abs(dr) == abs(dc))
        if (dr != 0 && dc != 0 && abs(dr) != abs(dc)) return emptyList()

        val rowStep = dr / steps
        val colStep = dc / steps

        return (1 until steps).map { step ->
            PatternPoint(
                row = p1.row + rowStep * step,
                col = p1.col + colStep * step
            )
        }
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return x
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?
    )
}
