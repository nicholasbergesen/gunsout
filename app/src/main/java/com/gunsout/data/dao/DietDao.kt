package com.gunsout.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gunsout.data.entity.FoodEntry
import com.gunsout.data.entity.MealTemplate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MealTemplateDao {
    @Query("SELECT * FROM meal_template WHERE userId = :userId ORDER BY mealType, name")
    fun observeAll(userId: String): Flow<List<MealTemplate>>

    @Query("SELECT * FROM meal_template WHERE userId = :userId ORDER BY id")
    suspend fun getAll(userId: String): List<MealTemplate>

    @Query("SELECT * FROM meal_template WHERE id = :id")
    suspend fun getById(id: Long): MealTemplate?

    @Query("SELECT * FROM meal_template WHERE userId = :userId AND seedKey = :seedKey LIMIT 1")
    suspend fun getBySeedKey(userId: String, seedKey: String): MealTemplate?

    @Insert
    suspend fun insert(template: MealTemplate): Long

    @Update
    suspend fun update(template: MealTemplate)

    @Query("DELETE FROM meal_template WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entry WHERE userId = :userId AND date = :date ORDER BY createdAt ASC")
    fun observeForDate(userId: String, date: LocalDate): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entry WHERE userId = :userId AND date BETWEEN :start AND :end ORDER BY date ASC, createdAt ASC")
    fun observeRange(userId: String, start: LocalDate, end: LocalDate): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entry WHERE userId = :userId ORDER BY id")
    suspend fun getAll(userId: String): List<FoodEntry>

    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Update
    suspend fun update(entry: FoodEntry)

    @Query("DELETE FROM food_entry WHERE id = :id")
    suspend fun delete(id: Long)
}
