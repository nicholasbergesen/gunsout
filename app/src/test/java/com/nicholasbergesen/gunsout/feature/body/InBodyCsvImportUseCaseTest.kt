package com.nicholasbergesen.gunsout.feature.body

import com.nicholasbergesen.gunsout.data.dao.BodyMetricsLogDao
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.repo.BodyRepository
import com.nicholasbergesen.gunsout.domain.inbody.InBodyCsvPayloadParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InBodyCsvImportUseCaseTest {

    @Test
    fun `import inserts csv body metrics`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val useCase = useCase(dao)

        val result = useCase.import("user", SAMPLE_CSV)

        assertTrue(result is InBodyCsvImportResult.Imported)
        val saved = dao.getOnDate("user", LocalDate.of(2026, 6, 7))!!
        assertEquals(80.4, saved.weightKg, 0.001)
        assertEquals(18.4, saved.bodyFatPct!!, 0.001)
        assertEquals(36.2, saved.muscleMassKg!!, 0.001)
        assertEquals(43.8, saved.waterLiters!!, 0.001)
        assertEquals(8, saved.visceralFatRating)
        assertEquals("InBody 270 CSV - 2026-06-07 15:58", saved.notes)
    }

    @Test
    fun `import merges without erasing existing notes`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val useCase = useCase(dao)
        val date = LocalDate.of(2026, 6, 7)
        dao.insert(
            BodyMetricsLog(
                userId = "user",
                date = date,
                weightKg = 79.0,
                bodyFatPct = 17.0,
                muscleMassKg = 35.0,
                notes = "manual note"
            )
        )

        val result = useCase.import("user", SAMPLE_CSV)

        assertTrue(result is InBodyCsvImportResult.Imported)
        val saved = dao.getOnDate("user", date)!!
        assertEquals(80.4, saved.weightKg, 0.001)
        assertEquals(18.4, saved.bodyFatPct!!, 0.001)
        assertEquals(36.2, saved.muscleMassKg!!, 0.001)
        assertEquals("manual note", saved.notes)
    }

    @Test
    fun `undo deletes inserted csv row`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val useCase = useCase(dao)

        val result = useCase.import("user", SAMPLE_CSV) as InBodyCsvImportResult.Imported
        useCase.undo(result.undo)

        assertNull(dao.getOnDate("user", LocalDate.of(2026, 6, 7)))
    }

    @Test
    fun `undo restores previous csv merge row`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val useCase = useCase(dao)
        val date = LocalDate.of(2026, 6, 7)
        val originalId = dao.insert(
            BodyMetricsLog(
                userId = "user",
                date = date,
                weightKg = 79.0,
                bodyFatPct = 17.0,
                muscleMassKg = 35.0,
                notes = "manual note"
            )
        )

        val result = useCase.import("user", SAMPLE_CSV) as InBodyCsvImportResult.Imported
        useCase.undo(result.undo)

        val restored = dao.getOnDate("user", date)!!
        assertEquals(originalId, restored.id)
        assertEquals(79.0, restored.weightKg, 0.001)
        assertEquals(17.0, restored.bodyFatPct!!, 0.001)
        assertEquals(35.0, restored.muscleMassKg!!, 0.001)
        assertEquals("manual note", restored.notes)
    }

    private fun useCase(dao: FakeBodyMetricsLogDao) =
        InBodyCsvImportUseCase(InBodyCsvPayloadParser(), BodyRepository(dao))

    private class FakeBodyMetricsLogDao : BodyMetricsLogDao {
        private val rows = linkedMapOf<Long, BodyMetricsLog>()
        private var nextId = 1L

        override fun observeAll(userId: String): Flow<List<BodyMetricsLog>> =
            flowOf(rows.values.filter { it.userId == userId }.sortedWith(compareByDescending<BodyMetricsLog> { it.date }.thenByDescending { it.id }))

        override fun observeSince(userId: String, since: LocalDate): Flow<List<BodyMetricsLog>> =
            flowOf(rows.values.filter { it.userId == userId && !it.date.isBefore(since) }.sortedBy { it.date })

        override suspend fun getLatest(userId: String): BodyMetricsLog? =
            rows.values.filter { it.userId == userId }.maxWithOrNull(compareBy<BodyMetricsLog> { it.date }.thenBy { it.id })

        override suspend fun getOnDate(userId: String, date: LocalDate): BodyMetricsLog? =
            rows.values.firstOrNull { it.userId == userId && it.date == date }

        override suspend fun insert(log: BodyMetricsLog): Long {
            val id = nextId++
            rows[id] = log.copy(id = id)
            return id
        }

        override suspend fun insertOrReplace(log: BodyMetricsLog): Long {
            val id = log.id.takeIf { it != 0L } ?: nextId++
            rows[id] = log.copy(id = id)
            return id
        }

        override suspend fun update(log: BodyMetricsLog) {
            rows[log.id] = log
        }

        override suspend fun delete(id: Long) {
            rows.remove(id)
        }
    }

    private companion object {
        const val SAMPLE_CSV =
            "\uFEFFDate,Measurement device.,Weight(kg),Skeletal Muscle Mass(kg),Percent Body Fat(%),Visceral Fat Level(Level),Total Body Water(L)\n" +
                "20260607155819,270,80.4,36.2,18.4,8.0,43.8\n"
    }
}
