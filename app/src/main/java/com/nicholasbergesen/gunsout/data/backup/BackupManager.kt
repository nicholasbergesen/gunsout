package com.nicholasbergesen.gunsout.data.backup

import androidx.room.withTransaction
import com.nicholasbergesen.gunsout.data.db.GunsoutDatabase
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import com.nicholasbergesen.gunsout.data.prefs.ActivityLevel
import com.nicholasbergesen.gunsout.data.prefs.GoalType
import com.nicholasbergesen.gunsout.data.prefs.Sex
import com.nicholasbergesen.gunsout.data.prefs.TrainingExperience
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import com.nicholasbergesen.gunsout.data.seed.SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION
import com.nicholasbergesen.gunsout.data.seed.Seeder
import com.nicholasbergesen.gunsout.ui.theme.ThemeStyle
import kotlinx.coroutines.CancellationException
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
    private val userPrefs: UserPreferences,
    private val seeder: Seeder
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

        // Full list of alternate links for this user, including the reason, for lossless round-trip.
        val alternates = db.exerciseAlternateDao().getAll(userId).map { it.toBackup() }

        val programExercises = days.flatMap { day -> db.programExerciseDao().getAllForDay(day.id).map { it.toBackup() } }

        val sessions = db.workoutSessionDao().observeAll(userId).first().map { it.toBackup() }
        val setEntries = sessions.flatMap { db.setEntryDao().getForSession(it.id).map { it.toBackup() } }

        val proteinEntries = db.proteinEntryDao().getAll(userId).map { it.toBackup() }
        val targetSnapshots = db.proteinTargetSnapshotDao().getAll(userId).map { it.toBackup() }
        val creatineSettings =
            (db.creatineDao().getSettings(userId) ?: CreatineSettings(userId)).toBackup()
        val creatineChecks = db.creatineDao().getAllChecks(userId).map { it.toBackup() }

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
            trainingExperience = profile.trainingExperience.name,
            activityLevel = profile.activityLevel.name,
            goalType = profile.goalType.name,
            kneeInjuryFlag = profile.kneeInjuryFlag,
            baselineWeekActive = profile.baselineWeekActive,
            themeStyle = profile.themeStyle.name,
            firstRunDone = profile.firstRunDone,
            profileSetupDone = profile.profileSetupDone,
            defaultProgramRefreshVersion = profile.defaultProgramRefreshVersion,
            seededMovementPatternBackfillVersion = profile.seededMovementPatternBackfillVersion
        )
        val overrides = userPrefs.targetOverrides(userId).first()
        val overridesBackup = TargetOverridesBackup(
            kcal = overrides.kcal,
            proteinG = overrides.proteinG
        )

        val backup = GunsoutBackup(
            schemaVersion = 8,
            exportedAtIso = LocalDateTime.now().toString(),
            programs = programs,
            programDays = days,
            exercises = exercises,
            exerciseAlternates = alternates,
            programExercises = programExercises,
            sessions = sessions,
            setEntries = setEntries,
            mealTemplates = emptyList(),
            foodEntries = emptyList(),
            supplements = emptyList(),
            supplementLogs = emptyList(),
            bodyMetricsLogs = bodyMetrics,
            userProfile = profileBackup,
            proteinEntries = proteinEntries,
            proteinTargetSnapshots = targetSnapshots,
            creatineSettings = creatineSettings,
            creatineChecks = creatineChecks,
            targetOverrides = overridesBackup
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
     * 3 (per-user profile fields plus macro overrides), 4 (themeStyle), 5 (water liters),
     * 6 (strength profile setup and movement pattern), 7 (retired program exercises), and
     * 8 (protein-first nutrition and creatine-only tracking).
     */
    suspend fun importFromJson(userId: String, jsonText: String): ImportResult = withContext(Dispatchers.IO) {
        val parsed = runCatching { json.decodeFromString(GunsoutBackup.serializer(), jsonText) }
            .getOrElse { return@withContext ImportResult.Error(it.message ?: "Parse failed") }

        if (parsed.schemaVersion !in 1..8) {
            return@withContext ImportResult.Error("Unsupported backup schema v${parsed.schemaVersion}")
        }
        parsed.nutritionValidationError()?.let { message ->
            return@withContext ImportResult.Error("Invalid nutrition backup: $message")
        }

        val backfillImportedSeededMovementPatterns =
            parsed.needsImportedSeededMovementPatternBackfill()
        db.withTransaction {
            val helper = db.openHelper.writableDatabase
            // Child-first delete order so FK constraints (when enabled) do not block the wipe.
            // Each delete is scoped to this user's rows only.
            for (sql in listOf(
                "DELETE FROM set_entry WHERE userId = ?",
                "DELETE FROM workout_session WHERE userId = ?",
                "DELETE FROM creatine_check WHERE userId = ?",
                "DELETE FROM creatine_settings WHERE userId = ?",
                "DELETE FROM body_metrics_log WHERE userId = ?",
                "DELETE FROM protein_target_snapshot WHERE userId = ?",
                "DELETE FROM protein_entry WHERE userId = ?",
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
                val newId = db.exerciseDao().insert(
                    b.toEntity(
                        userId = userId,
                        backfillLegacySeededMovementPattern = backfillImportedSeededMovementPatterns
                    ).copy(id = 0)
                )
                exerciseIdMap[b.id] = newId
            }

            for (b in parsed.exerciseAlternates) {
                val exId = exerciseIdMap[b.exerciseId]
                    ?: error("Backup references unknown exercise id ${b.exerciseId} from exercise_alternate")
                val altId = exerciseIdMap[b.alternateExerciseId]
                    ?: error("Backup references unknown alternate exercise id ${b.alternateExerciseId} from exercise_alternate")
                // ExerciseAlternate has a composite PK (exerciseId, alternateExerciseId), no
                // autogen, so both FK fields must be the remapped values.
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
                // programExerciseId is nullable, null when the set is freeform (not tied to a
                // planned program exercise, e.g. an extra accessory set the user logged ad-hoc).
                val peId = b.programExerciseId?.let { old ->
                    programExerciseIdMap[old]
                        ?: error("Backup references unknown program_exercise id $old from set_entry ${b.id}")
                }
                // exerciseIdSnapshot is the exercise the user actually performed at the time the
                // set was recorded. Remap when the snapshot resolves to a known exercise; fall
                // back to the literal old ID if the snapshot points at an exercise that's no
                // longer in the backup (e.g. the user archived it before exporting). The literal
                // ID will just be an inert number in that case because there is no FK on this column.
                val exSnap = exerciseIdMap[b.exerciseIdSnapshot] ?: b.exerciseIdSnapshot
                val entity = b.toEntity(userId).copy(
                    id = 0,
                    sessionId = sessId,
                    programExerciseId = peId,
                    exerciseIdSnapshot = exSnap
                )
                db.setEntryDao().insert(entity)
            }

            parsed.proteinEntriesForImport(userId).forEach { entry ->
                db.proteinEntryDao().insert(entry.copy(id = 0))
            }
            parsed.proteinTargetSnapshotsForImport(userId).forEach {
                db.proteinTargetSnapshotDao().upsert(it)
            }
            db.creatineDao().upsertSettings(parsed.creatineSettingsForImport(userId))
            parsed.creatineChecksForImport(userId).forEach {
                db.creatineDao().insertCheck(it)
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
                p.toUserProfile().withImportSeedState(parsed)
            }
        } ?: userPrefs.update(userId) {
            it.withProfilelessImportSeedState(parsed)
        }

        // Reset overrides first so an old v1/v2 import (with no override field) clears any
        // stale overrides from the current user. Then apply imported overrides if present.
        userPrefs.resetTargetOverrides(userId)
        (parsed.targetOverrides ?: parsed.macroOverrides?.let {
            TargetOverridesBackup(kcal = it.kcal, proteinG = it.proteinG)
        })?.let { overrides ->
            userPrefs.updateTargetOverrides(userId) {
                com.nicholasbergesen.gunsout.data.prefs.TargetOverrides(
                    kcal = overrides.kcal?.takeIf { value -> value > 0 },
                    proteinG = overrides.proteinG?.takeIf { value -> value > 0 }
                )
            }
        }

        completeSuccessfulImportAfterSeedRefresh(
            userId = userId,
            totalRows = parsed.importRowCount(),
            refreshSeededProgram = seeder::seedIfNeeded
        )
    }
}

sealed class ImportResult {
    data class Success(val totalRows: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

internal fun GunsoutBackup.importRowCount(): Int =
    programs.size + programDays.size + exercises.size +
        programExercises.size + sessions.size + setEntries.size +
        bodyMetricsLogs.size + retainedNutritionRowCount()

private fun GunsoutBackup.retainedNutritionRowCount(): Int {
    if (schemaVersion >= 8) {
        return proteinEntries.size + proteinTargetSnapshots.size +
            (if (creatineSettings == null) 0 else 1) + creatineChecks.size
    }

    val creatineIds = supplements
        .filter { it.seedKey == LEGACY_CREATINE_SEED_KEY && it.unit == "G" }
        .mapTo(mutableSetOf(), SupplementBackup::id)
    return foodEntries.count { it.proteinG.isFinite() && it.proteinG > 0.0 } +
        (if (creatineIds.isEmpty()) 0 else 1) +
        supplementLogs.count { it.supplementId in creatineIds && it.unit == "G" }
}

internal fun GunsoutBackup.nutritionValidationError(): String? {
    if (schemaVersion < 8) return null
    if (proteinEntries.any { it.grams <= 0 }) return "protein grams must be positive"
    if (proteinEntries.any { runCatching { java.time.LocalDate.parse(it.date) }.isFailure }) {
        return "protein entry date is invalid"
    }
    if (proteinTargetSnapshots.any { it.targetGrams <= 0 }) {
        return "protein targets must be positive"
    }
    if (
        proteinTargetSnapshots.any {
            runCatching { java.time.LocalDate.parse(it.date) }.isFailure
        }
    ) {
        return "protein target date is invalid"
    }
    if (proteinTargetSnapshots.map { it.date }.distinct().size != proteinTargetSnapshots.size) {
        return "protein target dates must be unique"
    }
    creatineSettings?.let { settings ->
        if (settings.doseGrams <= 0) return "creatine dose must be positive"
        if (
            settings.reminderTime != null &&
            runCatching { java.time.LocalTime.parse(settings.reminderTime) }.isFailure
        ) {
            return "creatine reminder time is invalid"
        }
    }
    if (creatineChecks.any { it.doseGrams <= 0 }) return "creatine check doses must be positive"
    if (creatineChecks.map { it.date }.distinct().size != creatineChecks.size) {
        return "creatine check dates must be unique"
    }
    if (
        creatineChecks.any {
            runCatching {
                java.time.LocalDate.parse(it.date)
                java.time.LocalDateTime.parse(it.takenAt)
            }.isFailure
        }
    ) {
        return "creatine check date or time is invalid"
    }
    val overrides = targetOverrides
    if (overrides?.kcal != null && overrides.kcal <= 0) {
        return "kcal override must be positive"
    }
    if (overrides?.proteinG != null && overrides.proteinG <= 0) {
        return "protein override must be positive"
    }
    return null
}

internal fun GunsoutBackup.proteinEntriesForImport(userId: String) =
    if (schemaVersion >= 8) {
        proteinEntries.map { it.toEntity(userId) }
    } else {
        foodEntries.mapNotNull { it.toProteinEntry(userId) }
    }

internal fun GunsoutBackup.proteinTargetSnapshotsForImport(userId: String) =
    if (schemaVersion >= 8) {
        proteinTargetSnapshots.map { it.toEntity(userId) }.also { snapshots ->
            require(snapshots.map { it.date }.distinct().size == snapshots.size) {
                "Backup contains duplicate protein target snapshot dates"
            }
        }
    } else {
        emptyList()
    }

internal fun GunsoutBackup.creatineSettingsForImport(userId: String): CreatineSettings {
    if (schemaVersion >= 8) {
        return creatineSettings?.toEntity(userId) ?: CreatineSettings(userId)
    }
    return supplements
        .firstNotNullOfOrNull { it.toCreatineSettings(userId) }
        ?: CreatineSettings(userId)
}

internal fun GunsoutBackup.creatineChecksForImport(userId: String) =
    if (schemaVersion >= 8) {
        creatineChecks.map { it.toEntity(userId) }.also { checks ->
            require(checks.map { it.date }.distinct().size == checks.size) {
                "Backup contains duplicate creatine check dates"
            }
        }
    } else {
        val creatineIds = supplements
            .filter { it.seedKey == LEGACY_CREATINE_SEED_KEY && it.unit == "G" }
            .mapTo(mutableSetOf(), SupplementBackup::id)
        supplementLogs
            .asSequence()
            .filter { it.supplementId in creatineIds }
            .mapNotNull { it.toCreatineCheck(userId) }
            .sortedBy { it.takenAt }
            .distinctBy { it.date }
            .toList()
    }

internal suspend fun completeSuccessfulImportAfterSeedRefresh(
    userId: String,
    totalRows: Int,
    refreshSeededProgram: suspend (String) -> Unit
): ImportResult {
    return try {
        refreshSeededProgram(userId)
        ImportResult.Success(totalRows)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        ImportResult.Error(
            "Import completed but seeded program refresh failed: ${error.message ?: error.javaClass.simpleName}"
        )
    }
}

internal fun UserProfileBackup.toUserProfile(): UserProfile = UserProfile(
    currentBodyWeightKg = currentBodyWeightKg,
    goalBodyWeightKg = goalBodyWeightKg,
    goalBodyFatPct = goalBodyFatPct,
    heightCm = heightCm,
    age = age,
    sex = sex?.let { runCatching { Sex.valueOf(it) }.getOrNull() },
    trainingExperience = trainingExperience
        ?.let { runCatching { TrainingExperience.valueOf(it) }.getOrNull() }
        ?: TrainingExperience.BEGINNER,
    activityLevel = activityLevel?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() }
        ?: ActivityLevel.MODERATE,
    goalType = goalType?.let { runCatching { GoalType.valueOf(it) }.getOrNull() } ?: GoalType.MAINTAIN,
    kneeInjuryFlag = kneeInjuryFlag,
    baselineWeekActive = baselineWeekActive,
    themeStyle = ThemeStyle.fromStoredName(themeStyle),
    firstRunDone = firstRunDone,
    profileSetupDone = profileSetupDone,
    defaultProgramRefreshVersion = defaultProgramRefreshVersion,
    seededMovementPatternBackfillVersion = seededMovementPatternBackfillVersion
)

internal fun UserProfile.withImportSeedState(backup: GunsoutBackup): UserProfile =
    copy(
        firstRunDone = firstRunDone || backup.importedActiveProgramCount() > 0,
        seededMovementPatternBackfillVersion = maxOf(
            seededMovementPatternBackfillVersion,
            backup.seededMovementPatternBackfillVersionAfterImport()
        )
    )

internal fun UserProfile.withProfilelessImportSeedState(backup: GunsoutBackup): UserProfile =
    copy(
        firstRunDone = backup.importedActiveProgramCount() > 0,
        defaultProgramRefreshVersion = 0
    ).withImportSeedState(backup)

internal fun GunsoutBackup.importedActiveProgramCount(): Int =
    programs.count { it.isActive }

private val GunsoutBackup.importedSeededMovementPatternBackfillVersion: Int
    get() = userProfile?.seededMovementPatternBackfillVersion ?: 0

internal fun GunsoutBackup.needsImportedSeededMovementPatternBackfill(): Boolean =
    importedSeededMovementPatternBackfillVersion < SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION

internal fun GunsoutBackup.seededMovementPatternBackfillVersionAfterImport(): Int =
    maxOf(
        importedSeededMovementPatternBackfillVersion,
        SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION
    )
