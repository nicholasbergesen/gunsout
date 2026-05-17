package com.gunsout.domain.kcal

import com.gunsout.data.entity.BodyMetricsLog
import java.time.temporal.ChronoUnit

/**
 * Suggests a kcal-target adjustment based on recent body weight trend.
 *
 * Conservative rules:
 *  - Needs at least 3 logs in the last [windowDays].
 *  - Computes a simple linear rate (kg / week) using first and last log in the window.
 *  - Cut goal (current > goal + 1 kg): aim for 0.3 to 1.2 kg/week loss.
 *      * Too fast (loss > 1.2 kg/week): suggest +150 kcal.
 *      * Too slow (loss < 0.3 kg/week, maintained, or gained): suggest -150 kcal.
 *      * In range: hold.
 *  - Bulk and maintain use mirrored thresholds.
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
        if (logs.size < 3) {
            return Suggestion("Need at least 3 weigh-ins to suggest an adjustment.", null, null)
        }
        val sorted = logs.sortedBy { it.date }
        val last = sorted.last()
        val cutoff = last.date.minusDays(windowDays.toLong())
        val windowed = sorted.filter { it.date >= cutoff }
        if (windowed.size < 2) {
            return Suggestion("Not enough recent weigh-ins in the last $windowDays days.", null, null)
        }
        val first = windowed.first()
        val days = ChronoUnit.DAYS.between(first.date, last.date).coerceAtLeast(1L).toDouble()
        val rateKgPerDay = (last.weightKg - first.weightKg) / days
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

    private fun roundTo50(value: Int): Int {
        val rounded = ((value + 25) / 50) * 50
        return rounded.coerceAtLeast(1200)
    }
}
