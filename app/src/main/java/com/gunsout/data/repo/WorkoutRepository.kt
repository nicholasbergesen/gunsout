package com.gunsout.data.repo

import com.gunsout.data.dao.ExerciseDao
import com.gunsout.data.dao.ProgramDao
import com.gunsout.data.dao.ProgramDayDao
import com.gunsout.data.dao.ProgramExerciseDao
import com.gunsout.data.dao.SetEntryDao
import com.gunsout.data.dao.WorkoutSessionDao
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.Program
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.ProgramExercise
import com.gunsout.data.entity.SessionStatus
import com.gunsout.data.entity.SetEntry
import com.gunsout.data.entity.WorkoutSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programExerciseDao: ProgramExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val setEntryDao: SetEntryDao
) {
    fun observeActiveProgram(): Flow<Program?> = programDao.observeActive()

    suspend fun getActiveProgramDays(): List<ProgramDay> {
        val active = programDao.getActive() ?: return emptyList()
        return programDayDao.getForProgram(active.id)
    }

    suspend fun getProgramDay(id: Long): ProgramDay? = programDayDao.getById(id)

    suspend fun getProgramExercises(programDayId: Long): List<ProgramExercise> =
        programExerciseDao.getForDay(programDayId)

    suspend fun getExercise(id: Long): Exercise? = exerciseDao.getById(id)

    fun observeAllExercises(): Flow<List<Exercise>> = exerciseDao.observeAll()

    suspend fun getRecentSessions(): List<WorkoutSession> =
        workoutSessionDao.getSince(LocalDate.now().minusDays(60))

    suspend fun getLastCompletedSession(): WorkoutSession? = workoutSessionDao.getLastCompleted()

    fun observeRecentCompleted(limit: Int = 10): Flow<List<WorkoutSession>> =
        workoutSessionDao.observeRecentCompleted(limit)

    suspend fun getInProgressSession(): WorkoutSession? = workoutSessionDao.getInProgress()

    suspend fun startSession(programDay: ProgramDay): Long {
        val session = WorkoutSession(
            date = LocalDate.now(),
            programDayId = programDay.id,
            programDayLabelSnapshot = programDay.label,
            status = SessionStatus.IN_PROGRESS
        )
        return workoutSessionDao.insert(session)
    }

    suspend fun getSetsForSession(sessionId: Long): List<SetEntry> =
        setEntryDao.getForSession(sessionId)

    fun observeSetsForSession(sessionId: Long): Flow<List<SetEntry>> =
        setEntryDao.observeForSession(sessionId)

    suspend fun getPreviousSetsForExercise(exerciseId: Long): List<SetEntry> =
        setEntryDao.getRecentForExercise(exerciseId)

    suspend fun logSet(set: SetEntry): Long = setEntryDao.insert(set)

    suspend fun updateSet(set: SetEntry) = setEntryDao.update(set)

    suspend fun completeSession(sessionId: Long, kneeFeel: Int?, notes: String?) {
        val s = workoutSessionDao.getById(sessionId) ?: return
        workoutSessionDao.update(
            s.copy(status = SessionStatus.COMPLETED, completedAt = LocalDateTime.now(), kneeFeel = kneeFeel, notes = notes)
        )
    }

    suspend fun markRestDay() {
        val active = programDao.getActive() ?: return
        val days = programDayDao.getForProgram(active.id)
        val rest = days.firstOrNull { it.isRest } ?: return
        workoutSessionDao.insert(
            WorkoutSession(
                date = LocalDate.now(),
                programDayId = rest.id,
                programDayLabelSnapshot = rest.label,
                status = SessionStatus.COMPLETED,
                startedAt = LocalDateTime.now(),
                completedAt = LocalDateTime.now()
            )
        )
    }

    suspend fun skipNextDay(nextProgramDay: ProgramDay) {
        workoutSessionDao.insert(
            WorkoutSession(
                date = LocalDate.now(),
                programDayId = nextProgramDay.id,
                programDayLabelSnapshot = nextProgramDay.label,
                status = SessionStatus.SKIPPED,
                startedAt = LocalDateTime.now(),
                completedAt = LocalDateTime.now()
            )
        )
    }
}
