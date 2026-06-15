package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.defaultMovementPatternFor
import org.junit.Assert.assertEquals
import org.junit.Test

class SeededExerciseNormalizerTest {

    @Test fun `one-time backfill repairs legacy migrated seeded isolation rows`() {
        val seed = ExerciseSeeds.all.single { it.key == "leg_extension" }.exercise
        val legacyRow = seed.copy(
            userId = "u",
            movementPattern = defaultMovementPatternFor(seed.primaryMuscleGroup)
        )

        val normalized = legacyRow.withSeededMovementPatternBackfill(enabled = true)

        assertEquals(MovementPattern.ISOLATION, normalized.movementPattern)
    }

    @Test fun `reseed normalization preserves default-valued edits once backfill version is done`() {
        val seed = ExerciseSeeds.all.single { it.key == "leg_extension" }.exercise
        val editedRow = seed.copy(
            userId = "u",
            movementPattern = defaultMovementPatternFor(seed.primaryMuscleGroup)
        )

        val normalized = editedRow.withSeededMovementPatternBackfill(enabled = false)

        assertEquals(MovementPattern.SQUAT, normalized.movementPattern)
    }

    @Test fun `one-time backfill preserves mismatched seeded rows`() {
        val seed = ExerciseSeeds.all.single { it.key == "leg_extension" }.exercise
        val mismatchedRow = seed.copy(
            userId = "u",
            equipment = Equipment.DUMBBELL,
            movementPattern = defaultMovementPatternFor(seed.primaryMuscleGroup)
        )

        val normalized = mismatchedRow.withSeededMovementPatternBackfill(enabled = true)

        assertEquals(MovementPattern.SQUAT, normalized.movementPattern)
    }

    @Test fun `one-time backfill preserves explicit non-default edits`() {
        val seed = ExerciseSeeds.all.single { it.key == "leg_extension" }.exercise
        val editedRow = seed.copy(
            userId = "u",
            movementPattern = MovementPattern.PULL
        )

        val normalized = editedRow.withSeededMovementPatternBackfill(enabled = true)

        assertEquals(MovementPattern.PULL, normalized.movementPattern)
    }
}
