package com.nicholasbergesen.gunsout.data.repo

import com.nicholasbergesen.gunsout.data.entity.SessionStatus
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WorkoutRepositoryCompleteSessionTest {

    @Test
    fun `empty completed session becomes a rest marker`() {
        val completedAt = LocalDateTime.of(2026, 6, 7, 10, 0)

        val updated = completedSessionUpdate(
            session = inProgressSession(),
            hasLoggedSets = false,
            completedAt = completedAt,
            kneeFeel = 3,
            notes = "Easy day",
            restLabel = "Rest"
        )

        assertNull(updated.programDayId)
        assertEquals("Rest", updated.programDayLabelSnapshot)
        assertEquals(SessionStatus.COMPLETED, updated.status)
        assertEquals(completedAt, updated.completedAt)
        assertEquals(3, updated.kneeFeel)
        assertEquals("Easy day", updated.notes)
    }

    @Test
    fun `completed session with logged sets preserves workout day`() {
        val completedAt = LocalDateTime.of(2026, 6, 7, 10, 0)

        val updated = completedSessionUpdate(
            session = inProgressSession(),
            hasLoggedSets = true,
            completedAt = completedAt,
            kneeFeel = null,
            notes = null,
            restLabel = "Rest"
        )

        assertEquals(1L, updated.programDayId)
        assertEquals("Upper A", updated.programDayLabelSnapshot)
        assertEquals(SessionStatus.COMPLETED, updated.status)
        assertEquals(completedAt, updated.completedAt)
    }

    private fun inProgressSession() = WorkoutSession(
        id = 7,
        userId = "user",
        date = LocalDate.of(2026, 6, 7),
        programDayId = 1,
        programDayLabelSnapshot = "Upper A",
        status = SessionStatus.IN_PROGRESS,
        startedAt = LocalDateTime.of(2026, 6, 7, 9, 0)
    )
}
