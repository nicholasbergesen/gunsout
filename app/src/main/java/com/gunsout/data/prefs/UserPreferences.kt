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

data class UserProfile(
    val currentBodyWeightKg: Double = 100.0,
    val goalBodyWeightKg: Double = 80.0,
    val goalBodyFatPct: Double? = null,
    val heightCm: Int? = null,
    val kneeInjuryFlag: Boolean = true,
    val baselineWeekActive: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val firstRunDone: Boolean = false
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Singleton
class UserPreferences @Inject constructor(
    private val context: android.content.Context
) {
    private object Keys {
        val currentBodyWeightKg = doublePreferencesKey("current_body_weight_kg")
        val goalBodyWeightKg = doublePreferencesKey("goal_body_weight_kg")
        val goalBodyFatPct = doublePreferencesKey("goal_body_fat_pct")
        val heightCm = intPreferencesKey("height_cm")
        val kneeInjuryFlag = booleanPreferencesKey("knee_injury_flag")
        val baselineWeekActive = booleanPreferencesKey("baseline_week_active")
        val themeMode = stringPreferencesKey("theme_mode")
        val firstRunDone = booleanPreferencesKey("first_run_done")
    }

    val profile: Flow<UserProfile> = context.userPrefsStore.data.map { p ->
        UserProfile(
            currentBodyWeightKg = p[Keys.currentBodyWeightKg] ?: 100.0,
            goalBodyWeightKg = p[Keys.goalBodyWeightKg] ?: 80.0,
            goalBodyFatPct = p[Keys.goalBodyFatPct],
            heightCm = p[Keys.heightCm],
            kneeInjuryFlag = p[Keys.kneeInjuryFlag] ?: true,
            baselineWeekActive = p[Keys.baselineWeekActive] ?: true,
            themeMode = p[Keys.themeMode]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            firstRunDone = p[Keys.firstRunDone] ?: false
        )
    }

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        context.userPrefsStore.edit { p ->
            val current = UserProfile(
                currentBodyWeightKg = p[Keys.currentBodyWeightKg] ?: 100.0,
                goalBodyWeightKg = p[Keys.goalBodyWeightKg] ?: 80.0,
                goalBodyFatPct = p[Keys.goalBodyFatPct],
                heightCm = p[Keys.heightCm],
                kneeInjuryFlag = p[Keys.kneeInjuryFlag] ?: true,
                baselineWeekActive = p[Keys.baselineWeekActive] ?: true,
                themeMode = p[Keys.themeMode]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
                firstRunDone = p[Keys.firstRunDone] ?: false
            )
            val next = transform(current)
            p[Keys.currentBodyWeightKg] = next.currentBodyWeightKg
            p[Keys.goalBodyWeightKg] = next.goalBodyWeightKg
            next.goalBodyFatPct?.let { p[Keys.goalBodyFatPct] = it } ?: p.remove(Keys.goalBodyFatPct)
            next.heightCm?.let { p[Keys.heightCm] = it } ?: p.remove(Keys.heightCm)
            p[Keys.kneeInjuryFlag] = next.kneeInjuryFlag
            p[Keys.baselineWeekActive] = next.baselineWeekActive
            p[Keys.themeMode] = next.themeMode.name
            p[Keys.firstRunDone] = next.firstRunDone
        }
    }
}
