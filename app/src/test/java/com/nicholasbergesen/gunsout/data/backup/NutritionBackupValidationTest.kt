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

    @Test
    fun `legacy validation rejects malformed retained nutrition fields`() {
        val creatine = legacyCreatine()
        val malformedProtein = legacyBackup().copy(
            foodEntries = listOf(legacyFood(date = "not-a-date", proteinG = 20.0))
        )
        val malformedReminder = legacyBackup().copy(
            supplements = listOf(creatine.copy(reminderTime = "25:00"))
        )
        val malformedCheck = legacyBackup().copy(
            supplements = listOf(creatine),
            supplementLogs = listOf(
                SupplementLogBackup(
                    id = 2,
                    supplementId = creatine.id,
                    date = "not-a-date",
                    doseTaken = 5.0,
                    unit = "G",
                    takenAt = "not-a-time"
                )
            )
        )

        assertEquals(
            "legacy protein entry date is invalid",
            malformedProtein.nutritionValidationError()
        )
        assertEquals(
            "legacy creatine reminder time is invalid",
            malformedReminder.nutritionValidationError()
        )
        assertEquals(
            "legacy creatine check date or time is invalid",
            malformedCheck.nutritionValidationError()
        )
    }

    @Test
    fun `legacy validation ignores malformed fields on discarded nutrition rows`() {
        val inactiveCreatine = legacyCreatine().copy(
            isActive = false,
            reminderTime = "invalid"
        )
        val unrelatedSupplement = legacyCreatine().copy(
            id = 2,
            seedKey = "vitamin_d",
            reminderTime = "invalid"
        )
        val backup = legacyBackup().copy(
            foodEntries = listOf(legacyFood(date = "not-a-date", proteinG = 0.0)),
            supplements = listOf(
                inactiveCreatine,
                unrelatedSupplement,
                legacyCreatine().copy(id = 4, reminderTime = "invalid")
            ),
            supplementLogs = listOf(
                SupplementLogBackup(
                    id = 3,
                    supplementId = unrelatedSupplement.id,
                    date = "not-a-date",
                    doseTaken = 1.0,
                    unit = "G",
                    takenAt = "not-a-time"
                )
            )
        )

        assertNull(backup.nutritionValidationError())
    }

    private fun legacyBackup() = emptyBackup(schemaVersion = 7)

    private fun legacyFood(date: String, proteinG: Double) = FoodEntryBackup(
        id = 1,
        date = date,
        mealType = "DINNER",
        name = "Dinner",
        kcal = 500,
        proteinG = proteinG,
        carbsG = 50.0,
        fatG = 10.0,
        createdAt = 1L
    )

    private fun legacyCreatine() = SupplementBackup(
        id = 1,
        name = "Creatine",
        defaultDose = 5.0,
        unit = "G",
        reminderTime = "09:30",
        isActive = true,
        isUserCreated = false,
        seedKey = LEGACY_CREATINE_SEED_KEY
    )

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
