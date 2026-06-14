package com.nicholasbergesen.gunsout.feature.workout

import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.prefs.TrainingExperience
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import com.nicholasbergesen.gunsout.domain.recommendation.ExerciseRecommendationEngine
import com.nicholasbergesen.gunsout.domain.recommendation.RecommendationTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class SessionExerciseResolverTest {

    @Test
    fun `v1 repaired retired row resolves to in progress snapshot exercise for display and logging`() {
        val retiredTrapBarRow = programExercise(
            id = retiredHipThrustRowId,
            exerciseId = trapBarDeadliftId,
            isRetired = true
        )
        val legacyHipThrustSets = listOf(
            setEntry(
                programExerciseId = retiredHipThrustRowId,
                exerciseIdSnapshot = hipThrustId,
                exerciseNameSnapshot = "Hip Thrust",
                setIndex = 1
            ),
            setEntry(
                programExerciseId = retiredHipThrustRowId,
                exerciseIdSnapshot = hipThrustId,
                exerciseNameSnapshot = "Hip Thrust",
                setIndex = 2
            )
        )

        val resolved = resolvedSessionExerciseIdentity(retiredTrapBarRow, legacyHipThrustSets)

        assertEquals(hipThrustId, resolved.exerciseId)
        assertEquals("Hip Thrust", resolved.fallbackName)
    }

    @Test
    fun `v1 repaired retired row resolves legacy prescription for visible sets rest timer and logging`() {
        val retiredTrapBarRow = programExercise(
            id = retiredHipThrustRowId,
            exerciseId = trapBarDeadliftId,
            sets = 3,
            repsMin = 8,
            repsMax = 10,
            restSec = 150,
            isRetired = true
        )
        val resolved = resolvedSessionExerciseIdentity(
            retiredTrapBarRow,
            listOf(
                setEntry(
                    programExerciseId = retiredHipThrustRowId,
                    exerciseIdSnapshot = hipThrustId,
                    exerciseNameSnapshot = "Hip Thrust"
                )
            )
        )
        val prescription = resolvedSessionExercisePrescription(
            programExercise = retiredTrapBarRow,
            identity = resolved,
            rowExerciseSeedKey = "trap_bar_deadlift",
            snapshotExerciseSeedKey = "hip_thrust"
        )
        val hipThrust = exercise(hipThrustId, "Hip Thrust", "hip_thrust")

        assertEquals(retiredHipThrustRowId, prescription.id)
        assertEquals(hipThrustId, prescription.exerciseId)
        assertEquals(3, prescription.sets)
        assertEquals(12, prescription.repsMin)
        assertEquals(12, prescription.repsMax)
        assertEquals(90, prescription.restSec)
        assertEquals("3 sets x 12-12 reps. Rest 90s", sessionProtocolLabel(prescription))
        assertEquals(listOf(1, 2, 3), sessionSetIndices(prescription).toList())

        val logged = sessionSetEntryForLog(
            userId = "user",
            sessionId = 99,
            programExercise = prescription,
            exercise = hipThrust,
            setIndex = 1,
            weightKg = 100.0,
            reps = 12,
            rpe = null,
            isWarmup = false,
            completedAt = LocalDateTime.parse("2026-06-14T12:00:00")
        )
        assertEquals(retiredHipThrustRowId, logged.programExerciseId)
        assertEquals(hipThrustId, logged.exerciseIdSnapshot)
        assertEquals("Hip Thrust", logged.exerciseNameSnapshot)

        val timer = restTimerRequestForLoggedSet(
            programExercise = prescription,
            exercise = hipThrust,
            setIndex = 1,
            itemIndex = 0,
            itemCount = 1,
            isWarmup = false
        )
        assertEquals(90, timer?.durationSec)
        assertEquals("Hip Thrust", timer?.exerciseName)
    }

    @Test
    fun `recommendation uses snapshot prescription rather than retired replacement prescription`() {
        val retiredTrapBarRow = programExercise(
            id = retiredHipThrustRowId,
            exerciseId = trapBarDeadliftId,
            sets = 3,
            repsMin = 8,
            repsMax = 10,
            restSec = 150,
            isRetired = true
        )
        val resolved = resolvedSessionExerciseIdentity(
            retiredTrapBarRow,
            listOf(
                setEntry(
                    programExerciseId = retiredHipThrustRowId,
                    exerciseIdSnapshot = hipThrustId,
                    exerciseNameSnapshot = "Hip Thrust"
                )
            )
        )
        val prescription = resolvedSessionExercisePrescription(
            programExercise = retiredTrapBarRow,
            identity = resolved,
            rowExerciseSeedKey = "trap_bar_deadlift",
            snapshotExerciseSeedKey = "hip_thrust"
        )

        val recommendation = ExerciseRecommendationEngine().recommend(
            prescription = prescription,
            exercise = exercise(hipThrustId, "Hip Thrust", "hip_thrust"),
            previousWorkingSets = listOf(
                setEntry(
                    programExerciseId = retiredHipThrustRowId,
                    exerciseIdSnapshot = hipThrustId,
                    exerciseNameSnapshot = "Hip Thrust",
                    setIndex = 1,
                    reps = 11,
                    weightKg = 100.0
                )
            ),
            baselineWeekActive = false,
            profile = UserProfile(trainingExperience = TrainingExperience.INTERMEDIATE),
            latestBodyLog = null,
            recentBodyLogs = emptyList()
        )

        assertEquals(RecommendationTarget.WEIGHT_KG, recommendation?.target)
        assertEquals(95.0, recommendation?.weightKg ?: 0.0, 0.0)
    }

    @Test
    fun `active rows keep their current program exercise identity`() {
        val activeTrapBarRow = programExercise(
            id = 401,
            exerciseId = trapBarDeadliftId,
            isRetired = false
        )
        val legacyHipThrustSets = listOf(
            setEntry(
                programExerciseId = 401,
                exerciseIdSnapshot = hipThrustId,
                exerciseNameSnapshot = "Hip Thrust"
            )
        )

        val resolved = resolvedSessionExerciseIdentity(activeTrapBarRow, legacyHipThrustSets)

        assertEquals(trapBarDeadliftId, resolved.exerciseId)
        assertNull(resolved.fallbackName)
        assertEquals(
            activeTrapBarRow,
            resolvedSessionExercisePrescription(
                programExercise = activeTrapBarRow,
                identity = resolved,
                rowExerciseSeedKey = "trap_bar_deadlift",
                snapshotExerciseSeedKey = "hip_thrust"
            )
        )
    }

    @Test
    fun `retired rows fall back to row identity when current snapshots are inconsistent`() {
        val retiredRow = programExercise(
            id = retiredHipThrustRowId,
            exerciseId = trapBarDeadliftId,
            isRetired = true
        )
        val mixedSets = listOf(
            setEntry(
                programExerciseId = retiredHipThrustRowId,
                exerciseIdSnapshot = hipThrustId,
                exerciseNameSnapshot = "Hip Thrust",
                setIndex = 1
            ),
            setEntry(
                programExerciseId = retiredHipThrustRowId,
                exerciseIdSnapshot = lyingLegRaiseId,
                exerciseNameSnapshot = "Lying Leg Raise",
                setIndex = 2
            )
        )

        val resolved = resolvedSessionExerciseIdentity(retiredRow, mixedSets)

        assertEquals(trapBarDeadliftId, resolved.exerciseId)
        assertNull(resolved.fallbackName)
        assertEquals(
            retiredRow,
            resolvedSessionExercisePrescription(
                programExercise = retiredRow,
                identity = resolved,
                rowExerciseSeedKey = "trap_bar_deadlift",
                snapshotExerciseSeedKey = "hip_thrust"
            )
        )
    }

    @Test
    fun `session override wins over retired row snapshot`() {
        val retiredRow = programExercise(
            id = retiredHipThrustRowId,
            exerciseId = trapBarDeadliftId,
            isRetired = true
        )
        val legacyHipThrustSets = listOf(
            setEntry(
                programExerciseId = retiredHipThrustRowId,
                exerciseIdSnapshot = hipThrustId,
                exerciseNameSnapshot = "Hip Thrust"
            )
        )

        val resolved = resolvedSessionExerciseIdentity(
            programExercise = retiredRow,
            currentSets = legacyHipThrustSets,
            overrideExerciseId = machineCrunchId
        )

        assertEquals(machineCrunchId, resolved.exerciseId)
        assertNull(resolved.fallbackName)
    }

    private fun programExercise(
        id: Long,
        exerciseId: Long,
        sets: Int = 3,
        repsMin: Int = 8,
        repsMax: Int = 10,
        restSec: Int = 90,
        isRetired: Boolean
    ) = ProgramExercise(
        id = id,
        userId = "user",
        programDayId = 1,
        orderIndex = 2,
        exerciseId = exerciseId,
        sets = sets,
        repsMin = repsMin,
        repsMax = repsMax,
        restSec = restSec,
        protocol = Protocol.STANDARD,
        isRetired = isRetired
    )

    private fun setEntry(
        programExerciseId: Long,
        exerciseIdSnapshot: Long,
        exerciseNameSnapshot: String,
        setIndex: Int = 1,
        reps: Int? = 8,
        weightKg: Double? = 100.0
    ) = SetEntry(
        userId = "user",
        sessionId = 99,
        programExerciseId = programExerciseId,
        exerciseIdSnapshot = exerciseIdSnapshot,
        exerciseNameSnapshot = exerciseNameSnapshot,
        setIndex = setIndex,
        weightKg = weightKg,
        reps = reps
    )

    private fun exercise(id: Long, name: String, seedKey: String) = Exercise(
        id = id,
        userId = "user",
        name = name,
        primaryMuscleGroup = MuscleGroup.GLUTES,
        equipment = Equipment.BARBELL,
        movementPattern = MovementPattern.HINGE,
        seedKey = seedKey
    )

    private companion object {
        const val retiredHipThrustRowId = 303L
        const val hipThrustId = 12L
        const val lyingLegRaiseId = 13L
        const val trapBarDeadliftId = 20L
        const val machineCrunchId = 21L
    }
}
