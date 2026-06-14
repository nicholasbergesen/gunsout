package com.nicholasbergesen.gunsout.data.backup

import com.nicholasbergesen.gunsout.ui.theme.ThemeStyle
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.prefs.TrainingExperience
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupThemeStyleTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test fun userProfileBackup_defaultsMissingThemeStyleToGunmetal() {
        val profile = profileBackup(themeStyle = null).toUserProfile()

        assertEquals(ThemeStyle.GUNMETAL_CRIMSON, profile.themeStyle)
    }

    @Test fun userProfileBackup_defaultsUnknownThemeStyleToGunmetal() {
        val profile = profileBackup(themeStyle = "OLD_THEME").toUserProfile()

        assertEquals(ThemeStyle.GUNMETAL_CRIMSON, profile.themeStyle)
    }

    @Test fun userProfileBackup_restoresKnownThemeStyle() {
        val profile = profileBackup(themeStyle = ThemeStyle.VIBRANT_GRADIENT.name).toUserProfile()

        assertEquals(ThemeStyle.VIBRANT_GRADIENT, profile.themeStyle)
    }

    @Test fun backupJson_encodesSchemaVersionAndThemeStyle() {
        val encoded = json.encodeToString(emptyBackup(ThemeStyle.SOFT_PASTEL))

        assertTrue(encoded.contains("\"schemaVersion\":6"))
        assertTrue(encoded.contains("\"themeStyle\":\"SOFT_PASTEL\""))
        assertFalse(encoded.contains("\"themeMode\""))
    }

    @Test fun legacyV3BackupWithoutThemeStyleStillDecodes() {
        val decoded = json.decodeFromString<GunsoutBackup>(
            """
            {
              "schemaVersion": 3,
              "exportedAtIso": "2026-05-30T00:00:00Z",
              "programs": [],
              "programDays": [],
              "exercises": [],
              "exerciseAlternates": [],
              "programExercises": [],
              "sessions": [],
              "setEntries": [],
              "mealTemplates": [],
              "foodEntries": [],
              "supplements": [],
              "supplementLogs": [],
              "bodyMetricsLogs": [],
              "userProfile": {
                "currentBodyWeightKg": 100.0,
                "goalBodyWeightKg": 80.0,
                "kneeInjuryFlag": true,
                "baselineWeekActive": true,
                "themeMode": "SYSTEM",
                "firstRunDone": true
              }
            }
            """.trimIndent()
        )

        assertEquals(3, decoded.schemaVersion)
        assertNull(decoded.userProfile?.themeStyle)
        assertEquals(ThemeStyle.GUNMETAL_CRIMSON, decoded.userProfile!!.toUserProfile().themeStyle)
        assertEquals(TrainingExperience.BEGINNER, decoded.userProfile!!.toUserProfile().trainingExperience)
        assertFalse(decoded.userProfile!!.toUserProfile().profileSetupDone)
        assertEquals(0, decoded.userProfile!!.toUserProfile().defaultProgramRefreshVersion)
    }

    @Test fun userProfileBackup_restoresDefaultProgramRefreshVersion() {
        val profile = profileBackup(themeStyle = null, defaultProgramRefreshVersion = 1).toUserProfile()

        assertEquals(1, profile.defaultProgramRefreshVersion)
    }

    @Test fun legacyExerciseWithoutMovementPatternDefaultsFromMuscleGroup() {
        val decoded = json.decodeFromString<ExerciseBackup>(
            """
            {
              "id": 1,
              "name": "Legacy Row",
              "primaryMuscleGroup": "BACK",
              "equipment": "CABLE",
              "defaultRestSec": 90,
              "isUserCreated": false,
              "isArchived": false,
              "seedKey": "legacy"
            }
            """.trimIndent()
        )

        assertEquals(MovementPattern.PULL, decoded.toEntity("u").movementPattern)
    }

    @Test fun legacySeededIsolationExerciseWithoutMovementPatternBackfillsSeedPattern() {
        val decoded = json.decodeFromString<ExerciseBackup>(
            """
            {
              "id": 1,
              "name": "Leg Extensions",
              "primaryMuscleGroup": "QUADS",
              "equipment": "MACHINE",
              "defaultRestSec": 60,
              "isUserCreated": false,
              "isArchived": false,
              "seedKey": "leg_extension"
            }
            """.trimIndent()
        )

        assertEquals(MovementPattern.ISOLATION, decoded.toEntity("u").movementPattern)
    }

    @Test fun legacyBodyMetricsWaterPercentRestoresAsLiters() {
        val restored = BodyMetricsLogBackup(
            id = 1,
            date = "2026-06-01",
            weightKg = 80.0,
            waterPct = 55.0
        ).toEntity("user")

        assertEquals(44.0, restored.waterLiters!!, 0.001)
    }

    private fun emptyBackup(themeStyle: ThemeStyle) = GunsoutBackup(
        schemaVersion = 6,
        exportedAtIso = "2026-05-30T00:00:00Z",
        programs = emptyList(),
        programDays = emptyList(),
        exercises = emptyList(),
        exerciseAlternates = emptyList(),
        programExercises = emptyList(),
        sessions = emptyList(),
        setEntries = emptyList(),
        mealTemplates = emptyList(),
        foodEntries = emptyList(),
        supplements = emptyList(),
        supplementLogs = emptyList(),
        bodyMetricsLogs = emptyList(),
        userProfile = profileBackup(themeStyle = themeStyle.name)
    )

    private fun profileBackup(
        themeStyle: String?,
        defaultProgramRefreshVersion: Int = 0
    ) = UserProfileBackup(
        currentBodyWeightKg = 100.0,
        goalBodyWeightKg = 80.0,
        kneeInjuryFlag = true,
        baselineWeekActive = true,
        themeStyle = themeStyle,
        firstRunDone = true,
        defaultProgramRefreshVersion = defaultProgramRefreshVersion
    )
}
