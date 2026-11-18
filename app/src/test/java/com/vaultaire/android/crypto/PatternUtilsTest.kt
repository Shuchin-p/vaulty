package com.vaultaire.android.crypto

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit tests for [PatternUtils] verifying:
 * 1. 5x5 pattern grid validation (length >= 6, <= 25, bounds 0..4, no duplicates, direction turn)
 * 2. Straight-line dot interpolation for horizontal, vertical, and 45-degree diagonal movements
 * 3. Direction change validation (minimum 1 turn)
 * 4. Deterministic domain-separated canonical pattern normalization (ByteArray payload)
 * 5. Pattern entropy and strength estimation
 */
class PatternUtilsTest {

    @Test
    fun testValidPatternWithTurnAndSixDots() {
        // Pattern: (0,0) -> (0,1) -> (0,2) -> (1,2) -> (2,2) -> (3,2)
        // Indices: 0, 1, 2, 7, 12, 17
        val indices = listOf(0, 1, 2, 7, 12, 17)
        val result = PatternUtils.validatePatternIndices(indices)
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun testPatternWithLessThanSixDotsIsRejected() {
        // Pattern with 5 dots: (0,0) -> (0,1) -> (0,2) -> (1,2) -> (2,2)
        val indices = listOf(0, 1, 2, 7, 12)
        val result = PatternUtils.validatePatternIndices(indices)
        assertFalse(result.isValid)
        assertEquals("Pattern must contain at least 6 points", result.errorMessage)
    }

    @Test
    fun testStraightLineWithoutTurnIsRejected() {
        // Horizontal straight line of 5 dots: 0, 1, 2, 3, 4 (< 6 points)
        val fiveDotResult = PatternUtils.validatePatternIndices(listOf(0, 1, 2, 3, 4))
        assertFalse(fiveDotResult.isValid)
        assertEquals("Pattern must contain at least 6 points", fiveDotResult.errorMessage)

        // Collinear pattern of 6 dots without turn (reversing along the same line)
        val sixDotResult = PatternUtils.validatePatternIndices(listOf(0, 1, 2, 3, 2, 1))
        assertFalse(sixDotResult.isValid)
        assertEquals(
            "Pattern must connect at least 6 dots with at least one direction change",
            sixDotResult.errorMessage
        )
    }

    @Test
    fun testHorizontalInterpolation() {
        // Jump from index 0 (0,0) to index 3 (0,3) in 5x5 grid
        // Should interpolate 1 (0,1) and 2 (0,2)
        val interpolated = PatternUtils.interpolateIndices(listOf(0, 3))
        assertEquals(listOf(0, 1, 2, 3), interpolated)
    }

    @Test
    fun testVerticalInterpolation() {
        // Jump from index 0 (0,0) to index 15 (3,0) in 5x5 grid
        // Should interpolate 5 (1,0) and 10 (2,0)
        val interpolated = PatternUtils.interpolateIndices(listOf(0, 15))
        assertEquals(listOf(0, 5, 10, 15), interpolated)
    }

    @Test
    fun testDiagonalInterpolation() {
        // Jump from index 0 (0,0) to index 18 (3,3) in 5x5 grid
        // (0,0) -> (1,1) -> (2,2) -> (3,3)
        // Indices: 0, 6, 12, 18
        val interpolated = PatternUtils.interpolateIndices(listOf(0, 18))
        assertEquals(listOf(0, 6, 12, 18), interpolated)
    }

    @Test
    fun testDirectionChangeDetection() {
        val straightPoints = listOf(
            PatternPoint(0, 0),
            PatternPoint(0, 1),
            PatternPoint(0, 2),
            PatternPoint(0, 3),
            PatternPoint(0, 4)
        )
        assertFalse(PatternUtils.hasDirectionChange(straightPoints))

        val turnedPoints = listOf(
            PatternPoint(0, 0),
            PatternPoint(0, 1),
            PatternPoint(0, 2),
            PatternPoint(1, 2),
            PatternPoint(2, 2),
            PatternPoint(3, 2)
        )
        assertTrue(PatternUtils.hasDirectionChange(turnedPoints))
    }

    @Test
    fun testPatternNormalization() {
        // Valid 6-point pattern with a turn: (0,0) -> (0,1) -> (0,2) -> (1,2) -> (2,2) -> (3,2)
        // Indices: 0, 1, 2, 7, 12, 17
        val points = listOf(
            PatternPoint(0, 0),
            PatternPoint(0, 1),
            PatternPoint(0, 2),
            PatternPoint(1, 2),
            PatternPoint(2, 2),
            PatternPoint(3, 2)
        )
        val normalized = PatternUtils.normalizePattern(points)

        // 1. Verify domain separator header
        val domainBytes = PatternUtils.DOMAIN_SEPARATOR.toByteArray(StandardCharsets.UTF_8)
        assertEquals(domainBytes.size + (points.size * 2), normalized.size)
        val prefix = normalized.sliceArray(0 until domainBytes.size)
        assertArrayEquals(domainBytes, prefix)

        // 2. Verify big-endian short index payload
        val buffer = ByteBuffer.wrap(normalized)
        val header = ByteArray(domainBytes.size)
        buffer.get(header)
        assertEquals(PatternUtils.DOMAIN_SEPARATOR, String(header, StandardCharsets.UTF_8))

        val decodedIndices = mutableListOf<Int>()
        while (buffer.hasRemaining()) {
            decodedIndices.add(buffer.short.toInt())
        }
        assertEquals(listOf(0, 1, 2, 7, 12, 17), decodedIndices)

        // 3. Verify equivalence with normalizePatternIndices
        val normalizedFromIndices = PatternUtils.normalizePatternIndices(listOf(0, 1, 2, 7, 12, 17))
        assertArrayEquals(normalized, normalizedFromIndices)
    }

    @Test
    fun testPatternNormalizationWithInterpolation() {
        // Pattern with jumps: (0,0) -> (0,2) -> (1,2) -> (1,4) -> (2,4) -> (3,4)
        // Raw points: 6 points. Jumps interpolate (0,1) and (1,3).
        // Total points after interpolation: 8 points.
        val points = listOf(
            PatternPoint(0, 0),
            PatternPoint(0, 2),
            PatternPoint(1, 2),
            PatternPoint(1, 4),
            PatternPoint(2, 4),
            PatternPoint(3, 4)
        )
        val normalized = PatternUtils.normalizePattern(points)

        val domainBytes = PatternUtils.DOMAIN_SEPARATOR.toByteArray(StandardCharsets.UTF_8)
        val buffer = ByteBuffer.wrap(normalized)
        val header = ByteArray(domainBytes.size)
        buffer.get(header)
        assertEquals(PatternUtils.DOMAIN_SEPARATOR, String(header, StandardCharsets.UTF_8))

        val decodedIndices = mutableListOf<Int>()
        while (buffer.hasRemaining()) {
            decodedIndices.add(buffer.short.toInt())
        }
        // Expected interpolated sequence: 0, 1, 2, 7, 8, 9, 14, 19
        assertEquals(listOf(0, 1, 2, 7, 8, 9, 14, 19), decodedIndices)
    }

    @Test
    fun testInvalidPatternNormalizationThrows() {
        // Pattern with < 6 points must throw IllegalArgumentException
        val shortPoints = listOf(
            PatternPoint(0, 0),
            PatternPoint(0, 1),
            PatternPoint(1, 1)
        )
        try {
            PatternUtils.normalizePattern(shortPoints)
            fail("Expected IllegalArgumentException for under-length pattern")
        } catch (e: IllegalArgumentException) {
            assertEquals("Pattern must contain at least 6 points", e.message)
        }
    }

    @Test
    fun testConsecutiveDuplicatePointsRejected() {
        val result = PatternUtils.validatePatternIndices(listOf(0, 0, 1, 2, 7, 12, 17))
        assertFalse(result.isValid)
        assertEquals("Pattern contains consecutive duplicate points", result.errorMessage)
    }

    @Test
    fun testPatternOutOfBoundsRejected() {
        val outOfBoundsPoints = listOf(
            PatternPoint(0, 0),
            PatternPoint(0, 1),
            PatternPoint(0, 2),
            PatternPoint(5, 2), // row 5 is out of bounds (0..4)
            PatternPoint(2, 2),
            PatternPoint(3, 2)
        )
        val result = PatternUtils.validatePattern(outOfBoundsPoints)
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("out of bounds") == true)
    }

    @Test
    fun testPatternStrengthCalculation() {
        val validPoints = listOf(
            PatternPoint(0, 0),
            PatternPoint(0, 1),
            PatternPoint(0, 2),
            PatternPoint(1, 2),
            PatternPoint(2, 2),
            PatternPoint(3, 2)
        )
        val strength = PatternUtils.calculatePatternStrength(validPoints)
        // 6 points: entropy ≈ log2(25) + 5 * log2(24) ≈ 27.57 bits
        assertTrue("Pattern entropy should be > 27 bits, was $strength", strength > 27.0)

        // Invalid pattern yields 0.0 entropy
        val invalidStrength = PatternUtils.calculatePatternStrength(listOf(PatternPoint(0, 0)))
        assertEquals(0.0, invalidStrength, 0.001)
    }

    @Test
    fun testFourTouchedPointsInterpolateToValidPattern() {
        // 4 raw points: (0,0) -> (0,2) -> (2,2) -> (2,0)
        // Indices: 0, 2, 12, 10
        // Interpolates to: 0, 1, 2, 7, 12, 11, 10 (7 dots, 2 direction turns)
        val rawIndices = listOf(0, 2, 12, 10)
        
        val validation = PatternUtils.validatePatternIndices(rawIndices)
        assertTrue("Pattern with 4 touched points interpolating to 7 dots must be valid", validation.isValid)
        
        val normalized = PatternUtils.normalizePatternIndices(rawIndices)
        val domainBytes = PatternUtils.DOMAIN_SEPARATOR.toByteArray(StandardCharsets.UTF_8)
        assertEquals(domainBytes.size + 14, normalized.size) // 20 + 7 * 2 = 34 bytes
    }
}
