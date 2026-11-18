package com.vaultaire.android.sync.engine

import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

/**
 * Tests for VectorClock operations.
 */
class VectorClockTest {

    private val deviceA = UUID.randomUUID()
    private val deviceB = UUID.randomUUID()
    private val deviceC = UUID.randomUUID()

    @Test
    fun `dominates: A dominates B when all clocks >= and one >`() {
        val a = mapOf(deviceA to 5L, deviceB to 3L)
        val b = mapOf(deviceA to 3L, deviceB to 2L)
        assertTrue(VectorClock.dominates(a, b))
    }

    @Test
    fun `dominates: A does not dominate B when any clock <`() {
        val a = mapOf(deviceA to 5L, deviceB to 1L)
        val b = mapOf(deviceA to 3L, deviceB to 3L)
        assertFalse(VectorClock.dominates(a, b))
    }

    @Test
    fun `dominates: equal clocks do not dominate`() {
        val a = mapOf(deviceA to 5L, deviceB to 3L)
        val b = mapOf(deviceA to 5L, deviceB to 3L)
        assertFalse(VectorClock.dominates(a, b))
    }

    @Test
    fun `dominates: empty clock dominated by non-empty`() {
        val a = mapOf(deviceA to 1L)
        val b = emptyMap<UUID, Long>()
        assertTrue(VectorClock.dominates(a, b))
    }

    @Test
    fun `concurrent: neither dominates`() {
        val a = mapOf(deviceA to 5L, deviceB to 1L)
        val b = mapOf(deviceA to 1L, deviceB to 5L)
        assertTrue(VectorClock.concurrent(a, b))
    }

    @Test
    fun `concurrent: not concurrent when one dominates`() {
        val a = mapOf(deviceA to 5L, deviceB to 3L)
        val b = mapOf(deviceA to 3L, deviceB to 2L)
        assertFalse(VectorClock.concurrent(a, b))
    }

    @Test
    fun `merge: takes max per device`() {
        val a = mapOf(deviceA to 5L, deviceB to 2L)
        val b = mapOf(deviceA to 3L, deviceB to 7L, deviceC to 1L)
        val merged = VectorClock.merge(a, b)
        
        assertEquals(5L, merged[deviceA]!!)
        assertEquals(7L, merged[deviceB]!!)
        assertEquals(1L, merged[deviceC]!!)
    }

    @Test
    fun `merge: empty with non-empty returns non-empty`() {
        val a = emptyMap<UUID, Long>()
        val b = mapOf(deviceA to 3L)
        val merged = VectorClock.merge(a, b)
        assertEquals(3L, merged[deviceA]!!)
    }

    @Test
    fun `merge: idempotent`() {
        val a = mapOf(deviceA to 5L, deviceB to 3L)
        val merged = VectorClock.merge(a, a)
        assertEquals(a, merged)
    }

    @Test
    fun `merge: commutative`() {
        val a = mapOf(deviceA to 5L, deviceB to 3L)
        val b = mapOf(deviceA to 3L, deviceB to 7L)
        assertEquals(VectorClock.merge(a, b), VectorClock.merge(b, a))
    }

    @Test
    fun `merge: associative`() {
        val a = mapOf(deviceA to 1L)
        val b = mapOf(deviceB to 2L)
        val c = mapOf(deviceC to 3L)
        val merged1 = VectorClock.merge(VectorClock.merge(a, b), c)
        val merged2 = VectorClock.merge(a, VectorClock.merge(b, c))
        assertEquals(merged1, merged2)
    }
}
