package com.gunsout.data.seed

import com.gunsout.data.dao.ExerciseAlternateDao
import com.gunsout.data.dao.ExerciseDao
import com.gunsout.data.dao.IngredientDao
import com.gunsout.data.dao.MealPlanDao
import com.gunsout.data.dao.MealTemplateDao
import com.gunsout.data.dao.ProgramDao
import com.gunsout.data.dao.ProgramDayDao
import com.gunsout.data.dao.ProgramExerciseDao
import com.gunsout.data.dao.SupplementDao
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.ExerciseAlternate
import com.gunsout.data.entity.MacroSource
import com.gunsout.data.entity.MealPlan
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.MealType
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
    private val mealPlanDao: MealPlanDao,
    private val mealTemplateDao: MealTemplateDao,
    private val ingredientDao: IngredientDao,
    private val supplementDao: SupplementDao,
    private val userPrefs: UserPreferences,
    private val reminderScheduler: com.gunsout.feature.supplements.SupplementReminderScheduler
) {

    suspend fun seedIfNeeded() {
        seedExercises()
        seedAlternates()
        seedIngredients()
        val firstRun = !userPrefs.profile.first().firstRunDone
        seedProgram(activateOnFirstRun = firstRun)
        seedMealPlan(activateOnFirstRun = firstRun)
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

    private suspend fun seedIngredients() {
        for (seed in IngredientSeeds.all) {
            if (ingredientDao.getBySeedKey(seed.ingredient.seedKey!!) == null) {
                ingredientDao.insert(seed.ingredient)
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

    private suspend fun seedMealPlan(activateOnFirstRun: Boolean) {
        val planSeedKey = "cut_2200"
        var plan = mealPlanDao.getBySeedKey(planSeedKey)
        if (plan == null) {
            val id = mealPlanDao.insert(MealPlan(
                name = "Cut at 2,200 kcal",
                kcalTarget = 2200,
                proteinG = 160,
                carbsG = 220,
                fatG = 70,
                notes = "Body recomposition cut. Adjust as your weigh-ins move.",
                isActive = activateOnFirstRun,
                isTemplate = true,
                seedKey = planSeedKey
            ))
            plan = mealPlanDao.getById(id)
        }
        val templateKey = "high_protein_plant_smoothie"
        if (mealTemplateDao.getBySeedKey(templateKey) == null) {
            mealTemplateDao.insert(MealTemplate(
                mealPlanId = plan?.id,
                name = "High-Protein Plant Smoothie",
                mealType = MealType.SMOOTHIE,
                macroSource = MacroSource.MANUAL,
                kcal = 350,
                proteinG = 35.0,
                carbsG = 35.0,
                fatG = 8.0,
                notes = "Placeholder macros: tap to edit with your real recipe.",
                seedKey = templateKey
            ))
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
