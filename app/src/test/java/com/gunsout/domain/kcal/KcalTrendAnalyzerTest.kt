package com.gunsout.domain.kcal

import com.gunsout.data.entity.BodyMetricsLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class KcalTrendAnalyzerTest {

    private fun log(date: LocalDate, weight: Double) = BodyMetricsLog(date = date, weightKg = weight)

    @Test fun `too few logs returns advice only`() {
        val s = KcalTrendAnalyzer.analyze(
            logs = listOf(log(LocalDate.of(2026, 5, 1), 100.0)),
            currentTargetKcal = 2200, currentWeightKg = 100.0, goalWeightKg = 80.0
        )
        assertNull(s.newKcalTarget)
    }

    @Test fun `cut on track holds target`() {
        val end = LocalDate.of(2026, 5, 14)
        val logs = listOf(
            log(end.minusDays(14), 100.0),
            log(end.minusDays(7), 99.3),
            log(end, 98.6) // ~0.7 kg/wk
        )
        val s = KcalTrendAnalyzer.analyze(logs, 2200, 98.6, 80.0)
        assertNull(s.newKcalTarget)
        assertNotNull(s.ratePerWeekKg)
    }

    @Test fun `cut too fast suggests adding kcal`() {
        val end = LocalDate.of(2026, 5, 14)
        val logs = listOf(
            log(end.minusDays(14), 100.0),
            log(end.minusDays(7), 98.5),
            log(end, 97.0) // ~1.5 kg/wk loss
        )
        val s = KcalTrendAnalyzer.analyze(logs, 2200, 97.0, 80.0)
        assertEquals(2350, s.newKcalTarget)
    }

    @Test fun `cut too slow suggests cutting kcal`() {
        val end = LocalDate.of(2026, 5, 14)
        val logs = listOf(
            log(end.minusDays(14), 100.0),
            log(end.minusDays(7), 99.9),
            log(end, 99.8) // ~0.1 kg/wk
        )
        val s = KcalTrendAnalyzer.analyze(logs, 2200, 99.8, 80.0)
        assertEquals(2050, s.newKcalTarget)
    }

    @Test fun `within 1 kg of goal maintains`() {
        val end = LocalDate.of(2026, 5, 14)
        val logs = listOf(
            log(end.minusDays(14), 80.5),
            log(end.minusDays(7), 80.4),
            log(end, 80.3)
        )
        val s = KcalTrendAnalyzer.analyze(logs, 2200, 80.3, 80.0)
        assertNull(s.newKcalTarget)
    }

    @Test fun `kcal never goes below 1200 floor`() {
        val end = LocalDate.of(2026, 5, 14)
        val logs = listOf(
            log(end.minusDays(14), 100.0),
            log(end.minusDays(7), 99.95),
            log(end, 99.9)
        )
        val s = KcalTrendAnalyzer.analyze(logs, 1200, 99.9, 80.0)
        assertEquals(1200, s.newKcalTarget)
    }
}
