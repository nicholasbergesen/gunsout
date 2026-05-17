package com.gunsout.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gunsout.data.entity.SetEntry
import com.gunsout.data.entity.SessionStatus
import com.gunsout.data.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_session ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE status = 'COMPLETED' ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecentCompleted(limit: Int = 20): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE status IN ('COMPLETED','SKIPPED') ORDER BY date DESC, id DESC LIMIT :limit")
    fun observeRecentRotation(limit: Int = 50): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE status = 'COMPLETED' ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLastCompleted(): WorkoutSession?

    @Query("SELECT * FROM workout_session WHERE status = 'IN_PROGRESS' ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getInProgress(): WorkoutSession?

    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSession?

    @Query("SELECT * FROM workout_session WHERE date >= :since ORDER BY date ASC")
    suspend fun getSince(since: LocalDate): List<WorkoutSession>

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
        WHERE exerciseIdSnapshot = :exerciseId
          AND sessionId IN (SELECT id FROM workout_session WHERE status = 'COMPLETED')
        ORDER BY id DESC
        LIMIT 50
    """)
    suspend fun getRecentForExercise(exerciseId: Long): List<SetEntry>

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
