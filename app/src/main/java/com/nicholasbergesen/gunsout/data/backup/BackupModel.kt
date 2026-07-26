package com.nicholasbergesen.gunsout.data.backup

import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.entity.CreatineCheck
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.ExerciseAlternate
import com.nicholasbergesen.gunsout.data.entity.defaultMovementPatternFor
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.ProteinEntry
import com.nicholasbergesen.gunsout.data.entity.ProteinTargetSnapshot
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession
import com.nicholasbergesen.gunsout.data.seed.withSeededMovementPatternBackfill
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

@Serializable
data class GunsoutBackup(
    val schemaVersion: Int,
    val exportedAtIso: String,
    val programs: List<ProgramBackup>,
    val programDays: List<ProgramDayBackup>,
    val exercises: List<ExerciseBackup>,
    val exerciseAlternates: List<ExerciseAlternateBackup>,
    val programExercises: List<ProgramExerciseBackup>,
    val sessions: List<WorkoutSessionBackup>,
    val setEntries: List<SetEntryBackup>,
    val mealTemplates: List<MealTemplateBackup>,
    val foodEntries: List<FoodEntryBackup>,
    val supplements: List<SupplementBackup>,
    val supplementLogs: List<SupplementLogBackup>,
    val bodyMetricsLogs: List<BodyMetricsLogBackup>,
    val userProfile: UserProfileBackup? = null,
    val macroOverrides: MacroOverridesBackup? = null,
    val proteinEntries: List<ProteinEntryBackup> = emptyList(),
    val proteinTargetSnapshots: List<ProteinTargetSnapshotBackup> = emptyList(),
    val creatineSettings: CreatineSettingsBackup? = null,
    val creatineChecks: List<CreatineCheckBackup> = emptyList(),
    val targetOverrides: TargetOverridesBackup? = null
)

@Serializable
data class UserProfileBackup(
    val currentBodyWeightKg: Double,
    val goalBodyWeightKg: Double,
    val goalBodyFatPct: Double? = null,
    val heightCm: Int? = null,
    val age: Int? = null,
    val sex: String? = null,
    val trainingExperience: String? = null,
    val activityLevel: String? = null,
    val goalType: String? = null,
    val kneeInjuryFlag: Boolean,
    val baselineWeekActive: Boolean,
    val themeMode: String? = null,
    val themeStyle: String? = null,
    val firstRunDone: Boolean,
    val profileSetupDone: Boolean = false,
    val defaultProgramRefreshVersion: Int = 0,
    val seededMovementPatternBackfillVersion: Int = 0
)

@Serializable
data class MacroOverridesBackup(
    val kcal: Int? = null,
    val proteinG: Int? = null,
    val carbsG: Int? = null,
    val fatG: Int? = null
)

@Serializable
data class TargetOverridesBackup(
    val kcal: Int? = null,
    val proteinG: Int? = null
)

@Serializable data class ProgramBackup(val id: Long, val name: String, val type: String, val notes: String? = null, val isActive: Boolean, val isTemplate: Boolean, val seedKey: String? = null, val createdAt: Long)
@Serializable data class ProgramDayBackup(val id: Long, val programId: Long, val orderIndex: Int, val label: String, val preferredDayOfWeek: String? = null, val isRest: Boolean)
@Serializable data class ExerciseBackup(val id: Long, val name: String, val primaryMuscleGroup: String, val equipment: String, val movementPattern: String? = null, val formNotes: String? = null, val defaultRestSec: Int, val baselineNote: String? = null, val isUserCreated: Boolean, val isArchived: Boolean, val seedKey: String? = null)
@Serializable data class ExerciseAlternateBackup(val exerciseId: Long, val alternateExerciseId: Long, val reason: String)
@Serializable data class ProgramExerciseBackup(val id: Long, val programDayId: Long, val orderIndex: Int, val exerciseId: Long, val sets: Int, val repsMin: Int, val repsMax: Int, val restSec: Int, val rpeTarget: Int? = null, val supersetGroupId: Int? = null, val protocol: String, val isRetired: Boolean = false)
@Serializable data class WorkoutSessionBackup(val id: Long, val date: String, val programDayId: Long? = null, val programDayLabelSnapshot: String, val status: String, val notes: String? = null, val kneeFeel: Int? = null, val startedAt: String, val completedAt: String? = null)
@Serializable data class SetEntryBackup(val id: Long, val sessionId: Long, val programExerciseId: Long? = null, val exerciseIdSnapshot: Long, val exerciseNameSnapshot: String, val setIndex: Int, val weightKg: Double? = null, val reps: Int? = null, val rpe: Int? = null, val isWarmup: Boolean, val completedAt: String? = null)
@Serializable data class MealTemplateBackup(val id: Long, val name: String, val mealType: String, val kcal: Int, val proteinG: Double, val carbsG: Double, val fatG: Double, val notes: String? = null, val seedKey: String? = null)
@Serializable data class FoodEntryBackup(val id: Long, val date: String, val mealType: String, val name: String, val kcal: Int, val proteinG: Double, val carbsG: Double, val fatG: Double, val sourceTemplateId: Long? = null, val createdAt: Long)
@Serializable data class SupplementBackup(val id: Long, val name: String, val defaultDose: Double, val unit: String, val notes: String? = null, val takeWith: String? = null, val reminderTime: String? = null, val isActive: Boolean, val isUserCreated: Boolean, val seedKey: String? = null)
@Serializable data class SupplementLogBackup(val id: Long, val supplementId: Long, val date: String, val doseTaken: Double, val unit: String, val takenAt: String)
@Serializable data class BodyMetricsLogBackup(val id: Long, val date: String, val weightKg: Double, val bodyFatPct: Double? = null, val muscleMassKg: Double? = null, val waterPct: Double? = null, val waterLiters: Double? = null, val boneMassKg: Double? = null, val visceralFatRating: Int? = null, val notes: String? = null)
@Serializable data class ProteinEntryBackup(val id: Long, val date: String, val grams: Int, val label: String? = null, val loggedAt: Long)
@Serializable data class ProteinTargetSnapshotBackup(val date: String, val targetGrams: Int)
@Serializable data class CreatineSettingsBackup(val doseGrams: Int, val reminderTime: String? = null)
@Serializable data class CreatineCheckBackup(val date: String, val doseGrams: Int, val takenAt: String)

fun Program.toBackup() = ProgramBackup(id, name, type.name, notes, isActive, isTemplate, seedKey, createdAt)
fun ProgramBackup.toEntity(userId: String) = Program(
    id = id, userId = userId, name = name, type = com.nicholasbergesen.gunsout.data.entity.ProgramType.valueOf(type),
    notes = notes, isActive = isActive, isTemplate = isTemplate, seedKey = seedKey, createdAt = createdAt
)

fun ProgramDay.toBackup() = ProgramDayBackup(id, programId, orderIndex, label, preferredDayOfWeek?.name, isRest)
fun ProgramDayBackup.toEntity(userId: String) = ProgramDay(
    id = id, userId = userId, programId = programId, orderIndex = orderIndex, label = label,
    preferredDayOfWeek = preferredDayOfWeek?.let { com.nicholasbergesen.gunsout.data.entity.DayHint.valueOf(it) }, isRest = isRest
)

fun Exercise.toBackup() = ExerciseBackup(id, name, primaryMuscleGroup.name, equipment.name, movementPattern.name, formNotes, defaultRestSec, baselineNote, isUserCreated, isArchived, seedKey)
fun ExerciseBackup.toEntity(
    userId: String,
    backfillLegacySeededMovementPattern: Boolean = false
): Exercise {
    val muscleGroup = com.nicholasbergesen.gunsout.data.entity.MuscleGroup.valueOf(primaryMuscleGroup)
    return Exercise(
        id = id, userId = userId, name = name,
        primaryMuscleGroup = muscleGroup,
        equipment = com.nicholasbergesen.gunsout.data.entity.Equipment.valueOf(equipment),
        movementPattern = movementPattern
            ?.let { runCatching { com.nicholasbergesen.gunsout.data.entity.MovementPattern.valueOf(it) }.getOrNull() }
            ?: defaultMovementPatternFor(muscleGroup),
        formNotes = formNotes, defaultRestSec = defaultRestSec, baselineNote = baselineNote,
        isUserCreated = isUserCreated, isArchived = isArchived, seedKey = seedKey
    ).withSeededMovementPatternBackfill(
        enabled = backfillLegacySeededMovementPattern
    )
}

fun ExerciseAlternate.toBackup() = ExerciseAlternateBackup(exerciseId, alternateExerciseId, reason.name)
fun ExerciseAlternateBackup.toEntity(userId: String) = ExerciseAlternate(
    userId = userId, exerciseId = exerciseId, alternateExerciseId = alternateExerciseId,
    reason = com.nicholasbergesen.gunsout.data.entity.AlternateReason.valueOf(reason)
)

fun ProgramExercise.toBackup() = ProgramExerciseBackup(id, programDayId, orderIndex, exerciseId, sets, repsMin, repsMax, restSec, rpeTarget, supersetGroupId, protocol.name, isRetired)
fun ProgramExerciseBackup.toEntity(userId: String) = ProgramExercise(
    id = id, userId = userId, programDayId = programDayId, orderIndex = orderIndex, exerciseId = exerciseId,
    sets = sets, repsMin = repsMin, repsMax = repsMax, restSec = restSec, rpeTarget = rpeTarget,
    supersetGroupId = supersetGroupId, protocol = com.nicholasbergesen.gunsout.data.entity.Protocol.valueOf(protocol),
    isRetired = isRetired
)

fun WorkoutSession.toBackup() = WorkoutSessionBackup(id, date.toString(), programDayId, programDayLabelSnapshot, status.name, notes, kneeFeel, startedAt.toString(), completedAt?.toString())
fun WorkoutSessionBackup.toEntity(userId: String) = WorkoutSession(
    id = id, userId = userId, date = LocalDate.parse(date), programDayId = programDayId,
    programDayLabelSnapshot = programDayLabelSnapshot,
    status = com.nicholasbergesen.gunsout.data.entity.SessionStatus.valueOf(status),
    notes = notes, kneeFeel = kneeFeel, startedAt = LocalDateTime.parse(startedAt),
    completedAt = completedAt?.let(LocalDateTime::parse)
)

fun SetEntry.toBackup() = SetEntryBackup(id, sessionId, programExerciseId, exerciseIdSnapshot, exerciseNameSnapshot, setIndex, weightKg, reps, rpe, isWarmup, completedAt?.toString())
fun SetEntryBackup.toEntity(userId: String) = SetEntry(
    id = id, userId = userId, sessionId = sessionId, programExerciseId = programExerciseId,
    exerciseIdSnapshot = exerciseIdSnapshot, exerciseNameSnapshot = exerciseNameSnapshot,
    setIndex = setIndex, weightKg = weightKg, reps = reps, rpe = rpe, isWarmup = isWarmup,
    completedAt = completedAt?.let(LocalDateTime::parse)
)

fun ProteinEntry.toBackup() =
    ProteinEntryBackup(id, date.toString(), grams, label, loggedAt)

fun ProteinEntryBackup.toEntity(userId: String): ProteinEntry {
    require(grams > 0) { "protein entry grams must be positive" }
    return ProteinEntry(
        id = id,
        userId = userId,
        date = LocalDate.parse(date),
        grams = grams,
        label = label?.trim()?.takeIf(String::isNotEmpty),
        loggedAt = loggedAt
    )
}

fun FoodEntryBackup.toProteinEntry(userId: String): ProteinEntry? {
    if (!proteinG.isFinite() || proteinG <= 0.0) return null
    return ProteinEntry(
        id = id,
        userId = userId,
        date = LocalDate.parse(date),
        grams = proteinG.roundToInt().coerceAtLeast(1),
        label = name.trim().takeIf(String::isNotEmpty),
        loggedAt = createdAt
    )
}

fun ProteinTargetSnapshot.toBackup() =
    ProteinTargetSnapshotBackup(date.toString(), targetGrams)

fun ProteinTargetSnapshotBackup.toEntity(userId: String): ProteinTargetSnapshot {
    require(targetGrams > 0) { "protein target must be positive" }
    return ProteinTargetSnapshot(
        userId = userId,
        date = LocalDate.parse(date),
        targetGrams = targetGrams
    )
}

fun CreatineSettings.toBackup() =
    CreatineSettingsBackup(doseGrams, reminderTime?.toString())

fun CreatineSettingsBackup.toEntity(userId: String): CreatineSettings {
    require(doseGrams > 0) { "creatine dose must be positive" }
    return CreatineSettings(
        userId = userId,
        doseGrams = doseGrams,
        reminderTime = reminderTime?.let(LocalTime::parse)
    )
}

fun SupplementBackup.toCreatineSettings(userId: String): CreatineSettings? {
    if (seedKey != LEGACY_CREATINE_SEED_KEY || unit != "G") return null
    val dose = defaultDose
        .takeIf { it.isFinite() && it > 0.0 }
        ?.roundToInt()
        ?.coerceAtLeast(1)
        ?: return null
    return CreatineSettings(
        userId = userId,
        doseGrams = dose,
        reminderTime = reminderTime?.let(LocalTime::parse)
    )
}

fun CreatineCheck.toBackup() =
    CreatineCheckBackup(date.toString(), doseGrams, takenAt.toString())

fun CreatineCheckBackup.toEntity(userId: String): CreatineCheck {
    require(doseGrams > 0) { "creatine check dose must be positive" }
    return CreatineCheck(
        userId = userId,
        date = LocalDate.parse(date),
        doseGrams = doseGrams,
        takenAt = LocalDateTime.parse(takenAt)
    )
}

fun SupplementLogBackup.toCreatineCheck(userId: String): CreatineCheck? {
    if (unit != "G" || !doseTaken.isFinite() || doseTaken <= 0.0) return null
    return CreatineCheck(
        userId = userId,
        date = LocalDate.parse(date),
        doseGrams = doseTaken.roundToInt().coerceAtLeast(1),
        takenAt = LocalDateTime.parse(takenAt)
    )
}

const val LEGACY_CREATINE_SEED_KEY = "creatine_mono"

fun BodyMetricsLog.toBackup() = BodyMetricsLogBackup(
    id = id,
    date = date.toString(),
    weightKg = weightKg,
    bodyFatPct = bodyFatPct,
    muscleMassKg = muscleMassKg,
    waterPct = waterPct,
    waterLiters = waterLiters,
    boneMassKg = boneMassKg,
    visceralFatRating = visceralFatRating,
    notes = notes
)
fun BodyMetricsLogBackup.toEntity(userId: String) = BodyMetricsLog(
    id = id, userId = userId, date = LocalDate.parse(date), weightKg = weightKg,
    bodyFatPct = bodyFatPct, muscleMassKg = muscleMassKg, waterPct = waterPct,
    waterLiters = waterLiters ?: waterPct?.let { weightKg * it / 100.0 },
    boneMassKg = boneMassKg, visceralFatRating = visceralFatRating, notes = notes
)
