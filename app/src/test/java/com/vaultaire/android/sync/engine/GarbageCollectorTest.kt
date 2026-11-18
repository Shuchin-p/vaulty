package com.vaultaire.android.sync.engine

import com.vaultaire.android.sync.model.*
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

/**
 * Tests for GarbageCollector.
 */
class GarbageCollectorTest {

    private val vaultId = UUID.randomUUID()
    private val deviceA = UUID.randomUUID()

    private fun createMetadata(
        fileIndex: Map<UUID, FileEntry> = emptyMap(),
        deletionMarkers: Set<DeletionMarker> = emptySet()
    ): SyncMetadata {
        return SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceA,
            vectorClock = mapOf(deviceA to 1L),
            fileIndex = fileIndex,
            deletionMarkers = deletionMarkers,
            pendingUploads = emptyList(),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )
    }

    private fun createFileEntry(fileId: UUID, chunkIds: List<UUID>): FileEntry {
        return FileEntry(
            fileId = fileId,
            contentHash = "hash".toByteArray(),
            chunkRefs = chunkIds.map { ChunkRef(it, UUID.randomUUID().toString(), 0, 1024, ByteArray(32)) },
            size = 1024,
            mimeType = "image/jpeg",
            addedBy = deviceA,
            addedAt = System.currentTimeMillis(),
            version = 1L
        )
    }

    @Test
    fun `collect: removes stale deletion markers older than 30 days`() {
        val fileId = UUID.randomUUID()
        val oldMarker = DeletionMarker(
            fileId = fileId,
            deletedBy = deviceA,
            deletedAt = System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000, // 31 days ago
            vectorClockSnapshot = mapOf(deviceA to 1L)
        )
        val metadata = createMetadata(deletionMarkers = setOf(oldMarker))

        val collected = GarbageCollector.collect(metadata)

        assertTrue("Old deletion marker should be removed", collected.deletionMarkers.isEmpty())
    }

    @Test
    fun `collect: keeps recent deletion markers`() {
        val fileId = UUID.randomUUID()
        val recentMarker = DeletionMarker(
            fileId = fileId,
            deletedBy = deviceA,
            deletedAt = System.currentTimeMillis() - 24 * 60 * 60 * 1000, // 1 day ago
            vectorClockSnapshot = mapOf(deviceA to 1L)
        )
        val metadata = createMetadata(deletionMarkers = setOf(recentMarker))

        val collected = GarbageCollector.collect(metadata)

        assertFalse("Recent deletion marker should be kept", collected.deletionMarkers.isEmpty())
    }

    @Test
    fun `removeAcknowledgedChanges: removes acknowledged pending changes`() {
        val changeId1 = UUID.randomUUID()
        val changeId2 = UUID.randomUUID()
        val change1 = PendingChange(
            changeId = changeId1,
            type = ChangeType.ADD,
            fileId = UUID.randomUUID(),
            chunkRefs = emptyList(),
            vectorClock = emptyMap(),
            timestamp = System.currentTimeMillis()
        )
        val change2 = PendingChange(
            changeId = changeId2,
            type = ChangeType.DELETE,
            fileId = UUID.randomUUID(),
            chunkRefs = null,
            vectorClock = emptyMap(),
            timestamp = System.currentTimeMillis()
        )
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceA,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = listOf(change1, change2),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val collected = GarbageCollector.removeAcknowledgedChanges(metadata, setOf(changeId1))

        assertEquals(1, collected.pendingUploads.size)
        assertEquals(changeId2, collected.pendingUploads[0].changeId)
    }

    @Test
    fun `removeAcknowledgedChanges: no-op when none acknowledged`() {
        val changeId = UUID.randomUUID()
        val change = PendingChange(
            changeId = changeId,
            type = ChangeType.ADD,
            fileId = UUID.randomUUID(),
            chunkRefs = emptyList(),
            vectorClock = emptyMap(),
            timestamp = System.currentTimeMillis()
        )
        val metadata = SyncMetadata(
            vaultId = vaultId,
            deviceId = deviceA,
            vectorClock = emptyMap(),
            fileIndex = emptyMap(),
            deletionMarkers = emptySet(),
            pendingUploads = listOf(change),
            lastSyncedTimestamp = System.currentTimeMillis(),
            syncFormatVersion = 1
        )

        val collected = GarbageCollector.removeAcknowledgedChanges(metadata, emptySet())

        assertEquals(metadata, collected)
    }
}
