package com.nicholasbergesen.gunsout.data.repo

import com.nicholasbergesen.gunsout.data.dao.BodyMetricsLogDao
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
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

    suspend fun log(
        userId: String,
        date: LocalDate,
        weightKg: Double,
        bodyFatPct: Double? = null,
        muscleMassKg: Double? = null,
        waterLiters: Double? = null,
        boneMassKg: Double? = null,
        visceralFatRating: Int? = null,
        notes: String? = null
    ): Long {
        val existing = bodyMetricsLogDao.getOnDate(userId, date)
        return if (existing == null) {
            bodyMetricsLogDao.insert(BodyMetricsLog(
                userId = userId,
                date = date, weightKg = weightKg,
                bodyFatPct = bodyFatPct, muscleMassKg = muscleMassKg, waterLiters = waterLiters,
                boneMassKg = boneMassKg, visceralFatRating = visceralFatRating, notes = notes
            ))
        } else {
            val merged = existing.copy(
                weightKg = weightKg,
                bodyFatPct = bodyFatPct ?: existing.bodyFatPct,
                muscleMassKg = muscleMassKg ?: existing.muscleMassKg,
                waterLiters = waterLiters ?: existing.waterLiters,
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
