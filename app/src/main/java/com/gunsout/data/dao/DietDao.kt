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
    @Query("SELECT * FROM meal_template ORDER BY mealType, name")
    fun observeAll(): Flow<List<MealTemplate>>

    @Query("SELECT * FROM meal_template ORDER BY id")
    suspend fun getAll(): List<MealTemplate>

    @Query("SELECT * FROM meal_template WHERE id = :id")
    suspend fun getById(id: Long): MealTemplate?

    @Query("SELECT * FROM meal_template WHERE seedKey = :seedKey LIMIT 1")
    suspend fun getBySeedKey(seedKey: String): MealTemplate?

    @Insert
    suspend fun insert(template: MealTemplate): Long

    @Update
    suspend fun update(template: MealTemplate)

    @Query("DELETE FROM meal_template WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY createdAt ASC")
    fun observeForDate(date: LocalDate): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entry WHERE date BETWEEN :start AND :end ORDER BY date ASC, createdAt ASC")
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entry ORDER BY id")
    suspend fun getAll(): List<FoodEntry>

    @Insert
    suspend fun insert(entry: FoodEntry): Long

    @Update
    suspend fun update(entry: FoodEntry)

    @Query("DELETE FROM food_entry WHERE id = :id")
    suspend fun delete(id: Long)
}
