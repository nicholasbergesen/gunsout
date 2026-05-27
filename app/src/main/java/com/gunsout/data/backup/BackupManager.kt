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

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val programs = db.programDao().observeAll().first().map { it.toBackup() }
        val days = programs.flatMap { db.programDayDao().getForProgram(it.id) }.map { it.toBackup() }
        val exercises = db.exerciseDao().observeAll().first().map { it.toBackup() }

        // Full list of alternate links including the reason — lossless round-trip.
        val alternates = db.exerciseAlternateDao().getAll().map { it.toBackup() }

        val programExercises = days.flatMap { day -> db.programExerciseDao().getForDay(day.id).map { it.toBackup() } }

        val sessions = db.workoutSessionDao().observeAll().first().map { it.toBackup() }
        val setEntries = sessions.flatMap { db.setEntryDao().getForSession(it.id).map { it.toBackup() } }

        val templates = db.mealTemplateDao().getAll().map { it.toBackup() }

        val foodEntries = db.foodEntryDao().getAll().map { it.toBackup() }

        val supplements = db.supplementDao().observeAll().first().map { it.toBackup() }
        val supplementLogs = db.supplementLogDao().getAll().map { it.toBackup() }

        val bodyMetrics = db.bodyMetricsLogDao().observeAll().first().map { it.toBackup() }

        // User profile lives in DataStore, not Room — include it explicitly.
        val profile = userPrefs.profile.first()
        val profileBackup = UserProfileBackup(
            currentBodyWeightKg = profile.currentBodyWeightKg,
            goalBodyWeightKg = profile.goalBodyWeightKg,
            goalBodyFatPct = profile.goalBodyFatPct,
            heightCm = profile.heightCm,
            kneeInjuryFlag = profile.kneeInjuryFlag,
            baselineWeekActive = profile.baselineWeekActive,
            themeMode = profile.themeMode.name,
            firstRunDone = profile.firstRunDone
        )

        val backup = GunsoutBackup(
            schemaVersion = 2,
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
            userProfile = profileBackup
        )
        json.encodeToString(GunsoutBackup.serializer(), backup)
    }

    /**
     * Replace all user data with the contents of [jsonText]. Wrapped in a transaction so a partial
     * import never leaves the DB half-populated. The current implementation REPLACES rather than
     * merges; callers should warn the user.
     *
     * Accepts schemaVersion 1 (no userProfile) and 2.
     */
    suspend fun importFromJson(jsonText: String): ImportResult = withContext(Dispatchers.IO) {
        val parsed = runCatching { json.decodeFromString(GunsoutBackup.serializer(), jsonText) }
            .getOrElse { return@withContext ImportResult.Error(it.message ?: "Parse failed") }

        if (parsed.schemaVersion !in 1..2) {
            return@withContext ImportResult.Error("Unsupported backup schema v${parsed.schemaVersion}")
        }

        db.withTransaction {
            val helper = db.openHelper.writableDatabase
            for (sql in listOf(
                "DELETE FROM set_entry",
                "DELETE FROM workout_session",
                "DELETE FROM supplement_log",
                "DELETE FROM supplement",
                "DELETE FROM body_metrics_log",
                "DELETE FROM food_entry",
                "DELETE FROM meal_template",
                "DELETE FROM program_exercise",
                "DELETE FROM exercise_alternate",
                "DELETE FROM exercise",
                "DELETE FROM program_day",
                "DELETE FROM program"
            )) helper.execSQL(sql)

            parsed.programs.forEach { db.programDao().insert(it.toEntity()) }
            parsed.programDays.forEach { db.programDayDao().insert(it.toEntity()) }
            parsed.exercises.forEach { db.exerciseDao().insert(it.toEntity()) }
            parsed.exerciseAlternates.forEach { db.exerciseAlternateDao().insert(it.toEntity()) }
            parsed.programExercises.forEach { db.programExerciseDao().insert(it.toEntity()) }
            parsed.sessions.forEach { db.workoutSessionDao().insert(it.toEntity()) }
            parsed.setEntries.forEach { db.setEntryDao().insert(it.toEntity()) }
            parsed.mealTemplates.forEach { db.mealTemplateDao().insert(it.toEntity()) }
            parsed.foodEntries.forEach { db.foodEntryDao().insert(it.toEntity()) }
            parsed.supplements.forEach { db.supplementDao().insert(it.toEntity()) }
            parsed.supplementLogs.forEach { db.supplementLogDao().insert(it.toEntity()) }
            parsed.bodyMetricsLogs.forEach { db.bodyMetricsLogDao().insert(it.toEntity()) }
        }

        // Restore profile outside the Room transaction.
        parsed.userProfile?.let { p ->
            userPrefs.update {
                UserProfile(
                    currentBodyWeightKg = p.currentBodyWeightKg,
                    goalBodyWeightKg = p.goalBodyWeightKg,
                    goalBodyFatPct = p.goalBodyFatPct,
                    heightCm = p.heightCm,
                    kneeInjuryFlag = p.kneeInjuryFlag,
                    baselineWeekActive = p.baselineWeekActive,
                    themeMode = runCatching { ThemeMode.valueOf(p.themeMode) }.getOrDefault(ThemeMode.SYSTEM),
                    firstRunDone = p.firstRunDone
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
