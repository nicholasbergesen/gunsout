package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.ProgramType
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

    @Test fun `seeded template metadata backfill preserves legacy notes on renamed upper lower template`() {
        val renamedProgram = Program(
            userId = "u",
            name = "My upper lower plan",
            type = ProgramType.CUSTOM,
            notes = "Balanced free-weight strength and hypertrophy across four weekly lifting days.",
            isTemplate = true,
            seedKey = ProgramSeeds.upperLower4Day.seedKey
        )

        val backfilled = Seeder.SeededProgramRefresh.backfillSeededTemplateMetadata(
            renamedProgram,
            ProgramSeeds.upperLower4Day
        )

        assertEquals("My upper lower plan", backfilled.name)
        assertEquals("Balanced free-weight strength and hypertrophy across four weekly lifting days.", backfilled.notes)
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

    @Test fun `seeded exercise refresh inserts before unchanged rows without reusing shifted identities`() {
        val existing = listOf(
            pe(id = 101, orderIndex = 0, exerciseId = 10, sets = 3, repsMin = 8, repsMax = 10, restSec = 90),
            pe(
                id = 102,
                orderIndex = 1,
                exerciseId = 11,
                sets = 5,
                repsMin = 2,
                repsMax = 3,
                restSec = 120,
                protocol = Protocol.PULL_UP_5X2_3
            ),
            pe(id = 103, orderIndex = 2, exerciseId = 12, sets = 3, repsMin = 10, repsMax = 10, restSec = 60),
            pe(id = 104, orderIndex = 3, exerciseId = 13, sets = 3, repsMin = 15, repsMax = 15, restSec = 60)
        )
        val refresh = Seeder.SeededProgramRefresh.buildProgramExerciseRefreshPlan(
            userId = "u",
            programDayId = 1,
            existingExercises = existing,
            seedKeysByExerciseId = mapOf(
                10L to "inc_db_bench",
                11L to "pull_ups",
                12L to "db_shoulder_press",
                13L to "db_rear_delt_fly"
            ),
            exerciseIdsBySeedKey = mapOf(
                "inc_db_bench" to 10L,
                "pull_ups" to 11L,
                "lat_pulldown" to 20L,
                "db_shoulder_press" to 12L,
                "db_rear_delt_fly" to 13L
            ),
            planExercises = ProgramSeeds.upperLower4Day.days
                .single { it.orderIndex == Seeder.SeededProgramRefresh.upperPushPullOrder }
                .exercises,
            staleProgramExerciseIdsToRetire = emptySet()
        ) ?: error("Expected refresh plan")

        assertEquals(listOf(101L, 102L, 0L, 103L, 104L), refresh.plannedRows.map { it.id })
        assertEquals(listOf(10L, 11L, 20L, 12L, 13L), refresh.plannedRows.map { it.exerciseId })
        assertEquals(listOf(0, 1, 2, 3, 4), refresh.plannedRows.map { it.orderIndex })
        assertFalse(refresh.plannedRows.single { it.exerciseId == 20L }.id in existing.map { it.id })
        assertTrue(refresh.staleRowsToRetire.isEmpty())
        assertTrue(refresh.staleRowIdsToDelete.isEmpty())
    }

    @Test fun `seeded exercise refresh retires completed-history and in-progress stale rows instead of leaving them active`() {
        val existing = listOf(
            pe(id = 201, orderIndex = 0, exerciseId = 10, sets = 3, repsMin = 10, repsMax = 12, restSec = 60),
            pe(id = 202, orderIndex = 1, exerciseId = 11, sets = 3, repsMin = 10, repsMax = 10, restSec = 90),
            pe(id = 203, orderIndex = 2, exerciseId = 12, sets = 3, repsMin = 12, repsMax = 12, restSec = 90),
            pe(id = 204, orderIndex = 3, exerciseId = 13, sets = 3, repsMin = 15, repsMax = 15, restSec = 60)
        )
        val seedKeysByExerciseId = mapOf(
            10L to "leg_curl",
            11L to "goblet_squat",
            12L to "hip_thrust",
            13L to "lying_leg_raise"
        )
        val exerciseIdsBySeedKey = mapOf(
            "leg_curl" to 10L,
            "goblet_squat" to 11L,
            "trap_bar_deadlift" to 20L,
            "machine_crunch" to 21L
        )
        val planExercises = ProgramSeeds.upperLower4Day.days
            .single { it.orderIndex == Seeder.SeededProgramRefresh.lowerPosteriorCoreOrder }
            .exercises
        val keptStaleRefresh = Seeder.SeededProgramRefresh.buildProgramExerciseRefreshPlan(
            userId = "u",
            programDayId = 1,
            existingExercises = existing,
            seedKeysByExerciseId = seedKeysByExerciseId,
            exerciseIdsBySeedKey = exerciseIdsBySeedKey,
            planExercises = planExercises,
            staleProgramExerciseIdsToRetire = setOf(203L, 204L)
        ) ?: error("Expected refresh plan")

        assertEquals(listOf(201L, 202L, 0L, 0L), keptStaleRefresh.plannedRows.map { it.id })
        assertEquals(listOf(10L, 11L, 20L, 21L), keptStaleRefresh.plannedRows.map { it.exerciseId })
        assertEquals(listOf(0, 1, 2, 3), keptStaleRefresh.plannedRows.map { it.orderIndex })
        assertFalse(keptStaleRefresh.plannedRows.drop(2).any { it.id in setOf(203L, 204L) })
        assertEquals(listOf(203L, 204L), keptStaleRefresh.staleRowsToRetire.map { it.id })
        assertEquals(listOf(2, 3), keptStaleRefresh.staleRowsToRetire.map { it.orderIndex })
        assertTrue(keptStaleRefresh.staleRowsToRetire.all { it.isRetired })
        assertTrue(keptStaleRefresh.staleRowIdsToDelete.isEmpty())

        val unreferencedStaleRefresh = Seeder.SeededProgramRefresh.buildProgramExerciseRefreshPlan(
            userId = "u",
            programDayId = 1,
            existingExercises = existing,
            seedKeysByExerciseId = seedKeysByExerciseId,
            exerciseIdsBySeedKey = exerciseIdsBySeedKey,
            planExercises = planExercises,
            staleProgramExerciseIdsToRetire = emptySet()
        ) ?: error("Expected refresh plan")

        assertTrue(unreferencedStaleRefresh.staleRowsToRetire.isEmpty())
        assertEquals(listOf(203L, 204L), unreferencedStaleRefresh.staleRowIdsToDelete)
    }

    @Test fun `already refreshed lower posterior core with legacy snapshots gets retired legacy prescriptions`() {
        val existing = listOf(
            pe(id = 301, orderIndex = 0, exerciseId = 10, sets = 3, repsMin = 10, repsMax = 12, restSec = 60),
            pe(id = 302, orderIndex = 1, exerciseId = 11, sets = 3, repsMin = 10, repsMax = 10, restSec = 90),
            pe(id = 303, orderIndex = 2, exerciseId = 20, sets = 3, repsMin = 8, repsMax = 10, restSec = 150),
            pe(id = 304, orderIndex = 3, exerciseId = 21, sets = 3, repsMin = 10, repsMax = 15, restSec = 60)
        )
        val seedKeysByExerciseId = mapOf(
            10L to "leg_curl",
            11L to "goblet_squat",
            20L to "trap_bar_deadlift",
            21L to "machine_crunch"
        )
        val planExercises = ProgramSeeds.upperLower4Day.days
            .single { it.orderIndex == Seeder.SeededProgramRefresh.lowerPosteriorCoreOrder }
            .exercises

        assertFalse(Seeder.SeededProgramRefresh.matchesLegacyLowerPosteriorCore(existing, seedKeysByExerciseId))
        assertTrue(Seeder.SeededProgramRefresh.matchesV1RefreshedLowerPosteriorCore(existing, seedKeysByExerciseId))
        val refreshState = Seeder.SeededProgramRefresh.refreshStateForProgramDay(
            order = Seeder.SeededProgramRefresh.lowerPosteriorCoreOrder,
            exercises = existing,
            seedKeysByExerciseId = seedKeysByExerciseId
        ) ?: error("Expected v1 repair state")
        val refresh = Seeder.SeededProgramRefresh.buildProgramExerciseRefreshPlan(
            userId = "u",
            programDayId = 1,
            existingExercises = existing,
            seedKeysByExerciseId = seedKeysByExerciseId,
            exerciseIdsBySeedKey = mapOf(
                "leg_curl" to 10L,
                "goblet_squat" to 11L,
                "trap_bar_deadlift" to 20L,
                "machine_crunch" to 21L
            ),
            planExercises = planExercises,
            staleProgramExerciseIdsToRetire = setOf(303L, 304L),
            existingPlannedSeedKeysToReplace = refreshState.existingPlannedSeedKeysToReplace,
            snapshotSeedKeysByProgramExerciseId = mapOf(
                303L to setOf("hip_thrust"),
                304L to setOf("lying_leg_raise")
            )
        ) ?: error("Expected refresh plan")

        assertEquals(Seeder.SeededProgramRefresh.lowerPosteriorCoreV1ReplacedSeedKeys, refreshState.existingPlannedSeedKeysToReplace)
        assertEquals(listOf(301L, 302L, 0L, 0L), refresh.plannedRows.map { it.id })
        assertEquals(listOf(10L, 11L, 20L, 21L), refresh.plannedRows.map { it.exerciseId })
        assertEquals(listOf(303L, 304L), refresh.staleRowsToRetire.map { it.id })
        assertEquals(listOf(3, 3), refresh.staleRowsToRetire.map { it.sets })
        assertEquals(listOf(12, 15), refresh.staleRowsToRetire.map { it.repsMin })
        assertEquals(listOf(12, 15), refresh.staleRowsToRetire.map { it.repsMax })
        assertEquals(listOf(90, 60), refresh.staleRowsToRetire.map { it.restSec })
        assertTrue(refresh.staleRowsToRetire.all { it.isRetired })
        assertTrue(refresh.staleRowIdsToDelete.isEmpty())
    }

    @Test fun `already refreshed lower posterior core with replacement snapshots preserves replacement prescriptions`() {
        val existing = listOf(
            pe(id = 301, orderIndex = 0, exerciseId = 10, sets = 3, repsMin = 10, repsMax = 12, restSec = 60),
            pe(id = 302, orderIndex = 1, exerciseId = 11, sets = 3, repsMin = 10, repsMax = 10, restSec = 90),
            pe(id = 303, orderIndex = 2, exerciseId = 20, sets = 3, repsMin = 8, repsMax = 10, restSec = 150),
            pe(id = 304, orderIndex = 3, exerciseId = 21, sets = 3, repsMin = 10, repsMax = 15, restSec = 60)
        )
        val seedKeysByExerciseId = mapOf(
            10L to "leg_curl",
            11L to "goblet_squat",
            20L to "trap_bar_deadlift",
            21L to "machine_crunch"
        )
        val refreshState = Seeder.SeededProgramRefresh.refreshStateForProgramDay(
            order = Seeder.SeededProgramRefresh.lowerPosteriorCoreOrder,
            exercises = existing,
            seedKeysByExerciseId = seedKeysByExerciseId
        ) ?: error("Expected v1 repair state")
        val refresh = Seeder.SeededProgramRefresh.buildProgramExerciseRefreshPlan(
            userId = "u",
            programDayId = 1,
            existingExercises = existing,
            seedKeysByExerciseId = seedKeysByExerciseId,
            exerciseIdsBySeedKey = mapOf(
                "leg_curl" to 10L,
                "goblet_squat" to 11L,
                "trap_bar_deadlift" to 20L,
                "machine_crunch" to 21L
            ),
            planExercises = ProgramSeeds.upperLower4Day.days
                .single { it.orderIndex == Seeder.SeededProgramRefresh.lowerPosteriorCoreOrder }
                .exercises,
            staleProgramExerciseIdsToRetire = setOf(303L, 304L),
            existingPlannedSeedKeysToReplace = refreshState.existingPlannedSeedKeysToReplace,
            snapshotSeedKeysByProgramExerciseId = mapOf(
                303L to setOf("trap_bar_deadlift"),
                304L to setOf("machine_crunch")
            )
        ) ?: error("Expected refresh plan")

        assertEquals(listOf(303L, 304L), refresh.staleRowsToRetire.map { it.id })
        assertEquals(listOf(3, 3), refresh.staleRowsToRetire.map { it.sets })
        assertEquals(listOf(8, 10), refresh.staleRowsToRetire.map { it.repsMin })
        assertEquals(listOf(10, 15), refresh.staleRowsToRetire.map { it.repsMax })
        assertEquals(listOf(150, 60), refresh.staleRowsToRetire.map { it.restSec })
        assertTrue(refresh.staleRowsToRetire.all { it.isRetired })
    }

    @Test fun `mixed snapshots do not prove a v1 replacement row should use legacy prescription`() {
        val existing = listOf(
            pe(id = 301, orderIndex = 0, exerciseId = 10, sets = 3, repsMin = 10, repsMax = 12, restSec = 60),
            pe(id = 302, orderIndex = 1, exerciseId = 11, sets = 3, repsMin = 10, repsMax = 10, restSec = 90),
            pe(id = 303, orderIndex = 2, exerciseId = 20, sets = 3, repsMin = 8, repsMax = 10, restSec = 150),
            pe(id = 304, orderIndex = 3, exerciseId = 21, sets = 3, repsMin = 10, repsMax = 15, restSec = 60)
        )
        val seedKeysByExerciseId = mapOf(
            10L to "leg_curl",
            11L to "goblet_squat",
            20L to "trap_bar_deadlift",
            21L to "machine_crunch"
        )
        val refreshState = Seeder.SeededProgramRefresh.refreshStateForProgramDay(
            order = Seeder.SeededProgramRefresh.lowerPosteriorCoreOrder,
            exercises = existing,
            seedKeysByExerciseId = seedKeysByExerciseId
        ) ?: error("Expected v1 repair state")
        val refresh = Seeder.SeededProgramRefresh.buildProgramExerciseRefreshPlan(
            userId = "u",
            programDayId = 1,
            existingExercises = existing,
            seedKeysByExerciseId = seedKeysByExerciseId,
            exerciseIdsBySeedKey = mapOf(
                "leg_curl" to 10L,
                "goblet_squat" to 11L,
                "trap_bar_deadlift" to 20L,
                "machine_crunch" to 21L
            ),
            planExercises = ProgramSeeds.upperLower4Day.days
                .single { it.orderIndex == Seeder.SeededProgramRefresh.lowerPosteriorCoreOrder }
                .exercises,
            staleProgramExerciseIdsToRetire = setOf(303L, 304L),
            existingPlannedSeedKeysToReplace = refreshState.existingPlannedSeedKeysToReplace,
            snapshotSeedKeysByProgramExerciseId = mapOf(
                303L to setOf("hip_thrust", "trap_bar_deadlift"),
                304L to setOf("lying_leg_raise")
            )
        ) ?: error("Expected refresh plan")

        assertEquals(listOf(303L, 304L), refresh.staleRowsToRetire.map { it.id })
        assertEquals(listOf(8, 15), refresh.staleRowsToRetire.map { it.repsMin })
        assertEquals(listOf(10, 15), refresh.staleRowsToRetire.map { it.repsMax })
        assertEquals(listOf(150, 60), refresh.staleRowsToRetire.map { it.restSec })
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
        supersetGroupId: Int? = null,
        id: Long = 0
    ) = ProgramExercise(
        id = id,
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
