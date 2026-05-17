package com.gunsout.data.repo

import com.gunsout.data.dao.BodyMetricsLogDao
import com.gunsout.data.dao.SupplementDao
import com.gunsout.data.dao.SupplementLogDao
import com.gunsout.data.entity.BodyMetricsLog
import com.gunsout.data.entity.Supplement
import com.gunsout.data.entity.SupplementLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyRepository @Inject constructor(
    private val bodyMetricsLogDao: BodyMetricsLogDao
) {
    fun observeAll(): Flow<List<BodyMetricsLog>> = bodyMetricsLogDao.observeAll()
    fun observeSince(since: LocalDate): Flow<List<BodyMetricsLog>> = bodyMetricsLogDao.observeSince(since)
    suspend fun getLatest(): BodyMetricsLog? = bodyMetricsLogDao.getLatest()
    suspend fun getOnDate(date: LocalDate): BodyMetricsLog? = bodyMetricsLogDao.getOnDate(date)

    /**
     * Upsert a body-metrics row by [date]. If a row already exists for that date, its existing
     * composition fields are preserved when the new input leaves them null. This way a quick
     * weight-only update at 6pm doesn't wipe out a full smart-scale snapshot taken in the morning.
     */
    suspend fun log(
        date: LocalDate,
        weightKg: Double,
        bodyFatPct: Double? = null,
        muscleMassKg: Double? = null,
        waterPct: Double? = null,
        boneMassKg: Double? = null,
        visceralFatRating: Int? = null,
        notes: String? = null
    ): Long {
        val existing = bodyMetricsLogDao.getOnDate(date)
        return if (existing == null) {
            bodyMetricsLogDao.insert(BodyMetricsLog(
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

    suspend fun delete(id: Long) = bodyMetricsLogDao.delete(id)
}

@Singleton
class SupplementRepository @Inject constructor(
    private val supplementDao: SupplementDao,
    private val supplementLogDao: SupplementLogDao
) {
    fun observeActive(): Flow<List<Supplement>> = supplementDao.observeActive()
    fun observeAll(): Flow<List<Supplement>> = supplementDao.observeAll()

    suspend fun update(supplement: Supplement) = supplementDao.update(supplement)

    fun observeLogsForDate(date: LocalDate): Flow<List<SupplementLog>> =
        supplementLogDao.observeForDate(date)

    suspend fun countForDate(supplementId: Long, date: LocalDate): Int =
        supplementLogDao.countForDate(supplementId, date)

    suspend fun recentForSupplement(supplementId: Long, since: LocalDate): List<SupplementLog> =
        supplementLogDao.recentForSupplement(supplementId, since)

    /** Logs a default-dose intake for today if not already logged. Returns true if a new log was created. */
    suspend fun markTakenToday(supplement: Supplement): Boolean {
        val today = LocalDate.now()
        if (supplementLogDao.countForDate(supplement.id, today) > 0) return false
        supplementLogDao.insert(SupplementLog(
            supplementId = supplement.id,
            date = today,
            doseTaken = supplement.defaultDose,
            unit = supplement.unit
        ))
        return true
    }
}
