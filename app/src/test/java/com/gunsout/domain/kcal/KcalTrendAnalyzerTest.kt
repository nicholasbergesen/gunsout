package com.gunsout.domain.kcal

import com.gunsout.data.entity.BodyMetricsLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class KcalTrendAnalyzerTest {

    private fun log(date: LocalDate, weight: Double) = BodyMetricsLog(date = date, weightKg = weight)

    private fun dailySeries(start: LocalDate, weights: List<Double>): List<BodyMetricsLog> =
        weights.mapIndexed { i, w -> log(start.plusDays(i.toLong()), w) }

    @Test fun `too few logs returns advice only`() {
        val s = KcalTrendAnalyzer.analyze(
            logs = listOf(log(LocalDate.of(2026, 5, 1), 100.0)),
            currentTargetKcal = 2200, currentWeightKg = 100.0, goalWeightKg = 80.0
        )
        assertNull(s.newKcalTarget)
    }

    @Test fun `window too short returns advice only`() {
        val end = LocalDate.of(2026, 5, 14)
        // 4 logs but spanning only 3 days
        val logs = listOf(
            log(end.minusDays(3), 100.0),
            log(end.minusDays(2), 99.9),
            log(end.minusDays(1), 99.8),
            log(end, 99.7)
        )
        val s = KcalTrendAnalyzer.analyze(logs, 2200, 99.7, 80.0)
        assertNull(s.newKcalTarget)
    }

    @Test fun `cut on track holds target`() {
        val start = LocalDate.of(2026, 5, 1)
        // Daily logs trending ~0.7 kg/week
        val logs = dailySeries(start, listOf(100.0, 99.9, 99.8, 99.7, 99.6, 99.5, 99.4, 99.3, 99.2, 99.1, 99.0))
        val s = KcalTrendAnalyzer.analyze(logs, 2200, logs.last().weightKg, 80.0)
        assertNull(s.newKcalTarget)
        assertNotNull(s.ratePerWeekKg)
    }

    @Test fun `cut too fast suggests adding kcal`() {
        val start = LocalDate.of(2026, 5, 1)
        // ~1.5 kg/week loss
        val logs = dailySeries(start, listOf(100.0, 99.8, 99.6, 99.4, 99.2, 99.0, 98.8, 98.6, 98.4, 98.2, 98.0))
        val s = KcalTrendAnalyzer.analyze(logs, 2200, logs.last().weightKg, 80.0)
        assertEquals(2350, s.newKcalTarget)
    }

    @Test fun `cut too slow suggests cutting kcal`() {
        val start = LocalDate.of(2026, 5, 1)
        // ~0.1 kg/week loss
        val logs = dailySeries(start,
            (0..10).map { 100.0 - it * (0.1 / 7.0) })
        val s = KcalTrendAnalyzer.analyze(logs, 2200, logs.last().weightKg, 80.0)
        assertEquals(2050, s.newKcalTarget)
    }

    @Test fun `within 1 kg of goal maintains`() {
        val start = LocalDate.of(2026, 5, 1)
        val logs = dailySeries(start, (0..10).map { 80.3 - it * 0.02 })
        val s = KcalTrendAnalyzer.analyze(logs, 2200, logs.last().weightKg, 80.0)
        assertNull(s.newKcalTarget)
    }

    @Test fun `kcal never goes below 1200 floor`() {
        val start = LocalDate.of(2026, 5, 1)
        // Basically flat → too slow → would push to 1050 but should floor at 1200.
        val logs = dailySeries(start, List(11) { i -> 100.0 - i * 0.001 })
        val s = KcalTrendAnalyzer.analyze(logs, 1200, logs.last().weightKg, 80.0)
        assertEquals(1200, s.newKcalTarget)
    }

    @Test fun `moving average is robust to a single noisy weigh-in`() {
        // Underlying trend is ~0.7 kg/week loss over 14 days. Insert one bogus +2 kg spike midway.
        val start = LocalDate.of(2026, 5, 1)
        val daily = (0..13).map { 100.0 - it * 0.1 }.toMutableList()
        daily[5] = daily[5] + 2.0 // noisy spike
        val logs = dailySeries(start, daily)
        val s = KcalTrendAnalyzer.analyze(logs, 2200, logs.last().weightKg, 80.0)
        // A single outlier in 14 points shifts the slope only slightly; the suggestion stays "on track".
        assertNull(s.newKcalTarget)
    }
}
