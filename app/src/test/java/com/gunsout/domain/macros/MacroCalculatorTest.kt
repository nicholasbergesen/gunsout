package com.gunsout.domain.macros

import com.gunsout.data.entity.Ingredient
import com.gunsout.data.entity.IngredientUnit
import com.gunsout.data.entity.MealTemplateIngredient
import org.junit.Assert.assertEquals
import org.junit.Test

class MacroCalculatorTest {

    private fun banana() = Ingredient(
        id = 1, name = "Banana",
        kcalPer100g = 89.0, proteinPer100g = 1.1, carbsPer100g = 23.0, fatPer100g = 0.3,
        defaultUnit = IngredientUnit.PIECE, gramsPerUnit = 118.0
    )

    private fun protein() = Ingredient(
        id = 2, name = "Plant Protein",
        kcalPer100g = 380.0, proteinPer100g = 75.0, carbsPer100g = 5.0, fatPer100g = 4.0,
        defaultUnit = IngredientUnit.G, gramsPerUnit = 1.0
    )

    private fun milk() = Ingredient(
        id = 3, name = "Almond Milk",
        kcalPer100g = 13.0, proteinPer100g = 0.4, carbsPer100g = 0.3, fatPer100g = 1.1,
        defaultUnit = IngredientUnit.ML, gramsPerUnit = 1.0
    )

    @Test fun `grams unit gives quantity as grams`() {
        assertEquals(50.0, MacroCalculator.gramsFor(protein(), 50.0, IngredientUnit.G), 0.001)
    }

    @Test fun `piece unit multiplies by gramsPerUnit`() {
        assertEquals(118.0, MacroCalculator.gramsFor(banana(), 1.0, IngredientUnit.PIECE), 0.001)
        assertEquals(236.0, MacroCalculator.gramsFor(banana(), 2.0, IngredientUnit.PIECE), 0.001)
    }

    @Test fun `ml unit treated as gramsPerUnit one for almond milk`() {
        assertEquals(200.0, MacroCalculator.gramsFor(milk(), 200.0, IngredientUnit.ML), 0.001)
    }

    @Test fun `macros for 30g of protein powder`() {
        val m = MacroCalculator.macrosFor(protein(), 30.0, IngredientUnit.G)
        assertEquals(114.0, m.kcal, 0.001) // 380 * 0.3
        assertEquals(22.5, m.protein, 0.001) // 75 * 0.3
    }

    @Test fun `macros for one banana`() {
        val m = MacroCalculator.macrosFor(banana(), 1.0, IngredientUnit.PIECE)
        assertEquals(89.0 * 1.18, m.kcal, 0.001)
        assertEquals(1.1 * 1.18, m.protein, 0.001)
    }

    @Test fun `totalFor sums a smoothie correctly`() {
        val rows = listOf(
            MealTemplateIngredient(1, 1, 2, 30.0, IngredientUnit.G), // 30g protein
            MealTemplateIngredient(2, 1, 1, 1.0, IngredientUnit.PIECE), // 1 banana
            MealTemplateIngredient(3, 1, 3, 200.0, IngredientUnit.ML) // 200ml almond milk
        )
        val ingredients = mapOf(1L to banana(), 2L to protein(), 3L to milk())
        val total = MacroCalculator.totalFor(rows, ingredients)
        // 30g protein: 114 kcal
        // 118g banana: 105.02 kcal
        // 200g almond milk: 26 kcal
        // Total: ~245.02
        assertEquals(245.02, total.kcal, 0.01)
        // protein: 22.5 + 1.298 + 0.8 = 24.598
        assertEquals(24.598, total.protein, 0.01)
    }

    @Test fun `unknown ingredient is silently skipped`() {
        val rows = listOf(MealTemplateIngredient(1, 1, 99, 10.0, IngredientUnit.G))
        val total = MacroCalculator.totalFor(rows, emptyMap())
        assertEquals(0.0, total.kcal, 0.001)
    }

    @Test fun `mixed units across one template work`() {
        val tbspPb = Ingredient(
            id = 4, name = "Peanut Butter",
            kcalPer100g = 588.0, proteinPer100g = 25.0, carbsPer100g = 20.0, fatPer100g = 50.0,
            defaultUnit = IngredientUnit.TBSP, gramsPerUnit = 16.0
        )
        val rows = listOf(MealTemplateIngredient(1, 1, 4, 2.0, IngredientUnit.TBSP))
        val total = MacroCalculator.totalFor(rows, mapOf(4L to tbspPb))
        // 2 tbsp * 16 g = 32 g; 588 * 0.32 = 188.16 kcal
        assertEquals(188.16, total.kcal, 0.01)
        assertEquals(8.0, total.protein, 0.01) // 25 * 0.32
    }
}
