package com.nicholasbergesen.gunsout.feature.workout

import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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

    private fun programExercise(id: Long, exerciseId: Long, isRetired: Boolean) = ProgramExercise(
        id = id,
        userId = "user",
        programDayId = 1,
        orderIndex = 2,
        exerciseId = exerciseId,
        sets = 3,
        repsMin = 8,
        repsMax = 10,
        restSec = 90,
        protocol = Protocol.STANDARD,
        isRetired = isRetired
    )

    private fun setEntry(
        programExerciseId: Long,
        exerciseIdSnapshot: Long,
        exerciseNameSnapshot: String,
        setIndex: Int = 1
    ) = SetEntry(
        userId = "user",
        sessionId = 99,
        programExerciseId = programExerciseId,
        exerciseIdSnapshot = exerciseIdSnapshot,
        exerciseNameSnapshot = exerciseNameSnapshot,
        setIndex = setIndex,
        weightKg = 100.0,
        reps = 8
    )

    private companion object {
        const val retiredHipThrustRowId = 303L
        const val hipThrustId = 12L
        const val lyingLegRaiseId = 13L
        const val trapBarDeadliftId = 20L
        const val machineCrunchId = 21L
    }
}
