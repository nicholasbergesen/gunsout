package com.gunsout.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gunsout.data.entity.BodyMetricsLog
import com.gunsout.data.entity.Supplement
import com.gunsout.data.entity.SupplementLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplement WHERE userId = :userId AND isActive = 1 ORDER BY name")
    fun observeActive(userId: String): Flow<List<Supplement>>

    @Query("SELECT * FROM supplement WHERE userId = :userId AND isActive = 1 ORDER BY name")
    suspend fun allActiveOnce(userId: String): List<Supplement>

    @Query("SELECT * FROM supplement WHERE userId = :userId ORDER BY name")
    fun observeAll(userId: String): Flow<List<Supplement>>

    @Query("SELECT * FROM supplement WHERE userId = :userId AND seedKey = :seedKey LIMIT 1")
    suspend fun getBySeedKey(userId: String, seedKey: String): Supplement?

    @Query("SELECT * FROM supplement WHERE id = :id")
    suspend fun getById(id: Long): Supplement?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(supplement: Supplement): Long

    @Update
    suspend fun update(supplement: Supplement)
}

@Dao
interface SupplementLogDao {
    @Query("SELECT * FROM supplement_log WHERE userId = :userId AND date = :date ORDER BY takenAt ASC")
    fun observeForDate(userId: String, date: LocalDate): Flow<List<SupplementLog>>

    @Query("SELECT * FROM supplement_log WHERE userId = :userId AND supplementId = :supplementId AND date >= :since ORDER BY date DESC")
    suspend fun recentForSupplement(userId: String, supplementId: Long, since: LocalDate): List<SupplementLog>

    @Query("SELECT * FROM supplement_log WHERE userId = :userId ORDER BY id")
    suspend fun getAll(userId: String): List<SupplementLog>

    @Query("SELECT COUNT(*) FROM supplement_log WHERE userId = :userId AND supplementId = :supplementId AND date = :date")
    suspend fun countForDate(userId: String, supplementId: Long, date: LocalDate): Int

    @Insert
    suspend fun insert(log: SupplementLog): Long

    @Query("DELETE FROM supplement_log WHERE id = :id")
    suspend fun delete(id: Long)
}

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

    @Update
    suspend fun update(log: BodyMetricsLog)

    @Query("DELETE FROM body_metrics_log WHERE id = :id")
    suspend fun delete(id: Long)
}
