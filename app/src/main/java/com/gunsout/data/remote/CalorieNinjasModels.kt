package com.gunsout.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CalorieNinjasResponse(
    val items: List<NutritionItem> = emptyList()
)

@Serializable
data class NutritionItem(
    val name: String,
    val calories: Double = 0.0,
    @SerialName("serving_size_g") val servingSizeG: Double = 0.0,
    @SerialName("fat_total_g") val fatTotalG: Double = 0.0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carbohydrates_total_g") val carbohydratesTotalG: Double = 0.0,
    @SerialName("sugar_g") val sugarG: Double = 0.0,
    @SerialName("fiber_g") val fiberG: Double = 0.0,
    @SerialName("sodium_mg") val sodiumMg: Double = 0.0
)
