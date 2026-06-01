package com.nicholasbergesen.gunsout.data.repo

import com.nicholasbergesen.gunsout.data.dao.BodyMetricsLogDao
import com.nicholasbergesen.gunsout.data.dao.SupplementDao
import com.nicholasbergesen.gunsout.data.dao.SupplementLogDao
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.entity.Supplement
import com.nicholasbergesen.gunsout.data.entity.SupplementLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyRepository @Inject constructor(
    private val bodyMetricsLogDao: BodyMetricsLogDao
) {
    fun observeAll(userId: String): Flow<List<BodyMetricsLog>> = bodyMetricsLogDao.observeAll(userId)
    fun observeSince(userId: String, since: LocalDate): Flow<List<BodyMetricsLog>> =
        bodyMetricsLogDao.observeSince(userId, since)
    suspend fun getLatest(userId: String): BodyMetricsLog? = bodyMetricsLogDao.getLatest(userId)
    suspend fun getOnDate(userId: String, date: LocalDate): BodyMetricsLog? =
        bodyMetricsLogDao.getOnDate(userId, date)

    /**
     * Upsert a body-metrics row by [date]. If a row already exists for that date, its existing
     * composition fields are preserved when the new input leaves them null. This way a quick
     * weight-only update at 6pm doesn't wipe out a full smart-scale snapshot taken in the morning.
     */
    suspend fun log(
        userId: String,
        date: LocalDate,
        weightKg: Double,
        bodyFatPct: Double? = null,
        muscleMassKg: Double? = null,
        waterPct: Double? = null,
        boneMassKg: Double? = null,
        visceralFatRating: Int? = null,
        notes: String? = null
    ): Long {
        val existing = bodyMetricsLogDao.getOnDate(userId, date)
        return if (existing == null) {
            bodyMetricsLogDao.insert(BodyMetricsLog(
                userId = userId,
                date = date, weightKg = weightKg,
                bodyFatPct = bodyFatPct, muscleMassKg = muscleMassKg, waterPct = waterPct,
                boneMassKg = boneMassKg, visceralFatRating = visceralFatRating, notes = notes
            ))
        } else {
            val merged = existing.copy(
                weightKg = weightKg,
                bodyFatPct = bodyFatPct ?: existing.bodyFatPct,
                muscleMassKg = muscleMassKg ?: existing.muscleMassKg,
                waterPct = waterPct ?: existing.waterPct,
                boneMassKg = boneMassKg ?: existing.boneMassKg,
                visceralFatRating = visceralFatRating ?: existing.visceralFatRating,
                notes = notes ?: existing.notes
            )
            bodyMetricsLogDao.update(merged)
            existing.id
        }
    }

    suspend fun restore(log: BodyMetricsLog) {
        bodyMetricsLogDao.insertOrReplace(log)
    }

    suspend fun delete(id: Long) = bodyMetricsLogDao.delete(id)
}

@Singleton
class SupplementRepository @Inject constructor(
    private val supplementDao: SupplementDao,
    private val supplementLogDao: SupplementLogDao,
    private val scheduler: com.nicholasbergesen.gunsout.feature.supplements.SupplementReminderScheduler
) {
    fun observeActive(userId: String): Flow<List<Supplement>> = supplementDao.observeActive(userId)
    fun observeAll(userId: String): Flow<List<Supplement>> = supplementDao.observeAll(userId)

    suspend fun update(supplement: Supplement) {
        supplementDao.update(supplement)
        scheduler.reschedule(supplement)
    }

    suspend fun setReminderTime(supplementId: Long, time: java.time.LocalTime?) {
        val s = supplementDao.getById(supplementId) ?: return
        val updated = s.copy(reminderTime = time)
        supplementDao.update(updated)
        scheduler.reschedule(updated)
    }

    fun observeLogsForDate(userId: String, date: LocalDate): Flow<List<SupplementLog>> =
        supplementLogDao.observeForDate(userId, date)

    suspend fun countForDate(userId: String, supplementId: Long, date: LocalDate): Int =
        supplementLogDao.countForDate(userId, supplementId, date)

    suspend fun recentForSupplement(userId: String, supplementId: Long, since: LocalDate): List<SupplementLog> =
        supplementLogDao.recentForSupplement(userId, supplementId, since)

    /** Logs a default-dose intake for today if not already logged. Returns true if a new log was created. */
    suspend fun markTakenToday(userId: String, supplement: Supplement): Boolean {
        val today = LocalDate.now()
        if (supplementLogDao.countForDate(userId, supplement.id, today) > 0) return false
        supplementLogDao.insert(SupplementLog(
            userId = userId,
            supplementId = supplement.id,
            date = today,
            doseTaken = supplement.defaultDose,
            unit = supplement.unit
        ))
        return true
    }
}
