package com.gunsout.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "meal_plan")
data class MealPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcalTarget: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val notes: String? = null,
    val isActive: Boolean = false,
    val isTemplate: Boolean = false,
    val seedKey: String? = null
)

@Entity(
    tableName = "meal_template",
    foreignKeys = [ForeignKey(
        entity = MealPlan::class,
        parentColumns = ["id"],
        childColumns = ["mealPlanId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("mealPlanId")]
)
data class MealTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealPlanId: Long?,
    val name: String,
    val mealType: MealType,
    val macroSource: MacroSource = MacroSource.MANUAL,
    val kcal: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val notes: String? = null,
    val seedKey: String? = null
)

@Entity(tableName = "ingredient")
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val defaultUnit: IngredientUnit = IngredientUnit.G,
    val gramsPerUnit: Double = 1.0,
    val isUserCreated: Boolean = false,
    val isArchived: Boolean = false,
    val seedKey: String? = null
)

@Entity(
    tableName = "meal_template_ingredient",
    foreignKeys = [
        ForeignKey(
            entity = MealTemplate::class,
            parentColumns = ["id"],
            childColumns = ["mealTemplateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("mealTemplateId"), Index("ingredientId")]
)
data class MealTemplateIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealTemplateId: Long,
    val ingredientId: Long,
    val quantity: Double,
    val unit: IngredientUnit,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "food_entry",
    indices = [Index("date")]
)
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
