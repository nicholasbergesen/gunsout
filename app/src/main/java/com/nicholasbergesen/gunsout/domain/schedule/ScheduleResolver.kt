package com.nicholasbergesen.gunsout.domain.schedule

import com.nicholasbergesen.gunsout.data.entity.DayHint
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Decides what session is next given a program, recent sessions, and today's date.
 *
 * Rotation rule: the active program defines ordered, non-rest ProgramDays.
 * The "rotation pointer" is just (lastCompletedOrder + 1) modulo count.
 * If the user has never completed a session, pointer is 0.
 *
 * The day-of-week hint (preferredDayOfWeek) is informational only.
 */
class ScheduleResolver {

    data class Suggestion(
        /** The next program day to do, or null if the program has no non-rest days. */
        val nextDay: ProgramDay?,
        /** Whether today's day-of-week matches the next day's preferred hint. */
        val onSchedule: Boolean,
        /** Days since the most recent completed session (null if never trained). */
        val daysSinceLastSession: Int?,
        /** If today's day-of-week matches a different non-rest day in the program, suggest that too. */
        val alternativeForToday: ProgramDay?
    )

    fun resolveNext(
        allDays: List<ProgramDay>,
        recentSessions: List<WorkoutSession>,
        today: LocalDate
    ): Suggestion {
        val nonRest = allDays.filter { !it.isRest }.sortedBy { it.orderIndex }
        if (nonRest.isEmpty()) {
            return Suggestion(nextDay = null, onSchedule = false, daysSinceLastSession = null, alternativeForToday = null)
        }

        // Find the last session that actually belongs to the rotation. A session whose programDayId
        // is null (e.g. explicit "mark today as rest") or points at a rest day must NOT advance or
        // reset the rotation pointer. See WorkoutRepository.markRestDay for the writer side.
        val nonRestIds = nonRest.map { it.id }.toSet()
        val lastCompleted = recentSessions.firstOrNull {
            (it.status == com.nicholasbergesen.gunsout.data.entity.SessionStatus.COMPLETED || it.status == com.nicholasbergesen.gunsout.data.entity.SessionStatus.SKIPPED) &&
                it.programDayId != null &&
                it.programDayId in nonRestIds
        }
        val nextDay = if (lastCompleted == null) {
            nonRest.first()
        } else {
            val lastIndexInRotation = nonRest.indexOfFirst { it.id == lastCompleted.programDayId }
            if (lastIndexInRotation < 0) nonRest.first()
            else nonRest[(lastIndexInRotation + 1) % nonRest.size]
        }

        val todayHint = today.dayOfWeek.toHint()
        val onSchedule = nextDay.preferredDayOfWeek == todayHint
        val alt = if (onSchedule) null else {
            allDays.firstOrNull { !it.isRest && it.preferredDayOfWeek == todayHint && it.id != nextDay.id }
        }

        // Days-since calculation uses any completed session (including a rest marker) so the UI can
        // honestly report when the user last did anything.
        val daysSince = recentSessions.firstOrNull { it.status == com.nicholasbergesen.gunsout.data.entity.SessionStatus.COMPLETED }
            ?.let { java.time.temporal.ChronoUnit.DAYS.between(it.date, today).toInt() }

        return Suggestion(nextDay, onSchedule, daysSince, alt)
    }

    private fun DayOfWeek.toHint(): DayHint = when (this) {
        DayOfWeek.MONDAY -> DayHint.MON
        DayOfWeek.TUESDAY -> DayHint.TUE
        DayOfWeek.WEDNESDAY -> DayHint.WED
        DayOfWeek.THURSDAY -> DayHint.THU
        DayOfWeek.FRIDAY -> DayHint.FRI
        DayOfWeek.SATURDAY -> DayHint.SAT
        DayOfWeek.SUNDAY -> DayHint.SUN
    }
}
