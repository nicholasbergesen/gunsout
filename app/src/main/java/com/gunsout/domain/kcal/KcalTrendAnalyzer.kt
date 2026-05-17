package com.gunsout.domain.kcal

import com.gunsout.data.entity.BodyMetricsLog
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Suggests a kcal-target adjustment based on recent body weight trend.
 *
 * Method: linear regression over all weigh-ins within the last [windowDays]. The slope (kg/day)
 * scaled to kg/week is robust to one or two noisy weigh-ins because the regression considers every
 * point — a single outlier moves the slope only slightly when the rest of the points stay on a
 * line.
 *
 * Rules:
 *  - Needs at least 4 logs spanning at least 7 days inside [windowDays].
 *  - Cut goal (current > goal + 1 kg): target 0.3-1.2 kg/week loss.
 *  - Bulk goal (current < goal - 1 kg): target 0.1-0.5 kg/week gain.
 *  - Otherwise: hold.
 *
 * Suggested target is rounded to the nearest 50 kcal and floored at 1200 kcal.
 */
object KcalTrendAnalyzer {

    enum class Direction { CUT, BULK, MAINTAIN }

    data class Suggestion(
        val text: String,
        val newKcalTarget: Int?,
        val ratePerWeekKg: Double?
    )

    fun analyze(
        logs: List<BodyMetricsLog>,
        currentTargetKcal: Int,
        currentWeightKg: Double,
        goalWeightKg: Double,
        windowDays: Int = 14
    ): Suggestion {
        if (logs.size < 4) {
            return Suggestion("Need at least 4 weigh-ins to suggest an adjustment.", null, null)
        }
        val sorted = logs.sortedBy { it.date }
        val last = sorted.last().date
        val cutoff = last.minusDays(windowDays.toLong())
        val windowed = sorted.filter { it.date >= cutoff }
        if (windowed.size < 4) {
            return Suggestion("Not enough recent weigh-ins in the last $windowDays days.", null, null)
        }
        val spanDays = ChronoUnit.DAYS.between(windowed.first().date, windowed.last().date)
        if (spanDays < 7) {
            return Suggestion("Trend window too short. Keep logging for a few more days.", null, null)
        }

        val base = windowed.first().date
        val points = windowed.map { row ->
            Point(day = ChronoUnit.DAYS.between(base, row.date), weight = row.weightKg)
        }
        val rateKgPerDay = linearSlope(points) ?: return Suggestion("Could not fit a trend.", null, null)
        val rateKgPerWeek = rateKgPerDay * 7.0

        val direction = when {
            currentWeightKg - goalWeightKg > 1.0 -> Direction.CUT
            goalWeightKg - currentWeightKg > 1.0 -> Direction.BULK
            else -> Direction.MAINTAIN
        }

        return when (direction) {
            Direction.CUT -> {
                val lossPerWeek = -rateKgPerWeek
                when {
                    lossPerWeek > 1.2 -> Suggestion(
                        "Losing too fast (${"%.2f".format(lossPerWeek)} kg/week). Add 150 kcal.",
                        roundTo50(currentTargetKcal + 150), rateKgPerWeek
                    )
                    lossPerWeek < 0.3 -> Suggestion(
                        "Loss too slow (${"%.2f".format(lossPerWeek)} kg/week). Cut 150 kcal.",
                        roundTo50(currentTargetKcal - 150), rateKgPerWeek
                    )
                    else -> Suggestion(
                        "On track. Holding ${currentTargetKcal} kcal (${"%.2f".format(lossPerWeek)} kg/week).",
                        null, rateKgPerWeek
                    )
                }
            }
            Direction.BULK -> {
                val gainPerWeek = rateKgPerWeek
                when {
                    gainPerWeek > 0.5 -> Suggestion(
                        "Gaining too fast (${"%.2f".format(gainPerWeek)} kg/week). Cut 150 kcal.",
                        roundTo50(currentTargetKcal - 150), rateKgPerWeek
                    )
                    gainPerWeek < 0.1 -> Suggestion(
                        "Gain too slow (${"%.2f".format(gainPerWeek)} kg/week). Add 150 kcal.",
                        roundTo50(currentTargetKcal + 150), rateKgPerWeek
                    )
                    else -> Suggestion(
                        "On track. Holding ${currentTargetKcal} kcal (${"%.2f".format(gainPerWeek)} kg/week).",
                        null, rateKgPerWeek
                    )
                }
            }
            Direction.MAINTAIN -> Suggestion(
                "Within 1 kg of goal. Hold ${currentTargetKcal} kcal (${"%.2f".format(rateKgPerWeek)} kg/week).",
                null, rateKgPerWeek
            )
        }
    }

    private data class Point(val day: Long, val weight: Double)

    /** Linear regression slope (kg per day). Returns null when the points are degenerate. */
    private fun linearSlope(points: List<Point>): Double? {
        if (points.size < 2) return null
        val n = points.size
        val meanX = points.sumOf { it.day.toDouble() } / n
        val meanY = points.sumOf { it.weight } / n
        var num = 0.0
        var den = 0.0
        for (p in points) {
            val dx = p.day - meanX
            num += dx * (p.weight - meanY)
            den += dx * dx
        }
        return if (den == 0.0) null else num / den
    }

    private fun roundTo50(value: Int): Int {
        val rounded = ((value + 25) / 50) * 50
        return rounded.coerceAtLeast(1200)
    }
}
