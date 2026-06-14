package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.ProgramType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramSeedsTest {

    @Test fun `program catalog contains ten targeted templates`() {
        assertEquals(
            listOf(
                "Upper / Lower 4-Day (Free Weights)",
                "Runner Strength 2-Day",
                "Runner Race Prep 3-Day",
                "Cyclist Strength 2-Day",
                "Male Strength 4-Day",
                "Female Strength 4-Day",
                "Beginner Full Body 3-Day",
                "Hypertrophy PPL 6-Day",
                "Body Recomposition 3-Day",
                "Climber Strength and Antagonists 3-Day"
            ),
            ProgramSeeds.all.map { it.name }
        )
    }

    @Test fun `program catalog uses unique stable seed keys`() {
        val seedKeys = ProgramSeeds.all.map { it.seedKey }

        assertEquals(10, seedKeys.size)
        assertEquals(seedKeys.size, seedKeys.toSet().size)
    }

    @Test fun `program catalog only references seeded exercises`() {
        val exerciseKeys = ExerciseSeeds.all.map { it.exercise.seedKey!! }.toSet()
        val missingKeys = ProgramSeeds.all
            .flatMap { it.days }
            .flatMap { it.exercises }
            .map { it.exerciseSeedKey }
            .filterNot { it in exerciseKeys }

        assertTrue("Missing exercise seed keys: $missingKeys", missingKeys.isEmpty())
    }

    @Test fun `exercise seed wrapper keys match persisted seed keys`() {
        val mismatches = ExerciseSeeds.all
            .filter { it.key != it.exercise.seedKey }
            .map { "${it.key} != ${it.exercise.seedKey}" }

        assertTrue("Mismatched seed keys: $mismatches", mismatches.isEmpty())
    }

    @Test fun `seeded isolation guide exercises use isolation movement pattern`() {
        val isolationGuideSeedKeys = listOf(
            "db_rear_delt_fly",
            "leg_extension",
            "db_lateral_raise",
            "db_bicep_curl",
            "db_overhead_tricep",
            "leg_curl",
            "db_lying_leg_curl",
            "pec_deck_fly",
            "cable_chest_fly",
            "face_pull",
            "triceps_pressdown",
            "rope_overhead_triceps",
            "preacher_curl",
            "machine_biceps_curl"
        )
        val seedsByKey = ExerciseSeeds.all.associateBy { it.key }
        val missingKeys = isolationGuideSeedKeys.filterNot { it in seedsByKey }

        assertTrue("Missing isolation guide seed keys: $missingKeys", missingKeys.isEmpty())

        val incorrectPatterns = isolationGuideSeedKeys
            .mapNotNull { key ->
                val pattern = seedsByKey.getValue(key).exercise.movementPattern
                if (pattern == MovementPattern.ISOLATION) null else "$key=$pattern"
            }

        assertTrue("Expected isolation movement patterns: $incorrectPatterns", incorrectPatterns.isEmpty())
    }

    @Test fun `seeded template metadata backfill adds missing notes and type`() {
        val legacyProgram = Program(
            userId = "u",
            name = ProgramSeeds.upperLower4Day.name,
            type = ProgramType.CUSTOM,
            notes = null,
            isTemplate = true,
            seedKey = ProgramSeeds.upperLower4Day.seedKey
        )

        val backfilled = Seeder.SeededProgramRefresh.backfillSeededTemplateMetadata(
            legacyProgram,
            ProgramSeeds.upperLower4Day
        )

        assertEquals(ProgramType.UPPER_LOWER, backfilled.type)
        assertEquals(ProgramSeeds.upperLower4Day.notes, backfilled.notes)
    }

    @Test fun `seeded template metadata backfill preserves existing notes`() {
        val legacyProgram = Program(
            userId = "u",
            name = ProgramSeeds.upperLower4Day.name,
            type = ProgramType.CUSTOM,
            notes = "Keep this note",
            isTemplate = true,
            seedKey = ProgramSeeds.upperLower4Day.seedKey
        )

        val backfilled = Seeder.SeededProgramRefresh.backfillSeededTemplateMetadata(
            legacyProgram,
            ProgramSeeds.upperLower4Day
        )

        assertEquals(ProgramType.UPPER_LOWER, backfilled.type)
        assertEquals("Keep this note", backfilled.notes)
    }

    @Test fun `seeded template metadata backfill ignores non template programs`() {
        val customProgram = Program(
            userId = "u",
            name = ProgramSeeds.upperLower4Day.name,
            type = ProgramType.CUSTOM,
            notes = null,
            isTemplate = false,
            seedKey = ProgramSeeds.upperLower4Day.seedKey
        )

        val backfilled = Seeder.SeededProgramRefresh.backfillSeededTemplateMetadata(
            customProgram,
            ProgramSeeds.upperLower4Day
        )

        assertEquals(customProgram, backfilled)
    }

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
