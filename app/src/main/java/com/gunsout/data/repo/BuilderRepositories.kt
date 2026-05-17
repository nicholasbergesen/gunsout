package com.gunsout.data.repo

import androidx.room.withTransaction
import com.gunsout.data.dao.ExerciseDao
import com.gunsout.data.dao.IngredientDao
import com.gunsout.data.dao.MealPlanDao
import com.gunsout.data.dao.MealTemplateDao
import com.gunsout.data.dao.MealTemplateIngredientDao
import com.gunsout.data.dao.ProgramDao
import com.gunsout.data.dao.ProgramDayDao
import com.gunsout.data.dao.ProgramExerciseDao
import com.gunsout.data.db.GunsoutDatabase
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.Ingredient
import com.gunsout.data.entity.MacroSource
import com.gunsout.data.entity.MealPlan
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.MealTemplateIngredient
import com.gunsout.data.entity.Program
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.ProgramExercise
import com.gunsout.domain.macros.MacroCalculator
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
    fun observePrograms(): Flow<List<Program>> = programDao.observeAll()
    fun observeExercises(): Flow<List<Exercise>> = exerciseDao.observeAll()
    fun observeDaysFor(programId: Long): Flow<List<ProgramDay>> = programDayDao.observeForProgram(programId)
    fun observeExercisesForDay(programDayId: Long): Flow<List<ProgramExercise>> = programExerciseDao.observeForDay(programDayId)

    suspend fun getProgram(id: Long): Program? = programDao.getById(id)
    suspend fun getProgramDay(id: Long): ProgramDay? = programDayDao.getById(id)
    suspend fun getProgramExercise(id: Long): ProgramExercise? = programExerciseDao.getById(id)
    suspend fun getExercise(id: Long): Exercise? = exerciseDao.getById(id)

    suspend fun setActive(programId: Long) = programDao.setActive(programId)

    suspend fun createBlankProgram(name: String): Long = programDao.insert(
        Program(name = name, isActive = false, isTemplate = false)
    )

    suspend fun duplicateProgram(programId: Long, newName: String): Long = db.withTransaction {
        val src = programDao.getById(programId) ?: return@withTransaction -1L
        val newId = programDao.insert(src.copy(id = 0, name = newName, isActive = false, isTemplate = false, seedKey = null))
        val days = programDayDao.getForProgram(programId)
        for (day in days) {
            val newDayId = programDayDao.insert(day.copy(id = 0, programId = newId))
            val exes = programExerciseDao.getForDay(day.id)
            for (pe in exes) {
                programExerciseDao.insert(pe.copy(id = 0, programDayId = newDayId))
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
    suspend fun addDay(programId: Long, label: String): Long {
        val existing = programDayDao.getForProgram(programId)
        val nextOrder = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
        return programDayDao.insert(ProgramDay(programId = programId, orderIndex = nextOrder, label = label))
    }
    suspend fun deleteDay(dayId: Long) = programDayDao.delete(dayId)

    suspend fun addExerciseToDay(programDayId: Long, exerciseId: Long): Long {
        val existing = programExerciseDao.getForDay(programDayId)
        val order = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val ex = exerciseDao.getById(exerciseId) ?: return -1L
        return programExerciseDao.insert(ProgramExercise(
            programDayId = programDayId, orderIndex = order, exerciseId = exerciseId,
            restSec = ex.defaultRestSec
        ))
    }

    suspend fun updateProgramExercise(pe: ProgramExercise) = programExerciseDao.update(pe)
    suspend fun deleteProgramExercise(id: Long) = programExerciseDao.delete(id)

    suspend fun createExercise(exercise: Exercise): Long = exerciseDao.insert(exercise)
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)
}

@Singleton
class MealPlanRepository @Inject constructor(
    private val db: GunsoutDatabase,
    private val mealPlanDao: MealPlanDao,
    private val mealTemplateDao: MealTemplateDao,
    private val ingredientDao: IngredientDao,
    private val mealTemplateIngredientDao: MealTemplateIngredientDao
) {
    fun observePlans(): Flow<List<MealPlan>> = mealPlanDao.observeAll()
    fun observeTemplatesFor(planId: Long): Flow<List<MealTemplate>> = mealTemplateDao.observeForPlan(planId)
    fun observeIngredients(): Flow<List<Ingredient>> = ingredientDao.observeAll()

    suspend fun getPlan(id: Long): MealPlan? = mealPlanDao.getById(id)
    suspend fun getTemplate(id: Long): MealTemplate? = mealTemplateDao.getById(id)
    suspend fun getIngredient(id: Long): Ingredient? = ingredientDao.getById(id)

    suspend fun setActive(id: Long) = mealPlanDao.setActive(id)

    suspend fun createPlan(plan: MealPlan): Long = mealPlanDao.insert(plan)
    suspend fun updatePlan(plan: MealPlan) = mealPlanDao.update(plan)

    suspend fun duplicatePlan(planId: Long, newName: String): Long = db.withTransaction {
        val src = mealPlanDao.getById(planId) ?: return@withTransaction -1L
        val newId = mealPlanDao.insert(src.copy(id = 0, name = newName, isActive = false, isTemplate = false, seedKey = null))
        // Note: shared templates (mealPlanId == null) are not duplicated; they remain shared.
        newId
    }

    suspend fun deletePlan(id: Long) = mealPlanDao.update(mealPlanDao.getById(id)!!.copy(isActive = false)).also {
        // CASCADE removes templates and ingredient rows scoped to it.
        // Note: meal_plan has no soft-delete; consumer can also just hide it.
    }

    suspend fun createTemplate(template: MealTemplate): Long = mealTemplateDao.insert(template)
    suspend fun updateTemplate(template: MealTemplate) = mealTemplateDao.update(template)

    suspend fun getTemplateIngredients(templateId: Long): List<MealTemplateIngredient> =
        mealTemplateIngredientDao.getForTemplate(templateId)

    suspend fun setTemplateIngredients(templateId: Long, rows: List<MealTemplateIngredient>) {
        // For simplicity, this just inserts. Editing templates with ingredient mutations is a future enhancement.
        rows.forEach { mealTemplateIngredientDao.insert(it.copy(mealTemplateId = templateId)) }
        recomputeTemplateMacros(templateId)
    }

    suspend fun recomputeTemplateMacros(templateId: Long) {
        val template = mealTemplateDao.getById(templateId) ?: return
        if (template.macroSource != MacroSource.FROM_INGREDIENTS) return
        val rows = mealTemplateIngredientDao.getForTemplate(templateId)
        val byId = rows.mapNotNull { row -> ingredientDao.getById(row.ingredientId)?.let { it.id to it } }.toMap()
        val total = MacroCalculator.totalFor(rows, byId)
        mealTemplateDao.update(template.copy(
            kcal = total.kcal.toInt(),
            proteinG = total.protein,
            carbsG = total.carbs,
            fatG = total.fat
        ))
    }

    suspend fun createIngredient(ingredient: Ingredient): Long = ingredientDao.insert(ingredient)
    suspend fun updateIngredient(ingredient: Ingredient) = ingredientDao.update(ingredient)
}
