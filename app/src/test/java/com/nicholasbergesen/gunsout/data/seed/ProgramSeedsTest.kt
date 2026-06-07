package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramSeedsTest {

    @Test fun `default program uses descriptive day labels`() {
        val labels = ProgramSeeds.upperLower4Day.days.map { it.label }

        assertEquals(
            listOf(
                "Upper Push/Pull",
                "Lower Strength",
                "Rest",
                "Upper Hypertrophy",
                "Lower Posterior/Core",
                "Rest",
                "Rest"
            ),
            labels
        )
    }

    @Test fun `lower posterior core uses loadable preferred exercises`() {
        val exerciseKeys = ProgramSeeds.upperLower4Day.days
            .single { it.orderIndex == 4 }
            .exercises
            .map { it.exerciseSeedKey }

        assertEquals(
            listOf("leg_curl", "goblet_squat", "trap_bar_deadlift", "machine_crunch"),
            exerciseKeys
        )
    }

    @Test fun `legacy day label refresh is limited to old labels`() {
        val planDay = ProgramSeeds.upperLower4Day.days.first { it.orderIndex == 4 }

        assertTrue(Seeder.SeededProgramRefresh.shouldRefreshLabel(day(label = "Lower B"), planDay))
        assertFalse(Seeder.SeededProgramRefresh.shouldRefreshLabel(day(label = "Custom lower"), planDay))
    }

    @Test fun `legacy lower posterior core refresh requires exact old prescription`() {
        val exercises = listOf(
            pe(orderIndex = 0, exerciseId = 10, sets = 3, repsMin = 10, repsMax = 12, restSec = 60),
            pe(orderIndex = 1, exerciseId = 11, sets = 3, repsMin = 10, repsMax = 10, restSec = 90),
            pe(orderIndex = 2, exerciseId = 12, sets = 3, repsMin = 12, repsMax = 12, restSec = 90),
            pe(orderIndex = 3, exerciseId = 13, sets = 3, repsMin = 15, repsMax = 15, restSec = 60)
        )
        val seedKeysByExerciseId = mapOf(
            10L to "leg_curl",
            11L to "goblet_squat",
            12L to "hip_thrust",
            13L to "lying_leg_raise"
        )

        assertTrue(Seeder.SeededProgramRefresh.matchesLegacyLowerPosteriorCore(exercises, seedKeysByExerciseId))
        assertFalse(Seeder.SeededProgramRefresh.matchesLegacyLowerPosteriorCore(
            exercises.map { if (it.orderIndex == 2) it.copy(repsMin = 10) else it },
            seedKeysByExerciseId
        ))
    }

    private fun day(label: String) = ProgramDay(
        userId = "u",
        programId = 1,
        orderIndex = 4,
        label = label
    )

    private fun pe(
        orderIndex: Int,
        exerciseId: Long,
        sets: Int,
        repsMin: Int,
        repsMax: Int,
        restSec: Int
    ) = ProgramExercise(
        userId = "u",
        programDayId = 1,
        orderIndex = orderIndex,
        exerciseId = exerciseId,
        sets = sets,
        repsMin = repsMin,
        repsMax = repsMax,
        restSec = restSec
    )
}
