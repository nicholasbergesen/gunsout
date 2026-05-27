package com.gunsout.data.seed

import com.gunsout.data.dao.ExerciseAlternateDao
import com.gunsout.data.dao.ExerciseDao
import com.gunsout.data.dao.ProgramDao
import com.gunsout.data.dao.ProgramDayDao
import com.gunsout.data.dao.ProgramExerciseDao
import com.gunsout.data.dao.SupplementDao
import com.gunsout.data.entity.ExerciseAlternate
import com.gunsout.data.entity.Program
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.ProgramExercise
import com.gunsout.data.entity.ProgramType
import com.gunsout.data.entity.Supplement
import com.gunsout.data.entity.SupplementUnit
import com.gunsout.data.prefs.UserPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the database with default programs, exercises, alternates and supplements for a single
 * userId. All inserts stamp [userId] and all `getBySeedKey` lookups are scoped to [userId] so that
 * each user receives their own copy of the catalog (rubber-duck issue #3).
 *
 * Phase 2b-2 placeholder: [UserPreferences] is still a single global store, so the `firstRunDone`
 * gate is effectively single-user. Phase 3 will make `UserPreferences` per-user; at that point
 * `firstRunDone` becomes a true per-user flag.
 */
@Singleton
class Seeder @Inject constructor(
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programExerciseDao: ProgramExerciseDao,
    private val exerciseDao: ExerciseDao,
    private val alternateDao: ExerciseAlternateDao,
    private val supplementDao: SupplementDao,
    private val userPrefs: UserPreferences,
    private val reminderScheduler: com.gunsout.feature.supplements.SupplementReminderScheduler
) {

    suspend fun seedIfNeeded(userId: String) {
        seedExercises(userId)
        seedAlternates(userId)
        val firstRun = !userPrefs.profile.first().firstRunDone
        seedProgram(userId, activateOnFirstRun = firstRun)
        seedSupplements(userId)
        // Re-arm any supplement reminders saved in the DB (e.g. after install on a new device or
        // after a backup-import). Boot is handled separately by SupplementBootReceiver.
        rearmReminders(userId)
        if (firstRun) {
            userPrefs.update { it.copy(firstRunDone = true) }
        }
    }

    private suspend fun rearmReminders(userId: String) {
        supplementDao.allActiveOnce(userId).forEach { reminderScheduler.reschedule(it) }
        reminderScheduler.ensureChannel()
    }

    private suspend fun seedExercises(userId: String) {
        for (seed in ExerciseSeeds.all) {
            val seedKey = seed.exercise.seedKey!!
            if (exerciseDao.getBySeedKey(userId, seedKey) == null) {
                exerciseDao.insert(seed.exercise.copy(userId = userId))
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
