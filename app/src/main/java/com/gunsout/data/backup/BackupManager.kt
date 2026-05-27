package com.gunsout.data.backup

import androidx.room.withTransaction
import com.gunsout.data.db.GunsoutDatabase
import com.gunsout.data.prefs.ThemeMode
import com.gunsout.data.prefs.UserPreferences
import com.gunsout.data.prefs.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val db: GunsoutDatabase,
    private val userPrefs: UserPreferences
) {
    // ignoreUnknownKeys lets legacy v1/v2 files import cleanly: their dropped fields
    // (mealPlans, ingredients, mealTemplateIngredients, macroSource, mealPlanId) are skipped
    // silently. Phase 4 will reintroduce dedicated legacy-only fields with id remapping.
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /**
     * Export everything for [userId] to a JSON envelope. Every query is user-scoped so other
     * users' data on the same device never leaks into the file.
     */
    suspend fun exportToJson(userId: String): String = withContext(Dispatchers.IO) {
        val programs = db.programDao().observeAll(userId).first().map { it.toBackup() }
        val days = programs.flatMap { db.programDayDao().getForProgram(it.id) }.map { it.toBackup() }
        val exercises = db.exerciseDao().observeAll(userId).first().map { it.toBackup() }

        // Full list of alternate links for this user, including the reason — lossless round-trip.
        val alternates = db.exerciseAlternateDao().getAll(userId).map { it.toBackup() }

        val programExercises = days.flatMap { day -> db.programExerciseDao().getForDay(day.id).map { it.toBackup() } }

        val sessions = db.workoutSessionDao().observeAll(userId).first().map { it.toBackup() }
        val setEntries = sessions.flatMap { db.setEntryDao().getForSession(it.id).map { it.toBackup() } }

        val templates = db.mealTemplateDao().getAll(userId).map { it.toBackup() }

        val foodEntries = db.foodEntryDao().getAll(userId).map { it.toBackup() }

        val supplements = db.supplementDao().observeAll(userId).first().map { it.toBackup() }
        val supplementLogs = db.supplementLogDao().getAll(userId).map { it.toBackup() }

        val bodyMetrics = db.bodyMetricsLogDao().observeAll(userId).first().map { it.toBackup() }

        // User profile lives in DataStore, not Room. Phase 2b-2 keeps UserPreferences single-user;
        // Phase 3 makes it per-user and this read switches to the per-user DataStore.
        val profile = userPrefs.profile.first()
        val profileBackup = UserProfileBackup(
            currentBodyWeightKg = profile.currentBodyWeightKg,
            goalBodyWeightKg = profile.goalBodyWeightKg,
            goalBodyFatPct = profile.goalBodyFatPct,
            heightCm = profile.heightCm,
            age = profile.age,
            sex = profile.sex?.name,
            activityLevel = profile.activityLevel.name,
            goalType = profile.goalType.name,
            kneeInjuryFlag = profile.kneeInjuryFlag,
            baselineWeekActive = profile.baselineWeekActive,
            themeMode = profile.themeMode.name,
            firstRunDone = profile.firstRunDone
        )
        val overrides = userPrefs.overrides.first()
        val overridesBackup = MacroOverridesBackup(
            kcal = overrides.kcal,
            proteinG = overrides.proteinG,
            carbsG = overrides.carbsG,
            fatG = overrides.fatG
        )

        val backup = GunsoutBackup(
            schemaVersion = 3,
            exportedAtIso = LocalDateTime.now().toString(),
            programs = programs,
            programDays = days,
            exercises = exercises,
            exerciseAlternates = alternates,
            programExercises = programExercises,
            sessions = sessions,
            setEntries = setEntries,
            mealTemplates = templates,
            foodEntries = foodEntries,
            supplements = supplements,
            supplementLogs = supplementLogs,
            bodyMetricsLogs = bodyMetrics,
            userProfile = profileBackup,
            macroOverrides = overridesBackup
        )
        json.encodeToString(GunsoutBackup.serializer(), backup)
    }

    /**
     * Replace [userId]'s data with the contents of [jsonText]. Wrapped in a transaction so a
     * partial import never leaves the DB half-populated. Other users' rows are untouched: every
     * delete is scoped by userId, and every imported row is stamped with [userId] regardless of
     * the userId carried in the file.
     *
     * Accepts schemaVersion 1 (no userProfile), 2 (single-user profile, no macro overrides),
     * and 3 (per-user profile fields plus macro overrides).
     */
    suspend fun importFromJson(userId: String, jsonText: String): ImportResult = withContext(Dispatchers.IO) {
        val parsed = runCatching { json.decodeFromString(GunsoutBackup.serializer(), jsonText) }
            .getOrElse { return@withContext ImportResult.Error(it.message ?: "Parse failed") }

        if (parsed.schemaVersion !in 1..3) {
            return@withContext ImportResult.Error("Unsupported backup schema v${parsed.schemaVersion}")
        }

        db.withTransaction {
            val helper = db.openHelper.writableDatabase
            // Child-first delete order so FK constraints (when enabled) do not block the wipe.
            // Each delete is scoped to this user's rows only.
            for (sql in listOf(
                "DELETE FROM set_entry WHERE userId = ?",
                "DELETE FROM workout_session WHERE userId = ?",
                "DELETE FROM supplement_log WHERE userId = ?",
                "DELETE FROM supplement WHERE userId = ?",
                "DELETE FROM body_metrics_log WHERE userId = ?",
                "DELETE FROM food_entry WHERE userId = ?",
                "DELETE FROM meal_template WHERE userId = ?",
                "DELETE FROM program_exercise WHERE userId = ?",
                "DELETE FROM exercise_alternate WHERE userId = ?",
                "DELETE FROM exercise WHERE userId = ?",
                "DELETE FROM program_day WHERE userId = ?",
                "DELETE FROM program WHERE userId = ?"
            )) helper.execSQL(sql, arrayOf(userId))

            parsed.programs.forEach { db.programDao().insert(it.toEntity(userId)) }
            parsed.programDays.forEach { db.programDayDao().insert(it.toEntity(userId)) }
            parsed.exercises.forEach { db.exerciseDao().insert(it.toEntity(userId)) }
            parsed.exerciseAlternates.forEach { db.exerciseAlternateDao().insert(it.toEntity(userId)) }
            parsed.programExercises.forEach { db.programExerciseDao().insert(it.toEntity(userId)) }
            parsed.sessions.forEach { db.workoutSessionDao().insert(it.toEntity(userId)) }
            parsed.setEntries.forEach { db.setEntryDao().insert(it.toEntity(userId)) }
            parsed.mealTemplates.forEach { db.mealTemplateDao().insert(it.toEntity(userId)) }
            parsed.foodEntries.forEach { db.foodEntryDao().insert(it.toEntity(userId)) }
            parsed.supplements.forEach { db.supplementDao().insert(it.toEntity(userId)) }
            parsed.supplementLogs.forEach { db.supplementLogDao().insert(it.toEntity(userId)) }
            parsed.bodyMetricsLogs.forEach { db.bodyMetricsLogDao().insert(it.toEntity(userId)) }
        }

        // Restore profile outside the Room transaction. UserPreferences is still single-user in
        // Phase 2b-2, so the imported profile fully replaces the current values.
        parsed.userProfile?.let { p ->
            userPrefs.update {
                UserProfile(
                    currentBodyWeightKg = p.currentBodyWeightKg,
                    goalBodyWeightKg = p.goalBodyWeightKg,
                    goalBodyFatPct = p.goalBodyFatPct,
                    heightCm = p.heightCm,
                    age = p.age,
                    sex = p.sex?.let { runCatching { com.gunsout.data.prefs.Sex.valueOf(it) }.getOrNull() },
                    activityLevel = p.activityLevel?.let { runCatching { com.gunsout.data.prefs.ActivityLevel.valueOf(it) }.getOrNull() } ?: com.gunsout.data.prefs.ActivityLevel.MODERATE,
                    goalType = p.goalType?.let { runCatching { com.gunsout.data.prefs.GoalType.valueOf(it) }.getOrNull() } ?: com.gunsout.data.prefs.GoalType.MAINTAIN,
                    kneeInjuryFlag = p.kneeInjuryFlag,
                    baselineWeekActive = p.baselineWeekActive,
                    themeMode = runCatching { ThemeMode.valueOf(p.themeMode) }.getOrDefault(ThemeMode.SYSTEM),
                    firstRunDone = p.firstRunDone
                )
            }
        }

        // Reset overrides first so an old v1/v2 import (with no macroOverrides field) clears any
        // stale overrides from the current user. Then apply imported overrides if present.
        userPrefs.resetOverrides()
        parsed.macroOverrides?.let { o ->
            userPrefs.updateOverrides {
                com.gunsout.data.prefs.MacroOverrides(
                    kcal = o.kcal,
                    proteinG = o.proteinG,
                    carbsG = o.carbsG,
                    fatG = o.fatG
                )
            }
        }

        ImportResult.Success(
            totalRows = parsed.programs.size + parsed.programDays.size + parsed.exercises.size +
                parsed.programExercises.size + parsed.sessions.size + parsed.setEntries.size +
                parsed.mealTemplates.size +
                parsed.foodEntries.size + parsed.supplements.size + parsed.supplementLogs.size +
                parsed.bodyMetricsLogs.size
        )
    }
}

sealed class ImportResult {
    data class Success(val totalRows: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
