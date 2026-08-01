package com.nicholasbergesen.gunsout.domain.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ProteinHistoryTest {
    private val today = LocalDate.of(2026, 7, 26)

    @Test
    fun `week uses seven rolling days and preserves missing days as gaps`() {
        val series = ProteinHistory.aggregate(
            range = ProteinHistoryRange.WEEK,
            today = today,
            records = listOf(
                ProteinDayRecord(today.minusDays(2), 100),
                ProteinDayRecord(today.minusDays(2), 25),
                ProteinDayRecord(today, 160)
            ),
            targets = listOf(
                ProteinTargetRecord(today.minusDays(2), 150),
                ProteinTargetRecord(today, 160)
            )
        )

        assertEquals(7, series.points.size)
        assertEquals(today.minusDays(6), series.points.first().startDate)
        assertEquals(today, series.points.last().endDate)
        assertNull(series.points[1].loggedGrams)
        assertEquals(125.0, series.points[4].loggedGrams!!, 0.001)
        assertEquals(150.0, series.points[4].targetGrams!!, 0.001)
        assertEquals(160.0, series.points.last().loggedGrams!!, 0.001)
    }

    @Test
    fun `month uses thirty rolling days rather than a calendar month`() {
        val series = ProteinHistory.aggregate(
            range = ProteinHistoryRange.MONTH,
            today = today,
            records = listOf(ProteinDayRecord(today.minusDays(29), 90)),
            targets = emptyList()
        )

        assertEquals(30, series.points.size)
        assertEquals(today.minusDays(29), series.points.first().startDate)
        assertEquals(90.0, series.points.first().loggedGrams!!, 0.001)
        assertNull(series.points.first().targetGrams)
    }

    @Test
    fun `year uses twelve month buckets and averages only logged days`() {
        val juneFirst = LocalDate.of(2026, 6, 1)
        val juneThird = LocalDate.of(2026, 6, 3)
        val julyFirst = LocalDate.of(2026, 7, 1)
        val julySecond = LocalDate.of(2026, 7, 2)
        val series = ProteinHistory.aggregate(
            range = ProteinHistoryRange.YEAR,
            today = today,
            records = listOf(
                ProteinDayRecord(juneFirst, 100),
                ProteinDayRecord(juneThird, 200),
                ProteinDayRecord(julyFirst, 140),
                ProteinDayRecord(julySecond, 180)
            ),
            targets = listOf(
                ProteinTargetRecord(juneFirst, 150),
                ProteinTargetRecord(julyFirst, 160),
                ProteinTargetRecord(julySecond, 160)
            )
        )

        assertEquals(12, series.points.size)
        assertEquals(YearMonth.of(2025, 8), YearMonth.from(series.points.first().startDate))
        val june = series.points.single { YearMonth.from(it.startDate) == YearMonth.of(2026, 6) }
        val july = series.points.last()
        assertEquals(150.0, june.loggedGrams!!, 0.001)
        assertEquals(2, june.loggedDays)
        assertNull(june.targetGrams)
        assertEquals(160.0, july.loggedGrams!!, 0.001)
        assertEquals(160.0, july.targetGrams!!, 0.001)
    }

    @Test
    fun `non-positive records do not turn missing data into zero`() {
        val series = ProteinHistory.aggregate(
            range = ProteinHistoryRange.WEEK,
            today = today,
            records = listOf(ProteinDayRecord(today, 0), ProteinDayRecord(today, -5)),
            targets = listOf(ProteinTargetRecord(today, 160))
        )

        assertNull(series.points.last().loggedGrams)
        assertNull(series.points.last().targetGrams)
        assertEquals(0, series.points.last().loggedDays)
    }
}
