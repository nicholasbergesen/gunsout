package com.gunsout.domain.progression

import com.gunsout.data.entity.Equipment
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.ProgramExercise
import com.gunsout.data.entity.Protocol
import com.gunsout.data.entity.SetEntry

/**
 * Decides what weight / volume to suggest for the next session based on the previous session's
 * working sets and the program's prescription.
 *
 * Rules (each applied in order, first match wins):
 *  - BASELINE_WEEK: while baseline week is active, suggest "keep collecting data".
 *  - PULL_UP_5X2_3: if total reps across sets is >= 18, graduate to 3x5. If < 9, suggest assisted variant.
 *  - HIT_TOP: if all working sets hit repsMax, suggest +2.5 kg (isolation) or +5 kg (compound).
 *  - MISSED_BOTTOM: if any working set was below repsMin, suggest -5 percent.
 *  - RPE_LOW: if all RPE values present and <= 7 at top of range, suggest +2.5 kg.
 *  - HOLD: keep current weight.
 */
class ProgressionEngine {

    sealed class Suggestion {
        object KeepCollectingData : Suggestion()
        data class IncreaseWeight(val deltaKg: Double, val reason: String) : Suggestion()
        data class DecreaseWeight(val ratio: Double, val reason: String) : Suggestion()
        data class HoldWeight(val reason: String) : Suggestion()
        data class GraduatePullUp(val newScheme: String, val reason: String) : Suggestion()
        data class RegressPullUp(val variant: String, val reason: String) : Suggestion()
    }

    fun suggest(
        prescription: ProgramExercise,
        exercise: Exercise,
        previousWorkingSets: List<SetEntry>,
        baselineWeekActive: Boolean
    ): Suggestion {
        if (baselineWeekActive) return Suggestion.KeepCollectingData
        if (previousWorkingSets.isEmpty()) return Suggestion.KeepCollectingData

        val working = previousWorkingSets.filter { !it.isWarmup }
        if (working.isEmpty()) return Suggestion.KeepCollectingData

        if (prescription.protocol == Protocol.PULL_UP_5X2_3) {
            val total = working.sumOf { it.reps ?: 0 }
            return when {
                total >= 18 -> Suggestion.GraduatePullUp("3x5", "You accumulated $total reps. Time to graduate to 3 sets of 5.")
                total < 9 -> Suggestion.RegressPullUp("assisted", "Only $total reps. Use band or assisted machine to build base strength.")
                else -> Suggestion.HoldWeight("Stay with 5x2-3. Total reps: $total.")
            }
        }

        val reps = working.mapNotNull { it.reps }
        if (reps.isEmpty()) return Suggestion.KeepCollectingData

        val hitTop = reps.all { it >= prescription.repsMax }
        val missedBottom = reps.any { it < prescription.repsMin }
        val rpe = working.mapNotNull { it.rpe }
        val rpeAllLow = rpe.isNotEmpty() && rpe.size == working.size && rpe.all { it <= 7 } && reps.all { it >= prescription.repsMax }

        val compound = when (exercise.equipment) {
            Equipment.BARBELL, Equipment.MACHINE -> true
            else -> false
        } && exercise.primaryMuscleGroup in setOf(
            com.gunsout.data.entity.MuscleGroup.QUADS,
            com.gunsout.data.entity.MuscleGroup.HAMSTRINGS,
            com.gunsout.data.entity.MuscleGroup.GLUTES,
            com.gunsout.data.entity.MuscleGroup.BACK,
            com.gunsout.data.entity.MuscleGroup.CHEST
        )
        val bigStep = if (compound) 5.0 else 2.5

        return when {
            hitTop -> Suggestion.IncreaseWeight(bigStep, "All sets hit ${prescription.repsMax}. Add ${bigStep} kg.")
            rpeAllLow -> Suggestion.IncreaseWeight(2.5, "RPE was low across all sets. Add 2.5 kg.")
            missedBottom -> Suggestion.DecreaseWeight(0.95, "You missed the bottom of the rep range. Drop 5 percent and rebuild.")
            else -> Suggestion.HoldWeight("Keep the weight. Push for more reps next session.")
        }
    }
}
