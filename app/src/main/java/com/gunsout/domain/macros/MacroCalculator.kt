package com.gunsout.domain.macros

import com.gunsout.data.entity.Ingredient
import com.gunsout.data.entity.IngredientUnit
import com.gunsout.data.entity.MealTemplateIngredient

/** Per-row macros. */
data class Macros(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double) {
    operator fun plus(other: Macros) =
        Macros(kcal + other.kcal, protein + other.protein, carbs + other.carbs, fat + other.fat)
    companion object { val ZERO = Macros(0.0, 0.0, 0.0, 0.0) }
}

object MacroCalculator {

    /**
     * Returns grams of [ingredient] represented by [quantity] in [unit].
     * If unit is G, returns quantity directly. Otherwise multiplies quantity by gramsPerUnit.
     */
    fun gramsFor(ingredient: Ingredient, quantity: Double, unit: IngredientUnit): Double {
        return when (unit) {
            IngredientUnit.G -> quantity
            else -> quantity * ingredient.gramsPerUnit
        }
    }

    /** Macros contributed by [quantity] [unit] of [ingredient]. */
    fun macrosFor(ingredient: Ingredient, quantity: Double, unit: IngredientUnit): Macros {
        val grams = gramsFor(ingredient, quantity, unit)
        val factor = grams / 100.0
        return Macros(
            kcal = ingredient.kcalPer100g * factor,
            protein = ingredient.proteinPer100g * factor,
            carbs = ingredient.carbsPer100g * factor,
            fat = ingredient.fatPer100g * factor
        )
    }

    /** Sum macros across template ingredient rows, given the ingredients map. */
    fun totalFor(
        rows: List<MealTemplateIngredient>,
        ingredientsById: Map<Long, Ingredient>
    ): Macros = rows.fold(Macros.ZERO) { acc, row ->
        val ingredient = ingredientsById[row.ingredientId] ?: return@fold acc
        acc + macrosFor(ingredient, row.quantity, row.unit)
    }
}
