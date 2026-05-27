package com.gunsout.data.repo

import androidx.room.withTransaction
import com.gunsout.data.dao.ExerciseDao
import com.gunsout.data.dao.ProgramDao
import com.gunsout.data.dao.ProgramDayDao
import com.gunsout.data.dao.ProgramExerciseDao
import com.gunsout.data.db.GunsoutDatabase
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.Program
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.ProgramExercise
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramRepository @Inject constructor(
    private val db: GunsoutDatabase,
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programExerciseDao: ProgramExerciseDao,
    private val exerciseDao: ExerciseDao
) {
    fun observePrograms(userId: String): Flow<List<Program>> = programDao.observeAll(userId)
    fun observeExercises(userId: String): Flow<List<Exercise>> = exerciseDao.observeAll(userId)
    fun observeDaysFor(programId: Long): Flow<List<ProgramDay>> = programDayDao.observeForProgram(programId)
    fun observeExercisesForDay(programDayId: Long): Flow<List<ProgramExercise>> = programExerciseDao.observeForDay(programDayId)

    suspend fun getProgram(id: Long): Program? = programDao.getById(id)
    suspend fun getProgramDay(id: Long): ProgramDay? = programDayDao.getById(id)
    suspend fun getProgramExercise(id: Long): ProgramExercise? = programExerciseDao.getById(id)
    suspend fun getExercise(id: Long): Exercise? = exerciseDao.getById(id)

    suspend fun setActive(userId: String, programId: Long) = programDao.setActive(userId, programId)

    suspend fun createBlankProgram(userId: String, name: String): Long = programDao.insert(
        Program(userId = userId, name = name, isActive = false, isTemplate = false)
    )

    suspend fun duplicateProgram(userId: String, programId: Long, newName: String): Long = db.withTransaction {
        val src = programDao.getById(programId) ?: return@withTransaction -1L
        val newId = programDao.insert(src.copy(
            id = 0, userId = userId, name = newName, isActive = false, isTemplate = false, seedKey = null
        ))
        val days = programDayDao.getForProgram(programId)
        for (day in days) {
            val newDayId = programDayDao.insert(day.copy(id = 0, userId = userId, programId = newId))
            val exes = programExerciseDao.getForDay(day.id)
            for (pe in exes) {
                programExerciseDao.insert(pe.copy(id = 0, userId = userId, programDayId = newDayId))
            }
        }
        newId
    }

    suspend fun renameProgram(programId: Long, newName: String) {
        val p = programDao.getById(programId) ?: return
        programDao.update(p.copy(name = newName))
    }

    suspend fun deleteProgram(programId: Long) = programDao.delete(programId)

    suspend fun updateDay(day: ProgramDay) = programDayDao.update(day)
    suspend fun addDay(userId: String, programId: Long, label: String): Long {
        val existing = programDayDao.getForProgram(programId)
        val nextOrder = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
        return programDayDao.insert(ProgramDay(
            userId = userId, programId = programId, orderIndex = nextOrder, label = label
        ))
    }
    suspend fun deleteDay(dayId: Long) = programDayDao.delete(dayId)

    suspend fun addExerciseToDay(userId: String, programDayId: Long, exerciseId: Long): Long {
        val existing = programExerciseDao.getForDay(programDayId)
        val order = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val ex = exerciseDao.getById(exerciseId) ?: return -1L
        return programExerciseDao.insert(ProgramExercise(
            userId = userId,
            programDayId = programDayId, orderIndex = order, exerciseId = exerciseId,
            restSec = ex.defaultRestSec
        ))
    }

    suspend fun updateProgramExercise(pe: ProgramExercise) = programExerciseDao.update(pe)
    suspend fun deleteProgramExercise(id: Long) = programExerciseDao.delete(id)

    suspend fun createExercise(exercise: Exercise): Long = exerciseDao.insert(exercise)
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)
}
