package com.gunsout.data.repo

import com.gunsout.data.dao.FoodEntryDao
import com.gunsout.data.dao.IngredientDao
import com.gunsout.data.dao.MealPlanDao
import com.gunsout.data.dao.MealTemplateDao
import com.gunsout.data.dao.MealTemplateIngredientDao
import com.gunsout.data.entity.FoodEntry
import com.gunsout.data.entity.Ingredient
import com.gunsout.data.entity.MealPlan
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.MealType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DietRepository @Inject constructor(
    private val mealPlanDao: MealPlanDao,
    private val mealTemplateDao: MealTemplateDao,
    private val ingredientDao: IngredientDao,
    private val mealTemplateIngredientDao: MealTemplateIngredientDao,
    private val foodEntryDao: FoodEntryDao
) {
    fun observeActivePlan(): Flow<MealPlan?> = mealPlanDao.observeActive()
    suspend fun getActivePlan(): MealPlan? = mealPlanDao.getActive()

    fun observeTemplatesForPlan(planId: Long): Flow<List<MealTemplate>> =
        mealTemplateDao.observeForPlan(planId)

    fun observeIngredients(): Flow<List<Ingredient>> = ingredientDao.observeAll()

    fun observeEntriesForDate(date: LocalDate): Flow<List<FoodEntry>> =
        foodEntryDao.observeForDate(date)

    fun observeEntriesRange(start: LocalDate, end: LocalDate): Flow<List<FoodEntry>> =
        foodEntryDao.observeRange(start, end)

    suspend fun logFromTemplate(
        template: MealTemplate,
        date: LocalDate = LocalDate.now(),
        multiplier: Double = 1.0
    ): Long {
        val m = multiplier.coerceAtLeast(0.0)
        val displayName = if (m == 1.0) template.name else "${template.name} (${formatMul(m)}x)"
        return foodEntryDao.insert(FoodEntry(
            date = date,
            mealType = template.mealType,
            name = displayName,
            kcal = (template.kcal * m).toInt(),
            proteinG = template.proteinG * m,
            carbsG = template.carbsG * m,
            fatG = template.fatG * m,
            sourceTemplateId = template.id
        ))
    }

    private fun formatMul(m: Double): String =
        if (m == m.toInt().toDouble()) m.toInt().toString() else "%.1f".format(m).trimEnd('0').trimEnd('.')

    suspend fun logCustomFood(
        date: LocalDate,
        mealType: MealType,
        name: String,
        kcal: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double
    ): Long = foodEntryDao.insert(FoodEntry(
        date = date,
        mealType = mealType,
        name = name,
        kcal = kcal,
        proteinG = proteinG,
        carbsG = carbsG,
        fatG = fatG
    ))

    suspend fun updateEntry(entry: FoodEntry) = foodEntryDao.update(entry)

    suspend fun deleteEntry(id: Long) = foodEntryDao.delete(id)
}
