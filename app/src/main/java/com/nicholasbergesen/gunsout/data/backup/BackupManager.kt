package com.nicholasbergesen.gunsout.data.backup

import androidx.room.withTransaction
import com.nicholasbergesen.gunsout.data.db.GunsoutDatabase
import com.nicholasbergesen.gunsout.data.prefs.ThemeMode
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
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

        // User profile lives in DataStore, not Room, and is now per-user (one DataStore file per
        // signed-in Google account, see UserPreferences). Exporting reads the calling user's
        // profile only.
        val profile = userPrefs.profile(userId).first()
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
        val overrides = userPrefs.overrides(userId).first()
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
     * Every parent row is inserted with `id = 0` so Room assigns a fresh autogen PK; the old ID
     * from the backup is captured in an old -> new map and used to rewrite every child FK.
     * Without this remapping, importing a backup whose row IDs already exist in the DB for
     * another user would hit a PK collision on insert and abort the whole transaction, leaving
     * the importing user with no data at all. Per-entity remapping is the only correct approach
     * once the database stores multiple users' rows side by side.
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

            // Insert order is parents-before-children. Each parent insert clears the backup ID
            // and captures the freshly autogen'd ID into a map. Children rewrite their FK fields
            // by lookup.
            val programIdMap = HashMap<Long, Long>(parsed.programs.size)
            for (b in parsed.programs) {
                val newId = db.programDao().insert(b.toEntity(userId).copy(id = 0))
                programIdMap[b.id] = newId
            }

            val programDayIdMap = HashMap<Long, Long>(parsed.programDays.size)
            for (b in parsed.programDays) {
                val parentId = programIdMap[b.programId]
                    ?: error("Backup references unknown program id ${b.programId} from program_day ${b.id}")
                val entity = b.toEntity(userId).copy(id = 0, programId = parentId)
                programDayIdMap[b.id] = db.programDayDao().insert(entity)
            }

            val exerciseIdMap = HashMap<Long, Long>(parsed.exercises.size)
            for (b in parsed.exercises) {
                val newId = db.exerciseDao().insert(b.toEntity(userId).copy(id = 0))
                exerciseIdMap[b.id] = newId
            }

            for (b in parsed.exerciseAlternates) {
                val exId = exerciseIdMap[b.exerciseId]
                    ?: error("Backup references unknown exercise id ${b.exerciseId} from exercise_alternate")
                val altId = exerciseIdMap[b.alternateExerciseId]
                    ?: error("Backup references unknown alternate exercise id ${b.alternateExerciseId} from exercise_alternate")
                // ExerciseAlternate has a composite PK (exerciseId, alternateExerciseId), no
                // autogen — both FK fields must be the remapped values.
                val entity = b.toEntity(userId).copy(exerciseId = exId, alternateExerciseId = altId)
                db.exerciseAlternateDao().insert(entity)
            }

            val programExerciseIdMap = HashMap<Long, Long>(parsed.programExercises.size)
            for (b in parsed.programExercises) {
                val dayId = programDayIdMap[b.programDayId]
                    ?: error("Backup references unknown program_day id ${b.programDayId} from program_exercise ${b.id}")
                val exId = exerciseIdMap[b.exerciseId]
                    ?: error("Backup references unknown exercise id ${b.exerciseId} from program_exercise ${b.id}")
                val entity = b.toEntity(userId).copy(
                    id = 0,
                    programDayId = dayId,
                    exerciseId = exId
                )
                programExerciseIdMap[b.id] = db.programExerciseDao().insert(entity)
            }

            val sessionIdMap = HashMap<Long, Long>(parsed.sessions.size)
            for (b in parsed.sessions) {
                // programDayId is nullable on WorkoutSession; null when the session is a rest day
                // or a freeform session not tied to a program day. Preserve the null in that case.
                val dayId = b.programDayId?.let { old ->
                    programDayIdMap[old]
                        ?: error("Backup references unknown program_day id $old from workout_session ${b.id}")
                }
                val entity = b.toEntity(userId).copy(id = 0, programDayId = dayId)
                sessionIdMap[b.id] = db.workoutSessionDao().insert(entity)
            }

            for (b in parsed.setEntries) {
                val sessId = sessionIdMap[b.sessionId]
                    ?: error("Backup references unknown workout_session id ${b.sessionId} from set_entry ${b.id}")
                // programExerciseId is nullable — null when the set is freeform (not tied to a
                // planned program exercise, e.g. an extra accessory set the user logged ad-hoc).
                val peId = b.programExerciseId?.let { old ->
                    programExerciseIdMap[old]
                        ?: error("Backup references unknown program_exercise id $old from set_entry ${b.id}")
                }
                // exerciseIdSnapshot is the exercise the user actually performed at the time the
                // set was recorded. Remap when the snapshot resolves to a known exercise; fall
                // back to the literal old ID if the snapshot points at an exercise that's no
                // longer in the backup (e.g. the user archived it before exporting). The literal
                // ID will just be an inert number in that case — no FK on this column.
                val exSnap = exerciseIdMap[b.exerciseIdSnapshot] ?: b.exerciseIdSnapshot
                val entity = b.toEntity(userId).copy(
                    id = 0,
                    sessionId = sessId,
                    programExerciseId = peId,
                    exerciseIdSnapshot = exSnap
                )
                db.setEntryDao().insert(entity)
            }

            val mealTemplateIdMap = HashMap<Long, Long>(parsed.mealTemplates.size)
            for (b in parsed.mealTemplates) {
                val newId = db.mealTemplateDao().insert(b.toEntity(userId).copy(id = 0))
                mealTemplateIdMap[b.id] = newId
            }

            for (b in parsed.foodEntries) {
                // sourceTemplateId is nullable — null when the food entry was logged ad-hoc
                // without picking a template. Preserve null, otherwise remap.
                val tmplId = b.sourceTemplateId?.let { old ->
                    mealTemplateIdMap[old]
                        ?: error("Backup references unknown meal_template id $old from food_entry ${b.id}")
                }
                val entity = b.toEntity(userId).copy(id = 0, sourceTemplateId = tmplId)
                db.foodEntryDao().insert(entity)
            }

            val supplementIdMap = HashMap<Long, Long>(parsed.supplements.size)
            for (b in parsed.supplements) {
                val newId = db.supplementDao().insert(b.toEntity(userId).copy(id = 0))
                supplementIdMap[b.id] = newId
            }

            for (b in parsed.supplementLogs) {
                val suppId = supplementIdMap[b.supplementId]
                    ?: error("Backup references unknown supplement id ${b.supplementId} from supplement_log ${b.id}")
                val entity = b.toEntity(userId).copy(id = 0, supplementId = suppId)
                db.supplementLogDao().insert(entity)
            }

            for (b in parsed.bodyMetricsLogs) {
                db.bodyMetricsLogDao().insert(b.toEntity(userId).copy(id = 0))
            }
        }

        // Restore profile outside the Room transaction. UserPreferences is per-user as of Phase 3,
        // so this write only affects the importing user's DataStore file; other Google accounts
        // signed in on the same device are unaffected.
        parsed.userProfile?.let { p ->
            userPrefs.update(userId) {
                UserProfile(
                    currentBodyWeightKg = p.currentBodyWeightKg,
                    goalBodyWeightKg = p.goalBodyWeightKg,
                    goalBodyFatPct = p.goalBodyFatPct,
                    heightCm = p.heightCm,
                    age = p.age,
                    sex = p.sex?.let { runCatching { com.nicholasbergesen.gunsout.data.prefs.Sex.valueOf(it) }.getOrNull() },
                    activityLevel = p.activityLevel?.let { runCatching { com.nicholasbergesen.gunsout.data.prefs.ActivityLevel.valueOf(it) }.getOrNull() } ?: com.nicholasbergesen.gunsout.data.prefs.ActivityLevel.MODERATE,
                    goalType = p.goalType?.let { runCatching { com.nicholasbergesen.gunsout.data.prefs.GoalType.valueOf(it) }.getOrNull() } ?: com.nicholasbergesen.gunsout.data.prefs.GoalType.MAINTAIN,
                    kneeInjuryFlag = p.kneeInjuryFlag,
                    baselineWeekActive = p.baselineWeekActive,
                    themeMode = runCatching { ThemeMode.valueOf(p.themeMode) }.getOrDefault(ThemeMode.SYSTEM),
                    firstRunDone = p.firstRunDone
                )
            }
        }

        // Reset overrides first so an old v1/v2 import (with no macroOverrides field) clears any
        // stale overrides from the current user. Then apply imported overrides if present.
        userPrefs.resetOverrides(userId)
        parsed.macroOverrides?.let { o ->
            userPrefs.updateOverrides(userId) {
                com.nicholasbergesen.gunsout.data.prefs.MacroOverrides(
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
