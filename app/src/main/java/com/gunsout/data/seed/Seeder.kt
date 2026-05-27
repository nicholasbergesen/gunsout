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

    suspend fun seedIfNeeded() {
        seedExercises()
        seedAlternates()
        val firstRun = !userPrefs.profile.first().firstRunDone
        seedProgram(activateOnFirstRun = firstRun)
        seedSupplements()
        // Re-arm any supplement reminders saved in the DB (e.g. after install on a new device or
        // after a backup-import). Boot is handled separately by SupplementBootReceiver.
        rearmReminders()
        if (firstRun) {
            userPrefs.update { it.copy(firstRunDone = true) }
        }
    }

    private suspend fun rearmReminders() {
        supplementDao.allActiveOnce().forEach { reminderScheduler.reschedule(it) }
        reminderScheduler.ensureChannel()
    }

    private suspend fun seedExercises() {
        for (seed in ExerciseSeeds.all) {
            if (exerciseDao.getBySeedKey(seed.exercise.seedKey!!) == null) {
                exerciseDao.insert(seed.exercise)
            }
        }
    }

    private suspend fun seedAlternates() {
        for (seed in ExerciseSeeds.all) {
            val parent = exerciseDao.getBySeedKey(seed.exercise.seedKey!!) ?: continue
            for ((altKey, reason) in seed.alternates) {
                val alt = exerciseDao.getBySeedKey(altKey) ?: continue
                alternateDao.insert(ExerciseAlternate(parent.id, alt.id, reason))
            }
        }
    }

    private suspend fun seedProgram(activateOnFirstRun: Boolean) {
        val planProgram = ProgramSeeds.upperLower4Day
        var program = programDao.getBySeedKey(planProgram.seedKey)
        if (program == null) {
            val newId = programDao.insert(Program(
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
                    programId = newId,
                    orderIndex = planDay.orderIndex,
                    label = planDay.label,
                    preferredDayOfWeek = planDay.preferredDayOfWeek,
                    isRest = planDay.isRest
                ))
                for ((i, pe) in planDay.exercises.withIndex()) {
                    val ex = exerciseDao.getBySeedKey(pe.exerciseSeedKey) ?: continue
                    programExerciseDao.insert(ProgramExercise(
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

    private suspend fun seedSupplements() {
        val key = "creatine_mono"
        if (supplementDao.getBySeedKey(key) == null) {
            supplementDao.insert(Supplement(
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
