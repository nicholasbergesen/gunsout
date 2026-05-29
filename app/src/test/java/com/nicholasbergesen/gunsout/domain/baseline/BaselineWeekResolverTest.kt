package com.nicholasbergesen.gunsout.domain.baseline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BaselineWeekResolverTest {

    private fun epochOf(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun `no active program respects flag default`() {
        assertTrue(BaselineWeekResolver.isActive(programCreatedAt = null, forcedFlag = true))
        assertFalse(BaselineWeekResolver.isActive(programCreatedAt = null, forcedFlag = false))
    }

    @Test fun `within first 7 days is active`() {
        val today = LocalDate.of(2026, 5, 18)
        for (daysBack in 0..6) {
            val created = epochOf(today.minusDays(daysBack.toLong()))
            assertTrue("day $daysBack should be in baseline",
                BaselineWeekResolver.isActive(created, forcedFlag = true, today = today))
        }
    }

    @Test fun `day 8 onwards is inactive`() {
        val today = LocalDate.of(2026, 5, 18)
        val created = epochOf(today.minusDays(7))
        assertFalse(BaselineWeekResolver.isActive(created, forcedFlag = true, today = today))
    }

    @Test fun `flag off forces inactive regardless of date`() {
        val today = LocalDate.of(2026, 5, 18)
        val created = epochOf(today)
        assertFalse(BaselineWeekResolver.isActive(created, forcedFlag = false, today = today))
    }
}
