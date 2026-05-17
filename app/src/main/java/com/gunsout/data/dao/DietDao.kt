package com.gunsout.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gunsout.data.entity.FoodEntry
import com.gunsout.data.entity.Ingredient
import com.gunsout.data.entity.MealPlan
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.MealTemplateIngredient
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plan ORDER BY isActive DESC, name ASC")
    fun observeAll(): Flow<List<MealPlan>>

    @Query("SELECT * FROM meal_plan WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<MealPlan?>

    @Query("SELECT * FROM meal_plan WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): MealPlan?

    @Query("SELECT * FROM meal_plan WHERE seedKey = :seedKey LIMIT 1")
    suspend fun getBySeedKey(seedKey: String): MealPlan?

    @Query("SELECT * FROM meal_plan WHERE id = :id")
    suspend fun getById(id: Long): MealPlan?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(plan: MealPlan): Long

    @Update
    suspend fun update(plan: MealPlan)

    @Query("UPDATE meal_plan SET isActive = 0")
    suspend fun clearActive()

    @Transaction
    suspend fun setActive(id: Long) {
        clearActive()
        val existing = getById(id) ?: return
        update(existing.copy(isActive = true))
    }
}

@Dao
interface MealTemplateDao {
    @Query("""
        SELECT * FROM meal_template
        WHERE mealPlanId = :planId OR mealPlanId IS NULL
        ORDER BY mealType, name
    """)
    fun observeForPlan(planId: Long): Flow<List<MealTemplate>>

    @Query("SELECT * FROM meal_template WHERE id = :id")
    suspend fun getById(id: Long): MealTemplate?

    @Query("SELECT * FROM meal_template WHERE seedKey = :seedKey LIMIT 1")
    suspend fun getBySeedKey(seedKey: String): MealTemplate?

    @Insert
    suspend fun insert(template: MealTemplate): Long

    @Update
    suspend fun update(template: MealTemplate)
}

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredient WHERE isArchived = 0 ORDER BY name")
    fun observeAll(): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredient WHERE id = :id")
    suspend fun getById(id: Long): Ingredient?

    @Query("SELECT * FROM ingredient WHERE seedKey = :seedKey LIMIT 1")
    suspend fun getBySeedKey(seedKey: String): Ingredient?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ingredient: Ingredient): Long

    @Update
    suspend fun update(ingredient: Ingredient)
}

@Dao
interface MealTemplateIngredientDao {
    @Query("SELECT * FROM meal_template_ingredient WHERE mealTemplateId = :templateId ORDER BY orderIndex")
    suspend fun getForTemplate(templateId: Long): List<MealTemplateIngredient>

    @Insert
    suspend fun insert(mti: MealTemplateIngredient): Long
}

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY createdAt ASC")
    fun observeForDate(date: LocalDate): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entry WHERE date BETWEEN :start AND :end ORDER BY date ASC, createdAt ASC")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<FoodEntry>>

    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Update
    suspend fun update(entry: FoodEntry)

    @Query("DELETE FROM food_entry WHERE id = :id")
    suspend fun delete(id: Long)
}
