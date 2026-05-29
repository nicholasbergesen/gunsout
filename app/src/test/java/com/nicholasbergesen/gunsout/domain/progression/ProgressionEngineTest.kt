package com.nicholasbergesen.gunsout.domain.progression

import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEngineTest {

    private val engine = ProgressionEngine()

    private fun pe(repsMin: Int = 8, repsMax: Int = 10, protocol: Protocol = Protocol.STANDARD, sets: Int = 3) =
        ProgramExercise(
            id = 1, userId = "u", programDayId = 1, orderIndex = 0, exerciseId = 1,
            sets = sets, repsMin = repsMin, repsMax = repsMax, restSec = 90,
            protocol = protocol
        )

    private fun exercise(
        muscle: MuscleGroup = MuscleGroup.CHEST,
        equipment: Equipment = Equipment.DUMBBELL
    ) = Exercise(id = 1, userId = "u", name = "X", primaryMuscleGroup = muscle, equipment = equipment)

    private fun set(weight: Double?, reps: Int?, rpe: Int? = null, isWarmup: Boolean = false) =
        SetEntry(
            userId = "u",
            sessionId = 1, programExerciseId = 1, exerciseIdSnapshot = 1,
            exerciseNameSnapshot = "X", setIndex = 1,
            weightKg = weight, reps = reps, rpe = rpe, isWarmup = isWarmup
        )

    @Test fun `baseline week suppresses suggestions`() {
        val result = engine.suggest(pe(), exercise(), listOf(set(50.0, 10)), baselineWeekActive = true)
        assertTrue(result is ProgressionEngine.Suggestion.KeepCollectingData)
    }

    @Test fun `no previous sets returns KeepCollectingData`() {
        val result = engine.suggest(pe(), exercise(), emptyList(), baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.KeepCollectingData)
    }

    @Test fun `only warmup sets returns KeepCollectingData`() {
        val result = engine.suggest(pe(), exercise(), listOf(set(20.0, 10, isWarmup = true)), baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.KeepCollectingData)
    }

    @Test fun `hits top of range on isolation gives plus 2_5 kg`() {
        val sets = List(3) { set(15.0, 10) }
        val result = engine.suggest(pe(), exercise(MuscleGroup.SHOULDERS, Equipment.DUMBBELL), sets, baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.IncreaseWeight)
        assertEquals(2.5, (result as ProgressionEngine.Suggestion.IncreaseWeight).deltaKg, 0.001)
    }

    @Test fun `hits top of range on big compound gives plus 5 kg`() {
        val sets = List(3) { set(120.0, 10) }
        val result = engine.suggest(pe(), exercise(MuscleGroup.QUADS, Equipment.BARBELL), sets, baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.IncreaseWeight)
        assertEquals(5.0, (result as ProgressionEngine.Suggestion.IncreaseWeight).deltaKg, 0.001)
    }

    @Test fun `missed bottom of range gives deload`() {
        val sets = listOf(set(50.0, 10), set(50.0, 8), set(50.0, 6))
        val result = engine.suggest(pe(), exercise(), sets, baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.DecreaseWeight)
        assertEquals(0.95, (result as ProgressionEngine.Suggestion.DecreaseWeight).ratio, 0.001)
    }

    @Test fun `RPE below 7 across all sets at top of range gives plus 2_5 kg`() {
        val sets = listOf(set(50.0, 10, rpe = 6), set(50.0, 10, rpe = 6), set(50.0, 10, rpe = 7))
        val result = engine.suggest(pe(), exercise(), sets, baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.IncreaseWeight)
        assertEquals(2.5, (result as ProgressionEngine.Suggestion.IncreaseWeight).deltaKg, 0.001)
    }

    @Test fun `RPE above 7 holds weight when not all sets hit top`() {
        val sets = listOf(set(50.0, 10, rpe = 9), set(50.0, 9, rpe = 9), set(50.0, 8, rpe = 10))
        val result = engine.suggest(pe(), exercise(), sets, baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.HoldWeight)
    }

    @Test fun `pull-up 5x2-3 graduates when total reps reach 18`() {
        val sets = listOf(set(null, 4), set(null, 4), set(null, 4), set(null, 3), set(null, 3))
        val result = engine.suggest(pe(protocol = Protocol.PULL_UP_5X2_3), exercise(MuscleGroup.BACK), sets, baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.GraduatePullUp)
    }

    @Test fun `pull-up 5x2-3 regresses to assisted when total reps below 9`() {
        val sets = listOf(set(null, 2), set(null, 2), set(null, 2))
        val result = engine.suggest(pe(protocol = Protocol.PULL_UP_5X2_3), exercise(MuscleGroup.BACK), sets, baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.RegressPullUp)
    }

    @Test fun `pull-up 5x2-3 holds in middle range`() {
        val sets = listOf(set(null, 3), set(null, 3), set(null, 3), set(null, 2))
        val result = engine.suggest(pe(protocol = Protocol.PULL_UP_5X2_3), exercise(MuscleGroup.BACK), sets, baselineWeekActive = false)
        assertTrue(result is ProgressionEngine.Suggestion.HoldWeight)
    }
}
