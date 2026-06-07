package com.nicholasbergesen.gunsout.data.analysis

import com.nicholasbergesen.gunsout.data.entity.SessionStatus
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ExerciseSessionExportTest {

    @Test
    fun `export includes completed workout sets grouped by exercise`() {
        val session = workoutSession(id = 7, label = "Upper A")
        val sets = listOf(
            set(id = 2, sessionId = 7, setIndex = 2, reps = 8),
            set(id = 1, sessionId = 7, setIndex = 1, reps = 10)
        )

        val export = buildExerciseSessionsExport("2026-06-07T12:00:00", listOf(session to sets))

        assertEquals(1, export.schemaVersion)
        assertEquals("gunsout-exercise-sessions", export.format)
        assertEquals(1, export.sessions.size)
        val exportedSession = export.sessions.single()
        assertEquals("WORKOUT", exportedSession.sessionType)
        assertEquals("Upper A", exportedSession.dayLabel)
        assertEquals(1, exportedSession.exercises.size)
        assertEquals("Bench press", exportedSession.exercises.single().name)
        assertEquals(listOf(1L, 2L), exportedSession.exercises.single().sets.map { it.sourceSetId })
        assertEquals(listOf(10, 8), exportedSession.exercises.single().sets.map { it.reps })

        val encoded = Json.encodeToString(ExerciseSessionsExport.serializer(), export)
        assertTrue(encoded.contains("\"format\":\"gunsout-exercise-sessions\""))
    }

    @Test
    fun `export represents sessions with no sets as rest days while preserving label`() {
        val session = workoutSession(id = 8, label = "Recovery")

        val export = buildExerciseSessionsExport("2026-06-07T12:00:00", listOf(session to emptyList()))

        val exportedSession = export.sessions.single()
        assertEquals("REST", exportedSession.sessionType)
        assertEquals("Recovery", exportedSession.dayLabel)
        assertTrue(exportedSession.exercises.isEmpty())
    }

    private fun workoutSession(id: Long, label: String) = WorkoutSession(
        id = id,
        userId = "user",
        date = LocalDate.of(2026, 6, 7),
        programDayId = 1,
        programDayLabelSnapshot = label,
        status = SessionStatus.COMPLETED,
        startedAt = LocalDateTime.of(2026, 6, 7, 9, 0),
        completedAt = LocalDateTime.of(2026, 6, 7, 10, 0)
    )

    private fun set(id: Long, sessionId: Long, setIndex: Int, reps: Int) = SetEntry(
        id = id,
        userId = "user",
        sessionId = sessionId,
        programExerciseId = 99,
        exerciseIdSnapshot = 12,
        exerciseNameSnapshot = "Bench press",
        setIndex = setIndex,
        weightKg = 80.0,
        reps = reps,
        rpe = 8,
        isWarmup = false,
        completedAt = LocalDateTime.of(2026, 6, 7, 9, setIndex)
    )
}
