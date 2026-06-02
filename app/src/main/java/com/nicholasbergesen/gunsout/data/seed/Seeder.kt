package com.nicholasbergesen.gunsout.data.seed

import androidx.room.withTransaction
import com.nicholasbergesen.gunsout.data.dao.ExerciseAlternateDao
import com.nicholasbergesen.gunsout.data.dao.ExerciseDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDayDao
import com.nicholasbergesen.gunsout.data.dao.ProgramExerciseDao
import com.nicholasbergesen.gunsout.data.dao.SupplementDao
import com.nicholasbergesen.gunsout.data.entity.ExerciseAlternate
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.ProgramType
import com.nicholasbergesen.gunsout.data.entity.Supplement
import com.nicholasbergesen.gunsout.data.entity.SupplementUnit
import com.nicholasbergesen.gunsout.data.entity.defaultMovementPatternFor
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the database with default programs, exercises, alternates and supplements for a single
 * userId. All inserts stamp [userId] and all `getBySeedKey` lookups are scoped to [userId] so that
 * each user receives their own copy of the catalog (rubber-duck issue #3).
 *
 * The seeding sequence is wrapped in a single Room transaction so that a partial failure rolls
 * back atomically. Without this, a retry from [com.nicholasbergesen.gunsout.feature.auth.AuthGate]'s error UI can
 * hit a half-seeded program where the parent row exists but children are missing, and
 * [seedProgram] would skip inserting the children because it short-circuits on parent presence.
 *
 * Per-user [UserPreferences] (Phase 3) makes the `firstRunDone` gate a real per-user flag —
 * each Google account that signs in gets a fresh DataStore file, defaults `firstRunDone = false`,
 * and goes through its own first-run program activation.
 */
@Singleton
class Seeder @Inject constructor(
    private val db: com.nicholasbergesen.gunsout.data.db.GunsoutDatabase,
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programExerciseDao: ProgramExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val alternateDao: ExerciseAlternateDao,
    private val supplementDao: SupplementDao,
    private val userPrefs: UserPreferences,
    private val reminderScheduler: com.nicholasbergesen.gunsout.feature.supplements.SupplementReminderScheduler
) {

    suspend fun seedIfNeeded(userId: String) {
        val firstRun = !userPrefs.profile(userId).first().firstRunDone
        db.withTransaction {
            seedExercises(userId)
            seedAlternates(userId)
            seedProgram(userId, activateOnFirstRun = firstRun)
            seedSupplements(userId)
        }
        // Re-arm any supplement reminders saved in the DB (e.g. after install on a new device or
        // after a backup-import). Boot is handled separately by SupplementBootReceiver.
        rearmReminders(userId)
        if (firstRun) {
            userPrefs.update(userId) { it.copy(firstRunDone = true) }
        }
    }

    private suspend fun rearmReminders(userId: String) {
        supplementDao.allActiveOnce(userId).forEach { reminderScheduler.reschedule(it) }
        reminderScheduler.ensureChannel()
    }

    private suspend fun seedExercises(userId: String) {
        for (seed in ExerciseSeeds.all) {
            val seedKey = seed.exercise.seedKey!!
            val existing = exerciseDao.getBySeedKey(userId, seedKey)
            if (existing == null) {
                exerciseDao.insert(seed.exercise.copy(userId = userId))
            } else if (
                existing.primaryMuscleGroup == seed.exercise.primaryMuscleGroup &&
                existing.equipment == seed.exercise.equipment &&
                existing.movementPattern == defaultMovementPatternFor(existing.primaryMuscleGroup) &&
                existing.movementPattern != seed.exercise.movementPattern
            ) {
                exerciseDao.update(existing.copy(movementPattern = seed.exercise.movementPattern))
            }
        }
    }

    private suspend fun seedAlternates(userId: String) {
        for (seed in ExerciseSeeds.all) {
            val parent = exerciseDao.getBySeedKey(userId, seed.exercise.seedKey!!) ?: continue
            for ((altKey, reason) in seed.alternates) {
                val alt = exerciseDao.getBySeedKey(userId, altKey) ?: continue
                alternateDao.insert(ExerciseAlternate(
                    userId = userId,
                    exerciseId = parent.id,
                    alternateExerciseId = alt.id,
                    reason = reason
                ))
            }
        }
    }

    private suspend fun seedProgram(userId: String, activateOnFirstRun: Boolean) {
        val planProgram = ProgramSeeds.upperLower4Day
        var program = programDao.getBySeedKey(userId, planProgram.seedKey)
        if (program == null) {
            val newId = programDao.insert(Program(
                userId = userId,
                name = planProgram.name,
                type = ProgramType.UPPER_LOWER,
                isActive = activateOnFirstRun,
                isTemplate = true,
                seedKey = planProgram.seedKey
            ))
            program = programDao.getById(newId)
            // Days + exercises
            for (planDay in planProgram.days) {
                val dayId = programDayDao.insert(ProgramDay(
                    userId = userId,
                    programId = newId,
                    orderIndex = planDay.orderIndex,
                    label = planDay.label,
                    preferredDayOfWeek = planDay.preferredDayOfWeek,
                    isRest = planDay.isRest
                ))
                for ((i, pe) in planDay.exercises.withIndex()) {
                    val ex = exerciseDao.getBySeedKey(userId, pe.exerciseSeedKey) ?: continue
                    programExerciseDao.insert(ProgramExercise(
                        userId = userId,
                        programDayId = dayId,
                        orderIndex = i,
                        exerciseId = ex.id,
                        sets = pe.sets,
                        repsMin = pe.repsMin,
                        repsMax = pe.repsMax,
                        restSec = pe.restSec,
                        rpeTarget = pe.rpeTarget,
                        supersetGroupId = pe.supersetGroupId,
                        protocol = pe.protocol
                    ))
                }
            }
        }
    }

    private suspend fun seedSupplements(userId: String) {
        val key = "creatine_mono"
        if (supplementDao.getBySeedKey(userId, key) == null) {
            supplementDao.insert(Supplement(
                userId = userId,
                name = "Creatine Monohydrate",
                defaultDose = 5.0,
                unit = SupplementUnit.G,
                notes = "Daily dosing. Loading phase optional. Take with water or your smoothie.",
                takeWith = "with water or smoothie",
                reminderTime = null,
                isActive = true,
                isUserCreated = false,
                seedKey = key
            ))
        }
    }
}
