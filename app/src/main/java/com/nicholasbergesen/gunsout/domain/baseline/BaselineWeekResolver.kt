package com.nicholasbergesen.gunsout.domain.baseline

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Decides whether the baseline week is active. Baseline week ends 7 calendar days after the active
 * program was first activated. The user can still force-override via [forcedFlag] in settings.
 *
 * Order of precedence:
 *  1. If the user has explicitly turned the toggle OFF in settings (forcedFlag = false), respect that.
 *  2. Otherwise, return true while the program is in its first 7 days, false after.
 *
 * The Settings toggle becomes a manual override rather than a thing the user has to remember to
 * flip on day 8.
 */
object BaselineWeekResolver {

    fun isActive(
        programCreatedAt: Long?,
        forcedFlag: Boolean,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        if (programCreatedAt == null) return forcedFlag

        val startDate = java.time.Instant.ofEpochMilli(programCreatedAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val daysIn = ChronoUnit.DAYS.between(startDate, today)
        val computed = daysIn in 0..6

        return if (!forcedFlag) false else computed
    }
}
