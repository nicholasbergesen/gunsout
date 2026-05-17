package com.gunsout.domain.schedule

import com.gunsout.data.entity.DayHint
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.SessionStatus
import com.gunsout.data.entity.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleResolverTest {

    private val resolver = ScheduleResolver()

    private val days = listOf(
        ProgramDay(id = 1, programId = 1, orderIndex = 0, label = "Upper A", preferredDayOfWeek = DayHint.MON),
        ProgramDay(id = 2, programId = 1, orderIndex = 1, label = "Lower A", preferredDayOfWeek = DayHint.TUE),
        ProgramDay(id = 3, programId = 1, orderIndex = 2, label = "Rest", preferredDayOfWeek = DayHint.WED, isRest = true),
        ProgramDay(id = 4, programId = 1, orderIndex = 3, label = "Upper B", preferredDayOfWeek = DayHint.THU),
        ProgramDay(id = 5, programId = 1, orderIndex = 4, label = "Lower B", preferredDayOfWeek = DayHint.FRI)
    )

    private fun session(programDayId: Long, status: SessionStatus, date: LocalDate) =
        WorkoutSession(
            id = programDayId * 10, date = date, programDayId = programDayId,
            programDayLabelSnapshot = "L", status = status,
            startedAt = LocalDateTime.now(),
            completedAt = if (status == SessionStatus.COMPLETED) LocalDateTime.now() else null
        )

    @Test fun `no history returns first non-rest day`() {
        val today = LocalDate.of(2026, 5, 18) // Monday
        val s = resolver.resolveNext(days, recentSessions = emptyList(), today)
        assertEquals(1L, s.nextDay?.id)
        assertTrue(s.onSchedule)
        assertNull(s.daysSinceLastSession)
    }

    @Test fun `after Upper A completed next is Lower A`() {
        val today = LocalDate.of(2026, 5, 19) // Tuesday
        val sessions = listOf(session(1, SessionStatus.COMPLETED, today.minusDays(1)))
        val s = resolver.resolveNext(days, sessions, today)
        assertEquals(2L, s.nextDay?.id)
        assertTrue(s.onSchedule)
        assertEquals(1, s.daysSinceLastSession)
    }

    @Test fun `after the last non-rest day rotation wraps`() {
        val today = LocalDate.of(2026, 5, 25) // Monday again
        val sessions = listOf(session(5, SessionStatus.COMPLETED, today.minusDays(3)))
        val s = resolver.resolveNext(days, sessions, today)
        assertEquals(1L, s.nextDay?.id)
        assertTrue(s.onSchedule)
    }

    @Test fun `skipped session still advances the pointer`() {
        val today = LocalDate.of(2026, 5, 20) // Wednesday
        val sessions = listOf(session(2, SessionStatus.SKIPPED, today.minusDays(1)))
        val s = resolver.resolveNext(days, sessions, today)
        assertEquals(4L, s.nextDay?.id) // Upper B
    }

    @Test fun `off-schedule day returns alternativeForToday`() {
        val today = LocalDate.of(2026, 5, 21) // Thursday
        // Last completed was Upper A on Monday: next is Lower A. Today is Thursday whose hint = Upper B.
        val sessions = listOf(session(1, SessionStatus.COMPLETED, today.minusDays(3)))
        val s = resolver.resolveNext(days, sessions, today)
        assertEquals(2L, s.nextDay?.id) // Lower A is what's next-due
        assertFalse(s.onSchedule)
        assertNotNull(s.alternativeForToday)
        assertEquals(4L, s.alternativeForToday?.id) // today's hint matches Upper B
    }

    @Test fun `empty non-rest set returns null nextDay`() {
        val onlyRest = listOf(ProgramDay(id = 1, programId = 1, orderIndex = 0, label = "Rest", isRest = true))
        val s = resolver.resolveNext(onlyRest, emptyList(), LocalDate.of(2026, 5, 18))
        assertNull(s.nextDay)
    }

    @Test fun `missed many days still suggests next without auto-skipping`() {
        val today = LocalDate.of(2026, 6, 1)
        val sessions = listOf(session(1, SessionStatus.COMPLETED, today.minusDays(14)))
        val s = resolver.resolveNext(days, sessions, today)
        assertEquals(2L, s.nextDay?.id)
        assertEquals(14, s.daysSinceLastSession)
    }
}
