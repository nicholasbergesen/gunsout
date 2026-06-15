package com.nicholasbergesen.gunsout.data.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupImportCompletionTest {

    @Test
    fun `successful legacy backup import refreshes seeded program before reporting success`() = runTest {
        val legacyBackup = Json { ignoreUnknownKeys = true }.decodeFromString<GunsoutBackup>(
            """
            {
              "schemaVersion": 1,
              "exportedAtIso": "2026-06-01T00:00:00",
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
              "bodyMetricsLogs": []
            }
            """.trimIndent()
        )
        val events = mutableListOf<String>()

        val result = completeSuccessfulImportAfterSeedRefresh(
            userId = "current-user",
            totalRows = legacyBackup.importRowCount()
        ) { userId ->
            events += "refresh:$userId"
        }
        events += when (result) {
            is ImportResult.Success -> "reported:success"
            is ImportResult.Error -> "reported:error"
        }

        assertEquals(1, legacyBackup.schemaVersion)
        assertEquals(listOf("refresh:current-user", "reported:success"), events)
        assertEquals(ImportResult.Success(totalRows = 0), result)
    }

    @Test
    fun `post import refresh failure is reported instead of success`() = runTest {
        val result = completeSuccessfulImportAfterSeedRefresh(
            userId = "current-user",
            totalRows = 1
        ) {
            error("refresh failed")
        }

        assertTrue(result is ImportResult.Error)
        assertEquals(
            "Import completed but seeded program refresh failed: refresh failed",
            (result as ImportResult.Error).message
        )
    }

    @Test
    fun `post import refresh cancellation propagates`() = runTest {
        val cancellation = CancellationException("import screen left")

        try {
            completeSuccessfulImportAfterSeedRefresh(
                userId = "current-user",
                totalRows = 1
            ) {
                throw cancellation
            }
            fail("Expected refresh cancellation to propagate")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
    }
}
