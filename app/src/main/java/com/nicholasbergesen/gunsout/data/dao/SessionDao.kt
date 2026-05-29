package com.nicholasbergesen.gunsout.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.SessionStatus
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_session WHERE userId = :userId ORDER BY date DESC, id DESC")
    fun observeAll(userId: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE userId = :userId AND status = 'COMPLETED' ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecentCompleted(userId: String, limit: Int = 20): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE userId = :userId AND status IN ('COMPLETED','SKIPPED') ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecentRotation(userId: String, limit: Int = 50): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE userId = :userId AND status = 'COMPLETED' ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLastCompleted(userId: String): WorkoutSession?

    @Query("SELECT * FROM workout_session WHERE userId = :userId AND status = 'IN_PROGRESS' ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getInProgress(userId: String): WorkoutSession?

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSession?

    @Query("SELECT * FROM workout_session WHERE userId = :userId AND date >= :since ORDER BY date ASC")
    suspend fun getSince(userId: String, since: LocalDate): List<WorkoutSession>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: WorkoutSession): Long

    @Update
    suspend fun update(session: WorkoutSession)

    @Query("UPDATE workout_session SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun setStatus(id: Long, status: SessionStatus, completedAt: java.time.LocalDateTime?)
}

@Dao
interface SetEntryDao {
    @Query("SELECT * FROM set_entry WHERE sessionId = :sessionId ORDER BY id ASC")
    fun observeForSession(sessionId: Long): Flow<List<SetEntry>>

    @Query("SELECT * FROM set_entry WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getForSession(sessionId: Long): List<SetEntry>

    @Query("""
        SELECT * FROM set_entry
        WHERE userId = :userId
          AND exerciseIdSnapshot = :exerciseId
          AND sessionId IN (SELECT id FROM workout_session WHERE userId = :userId AND status = 'COMPLETED')
        ORDER BY id DESC
        LIMIT 50
    """)
    suspend fun getRecentForExercise(userId: String, exerciseId: Long): List<SetEntry>

    @Query("""
        SELECT * FROM set_entry
        WHERE sessionId = :sessionId AND programExerciseId = :programExerciseId
          AND setIndex = :setIndex AND isWarmup = :isWarmup
        LIMIT 1
    """)
    suspend fun findExisting(sessionId: Long, programExerciseId: Long?, setIndex: Int, isWarmup: Boolean): SetEntry?

    @androidx.room.Upsert
    suspend fun upsert(set: SetEntry): Long

    @Insert
    suspend fun insert(set: SetEntry): Long

    @Update
    suspend fun update(set: SetEntry)

    @Query("DELETE FROM set_entry WHERE id = :id")
    suspend fun delete(id: Long)
}
