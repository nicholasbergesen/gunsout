package com.nicholasbergesen.gunsout.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.ExerciseAlternate
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {
    @Query("SELECT * FROM program WHERE userId = :userId ORDER BY isActive DESC, name ASC")
    fun observeAll(userId: String): Flow<List<Program>>

    @Query("SELECT * FROM program WHERE userId = :userId AND isActive = 1 LIMIT 1")
    fun observeActive(userId: String): Flow<Program?>

    @Query("SELECT * FROM program WHERE userId = :userId AND isActive = 1 LIMIT 1")
    suspend fun getActive(userId: String): Program?

    @Query("SELECT * FROM program WHERE id = :id")
    suspend fun getById(id: Long): Program?

    @Query("SELECT * FROM program WHERE userId = :userId AND seedKey = :seedKey LIMIT 1")
    suspend fun getBySeedKey(userId: String, seedKey: String): Program?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(program: Program): Long

    @Update
    suspend fun update(program: Program)

    @Query("UPDATE program SET isActive = 0 WHERE userId = :userId")
    suspend fun clearActive(userId: String)

    @Transaction
    suspend fun setActive(userId: String, id: Long) {
        clearActive(userId)
        val existing = getById(id) ?: return
        update(existing.copy(isActive = true))
    }

    @Query("DELETE FROM program WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ProgramDayDao {
    @Query("SELECT * FROM program_day WHERE programId = :programId ORDER BY orderIndex ASC")
    fun observeForProgram(programId: Long): Flow<List<ProgramDay>>

    @Query("SELECT * FROM program_day WHERE programId = :programId ORDER BY orderIndex ASC")
    suspend fun getForProgram(programId: Long): List<ProgramDay>

    @Query("SELECT * FROM program_day WHERE id = :id")
    suspend fun getById(id: Long): ProgramDay?

    @Insert
    suspend fun insert(day: ProgramDay): Long

    @Update
    suspend fun update(day: ProgramDay)

    @Query("DELETE FROM program_day WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise WHERE userId = :userId AND isArchived = 0 ORDER BY primaryMuscleGroup, name")
    fun observeAll(userId: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Query("SELECT * FROM exercise WHERE userId = :userId AND seedKey = :seedKey LIMIT 1")
    suspend fun getBySeedKey(userId: String, seedKey: String): Exercise?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)
}

@Dao
interface ExerciseAlternateDao {
    @Query("""
        SELECT e.* FROM exercise e
        INNER JOIN exercise_alternate a ON a.alternateExerciseId = e.id
        WHERE a.exerciseId = :exerciseId AND e.isArchived = 0
    """)
    suspend fun getAlternates(exerciseId: Long): List<Exercise>

    /** All links for the given user, for backup. */
    @Query("SELECT * FROM exercise_alternate WHERE userId = :userId")
    suspend fun getAll(userId: String): List<ExerciseAlternate>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: ExerciseAlternate)
}

@Dao
interface ProgramExerciseDao {
    @Query("SELECT * FROM program_exercise WHERE programDayId = :programDayId ORDER BY orderIndex ASC")
    fun observeForDay(programDayId: Long): Flow<List<ProgramExercise>>

    @Query("SELECT * FROM program_exercise WHERE programDayId = :programDayId ORDER BY orderIndex ASC")
    suspend fun getForDay(programDayId: Long): List<ProgramExercise>

    @Query("SELECT * FROM program_exercise WHERE id = :id")
    suspend fun getById(id: Long): ProgramExercise?

    @Insert
    suspend fun insert(pe: ProgramExercise): Long

    @Update
    suspend fun update(pe: ProgramExercise)

    @Query("DELETE FROM program_exercise WHERE id = :id")
    suspend fun delete(id: Long)
}
