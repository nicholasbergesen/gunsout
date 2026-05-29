package com.nicholasbergesen.gunsout.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

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

/**
 * Per-user profile and macro-override store backed by one DataStore file per signed-in Google
 * account. File name = `user_prefs_<sha256(userId)>.preferences_pb` so distinct Google `sub`
 * claims can never collide on the same file, even if the source of `userId` ever changes from
 * the current numeric Google `sub`. The DataStore directory is excluded from Android Auto Backup
 * in `backup_rules.xml` / `data_extraction_rules.xml`, so per-user prefs stay on-device.
 *
 * Every read/write takes `userId` as a parameter; the singleton owns no implicit "current user"
 * state. Callers that need a reactive view should drive their flow with
 * [com.nicholasbergesen.gunsout.auth.CurrentUserIdProvider.currentUserId] and `flatMapLatest`, so that a sign-out
 * / sign-in to a different account switches the visible profile without ever leaking values.
 */
@Singleton
class UserPreferences @Inject constructor(
    context: Context
) {
    private val appContext = context.applicationContext

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

    // Process-lifetime cache of one DataStore per userId. DataStore enforces single-instance per
    // file across the process, so this map exists to share the canonical instance across callers
    // and to keep DataStore's internal coroutine scope alive for the life of the app. The map is
    // keyed by the raw userId; the hashed value is only used for the filename.
    private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

    private fun storeFor(userId: String): DataStore<Preferences> {
        require(userId.isNotBlank()) { "userId must not be blank" }
        return stores.computeIfAbsent(userId) {
            val fileName = "user_prefs_${sha256Hex(userId)}"
            PreferenceDataStoreFactory.create(
                produceFile = { appContext.preferencesDataStoreFile(fileName) }
            )
        }
    }

    fun profile(userId: String): Flow<UserProfile> =
        storeFor(userId).data.map { it.toProfile() }

    fun overrides(userId: String): Flow<MacroOverrides> =
        storeFor(userId).data.map { it.toOverrides() }

    suspend fun update(userId: String, transform: (UserProfile) -> UserProfile) {
        storeFor(userId).edit { p ->
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

    suspend fun updateOverrides(userId: String, transform: (MacroOverrides) -> MacroOverrides) {
        storeFor(userId).edit { p ->
            val next = transform(p.toOverrides())
            next.kcal?.let { p[Keys.overrideKcal] = it } ?: p.remove(Keys.overrideKcal)
            next.proteinG?.let { p[Keys.overrideProteinG] = it } ?: p.remove(Keys.overrideProteinG)
            next.carbsG?.let { p[Keys.overrideCarbsG] = it } ?: p.remove(Keys.overrideCarbsG)
            next.fatG?.let { p[Keys.overrideFatG] = it } ?: p.remove(Keys.overrideFatG)
        }
    }

    /** Clears every override; the daily target falls back to the suggestion. */
    suspend fun resetOverrides(userId: String) {
        updateOverrides(userId) { MacroOverrides() }
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

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xff
            sb.append(HEX[v ushr 4])
            sb.append(HEX[v and 0x0f])
        }
        return sb.toString()
    }

    private companion object {
        private val HEX = "0123456789abcdef".toCharArray()
    }
}

