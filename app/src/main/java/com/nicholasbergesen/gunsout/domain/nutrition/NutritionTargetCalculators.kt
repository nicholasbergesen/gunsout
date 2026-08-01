package com.nicholasbergesen.gunsout.domain.nutrition

import com.nicholasbergesen.gunsout.data.prefs.ActivityLevel
import com.nicholasbergesen.gunsout.data.prefs.GoalType
import com.nicholasbergesen.gunsout.data.prefs.Sex
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import kotlin.math.max
import kotlin.math.roundToInt

enum class TargetSource { SUGGESTED, OVERRIDDEN }

data class CalorieTarget(val kcal: Int, val source: TargetSource)

data class ProteinTarget(val grams: Int, val source: TargetSource)

object CalorieTargetCalculator {
    private val HEIGHT_CM_RANGE = 100.0..250.0
    private val WEIGHT_KG_RANGE = 30.0..300.0
    private val AGE_RANGE = 13..100

    fun suggest(profile: UserProfile): Int? {
        val age = profile.age ?: return null
        val sex = profile.sex ?: return null
        val height = profile.heightCm?.toDouble() ?: return null
        if (age !in AGE_RANGE || height !in HEIGHT_CM_RANGE) return null
        if (profile.currentBodyWeightKg !in WEIGHT_KG_RANGE) return null
        if (profile.goalBodyWeightKg !in WEIGHT_KG_RANGE) return null

        val bmr = when (sex) {
            Sex.MALE -> 10.0 * profile.currentBodyWeightKg + 6.25 * height - 5.0 * age + 5.0
            Sex.FEMALE -> 10.0 * profile.currentBodyWeightKg + 6.25 * height - 5.0 * age - 161.0
        }
        val activityMultiplier = when (profile.activityLevel) {
            ActivityLevel.SEDENTARY -> 1.2
            ActivityLevel.LIGHT -> 1.375
            ActivityLevel.MODERATE -> 1.55
            ActivityLevel.ACTIVE -> 1.725
            ActivityLevel.VERY_ACTIVE -> 1.9
        }
        val goalDelta = when (profile.goalType) {
            GoalType.CUT -> -500.0
            GoalType.MAINTAIN -> 0.0
            GoalType.BULK -> 300.0
        }
        return max(1200.0, bmr * activityMultiplier + goalDelta).toInt()
    }

    fun effective(profile: UserProfile, overrideKcal: Int?): CalorieTarget? {
        if (overrideKcal != null && overrideKcal > 0) {
            return CalorieTarget(overrideKcal, TargetSource.OVERRIDDEN)
        }
        return suggest(profile)?.let { CalorieTarget(it, TargetSource.SUGGESTED) }
    }
}

object ProteinTargetCalculator {
    private val GOAL_WEIGHT_KG_RANGE = 30.0..300.0
    private const val PROTEIN_GRAMS_PER_KG = 2.0

    fun suggest(profile: UserProfile): Int? {
        val goalWeight = profile.goalBodyWeightKg
        if (goalWeight !in GOAL_WEIGHT_KG_RANGE) return null
        return (goalWeight * PROTEIN_GRAMS_PER_KG).roundToInt()
    }

    fun effective(profile: UserProfile, overrideGrams: Int?): ProteinTarget? {
        if (overrideGrams != null && overrideGrams > 0) {
            return ProteinTarget(overrideGrams, TargetSource.OVERRIDDEN)
        }
        return suggest(profile)?.let { ProteinTarget(it, TargetSource.SUGGESTED) }
    }
}
