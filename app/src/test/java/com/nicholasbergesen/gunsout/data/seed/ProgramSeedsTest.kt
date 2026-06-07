package com.nicholasbergesen.gunsout.data.seed

import org.junit.Assert.assertEquals
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
            .single { it.label == "Lower Posterior/Core" }
            .exercises
            .map { it.exerciseSeedKey }

        assertEquals(
            listOf("leg_curl", "goblet_squat", "trap_bar_deadlift", "machine_crunch"),
            exerciseKeys
        )
    }
}
