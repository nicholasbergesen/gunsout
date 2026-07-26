package com.nicholasbergesen.gunsout.data.repo

import com.nicholasbergesen.gunsout.data.dao.ProteinEntryDao
import com.nicholasbergesen.gunsout.data.dao.ProteinTargetSnapshotDao
import com.nicholasbergesen.gunsout.data.entity.ProteinEntry
import com.nicholasbergesen.gunsout.data.entity.ProteinTargetSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProteinRepositoryTest {
    @Test
    fun `entries require positive grams and normalize optional labels`() = runTest {
        val entries = FakeProteinEntryDao()
        val repository = ProteinRepository(entries, FakeProteinTargetSnapshotDao())
        val date = LocalDate.of(2026, 7, 26)

        val labeledId = repository.addEntry("user", date, 35, "  Chicken lunch  ", 200L)
        val unlabeledId = repository.addEntry("user", date, 20, "   ", 100L)

        assertEquals("Chicken lunch", entries.row(labeledId).label)
        assertNull(entries.row(unlabeledId).label)
        assertEquals(
            listOf(labeledId, unlabeledId),
            repository.observeEntriesForDate("user", date).first().map { it.id }
        )
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest {
                repository.addEntry("user", date, 0, null)
            }
        }
    }

    @Test
    fun `updates and deletes remain user scoped and undo preserves original time`() = runTest {
        val entries = FakeProteinEntryDao()
        val repository = ProteinRepository(entries, FakeProteinTargetSnapshotDao())
        val date = LocalDate.of(2026, 7, 26)
        val id = repository.addEntry("user-a", date, 30, "Shake", 123L)
        val original = entries.row(id)

        assertFalse(repository.updateEntry("user-b", id, 40, "Other"))
        assertEquals(30, entries.row(id).grams)
        assertTrue(repository.updateEntry("user-a", id, 40, "Updated"))
        assertFalse(repository.deleteEntry("user-b", id))
        assertTrue(repository.deleteEntry("user-a", id))

        val restoredId = repository.restoreEntry("user-a", original)
        val restored = entries.row(restoredId)
        assertEquals(original.loggedAt, restored.loggedAt)
        assertEquals(original.label, restored.label)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest {
                repository.restoreEntry("user-b", original)
            }
        }
    }

    @Test
    fun `today target can change or become unknown while past targets stay frozen`() = runTest {
        val snapshots = FakeProteinTargetSnapshotDao()
        val repository = ProteinRepository(FakeProteinEntryDao(), snapshots)
        val today = LocalDate.of(2026, 7, 26)
        val yesterday = today.minusDays(1)

        repository.syncTodayTarget("user", today, 160, today)
        repository.syncTodayTarget("user", today, 170, today)
        repository.syncTodayTarget("user", yesterday, 150, today)

        assertEquals(170, snapshots.row("user", today)?.targetGrams)
        assertNull(snapshots.row("user", yesterday))

        repository.syncTodayTarget("user", today, null, today)
        assertNull(snapshots.row("user", today))
    }

    private class FakeProteinEntryDao : ProteinEntryDao {
        private val rows = linkedMapOf<Long, ProteinEntry>()
        private var nextId = 1L

        override fun observeForDate(userId: String, date: LocalDate): Flow<List<ProteinEntry>> =
            flowOf(
                rows.values
                    .filter { it.userId == userId && it.date == date }
                    .sortedWith(
                        compareByDescending<ProteinEntry> { it.loggedAt }
                            .thenByDescending { it.id }
                    )
            )

        override fun observeRange(
            userId: String,
            start: LocalDate,
            end: LocalDate
        ): Flow<List<ProteinEntry>> =
            flowOf(
                rows.values
                    .filter {
                        it.userId == userId &&
                            !it.date.isBefore(start) &&
                            !it.date.isAfter(end)
                    }
                    .sortedWith(
                        compareBy<ProteinEntry> { it.date }
                            .thenBy { it.loggedAt }
                            .thenBy { it.id }
                    )
            )

        override suspend fun getAll(userId: String): List<ProteinEntry> =
            rows.values.filter { it.userId == userId }.sortedBy { it.id }

        override suspend fun insert(entry: ProteinEntry): Long {
            val id = entry.id.takeIf { it != 0L } ?: nextId++
            rows[id] = entry.copy(id = id)
            return id
        }

        override suspend fun update(
            userId: String,
            id: Long,
            grams: Int,
            label: String?
        ): Int {
            val current = rows[id]?.takeIf { it.userId == userId } ?: return 0
            rows[id] = current.copy(grams = grams, label = label)
            return 1
        }

        override suspend fun delete(userId: String, id: Long): Int {
            val current = rows[id]?.takeIf { it.userId == userId } ?: return 0
            rows.remove(current.id)
            return 1
        }

        fun row(id: Long): ProteinEntry = rows.getValue(id)
    }

    private class FakeProteinTargetSnapshotDao : ProteinTargetSnapshotDao {
        private val rows = linkedMapOf<Pair<String, LocalDate>, ProteinTargetSnapshot>()

        override fun observeRange(
            userId: String,
            start: LocalDate,
            end: LocalDate
        ): Flow<List<ProteinTargetSnapshot>> =
            flowOf(
                rows.values.filter {
                    it.userId == userId &&
                        !it.date.isBefore(start) &&
                        !it.date.isAfter(end)
                }.sortedBy { it.date }
            )

        override suspend fun getAll(userId: String): List<ProteinTargetSnapshot> =
            rows.values.filter { it.userId == userId }.sortedBy { it.date }

        override suspend fun upsert(snapshot: ProteinTargetSnapshot) {
            rows[snapshot.userId to snapshot.date] = snapshot
        }

        override suspend fun delete(userId: String, date: LocalDate): Int =
            if (rows.remove(userId to date) == null) 0 else 1

        fun row(userId: String, date: LocalDate): ProteinTargetSnapshot? =
            rows[userId to date]
    }
}
