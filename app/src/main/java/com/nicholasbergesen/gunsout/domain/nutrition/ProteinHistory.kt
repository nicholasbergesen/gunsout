package com.nicholasbergesen.gunsout.domain.nutrition

import java.time.LocalDate
import java.time.YearMonth

enum class ProteinHistoryRange { WEEK, MONTH, YEAR }

data class ProteinDayRecord(val date: LocalDate, val grams: Int)

data class ProteinTargetRecord(val date: LocalDate, val grams: Int)

data class ProteinHistoryPoint(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val loggedGrams: Double?,
    val targetGrams: Double?,
    val loggedDays: Int
)

data class ProteinHistorySeries(
    val range: ProteinHistoryRange,
    val points: List<ProteinHistoryPoint>
)

object ProteinHistory {
    fun aggregate(
        range: ProteinHistoryRange,
        today: LocalDate,
        records: List<ProteinDayRecord>,
        targets: List<ProteinTargetRecord>
    ): ProteinHistorySeries {
        val dailyTotals = records
            .asSequence()
            .filter { it.grams > 0 }
            .groupBy(ProteinDayRecord::date)
            .mapValues { (_, values) -> values.sumOf { it.grams.toLong() }.toDouble() }
        val targetsByDate = targets
            .asSequence()
            .filter { it.grams > 0 }
            .associate { it.date to it.grams.toDouble() }

        val points = when (range) {
            ProteinHistoryRange.WEEK ->
                dailyPoints(today.minusDays(6), today, dailyTotals, targetsByDate)
            ProteinHistoryRange.MONTH ->
                dailyPoints(today.minusDays(29), today, dailyTotals, targetsByDate)
            ProteinHistoryRange.YEAR ->
                monthlyPoints(today, dailyTotals, targetsByDate)
        }
        return ProteinHistorySeries(range, points)
    }

    private fun dailyPoints(
        start: LocalDate,
        end: LocalDate,
        totals: Map<LocalDate, Double>,
        targets: Map<LocalDate, Double>
    ): List<ProteinHistoryPoint> =
        generateSequence(start) { date -> date.plusDays(1).takeUnless { it.isAfter(end) } }
            .map { date ->
                val logged = totals[date]
                ProteinHistoryPoint(
                    startDate = date,
                    endDate = date,
                    loggedGrams = logged,
                    targetGrams = logged?.let { targets[date] },
                    loggedDays = if (logged == null) 0 else 1
                )
            }
            .toList()

    private fun monthlyPoints(
        today: LocalDate,
        totals: Map<LocalDate, Double>,
        targets: Map<LocalDate, Double>
    ): List<ProteinHistoryPoint> {
        val currentMonth = YearMonth.from(today)
        return (11 downTo 0).map { monthsAgo ->
            val month = currentMonth.minusMonths(monthsAgo.toLong())
            val start = month.atDay(1)
            val end = minOf(month.atEndOfMonth(), today)
            val loggedDates = totals.keys
                .filter { date -> !date.isBefore(start) && !date.isAfter(end) }
                .sorted()
            val loggedAverage = loggedDates
                .takeIf { it.isNotEmpty() }
                ?.map { date -> totals.getValue(date) }
                ?.average()
            val targetAverage = loggedDates
                .takeIf { dates -> dates.isNotEmpty() && dates.all(targets::containsKey) }
                ?.map { date -> targets.getValue(date) }
                ?.average()
            ProteinHistoryPoint(
                startDate = start,
                endDate = end,
                loggedGrams = loggedAverage,
                targetGrams = targetAverage,
                loggedDays = loggedDates.size
            )
        }
    }
}
