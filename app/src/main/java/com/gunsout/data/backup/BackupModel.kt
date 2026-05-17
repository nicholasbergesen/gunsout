package com.gunsout.data.backup

import com.gunsout.data.entity.BodyMetricsLog
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.ExerciseAlternate
import com.gunsout.data.entity.FoodEntry
import com.gunsout.data.entity.Ingredient
import com.gunsout.data.entity.MealPlan
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.MealTemplateIngredient
import com.gunsout.data.entity.Program
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.ProgramExercise
import com.gunsout.data.entity.SetEntry
import com.gunsout.data.entity.Supplement
import com.gunsout.data.entity.SupplementLog
import com.gunsout.data.entity.WorkoutSession
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Serializable
data class GunsoutBackup(
    val schemaVersion: Int = 2,
    val exportedAtIso: String,
    val programs: List<ProgramBackup>,
    val programDays: List<ProgramDayBackup>,
    val exercises: List<ExerciseBackup>,
    val exerciseAlternates: List<ExerciseAlternateBackup>,
    val programExercises: List<ProgramExerciseBackup>,
    val sessions: List<WorkoutSessionBackup>,
    val setEntries: List<SetEntryBackup>,
    val mealPlans: List<MealPlanBackup>,
    val mealTemplates: List<MealTemplateBackup>,
    val ingredients: List<IngredientBackup>,
    val mealTemplateIngredients: List<MealTemplateIngredientBackup>,
    val foodEntries: List<FoodEntryBackup>,
    val supplements: List<SupplementBackup>,
    val supplementLogs: List<SupplementLogBackup>,
    val bodyMetricsLogs: List<BodyMetricsLogBackup>,
    val userProfile: UserProfileBackup? = null
)

@Serializable
data class UserProfileBackup(
    val currentBodyWeightKg: Double,
    val goalBodyWeightKg: Double,
    val goalBodyFatPct: Double? = null,
    val heightCm: Int? = null,
    val kneeInjuryFlag: Boolean,
    val baselineWeekActive: Boolean,
    val themeMode: String,
    val firstRunDone: Boolean
)

@Serializable data class ProgramBackup(val id: Long, val name: String, val type: String, val notes: String? = null, val isActive: Boolean, val isTemplate: Boolean, val seedKey: String? = null, val createdAt: Long)
@Serializable data class ProgramDayBackup(val id: Long, val programId: Long, val orderIndex: Int, val label: String, val preferredDayOfWeek: String? = null, val isRest: Boolean)
@Serializable data class ExerciseBackup(val id: Long, val name: String, val primaryMuscleGroup: String, val equipment: String, val formNotes: String? = null, val defaultRestSec: Int, val baselineNote: String? = null, val isUserCreated: Boolean, val isArchived: Boolean, val seedKey: String? = null)
@Serializable data class ExerciseAlternateBackup(val exerciseId: Long, val alternateExerciseId: Long, val reason: String)
@Serializable data class ProgramExerciseBackup(val id: Long, val programDayId: Long, val orderIndex: Int, val exerciseId: Long, val sets: Int, val repsMin: Int, val repsMax: Int, val restSec: Int, val rpeTarget: Int? = null, val supersetGroupId: Int? = null, val protocol: String)
@Serializable data class WorkoutSessionBackup(val id: Long, val date: String, val programDayId: Long? = null, val programDayLabelSnapshot: String, val status: String, val notes: String? = null, val kneeFeel: Int? = null, val startedAt: String, val completedAt: String? = null)
@Serializable data class SetEntryBackup(val id: Long, val sessionId: Long, val programExerciseId: Long? = null, val exerciseIdSnapshot: Long, val exerciseNameSnapshot: String, val setIndex: Int, val weightKg: Double? = null, val reps: Int? = null, val rpe: Int? = null, val isWarmup: Boolean, val completedAt: String? = null)
@Serializable data class MealPlanBackup(val id: Long, val name: String, val kcalTarget: Int, val proteinG: Int, val carbsG: Int, val fatG: Int, val notes: String? = null, val isActive: Boolean, val isTemplate: Boolean, val seedKey: String? = null)
@Serializable data class MealTemplateBackup(val id: Long, val mealPlanId: Long? = null, val name: String, val mealType: String, val macroSource: String, val kcal: Int, val proteinG: Double, val carbsG: Double, val fatG: Double, val notes: String? = null, val seedKey: String? = null)
@Serializable data class IngredientBackup(val id: Long, val name: String, val kcalPer100g: Double, val proteinPer100g: Double, val carbsPer100g: Double, val fatPer100g: Double, val defaultUnit: String, val gramsPerUnit: Double, val isUserCreated: Boolean, val isArchived: Boolean, val seedKey: String? = null)
@Serializable data class MealTemplateIngredientBackup(val id: Long, val mealTemplateId: Long, val ingredientId: Long, val quantity: Double, val unit: String, val orderIndex: Int)
@Serializable data class FoodEntryBackup(val id: Long, val date: String, val mealType: String, val name: String, val kcal: Int, val proteinG: Double, val carbsG: Double, val fatG: Double, val sourceTemplateId: Long? = null, val createdAt: Long)
@Serializable data class SupplementBackup(val id: Long, val name: String, val defaultDose: Double, val unit: String, val notes: String? = null, val takeWith: String? = null, val reminderTime: String? = null, val isActive: Boolean, val isUserCreated: Boolean, val seedKey: String? = null)
@Serializable data class SupplementLogBackup(val id: Long, val supplementId: Long, val date: String, val doseTaken: Double, val unit: String, val takenAt: String)
@Serializable data class BodyMetricsLogBackup(val id: Long, val date: String, val weightKg: Double, val bodyFatPct: Double? = null, val muscleMassKg: Double? = null, val waterPct: Double? = null, val boneMassKg: Double? = null, val visceralFatRating: Int? = null, val notes: String? = null)

fun Program.toBackup() = ProgramBackup(id, name, type.name, notes, isActive, isTemplate, seedKey, createdAt)
fun ProgramBackup.toEntity() = Program(id, name, com.gunsout.data.entity.ProgramType.valueOf(type), notes, isActive, isTemplate, seedKey, createdAt)

fun ProgramDay.toBackup() = ProgramDayBackup(id, programId, orderIndex, label, preferredDayOfWeek?.name, isRest)
fun ProgramDayBackup.toEntity() = ProgramDay(id, programId, orderIndex, label, preferredDayOfWeek?.let { com.gunsout.data.entity.DayHint.valueOf(it) }, isRest)

fun Exercise.toBackup() = ExerciseBackup(id, name, primaryMuscleGroup.name, equipment.name, formNotes, defaultRestSec, baselineNote, isUserCreated, isArchived, seedKey)
fun ExerciseBackup.toEntity() = Exercise(id, name, com.gunsout.data.entity.MuscleGroup.valueOf(primaryMuscleGroup), com.gunsout.data.entity.Equipment.valueOf(equipment), formNotes, defaultRestSec, baselineNote, isUserCreated, isArchived, seedKey)

fun ExerciseAlternate.toBackup() = ExerciseAlternateBackup(exerciseId, alternateExerciseId, reason.name)
fun ExerciseAlternateBackup.toEntity() = ExerciseAlternate(exerciseId, alternateExerciseId, com.gunsout.data.entity.AlternateReason.valueOf(reason))

fun ProgramExercise.toBackup() = ProgramExerciseBackup(id, programDayId, orderIndex, exerciseId, sets, repsMin, repsMax, restSec, rpeTarget, supersetGroupId, protocol.name)
fun ProgramExerciseBackup.toEntity() = ProgramExercise(id, programDayId, orderIndex, exerciseId, sets, repsMin, repsMax, restSec, rpeTarget, supersetGroupId, com.gunsout.data.entity.Protocol.valueOf(protocol))

fun WorkoutSession.toBackup() = WorkoutSessionBackup(id, date.toString(), programDayId, programDayLabelSnapshot, status.name, notes, kneeFeel, startedAt.toString(), completedAt?.toString())
fun WorkoutSessionBackup.toEntity() = WorkoutSession(id, LocalDate.parse(date), programDayId, programDayLabelSnapshot, com.gunsout.data.entity.SessionStatus.valueOf(status), notes, kneeFeel, LocalDateTime.parse(startedAt), completedAt?.let(LocalDateTime::parse))

fun SetEntry.toBackup() = SetEntryBackup(id, sessionId, programExerciseId, exerciseIdSnapshot, exerciseNameSnapshot, setIndex, weightKg, reps, rpe, isWarmup, completedAt?.toString())
fun SetEntryBackup.toEntity() = SetEntry(id, sessionId, programExerciseId, exerciseIdSnapshot, exerciseNameSnapshot, setIndex, weightKg, reps, rpe, isWarmup, completedAt?.let(LocalDateTime::parse))

fun MealPlan.toBackup() = MealPlanBackup(id, name, kcalTarget, proteinG, carbsG, fatG, notes, isActive, isTemplate, seedKey)
fun MealPlanBackup.toEntity() = MealPlan(id, name, kcalTarget, proteinG, carbsG, fatG, notes, isActive, isTemplate, seedKey)

fun MealTemplate.toBackup() = MealTemplateBackup(id, mealPlanId, name, mealType.name, macroSource.name, kcal, proteinG, carbsG, fatG, notes, seedKey)
fun MealTemplateBackup.toEntity() = MealTemplate(id, mealPlanId, name, com.gunsout.data.entity.MealType.valueOf(mealType), com.gunsout.data.entity.MacroSource.valueOf(macroSource), kcal, proteinG, carbsG, fatG, notes, seedKey)

fun Ingredient.toBackup() = IngredientBackup(id, name, kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g, defaultUnit.name, gramsPerUnit, isUserCreated, isArchived, seedKey)
fun IngredientBackup.toEntity() = Ingredient(id, name, kcalPer100g, proteinPer100g, carbsPer100g, fatPer100g, com.gunsout.data.entity.IngredientUnit.valueOf(defaultUnit), gramsPerUnit, isUserCreated, isArchived, seedKey)

fun MealTemplateIngredient.toBackup() = MealTemplateIngredientBackup(id, mealTemplateId, ingredientId, quantity, unit.name, orderIndex)
fun MealTemplateIngredientBackup.toEntity() = MealTemplateIngredient(id, mealTemplateId, ingredientId, quantity, com.gunsout.data.entity.IngredientUnit.valueOf(unit), orderIndex)

fun FoodEntry.toBackup() = FoodEntryBackup(id, date.toString(), mealType.name, name, kcal, proteinG, carbsG, fatG, sourceTemplateId, createdAt)
fun FoodEntryBackup.toEntity() = FoodEntry(id, LocalDate.parse(date), com.gunsout.data.entity.MealType.valueOf(mealType), name, kcal, proteinG, carbsG, fatG, sourceTemplateId, createdAt)

fun Supplement.toBackup() = SupplementBackup(id, name, defaultDose, unit.name, notes, takeWith, reminderTime?.toString(), isActive, isUserCreated, seedKey)
fun SupplementBackup.toEntity() = Supplement(id, name, defaultDose, com.gunsout.data.entity.SupplementUnit.valueOf(unit), notes, takeWith, reminderTime?.let(LocalTime::parse), isActive, isUserCreated, seedKey)

fun SupplementLog.toBackup() = SupplementLogBackup(id, supplementId, date.toString(), doseTaken, unit.name, takenAt.toString())
fun SupplementLogBackup.toEntity() = SupplementLog(id, supplementId, LocalDate.parse(date), doseTaken, com.gunsout.data.entity.SupplementUnit.valueOf(unit), LocalDateTime.parse(takenAt))

fun BodyMetricsLog.toBackup() = BodyMetricsLogBackup(id, date.toString(), weightKg, bodyFatPct, muscleMassKg, waterPct, boneMassKg, visceralFatRating, notes)
fun BodyMetricsLogBackup.toEntity() = BodyMetricsLog(id, LocalDate.parse(date), weightKg, bodyFatPct, muscleMassKg, waterPct, boneMassKg, visceralFatRating, notes)
