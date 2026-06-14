package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.ProgramType
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.entity.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramSeedsTest {

    @Test fun `program catalog contains ten targeted templates`() {
        assertEquals(
            listOf(
                "Upper / Lower 4-Day",
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

    @Test fun `upper lower template name and notes do not claim free weights only`() {
        assertEquals("Upper / Lower 4-Day", ProgramSeeds.upperLower4Day.name)
        assertFalse(ProgramSeeds.upperLower4Day.notes.orEmpty().contains("free-weight", ignoreCase = true))
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

    @Test fun `seeded template metadata backfill refreshes legacy upper lower name and notes`() {
        val legacyProgram = Program(
            userId = "u",
            name = "Upper / Lower 4-Day (Free Weights)",
            type = ProgramType.CUSTOM,
            notes = "Balanced free-weight strength and hypertrophy across four weekly lifting days.",
            isTemplate = true,
            seedKey = ProgramSeeds.upperLower4Day.seedKey
        )

        val backfilled = Seeder.SeededProgramRefresh.backfillSeededTemplateMetadata(
            legacyProgram,
            ProgramSeeds.upperLower4Day
        )

        assertEquals("Upper / Lower 4-Day", backfilled.name)
        assertEquals(ProgramSeeds.upperLower4Day.notes, backfilled.notes)
    }

    @Test fun `seeded template metadata backfill preserves user renamed upper lower template`() {
        val renamedProgram = Program(
            userId = "u",
            name = "My upper lower plan",
            type = ProgramType.CUSTOM,
            notes = "Keep this note",
            isTemplate = true,
            seedKey = ProgramSeeds.upperLower4Day.seedKey
        )

        val backfilled = Seeder.SeededProgramRefresh.backfillSeededTemplateMetadata(
            renamedProgram,
            ProgramSeeds.upperLower4Day
        )

        assertEquals("My upper lower plan", backfilled.name)
        assertEquals("Keep this note", backfilled.notes)
    }

    @Test fun `seeded content refresh is limited to catalog named upper lower template`() {
        val catalogNamedProgram = Program(
            userId = "u",
            name = ProgramSeeds.upperLower4Day.name,
            isTemplate = true,
            seedKey = ProgramSeeds.upperLower4Day.seedKey
        )
        val renamedProgram = catalogNamedProgram.copy(name = "My upper lower plan")

        assertTrue(Seeder.SeededProgramRefresh.shouldRefreshSeededProgramContents(
            catalogNamedProgram,
            ProgramSeeds.upperLower4Day
        ))
        assertFalse(Seeder.SeededProgramRefresh.shouldRefreshSeededProgramContents(
            renamedProgram,
            ProgramSeeds.upperLower4Day
        ))
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

    @Test fun `upper push pull includes lat pulldown`() {
        val exerciseKeys = ProgramSeeds.upperLower4Day.days
            .single { it.orderIndex == 0 }
            .exercises
            .map { it.exerciseSeedKey }

        assertEquals(
            listOf("inc_db_bench", "pull_ups", "lat_pulldown", "db_shoulder_press", "db_rear_delt_fly"),
            exerciseKeys
        )
    }

    @Test fun `upper hypertrophy includes forearm accessories`() {
        val exerciseKeys = ProgramSeeds.upperLower4Day.days
            .single { it.orderIndex == 3 }
            .exercises
            .map { it.exerciseSeedKey }

        assertEquals(
            listOf(
                "chest_supported_row",
                "flat_db_bench",
                "db_lateral_raise",
                "db_bicep_curl",
                "db_overhead_tricep",
                "db_wrist_curl",
                "db_reverse_wrist_curl"
            ),
            exerciseKeys
        )
    }

    @Test fun `forearm seeds use explicit forearm isolation metadata`() {
        val forearmSeeds = ExerciseSeeds.all
            .filter { it.key in setOf("db_wrist_curl", "db_reverse_wrist_curl") }
            .map { it.exercise }

        assertEquals(2, forearmSeeds.size)
        assertTrue(forearmSeeds.all { it.primaryMuscleGroup == MuscleGroup.FOREARMS })
        assertTrue(forearmSeeds.all { it.movementPattern == MovementPattern.ISOLATION })
        assertTrue(forearmSeeds.all { it.defaultRestSec == 45 })
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

    @Test fun `legacy upper push pull refresh requires exact old prescription`() {
        val exercises = listOf(
            pe(orderIndex = 0, exerciseId = 10, sets = 3, repsMin = 8, repsMax = 10, restSec = 90),
            pe(
                orderIndex = 1,
                exerciseId = 11,
                sets = 5,
                repsMin = 2,
                repsMax = 3,
                restSec = 120,
                protocol = Protocol.PULL_UP_5X2_3
            ),
            pe(orderIndex = 2, exerciseId = 12, sets = 3, repsMin = 10, repsMax = 10, restSec = 60),
            pe(orderIndex = 3, exerciseId = 13, sets = 3, repsMin = 15, repsMax = 15, restSec = 60)
        )
        val seedKeysByExerciseId = mapOf(
            10L to "inc_db_bench",
            11L to "pull_ups",
            12L to "db_shoulder_press",
            13L to "db_rear_delt_fly"
        )

        assertTrue(Seeder.SeededProgramRefresh.matchesLegacyUpperPushPull(exercises, seedKeysByExerciseId))
        assertFalse(Seeder.SeededProgramRefresh.matchesLegacyUpperPushPull(
            exercises.map { if (it.orderIndex == 1) it.copy(protocol = Protocol.STANDARD) else it },
            seedKeysByExerciseId
        ))
    }

    @Test fun `legacy upper hypertrophy refresh requires exact old supersets`() {
        val exercises = listOf(
            pe(orderIndex = 0, exerciseId = 10, sets = 3, repsMin = 8, repsMax = 10, restSec = 90),
            pe(orderIndex = 1, exerciseId = 11, sets = 3, repsMin = 10, repsMax = 10, restSec = 90),
            pe(orderIndex = 2, exerciseId = 12, sets = 4, repsMin = 12, repsMax = 15, restSec = 60),
            pe(orderIndex = 3, exerciseId = 13, sets = 3, repsMin = 10, repsMax = 10, restSec = 60, supersetGroupId = 1),
            pe(orderIndex = 4, exerciseId = 14, sets = 3, repsMin = 12, repsMax = 12, restSec = 60, supersetGroupId = 1)
        )
        val seedKeysByExerciseId = mapOf(
            10L to "chest_supported_row",
            11L to "flat_db_bench",
            12L to "db_lateral_raise",
            13L to "db_bicep_curl",
            14L to "db_overhead_tricep"
        )

        assertTrue(Seeder.SeededProgramRefresh.matchesLegacyUpperHypertrophy(exercises, seedKeysByExerciseId))
        assertFalse(Seeder.SeededProgramRefresh.matchesLegacyUpperHypertrophy(
            exercises.map { if (it.orderIndex == 3) it.copy(supersetGroupId = null) else it },
            seedKeysByExerciseId
        ))
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
        restSec: Int,
        protocol: Protocol = Protocol.STANDARD,
        supersetGroupId: Int? = null
    ) = ProgramExercise(
        userId = "u",
        programDayId = 1,
        orderIndex = orderIndex,
        exerciseId = exerciseId,
        sets = sets,
        repsMin = repsMin,
        repsMax = repsMax,
        restSec = restSec,
        protocol = protocol,
        supersetGroupId = supersetGroupId
    )
}
