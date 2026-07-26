package com.nicholasbergesen.gunsout.data.repo

import com.nicholasbergesen.gunsout.data.dao.CreatineDao
import com.nicholasbergesen.gunsout.data.dao.ProteinEntryDao
import com.nicholasbergesen.gunsout.data.dao.ProteinTargetSnapshotDao
import com.nicholasbergesen.gunsout.data.entity.CreatineCheck
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import com.nicholasbergesen.gunsout.data.entity.ProteinEntry
import com.nicholasbergesen.gunsout.data.entity.ProteinTargetSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProteinRepository @Inject constructor(
    private val entryDao: ProteinEntryDao,
    private val targetSnapshotDao: ProteinTargetSnapshotDao
) {
    fun observeEntriesForDate(userId: String, date: LocalDate): Flow<List<ProteinEntry>> =
        entryDao.observeForDate(userId, date)

    fun observeEntriesRange(
        userId: String,
        start: LocalDate,
        end: LocalDate
    ): Flow<List<ProteinEntry>> {
        require(!end.isBefore(start)) { "end must not be before start" }
        return entryDao.observeRange(userId, start, end)
    }

    fun observeTargetSnapshots(
        userId: String,
        start: LocalDate,
        end: LocalDate
    ): Flow<List<ProteinTargetSnapshot>> {
        require(!end.isBefore(start)) { "end must not be before start" }
        return targetSnapshotDao.observeRange(userId, start, end)
    }

    suspend fun getAllEntries(userId: String): List<ProteinEntry> = entryDao.getAll(userId)

    suspend fun getAllTargetSnapshots(userId: String): List<ProteinTargetSnapshot> =
        targetSnapshotDao.getAll(userId)

    suspend fun addEntry(
        userId: String,
        date: LocalDate,
        grams: Int,
        label: String?,
        loggedAt: Long = System.currentTimeMillis()
    ): Long {
        require(grams > 0) { "protein grams must be positive" }
        return entryDao.insert(
            ProteinEntry(
                userId = userId,
                date = date,
                grams = grams,
                label = label.normalizedLabel(),
                loggedAt = loggedAt
            )
        )
    }

    suspend fun updateEntry(userId: String, entryId: Long, grams: Int, label: String?): Boolean {
        require(grams > 0) { "protein grams must be positive" }
        return entryDao.update(userId, entryId, grams, label.normalizedLabel()) == 1
    }

    suspend fun deleteEntry(userId: String, entryId: Long): Boolean =
        entryDao.delete(userId, entryId) == 1

    suspend fun restoreEntry(userId: String, entry: ProteinEntry): Long {
        require(entry.userId == userId) { "entry belongs to a different user" }
        require(entry.grams > 0) { "protein grams must be positive" }
        return entryDao.insert(entry.copy(id = 0))
    }

    suspend fun syncTodayTarget(
        userId: String,
        date: LocalDate,
        targetGrams: Int?,
        today: LocalDate = LocalDate.now()
    ) {
        if (date != today) return
        if (targetGrams == null) {
            targetSnapshotDao.delete(userId, date)
        } else {
            require(targetGrams > 0) { "protein target must be positive" }
            targetSnapshotDao.upsert(ProteinTargetSnapshot(userId, date, targetGrams))
        }
    }

    private fun String?.normalizedLabel(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}

@Singleton
class CreatineRepository @Inject constructor(
    private val creatineDao: CreatineDao,
    private val reminderUpdater: CreatineReminderUpdater
) {
    fun observeSettings(userId: String): Flow<CreatineSettings> =
        creatineDao.observeSettings(userId).map { it ?: CreatineSettings(userId) }

    fun observeCheck(userId: String, date: LocalDate): Flow<CreatineCheck?> =
        creatineDao.observeCheck(userId, date)

    suspend fun ensureSettings(userId: String) {
        creatineDao.insertSettings(CreatineSettings(userId))
    }

    suspend fun updateSettings(userId: String, doseGrams: Int, reminderTime: LocalTime?) {
        require(doseGrams > 0) { "creatine dose must be positive" }
        val settings = CreatineSettings(userId, doseGrams, reminderTime)
        creatineDao.upsertSettings(settings)
        reminderUpdater.reschedule(settings)
    }

    suspend fun setTaken(userId: String, date: LocalDate, taken: Boolean) {
        if (!taken) {
            creatineDao.deleteCheck(userId, date)
            return
        }
        val settings = creatineDao.getSettings(userId) ?: CreatineSettings(userId).also {
            creatineDao.insertSettings(it)
        }
        creatineDao.insertCheck(
            CreatineCheck(
                userId = userId,
                date = date,
                doseGrams = settings.doseGrams
            )
        )
    }
}

fun interface CreatineReminderUpdater {
    fun reschedule(settings: CreatineSettings)
}
