package com.nicholasbergesen.gunsout.data.backup

import com.nicholasbergesen.gunsout.ui.theme.ThemeStyle
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

        assertTrue(encoded.contains("\"schemaVersion\":5"))
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
        schemaVersion = 5,
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

    private fun profileBackup(themeStyle: String?) = UserProfileBackup(
        currentBodyWeightKg = 100.0,
        goalBodyWeightKg = 80.0,
        kneeInjuryFlag = true,
        baselineWeekActive = true,
        themeStyle = themeStyle,
        firstRunDone = true
    )
}
