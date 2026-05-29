package com.nicholasbergesen.gunsout.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "meal_template",
    indices = [Index("userId")]
)
data class MealTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val mealType: MealType,
    val kcal: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val notes: String? = null,
    val seedKey: String? = null
)

@Entity(
    tableName = "food_entry",
    indices = [Index("date"), Index("userId")]
)
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: LocalDate,
    val mealType: MealType,
    val name: String,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val sourceTemplateId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
