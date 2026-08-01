package com.nicholasbergesen.gunsout.feature.creatine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class CreatineReminderScheduleTest {
    @Test
    fun `next reminder uses today when configured local time is still ahead`() {
        val now = ZonedDateTime.parse("2026-08-01T07:00:00+09:00[Asia/Tokyo]")

        val trigger = nextCreatineReminder(now, LocalTime.of(8, 30))

        assertEquals(
            ZonedDateTime.parse("2026-08-01T08:30:00+09:00[Asia/Tokyo]"),
            trigger
        )
    }

    @Test
    fun `next reminder advances to tomorrow at and after configured time`() {
        val zone = ZoneId.of("Asia/Tokyo")
        val reminderTime = LocalTime.of(8, 30)

        assertEquals(
            ZonedDateTime.parse("2026-08-02T08:30:00+09:00[Asia/Tokyo]"),
            nextCreatineReminder(
                ZonedDateTime.of(2026, 8, 1, 8, 30, 0, 0, zone),
                reminderTime
            )
        )
        assertEquals(
            ZonedDateTime.parse("2026-08-02T08:30:00+09:00[Asia/Tokyo]"),
            nextCreatineReminder(
                ZonedDateTime.of(2026, 8, 1, 12, 0, 0, 0, zone),
                reminderTime
            )
        )
    }

    @Test
    fun `next reminder preserves local time across daylight saving transitions`() {
        val zone = ZoneId.of("America/New_York")
        val reminderTime = LocalTime.of(8, 0)
        val beforeSpringForward =
            ZonedDateTime.of(2026, 3, 7, 8, 0, 0, 0, zone)
        val beforeFallBack =
            ZonedDateTime.of(2026, 10, 31, 8, 0, 0, 0, zone)

        val springTrigger = nextCreatineReminder(beforeSpringForward, reminderTime)
        val fallTrigger = nextCreatineReminder(beforeFallBack, reminderTime)

        assertEquals(LocalTime.of(8, 0), springTrigger.toLocalTime())
        assertEquals(LocalTime.of(8, 0), fallTrigger.toLocalTime())
        assertEquals(Duration.ofHours(23), Duration.between(beforeSpringForward, springTrigger))
        assertEquals(Duration.ofHours(25), Duration.between(beforeFallBack, fallTrigger))
    }

    @Test
    fun `next reminder uses the current timezone after a zone change`() {
        val reminderTime = LocalTime.of(8, 0)
        val sameInstantInTokyo = ZonedDateTime
            .parse("2026-08-01T00:00:00Z")
            .withZoneSameInstant(ZoneId.of("Asia/Tokyo"))
        val sameInstantInLosAngeles = ZonedDateTime
            .parse("2026-08-01T00:00:00Z")
            .withZoneSameInstant(ZoneId.of("America/Los_Angeles"))

        assertEquals(
            ZonedDateTime.parse("2026-08-02T08:00:00+09:00[Asia/Tokyo]"),
            nextCreatineReminder(sameInstantInTokyo, reminderTime)
        )
        assertEquals(
            ZonedDateTime.parse("2026-08-01T08:00:00-07:00[America/Los_Angeles]"),
            nextCreatineReminder(sameInstantInLosAngeles, reminderTime)
        )
    }
}
