package com.nicholasbergesen.gunsout.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BodyMetricsLogDao {
    @Query("SELECT * FROM body_metrics_log WHERE userId = :userId ORDER BY date DESC, id DESC")
    fun observeAll(userId: String): Flow<List<BodyMetricsLog>>

    @Query("SELECT * FROM body_metrics_log WHERE userId = :userId AND date >= :since ORDER BY date ASC")
    fun observeSince(userId: String, since: LocalDate): Flow<List<BodyMetricsLog>>

    @Query("SELECT * FROM body_metrics_log WHERE userId = :userId ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLatest(userId: String): BodyMetricsLog?

    @Query("SELECT * FROM body_metrics_log WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getOnDate(userId: String, date: LocalDate): BodyMetricsLog?

    @Insert
    suspend fun insert(log: BodyMetricsLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(log: BodyMetricsLog): Long

    @Update
    suspend fun update(log: BodyMetricsLog)

    @Query("DELETE FROM body_metrics_log WHERE id = :id")
    suspend fun delete(id: Long)
}
