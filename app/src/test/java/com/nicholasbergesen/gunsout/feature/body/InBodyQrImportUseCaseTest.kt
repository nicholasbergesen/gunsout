package com.nicholasbergesen.gunsout.feature.body

import com.nicholasbergesen.gunsout.data.dao.BodyMetricsLogDao
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.repo.BodyRepository
import com.nicholasbergesen.gunsout.domain.inbody.InBodyQrParseFailure
import com.nicholasbergesen.gunsout.domain.inbody.InBodyQrPayloadParser
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InBodyQrImportUseCaseTest {

    @Test
    fun `import inserts new body log with provenance note`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val useCase = useCase(dao)

        val result = useCase.import("user", SAMPLE_QR_URL)

        assertTrue(result is InBodyQrImportResult.Imported)
        val saved = dao.getOnDate("user", LocalDate.of(2025, 10, 20))!!
        assertEquals(67.8, saved.weightKg, 0.001)
        assertEquals(14.2, saved.bodyFatPct!!, 0.001)
        assertEquals(32.8, saved.muscleMassKg!!, 0.001)
        assertEquals(3, saved.visceralFatRating)
        assertNull(saved.waterPct)
        assertNull(saved.boneMassKg)
        assertEquals("InBody 270 #27379084 · 2025-10-20 10:40", saved.notes)
    }

    @Test
    fun `import merges without erasing existing optional fields or notes`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val useCase = useCase(dao)
        val date = LocalDate.of(2025, 10, 20)
        dao.insert(
            BodyMetricsLog(
                userId = "user",
                date = date,
                weightKg = 66.0,
                waterPct = 55.0,
                boneMassKg = 3.1,
                notes = "manual note"
            )
        )

        val result = useCase.import("user", SAMPLE_QR_URL)

        assertTrue(result is InBodyQrImportResult.Imported)
        val saved = dao.getOnDate("user", date)!!
        assertEquals(67.8, saved.weightKg, 0.001)
        assertEquals(14.2, saved.bodyFatPct!!, 0.001)
        assertEquals(32.8, saved.muscleMassKg!!, 0.001)
        assertEquals(55.0, saved.waterPct!!, 0.001)
        assertEquals(3.1, saved.boneMassKg!!, 0.001)
        assertEquals("manual note", saved.notes)
    }

    @Test
    fun `undo deletes inserted import row`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val useCase = useCase(dao)

        val result = useCase.import("user", SAMPLE_QR_URL) as InBodyQrImportResult.Imported
        useCase.undo(result.undo)

        assertNull(dao.getOnDate("user", LocalDate.of(2025, 10, 20)))
    }

    @Test
    fun `undo restores previous row after merge`() = runTest {
        val dao = FakeBodyMetricsLogDao()
        val useCase = useCase(dao)
        val date = LocalDate.of(2025, 10, 20)
        val originalId = dao.insert(
            BodyMetricsLog(
                userId = "user",
                date = date,
                weightKg = 66.0,
                bodyFatPct = 12.0,
                muscleMassKg = 31.0,
                waterPct = 55.0,
                boneMassKg = 3.1,
                visceralFatRating = 2,
                notes = "manual note"
            )
        )

        val result = useCase.import("user", SAMPLE_QR_URL) as InBodyQrImportResult.Imported
        useCase.undo(result.undo)

        val restored = dao.getOnDate("user", date)!!
        assertEquals(originalId, restored.id)
        assertEquals(66.0, restored.weightKg, 0.001)
        assertEquals(12.0, restored.bodyFatPct!!, 0.001)
        assertEquals(31.0, restored.muscleMassKg!!, 0.001)
        assertEquals(55.0, restored.waterPct!!, 0.001)
        assertEquals(3.1, restored.boneMassKg!!, 0.001)
        assertEquals(2, restored.visceralFatRating)
        assertEquals("manual note", restored.notes)
    }

    @Test
    fun `non inbody qr returns specific failure`() = runTest {
        val result = useCase(FakeBodyMetricsLogDao()).import("user", "https://example.com?IBData=abc")

        assertEquals(InBodyQrParseFailure.NOT_INBODY_QR, (result as InBodyQrImportResult.Failed).failure)
        assertEquals("That doesn't look like an InBody QR code", result.message)
    }

    private fun useCase(dao: FakeBodyMetricsLogDao) =
        InBodyQrImportUseCase(InBodyQrPayloadParser(), BodyRepository(dao))

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
        const val SAMPLE_QR_URL =
            "https://qrcode.inbody.com?IBData=270-30F92004316!27379084!!!!!!!!!!!!!!!!!!!!17500310M20251020104037011501020124040003500428009700810162042603790463058106780573077510060328102500955022101420810678!0000!0000!0000162600770080009003490811042612000000000000000000003446337202292379231730673027018320922041PASS00113003890101042103200674022000160150008511111001111111111100820270-2DM-0416!!!!0185023000000100020010100"
    }
}
