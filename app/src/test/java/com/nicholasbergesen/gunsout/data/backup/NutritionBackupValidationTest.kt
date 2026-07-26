package com.nicholasbergesen.gunsout.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionBackupValidationTest {
    @Test
    fun `valid v8 nutrition backup passes validation`() {
        val backup = emptyBackup().copy(
            proteinEntries = listOf(
                ProteinEntryBackup(1, "2026-07-26", 35, "Lunch", 123L)
            ),
            proteinTargetSnapshots = listOf(
                ProteinTargetSnapshotBackup("2026-07-26", 160)
            ),
            creatineSettings = CreatineSettingsBackup(5, "08:30"),
            creatineChecks = listOf(
                CreatineCheckBackup("2026-07-26", 5, "2026-07-26T08:31")
            ),
            targetOverrides = TargetOverridesBackup(kcal = 2500, proteinG = 170)
        )

        assertNull(backup.nutritionValidationError())
    }

    @Test
    fun `v8 validation rejects duplicate daily identities`() {
        val duplicateTargets = emptyBackup().copy(
            proteinTargetSnapshots = listOf(
                ProteinTargetSnapshotBackup("2026-07-26", 160),
                ProteinTargetSnapshotBackup("2026-07-26", 170)
            )
        )
        val duplicateChecks = emptyBackup().copy(
            creatineChecks = listOf(
                CreatineCheckBackup("2026-07-26", 5, "2026-07-26T08:31"),
                CreatineCheckBackup("2026-07-26", 5, "2026-07-26T09:00")
            )
        )

        assertEquals(
            "protein target dates must be unique",
            duplicateTargets.nutritionValidationError()
        )
        assertEquals(
            "creatine check dates must be unique",
            duplicateChecks.nutritionValidationError()
        )
    }

    @Test
    fun `v8 validation rejects invalid values and encodings`() {
        assertEquals(
            "protein grams must be positive",
            emptyBackup().copy(
                proteinEntries = listOf(
                    ProteinEntryBackup(1, "2026-07-26", 0, null, 1L)
                )
            ).nutritionValidationError()
        )
        assertEquals(
            "creatine reminder time is invalid",
            emptyBackup().copy(
                creatineSettings = CreatineSettingsBackup(5, "25:00")
            ).nutritionValidationError()
        )
        assertEquals(
            "protein override must be positive",
            emptyBackup().copy(
                targetOverrides = TargetOverridesBackup(proteinG = -1)
            ).nutritionValidationError()
        )
    }

    @Test
    fun `legacy backups remain governed by conversion rules`() {
        val legacy = emptyBackup(schemaVersion = 7).copy(
            foodEntries = listOf(
                FoodEntryBackup(
                    id = 1,
                    date = "2026-07-26",
                    mealType = "DINNER",
                    name = "Dinner",
                    kcal = 500,
                    proteinG = 0.0,
                    carbsG = 50.0,
                    fatG = 10.0,
                    createdAt = 1L
                )
            )
        )

        assertNull(legacy.nutritionValidationError())
    }

    private fun emptyBackup(schemaVersion: Int = 8) = GunsoutBackup(
        schemaVersion = schemaVersion,
        exportedAtIso = "2026-07-26T00:00:00",
        programs = emptyList(),
        programDays = emptyList(),
        exercises = emptyList(),
        exerciseAlternates = emptyList(),
        programExercises = emptyList(),
        sessions = emptyList(),
        setEntries = emptyList(),
        bodyMetricsLogs = emptyList()
    )
}
