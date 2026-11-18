package com.vaultaire.android.sync.engine

import com.vaultaire.android.sync.model.*
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

/**
 * Property-based tests for MergeEngine.
 *
 * T-UN-01: Commutative - merge(A, B) == merge(B, A)
 * T-UN-02: Associative - merge(merge(A, B), C) == merge(A, merge(B, C))
 * T-UN-03: Idempotent - merge(A, A) == A
 * T-UN-04: Convergent - repeated merges converge to same state
 * T-UN-05: Add-wins over unseen delete
 * T-UN-06: Deduplication - identical content collapses
 * T-UN-07: Tombstone dominance - seen deletes apply
 * T-UN-08: Concurrent modification - both versions kept
 * T-UN-09: Vector clock merge - max per device
 */
class MergeEngineTest {

    private val engine = MergeEngine()
    private val vaultId = UUID.randomUUID()
    private val deviceA = UUID.randomUUID()
    private val deviceB = UUID.randomUUID()
    private val deviceC = UUID.randomUUID()

    private fun createMetadata(
        deviceId: UUID,
        vectorClock: Map<UUID, Long> = emptyMap(),
        fileIndex: Map<UUID, FileEntry> = emptyMap(),
        deletionMarkers: Set<DeletionMarker> = emptySet()
    ): SyncMetadata {
        return SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceId,
            vectorClock = vectorClock,
            fileIndex = fileIndex,
            deletionMarkers = deletionMarkers,
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )
    }

    private fun createFileEntry(
        fileId: UUID,
        contentHash: ByteArray,
        addedBy: UUID,
        version: Long,
        chunkRefs: List<ChunkRef> = emptyList()
    ): FileEntry {
        return FileEntry(
            fileId = fileId,
            contentHash = contentHash,
            chunkRefs = chunkRefs,
            size = 1024,
            mimeType = "image/jpeg",
            addedBy = addedBy,
            addedAt = System.currentTimeMillis(),
            version = version
        )
    }

    private fun createChunkRef(): ChunkRef {
        return ChunkRef(
            chunkId = UUID.randomUUID(),
            driveRecordName = UUID.randomUUID().toString(),
            offset = 0,
            length = 1024,
            encryptedHash = ByteArray(32)
        )
    }

    @Test
    fun `T-UN-01: merge is commutative`() {
        val metadataA = createMetadata(deviceA, mapOf(deviceA to 2L))
        val metadataB = createMetadata(deviceB, mapOf(deviceB to 3L))

        val mergedAB = engine.merge(metadataA, metadataB)
        val mergedBA = engine.merge(metadataB, metadataA)

        assertEquals(mergedAB.vectorClock, mergedBA.vectorClock)
        assertEquals(mergedAB.fileIndex, mergedBA.fileIndex)
    }

    @Test
    fun `T-UN-02: merge is associative`() {
        val metadataA = createMetadata(deviceA, mapOf(deviceA to 1L))
        val metadataB = createMetadata(deviceB, mapOf(deviceB to 2L))
        val metadataC = createMetadata(deviceC, mapOf(deviceC to 3L))

        val mergedAB_C = engine.merge(engine.merge(metadataA, metadataB), metadataC)
        val mergedA_BC = engine.merge(metadataA, engine.merge(metadataB, metadataC))

        assertEquals(mergedAB_C.vectorClock, mergedA_BC.vectorClock)
        assertEquals(mergedAB_C.fileIndex, mergedA_BC.fileIndex)
    }

    @Test
    fun `T-UN-03: merge is idempotent`() {
        val metadata = createMetadata(deviceA, mapOf(deviceA to 5L))
        val merged = engine.merge(metadata, metadata)

        assertEquals(metadata.vectorClock, merged.vectorClock)
        assertEquals(metadata.fileIndex, merged.fileIndex)
        assertEquals(metadata.deletionMarkers, merged.deletionMarkers)
    }

    @Test
    fun `T-UN-04: repeated merges converge`() {
        val metadataA = createMetadata(deviceA, mapOf(deviceA to 1L))
        val metadataB = createMetadata(deviceB, mapOf(deviceB to 1L))

        var state = metadataA
        repeat(10) {
            state = engine.merge(state, metadataB)
            state = engine.merge(state, metadataA)
        }

        val finalMerge = engine.merge(metadataA, metadataB)
        assertEquals(finalMerge.vectorClock, state.vectorClock)
    }

    @Test
    fun `T-UN-05: add wins over unseen delete`() {
        val fileId = UUID.randomUUID()
        val contentHash = "content".toByteArray()

        // Device A adds file
        val entryA = createFileEntry(fileId, contentHash, deviceA, 1L)
        val metadataA = createMetadata(deviceA, mapOf(deviceA to 1L), mapOf(fileId to entryA))

        // Device B deletes same file (without having seen A's add)
        val marker = DeletionMarker(
            fileId = fileId,
            deletedBy = deviceB,
            deletedAt = System.currentTimeMillis(),
            vectorClockSnapshot = mapOf(deviceB to 1L) // B hasn't seen A's clock
        )
        val metadataB = createMetadata(deviceB, mapOf(deviceB to 1L), emptyMap(), setOf(marker))

        val merged = engine.merge(metadataA, metadataB)

        // Add should win - file should still exist
        assertTrue("File should exist after add-wins merge", merged.fileIndex.containsKey(fileId))
    }

    @Test
    fun `T-UN-06: deduplication - identical content collapses`() {
        val fileId = UUID.randomUUID()
        val contentHash = "same content".toByteArray()
        val chunkRef = createChunkRef()

        val entryA = createFileEntry(fileId, contentHash, deviceA, 1L, listOf(chunkRef))
        val entryB = createFileEntry(fileId, contentHash, deviceB, 1L, listOf(chunkRef))

        val metadataA = createMetadata(deviceA, mapOf(deviceA to 1L), mapOf(fileId to entryA))
        val metadataB = createMetadata(deviceB, mapOf(deviceB to 1L), mapOf(fileId to entryB))

        val merged = engine.merge(metadataA, metadataB)

        // Should have exactly one entry
        assertEquals(1, merged.fileIndex.size)
        val mergedEntry = merged.fileIndex[fileId]!!
        
        // Should combine chunk refs
        assertTrue(mergedEntry.chunkRefs.size >= 1)
    }

    @Test
    fun `T-UN-07: tombstone dominance - seen deletes apply`() {
        val fileId = UUID.randomUUID()
        val contentHash = "content".toByteArray()

        // Device A adds file
        val entryA = createFileEntry(fileId, contentHash, deviceA, 1L)
        val metadataA = createMetadata(deviceA, mapOf(deviceA to 1L), mapOf(fileId to entryA))

        // Device B deletes AFTER seeing A's version (clock shows A:1)
        val marker = DeletionMarker(
            fileId = fileId,
            deletedBy = deviceB,
            deletedAt = System.currentTimeMillis(),
            vectorClockSnapshot = mapOf(deviceA to 1L, deviceB to 2L) // B saw A's version
        )
        val metadataB = createMetadata(deviceB, mapOf(deviceA to 1L, deviceB to 2L), emptyMap(), setOf(marker))

        val merged = engine.merge(metadataA, metadataB)

        // Delete should apply - file should be gone
        assertFalse("File should be deleted when deleter saw the version", merged.fileIndex.containsKey(fileId))
        assertTrue("Deletion marker should be preserved", merged.deletionMarkers.contains(marker))
    }

    @Test
    fun `T-UN-08: concurrent modification - both versions kept`() {
        val fileIdA = UUID.randomUUID()
        val fileIdB = UUID.randomUUID()
        val contentHashA = "content A".toByteArray()
        val contentHashB = "content B".toByteArray()

        // Device A adds file
        val entryA = createFileEntry(fileIdA, contentHashA, deviceA, 1L)
        val metadataA = createMetadata(deviceA, mapOf(deviceA to 1L), mapOf(fileIdA to entryA))

        // Device B adds DIFFERENT content with same logical file ID (concurrent)
        val entryB = createFileEntry(fileIdA, contentHashB, deviceB, 1L) // Same fileId!
        val metadataB = createMetadata(deviceB, mapOf(deviceB to 1L), mapOf(fileIdA to entryB))

        val merged = engine.merge(metadataA, metadataB)

        // Should have both files (remote renamed to new fileId)
        assertEquals(2, merged.fileIndex.size)
        assertTrue(merged.fileIndex.containsKey(fileIdA))
        // The second file should have a different ID
        val mergedEntries = merged.fileIndex.values.toList()
        val hasDifferentContent = mergedEntries.any { it.contentHash == contentHashB }
        assertTrue("Both versions should be preserved", hasDifferentContent)
    }

    @Test
    fun `T-UN-09: vector clock merge takes max per device`() {
        val metadataA = createMetadata(deviceA, mapOf(deviceA to 5L, deviceB to 2L))
        val metadataB = createMetadata(deviceB, mapOf(deviceA to 3L, deviceB to 7L))

        val merged = engine.merge(metadataA, metadataB)

        assertEquals(5L, merged.vectorClock[deviceA]!!)
        assertEquals(7L, merged.vectorClock[deviceB]!!)
    }
}
