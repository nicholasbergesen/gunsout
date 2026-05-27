package com.gunsout.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class Sex { MALE, FEMALE }
enum class ActivityLevel { SEDENTARY, LIGHT, MODERATE, ACTIVE, VERY_ACTIVE }
enum class GoalType { CUT, MAINTAIN, BULK }

data class UserProfile(
    val currentBodyWeightKg: Double = 100.0,
    val goalBodyWeightKg: Double = 80.0,
    val goalBodyFatPct: Double? = null,
    val heightCm: Int? = null,
    val age: Int? = null,
    val sex: Sex? = null,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goalType: GoalType = GoalType.MAINTAIN,
    val kneeInjuryFlag: Boolean = true,
    val baselineWeekActive: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val firstRunDone: Boolean = false
)

/** Optional manual overrides for the daily macro target. Null means "use the suggestion". */
data class MacroOverrides(
    val kcal: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null
) {
    fun isEmpty(): Boolean = kcal == null && proteinG == null && carbsG == null && fatG == null
}

@Singleton
class UserPreferences @Inject constructor(
    private val context: android.content.Context
) {
    private object Keys {
        val currentBodyWeightKg = doublePreferencesKey("current_body_weight_kg")
        val goalBodyWeightKg = doublePreferencesKey("goal_body_weight_kg")
        val goalBodyFatPct = doublePreferencesKey("goal_body_fat_pct")
        val heightCm = intPreferencesKey("height_cm")
        val age = intPreferencesKey("age")
        val sex = stringPreferencesKey("sex")
        val activityLevel = stringPreferencesKey("activity_level")
        val goalType = stringPreferencesKey("goal_type")
        val kneeInjuryFlag = booleanPreferencesKey("knee_injury_flag")
        val baselineWeekActive = booleanPreferencesKey("baseline_week_active")
        val themeMode = stringPreferencesKey("theme_mode")
        val firstRunDone = booleanPreferencesKey("first_run_done")
        val overrideKcal = intPreferencesKey("override_kcal")
        val overrideProteinG = intPreferencesKey("override_protein_g")
        val overrideCarbsG = intPreferencesKey("override_carbs_g")
        val overrideFatG = intPreferencesKey("override_fat_g")
    }

    val profile: Flow<UserProfile> = context.userPrefsStore.data.map { p -> p.toProfile() }
    val overrides: Flow<MacroOverrides> = context.userPrefsStore.data.map { p -> p.toOverrides() }

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        context.userPrefsStore.edit { p ->
            val next = transform(p.toProfile())
            p[Keys.currentBodyWeightKg] = next.currentBodyWeightKg
            p[Keys.goalBodyWeightKg] = next.goalBodyWeightKg
            next.goalBodyFatPct?.let { p[Keys.goalBodyFatPct] = it } ?: p.remove(Keys.goalBodyFatPct)
            next.heightCm?.let { p[Keys.heightCm] = it } ?: p.remove(Keys.heightCm)
            next.age?.let { p[Keys.age] = it } ?: p.remove(Keys.age)
            next.sex?.let { p[Keys.sex] = it.name } ?: p.remove(Keys.sex)
            p[Keys.activityLevel] = next.activityLevel.name
            p[Keys.goalType] = next.goalType.name
            p[Keys.kneeInjuryFlag] = next.kneeInjuryFlag
            p[Keys.baselineWeekActive] = next.baselineWeekActive
            p[Keys.themeMode] = next.themeMode.name
            p[Keys.firstRunDone] = next.firstRunDone
        }
    }

    suspend fun updateOverrides(transform: (MacroOverrides) -> MacroOverrides) {
        context.userPrefsStore.edit { p ->
            val next = transform(p.toOverrides())
            next.kcal?.let { p[Keys.overrideKcal] = it } ?: p.remove(Keys.overrideKcal)
            next.proteinG?.let { p[Keys.overrideProteinG] = it } ?: p.remove(Keys.overrideProteinG)
            next.carbsG?.let { p[Keys.overrideCarbsG] = it } ?: p.remove(Keys.overrideCarbsG)
            next.fatG?.let { p[Keys.overrideFatG] = it } ?: p.remove(Keys.overrideFatG)
        }
    }

    /** Clears every override; the daily target falls back to the suggestion. */
    suspend fun resetOverrides() {
        updateOverrides { MacroOverrides() }
    }

    private fun Preferences.toProfile(): UserProfile = UserProfile(
        currentBodyWeightKg = this[Keys.currentBodyWeightKg] ?: 100.0,
        goalBodyWeightKg = this[Keys.goalBodyWeightKg] ?: 80.0,
        goalBodyFatPct = this[Keys.goalBodyFatPct],
        heightCm = this[Keys.heightCm],
        age = this[Keys.age],
        sex = this[Keys.sex]?.let { runCatching { Sex.valueOf(it) }.getOrNull() },
        activityLevel = this[Keys.activityLevel]?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() } ?: ActivityLevel.MODERATE,
        goalType = this[Keys.goalType]?.let { runCatching { GoalType.valueOf(it) }.getOrNull() } ?: GoalType.MAINTAIN,
        kneeInjuryFlag = this[Keys.kneeInjuryFlag] ?: true,
        baselineWeekActive = this[Keys.baselineWeekActive] ?: true,
        themeMode = this[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        firstRunDone = this[Keys.firstRunDone] ?: false
    )

    private fun Preferences.toOverrides(): MacroOverrides = MacroOverrides(
        kcal = this[Keys.overrideKcal],
        proteinG = this[Keys.overrideProteinG],
        carbsG = this[Keys.overrideCarbsG],
        fatG = this[Keys.overrideFatG]
    )
}

