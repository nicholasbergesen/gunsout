package com.gunsout.data.seed

import com.gunsout.data.entity.Ingredient
import com.gunsout.data.entity.IngredientUnit

object IngredientSeeds {
    data class Seed(val key: String, val ingredient: Ingredient)

    val all: List<Seed> = listOf(
        Seed("plant_protein", Ingredient(
            name = "Plant Protein Powder",
            kcalPer100g = 380.0, proteinPer100g = 75.0, carbsPer100g = 5.0, fatPer100g = 4.0,
            defaultUnit = IngredientUnit.G, gramsPerUnit = 1.0,
            seedKey = "plant_protein"
        )),
        Seed("whey_protein", Ingredient(
            name = "Whey Protein Powder",
            kcalPer100g = 400.0, proteinPer100g = 80.0, carbsPer100g = 5.0, fatPer100g = 6.0,
            defaultUnit = IngredientUnit.G, gramsPerUnit = 1.0,
            seedKey = "whey_protein"
        )),
        Seed("banana", Ingredient(
            name = "Banana",
            kcalPer100g = 89.0, proteinPer100g = 1.1, carbsPer100g = 23.0, fatPer100g = 0.3,
            defaultUnit = IngredientUnit.PIECE, gramsPerUnit = 118.0,
            seedKey = "banana"
        )),
        Seed("oats", Ingredient(
            name = "Rolled Oats",
            kcalPer100g = 379.0, proteinPer100g = 13.0, carbsPer100g = 68.0, fatPer100g = 7.0,
            defaultUnit = IngredientUnit.G, gramsPerUnit = 1.0,
            seedKey = "oats"
        )),
        Seed("almond_milk", Ingredient(
            name = "Almond Milk (Unsweetened)",
            kcalPer100g = 13.0, proteinPer100g = 0.4, carbsPer100g = 0.3, fatPer100g = 1.1,
            defaultUnit = IngredientUnit.ML, gramsPerUnit = 1.0,
            seedKey = "almond_milk"
        )),
        Seed("peanut_butter", Ingredient(
            name = "Peanut Butter",
            kcalPer100g = 588.0, proteinPer100g = 25.0, carbsPer100g = 20.0, fatPer100g = 50.0,
            defaultUnit = IngredientUnit.TBSP, gramsPerUnit = 16.0,
            seedKey = "peanut_butter"
        )),
        Seed("spinach", Ingredient(
            name = "Spinach",
            kcalPer100g = 23.0, proteinPer100g = 2.9, carbsPer100g = 3.6, fatPer100g = 0.4,
            defaultUnit = IngredientUnit.G, gramsPerUnit = 1.0,
            seedKey = "spinach"
        )),
        Seed("frozen_berries", Ingredient(
            name = "Frozen Berries Mix",
            kcalPer100g = 50.0, proteinPer100g = 1.0, carbsPer100g = 12.0, fatPer100g = 0.3,
            defaultUnit = IngredientUnit.G, gramsPerUnit = 1.0,
            seedKey = "frozen_berries"
        ))
    )
}
