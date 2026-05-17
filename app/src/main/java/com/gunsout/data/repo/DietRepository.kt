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

    suspend fun logFromTemplate(template: MealTemplate, date: LocalDate = LocalDate.now()): Long =
        foodEntryDao.insert(FoodEntry(
            date = date,
            mealType = template.mealType,
            name = template.name,
            kcal = template.kcal,
            proteinG = template.proteinG,
            carbsG = template.carbsG,
            fatG = template.fatG,
            sourceTemplateId = template.id
        ))

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

    suspend fun deleteEntry(id: Long) = foodEntryDao.delete(id)
}
