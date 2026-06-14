package com.nicholasbergesen.gunsout.data.repo

import com.nicholasbergesen.gunsout.data.dao.BodyMetricsLogDao
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyRepositoryTest {

    @Test fun `first body log inserts all supplied composition fields`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val repository = BodyRepository(dao)
        val date = LocalDate.of(2026, 6, 14)

        val id = repository.log(
            userId = "user",
            date = date,
            weightKg = 96.2,
            bodyFatPct = 18.4,
            muscleMassKg = 70.1,
            waterLiters = 52.9,
            boneMassKg = 3.5,
            visceralFatRating = 8,
            notes = "morning scan"
        )

        val saved = dao.getOnDate("user", date)!!
        assertEquals(id, saved.id)
        assertEquals(96.2, saved.weightKg, 0.001)
        assertEquals(18.4, saved.bodyFatPct!!, 0.001)
        assertEquals(70.1, saved.muscleMassKg!!, 0.001)
        assertEquals(52.9, saved.waterLiters!!, 0.001)
        assertEquals(3.5, saved.boneMassKg!!, 0.001)
        assertEquals(8, saved.visceralFatRating)
        assertEquals("morning scan", saved.notes)
    }

    @Test fun `same day body log preserves optional fields left blank`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val repository = BodyRepository(dao)
        val date = LocalDate.of(2026, 6, 14)
        val originalId = dao.insert(
            BodyMetricsLog(
                userId = "user",
                date = date,
                weightKg = 97.0,
                bodyFatPct = 19.0,
                muscleMassKg = 70.0,
                waterLiters = 51.0,
                boneMassKg = 3.3,
                visceralFatRating = 9,
                notes = "full scan"
            )
        )

        val returnedId = repository.log(
            userId = "user",
            date = date,
            weightKg = 96.5,
            bodyFatPct = 18.7
        )

        val merged = dao.getOnDate("user", date)!!
        assertEquals(originalId, returnedId)
        assertEquals(96.5, merged.weightKg, 0.001)
        assertEquals(18.7, merged.bodyFatPct!!, 0.001)
        assertEquals(70.0, merged.muscleMassKg!!, 0.001)
        assertEquals(51.0, merged.waterLiters!!, 0.001)
        assertEquals(3.3, merged.boneMassKg!!, 0.001)
        assertEquals(9, merged.visceralFatRating)
        assertEquals("full scan", merged.notes)
    }

    @Test fun `restored rows can be observed and deleted by id`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val repository = BodyRepository(dao)
        val older = BodyMetricsLog(id = 20, userId = "user", date = LocalDate.of(2026, 6, 1), weightKg = 98.0)
        val newer = BodyMetricsLog(id = 21, userId = "user", date = LocalDate.of(2026, 6, 14), weightKg = 96.0)

        repository.restore(older)
        repository.restore(newer)

        assertEquals(newer, repository.getLatest("user"))
        assertEquals(listOf(older, newer), repository.observeSince("user", LocalDate.of(2026, 6, 1)).first())
        assertEquals(listOf(newer, older), repository.observeAll("user").first())

        repository.delete(newer.id)
        assertNull(repository.getOnDate("user", newer.date))
    }

    private class FakeBodyMetricsLogDao : BodyMetricsLogDao {
        private val rows = linkedMapOf<Long, BodyMetricsLog>()
        private var nextId = 1L

        override fun observeAll(userId: String): Flow<List<BodyMetricsLog>> =
            flowOf(rows.values.filter { it.userId == userId }
                .sortedWith(compareByDescending<BodyMetricsLog> { it.date }.thenByDescending { it.id }))

        override fun observeSince(userId: String, since: LocalDate): Flow<List<BodyMetricsLog>> =
            flowOf(rows.values.filter { it.userId == userId && !it.date.isBefore(since) }.sortedBy { it.date })

        override suspend fun getLatest(userId: String): BodyMetricsLog? =
            rows.values.filter { it.userId == userId }.maxWithOrNull(compareBy<BodyMetricsLog> { it.date }.thenBy { it.id })

        override suspend fun getOnDate(userId: String, date: LocalDate): BodyMetricsLog? =
            rows.values.firstOrNull { it.userId == userId && it.date == date }

        override suspend fun insert(log: BodyMetricsLog): Long {
            val id = log.id.takeIf { it != 0L } ?: nextId++
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
}
