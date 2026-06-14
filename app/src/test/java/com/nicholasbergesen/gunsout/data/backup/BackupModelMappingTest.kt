package com.nicholasbergesen.gunsout.data.backup

import com.nicholasbergesen.gunsout.data.entity.AlternateReason
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.entity.DayHint
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.ExerciseAlternate
import com.nicholasbergesen.gunsout.data.entity.FoodEntry
import com.nicholasbergesen.gunsout.data.entity.MealTemplate
import com.nicholasbergesen.gunsout.data.entity.MealType
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.ProgramType
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.SessionStatus
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.Supplement
import com.nicholasbergesen.gunsout.data.entity.SupplementLog
import com.nicholasbergesen.gunsout.data.entity.SupplementUnit
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BackupModelMappingTest {

    @Test fun `room entities round trip through backup models for the target user`() {
        val sourceUser = "source-user"
        val targetUser = "target-user"
        val createdAt = 1_776_000_000_000L
        val sessionStartedAt = LocalDateTime.of(2026, 6, 14, 6, 30)
        val sessionCompletedAt = LocalDateTime.of(2026, 6, 14, 7, 45)
        val supplementTakenAt = LocalDateTime.of(2026, 6, 14, 8, 0)
        val foodDate = LocalDate.of(2026, 6, 14)

        val program = Program(
            id = 10,
            userId = sourceUser,
            name = "Cutting block",
            type = ProgramType.UPPER_LOWER,
            notes = "Protect knee",
            isActive = true,
            isTemplate = true,
            seedKey = "upper_lower",
            createdAt = createdAt
        )
        val day = ProgramDay(
            id = 11,
            userId = sourceUser,
            programId = program.id,
            orderIndex = 2,
            label = "Lower posterior",
            preferredDayOfWeek = DayHint.SUN,
            isRest = false
        )
        val exercise = Exercise(
            id = 12,
            userId = sourceUser,
            name = "Trap bar deadlift",
            primaryMuscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.HINGE,
            formNotes = "Brace first",
            defaultRestSec = 150,
            baselineNote = "Start light",
            isUserCreated = false,
            isArchived = true,
            seedKey = "trap_bar_deadlift"
        )
        val alternate = ExerciseAlternate(
            userId = sourceUser,
            exerciseId = exercise.id,
            alternateExerciseId = 99,
            reason = AlternateReason.INJURY
        )
        val programExercise = ProgramExercise(
            id = 13,
            userId = sourceUser,
            programDayId = day.id,
            orderIndex = 1,
            exerciseId = exercise.id,
            sets = 4,
            repsMin = 5,
            repsMax = 8,
            restSec = 180,
            rpeTarget = 8,
            supersetGroupId = 2,
            protocol = Protocol.AMRAP
        )
        val session = WorkoutSession(
            id = 14,
            userId = sourceUser,
            date = foodDate,
            programDayId = day.id,
            programDayLabelSnapshot = day.label,
            status = SessionStatus.COMPLETED,
            notes = "Felt solid",
            kneeFeel = 2,
            startedAt = sessionStartedAt,
            completedAt = sessionCompletedAt
        )
        val set = SetEntry(
            id = 15,
            userId = sourceUser,
            sessionId = session.id,
            programExerciseId = programExercise.id,
            exerciseIdSnapshot = exercise.id,
            exerciseNameSnapshot = exercise.name,
            setIndex = 3,
            weightKg = 90.0,
            reps = 8,
            rpe = 7,
            isWarmup = true,
            completedAt = sessionCompletedAt
        )
        val template = MealTemplate(
            id = 16,
            userId = sourceUser,
            name = "Smoothie",
            mealType = MealType.SMOOTHIE,
            kcal = 500,
            proteinG = 40.0,
            carbsG = 50.0,
            fatG = 10.0,
            notes = "Post workout",
            seedKey = "smoothie"
        )
        val food = FoodEntry(
            id = 17,
            userId = sourceUser,
            date = foodDate,
            mealType = MealType.DINNER,
            name = "Rice bowl",
            kcal = 650,
            proteinG = 45.0,
            carbsG = 80.0,
            fatG = 15.0,
            sourceTemplateId = template.id,
            createdAt = createdAt
        )
        val supplement = Supplement(
            id = 18,
            userId = sourceUser,
            name = "Creatine",
            defaultDose = 5.0,
            unit = SupplementUnit.G,
            notes = "Daily",
            takeWith = "water",
            reminderTime = LocalTime.of(9, 30),
            isActive = true,
            isUserCreated = false,
            seedKey = "creatine_mono"
        )
        val supplementLog = SupplementLog(
            id = 19,
            userId = sourceUser,
            supplementId = supplement.id,
            date = foodDate,
            doseTaken = 5.0,
            unit = SupplementUnit.G,
            takenAt = supplementTakenAt
        )
        val bodyLog = BodyMetricsLog(
            id = 20,
            userId = sourceUser,
            date = foodDate,
            weightKg = 96.2,
            bodyFatPct = 18.4,
            muscleMassKg = 70.1,
            waterPct = 55.0,
            waterLiters = 52.9,
            boneMassKg = 3.5,
            visceralFatRating = 8,
            notes = "InBody"
        )

        assertEquals(program.copy(userId = targetUser), program.toBackup().toEntity(targetUser))
        assertEquals(day.copy(userId = targetUser), day.toBackup().toEntity(targetUser))
        assertEquals(exercise.copy(userId = targetUser), exercise.toBackup().toEntity(targetUser))
        assertEquals(alternate.copy(userId = targetUser), alternate.toBackup().toEntity(targetUser))
        assertEquals(programExercise.copy(userId = targetUser), programExercise.toBackup().toEntity(targetUser))
        assertEquals(session.copy(userId = targetUser), session.toBackup().toEntity(targetUser))
        assertEquals(set.copy(userId = targetUser), set.toBackup().toEntity(targetUser))
        assertEquals(template.copy(userId = targetUser), template.toBackup().toEntity(targetUser))
        assertEquals(food.copy(userId = targetUser), food.toBackup().toEntity(targetUser))
        assertEquals(supplement.copy(userId = targetUser), supplement.toBackup().toEntity(targetUser))
        assertEquals(supplementLog.copy(userId = targetUser), supplementLog.toBackup().toEntity(targetUser))
        assertEquals(bodyLog.copy(userId = targetUser), bodyLog.toBackup().toEntity(targetUser))
    }
}
