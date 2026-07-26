package com.nicholasbergesen.gunsout.data.repo

import com.nicholasbergesen.gunsout.data.dao.CreatineDao
import com.nicholasbergesen.gunsout.data.entity.CreatineCheck
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class CreatineRepositoryTest {
    @Test
    fun `checking is idempotent and unchecking removes todays record`() = runTest {
        val dao = FakeCreatineDao()
        val repository = CreatineRepository(dao) {}
        val date = LocalDate.of(2026, 7, 26)

        repository.ensureSettings("user")
        repository.setTaken("user", date, true)
        repository.setTaken("user", date, true)

        assertEquals(5, dao.getCheck("user", date)?.doseGrams)
        assertEquals(1, dao.checkCount())

        repository.setTaken("user", date, false)
        assertNull(dao.getCheck("user", date))
    }

    @Test
    fun `dose edits preserve an existing check and apply after rechecking`() = runTest {
        val dao = FakeCreatineDao()
        val rescheduled = mutableListOf<CreatineSettings>()
        val repository = CreatineRepository(dao, rescheduled::add)
        val date = LocalDate.of(2026, 7, 26)

        repository.ensureSettings("user")
        repository.setTaken("user", date, true)
        repository.updateSettings("user", 7, LocalTime.of(8, 30))

        assertEquals(5, dao.getCheck("user", date)?.doseGrams)
        assertEquals(7, repository.observeSettings("user").first().doseGrams)
        assertEquals(LocalTime.of(8, 30), rescheduled.single().reminderTime)

        repository.setTaken("user", date, false)
        repository.setTaken("user", date, true)
        assertEquals(7, dao.getCheck("user", date)?.doseGrams)
    }

    @Test
    fun `creatine settings reject non-positive doses`() {
        val repository = CreatineRepository(FakeCreatineDao()) {}

        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.updateSettings("user", 0, null) }
        }
    }

    private class FakeCreatineDao : CreatineDao {
        private val settings = linkedMapOf<String, CreatineSettings>()
        private val checks = linkedMapOf<Pair<String, LocalDate>, CreatineCheck>()

        override fun observeSettings(userId: String): Flow<CreatineSettings?> =
            flowOf(settings[userId])

        override suspend fun getSettings(userId: String): CreatineSettings? = settings[userId]

        override suspend fun insertSettings(settings: CreatineSettings): Long {
            if (this.settings.putIfAbsent(settings.userId, settings) != null) return -1
            return 1
        }

        override suspend fun upsertSettings(settings: CreatineSettings) {
            this.settings[settings.userId] = settings
        }

        override fun observeCheck(userId: String, date: LocalDate): Flow<CreatineCheck?> =
            flowOf(checks[userId to date])

        override suspend fun getCheck(userId: String, date: LocalDate): CreatineCheck? =
            checks[userId to date]

        override suspend fun getAllChecks(userId: String): List<CreatineCheck> =
            checks.values.filter { it.userId == userId }.sortedBy { it.date }

        override suspend fun insertCheck(check: CreatineCheck): Long {
            if (checks.putIfAbsent(check.userId to check.date, check) != null) return -1
            return 1
        }

        override suspend fun deleteCheck(userId: String, date: LocalDate): Int =
            if (checks.remove(userId to date) == null) 0 else 1

        fun checkCount(): Int = checks.size
    }
}
