package com.nicholasbergesen.gunsout.domain.nutrition

import com.nicholasbergesen.gunsout.data.prefs.ActivityLevel
import com.nicholasbergesen.gunsout.data.prefs.GoalType
import com.nicholasbergesen.gunsout.data.prefs.MacroOverrides
import com.nicholasbergesen.gunsout.data.prefs.Sex
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import kotlin.math.max

/**
 * Suggested daily macros for a profile, computed locally via Mifflin-St Jeor
 * BMR + activity multiplier + goal delta. Same units used everywhere: kcal as
 * Int, protein/carbs/fat as Int grams.
 */
data class MacroSuggestion(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int
)

/**
 * Final macro target displayed in the Diet screen. Built by merging an
 * optional [MacroSuggestion] with any user-supplied overrides via
 * [effectiveTarget].
 */
data class MacroTarget(
    val kcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val source: Source
) {
    enum class Source { SUGGESTED, OVERRIDDEN }
}

/**
 * Local-only macro target math. No external API, no IO, no Android deps.
 *
 * Returns `null` from [suggest] when the profile is missing the inputs needed
 * to compute a sane suggestion (age, sex, height, weight, goal body weight),
 * or when any input is out of range. The Diet screen renders a CTA prompting
 * the user to fill those fields in Settings instead of showing zeros.
 */
object MacroTargetCalculator {

    private val HEIGHT_CM_RANGE = 100.0..250.0
    private val WEIGHT_KG_RANGE = 30.0..300.0
    private val AGE_RANGE = 13..100
    private const val PROTEIN_G_PER_KG_MIN = 1.6
    private const val PROTEIN_G_PER_KG_TARGET = 2.0
    private const val PROTEIN_G_PER_KG_MAX = 2.4
    private const val FAT_G_PER_KG = 0.9

    fun suggest(profile: UserProfile): MacroSuggestion? {
        val age = profile.age ?: return null
        val sex = profile.sex ?: return null
        val height = profile.heightCm?.toDouble() ?: return null
        if (age !in AGE_RANGE) return null
        if (height !in HEIGHT_CM_RANGE) return null
        val currentWeight = profile.currentBodyWeightKg
        val goalWeight = profile.goalBodyWeightKg
        if (currentWeight !in WEIGHT_KG_RANGE) return null
        if (goalWeight !in WEIGHT_KG_RANGE) return null

        val bmr = when (sex) {
            Sex.MALE -> 10.0 * currentWeight + 6.25 * height - 5.0 * age + 5.0
            Sex.FEMALE -> 10.0 * currentWeight + 6.25 * height - 5.0 * age - 161.0
        }
        val activityMultiplier = when (profile.activityLevel) {
            ActivityLevel.SEDENTARY -> 1.2
            ActivityLevel.LIGHT -> 1.375
            ActivityLevel.MODERATE -> 1.55
            ActivityLevel.ACTIVE -> 1.725
            ActivityLevel.VERY_ACTIVE -> 1.9
        }
        val maintenance = bmr * activityMultiplier
        val goalDelta = when (profile.goalType) {
            GoalType.CUT -> -500.0
            GoalType.MAINTAIN -> 0.0
            GoalType.BULK -> 300.0
        }
        val kcal = max(1200.0, maintenance + goalDelta).toInt()

        val proteinPerKg = PROTEIN_G_PER_KG_TARGET.coerceIn(PROTEIN_G_PER_KG_MIN, PROTEIN_G_PER_KG_MAX)
        val proteinG = (proteinPerKg * goalWeight).toInt()
        val fatG = (FAT_G_PER_KG * goalWeight).toInt()
        val proteinKcal = proteinG * 4
        val fatKcal = fatG * 9
        val carbsG = max(0, (kcal - proteinKcal - fatKcal) / 4)

        return MacroSuggestion(
            kcal = kcal,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG
        )
    }

    /**
     * Merges a [profile] with user-supplied [overrides] into the daily target
     * the Diet screen displays.
     *
     * Returns `null` if [suggest] returns null AND the user has not supplied
     * every override field. Partial overrides without a suggestion are
     * intentionally treated as "no target" so the screen never shows
     * 0-kcal / 0-g implicit targets.
     */
    fun effectiveTarget(profile: UserProfile, overrides: MacroOverrides): MacroTarget? {
        val suggestion = suggest(profile)
        val hasAllOverrides =
            overrides.kcal != null && overrides.proteinG != null &&
            overrides.carbsG != null && overrides.fatG != null
        val hasAnyOverride =
            overrides.kcal != null || overrides.proteinG != null ||
            overrides.carbsG != null || overrides.fatG != null
        return when {
            suggestion == null && hasAllOverrides -> MacroTarget(
                kcal = overrides.kcal!!,
                proteinG = overrides.proteinG!!,
                carbsG = overrides.carbsG!!,
                fatG = overrides.fatG!!,
                source = MacroTarget.Source.OVERRIDDEN
            )
            suggestion == null -> null
            else -> MacroTarget(
                kcal = overrides.kcal ?: suggestion.kcal,
                proteinG = overrides.proteinG ?: suggestion.proteinG,
                carbsG = overrides.carbsG ?: suggestion.carbsG,
                fatG = overrides.fatG ?: suggestion.fatG,
                source = if (hasAnyOverride) MacroTarget.Source.OVERRIDDEN else MacroTarget.Source.SUGGESTED
            )
        }
    }
}
