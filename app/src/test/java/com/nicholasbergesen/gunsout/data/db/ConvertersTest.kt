package com.nicholasbergesen.gunsout.data.db

import com.nicholasbergesen.gunsout.data.entity.AlternateReason
import com.nicholasbergesen.gunsout.data.entity.DayHint
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.MealType
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.entity.ProgramType
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.SessionStatus
import com.nicholasbergesen.gunsout.data.entity.SupplementUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ConvertersTest {
    private val converters = Converters()

    @Test fun `date and time converters round trip values and preserve nulls`() {
        val date = LocalDate.of(2026, 6, 14)
        val dateTime = LocalDateTime.of(2026, 6, 14, 5, 45, 30)
        val time = LocalTime.of(21, 15)

        assertEquals("2026-06-14", converters.localDateToString(date))
        assertEquals(date, converters.stringToLocalDate("2026-06-14"))
        assertEquals("2026-06-14T05:45:30", converters.localDateTimeToString(dateTime))
        assertEquals(dateTime, converters.stringToLocalDateTime("2026-06-14T05:45:30"))
        assertEquals("21:15", converters.localTimeToString(time))
        assertEquals(time, converters.stringToLocalTime("21:15"))

        assertNull(converters.localDateToString(null))
        assertNull(converters.stringToLocalDate(null))
        assertNull(converters.localDateTimeToString(null))
        assertNull(converters.stringToLocalDateTime(null))
        assertNull(converters.localTimeToString(null))
        assertNull(converters.stringToLocalTime(null))
    }

    @Test fun `enum converters round trip values and preserve nulls`() {
        assertEquals("BARBELL", converters.equipmentToString(Equipment.BARBELL))
        assertEquals(Equipment.BARBELL, converters.stringToEquipment("BARBELL"))
        assertEquals("BACK", converters.muscleGroupToString(MuscleGroup.BACK))
        assertEquals(MuscleGroup.BACK, converters.stringToMuscleGroup("BACK"))
        assertEquals("HINGE", converters.movementPatternToString(MovementPattern.HINGE))
        assertEquals(MovementPattern.HINGE, converters.stringToMovementPattern("HINGE"))
        assertEquals("INJURY", converters.alternateReasonToString(AlternateReason.INJURY))
        assertEquals(AlternateReason.INJURY, converters.stringToAlternateReason("INJURY"))
        assertEquals("AMRAP", converters.protocolToString(Protocol.AMRAP))
        assertEquals(Protocol.AMRAP, converters.stringToProtocol("AMRAP"))
        assertEquals("SKIPPED", converters.sessionStatusToString(SessionStatus.SKIPPED))
        assertEquals(SessionStatus.SKIPPED, converters.stringToSessionStatus("SKIPPED"))
        assertEquals("SMOOTHIE", converters.mealTypeToString(MealType.SMOOTHIE))
        assertEquals(MealType.SMOOTHIE, converters.stringToMealType("SMOOTHIE"))
        assertEquals("CAPSULE", converters.supplementUnitToString(SupplementUnit.CAPSULE))
        assertEquals(SupplementUnit.CAPSULE, converters.stringToSupplementUnit("CAPSULE"))
        assertEquals("PPL", converters.programTypeToString(ProgramType.PPL))
        assertEquals(ProgramType.PPL, converters.stringToProgramType("PPL"))
        assertEquals("SUN", converters.dayHintToString(DayHint.SUN))
        assertEquals(DayHint.SUN, converters.stringToDayHint("SUN"))

        assertNull(converters.equipmentToString(null))
        assertNull(converters.stringToEquipment(null))
        assertNull(converters.muscleGroupToString(null))
        assertNull(converters.stringToMuscleGroup(null))
        assertNull(converters.movementPatternToString(null))
        assertNull(converters.stringToMovementPattern(null))
        assertNull(converters.alternateReasonToString(null))
        assertNull(converters.stringToAlternateReason(null))
        assertNull(converters.protocolToString(null))
        assertNull(converters.stringToProtocol(null))
        assertNull(converters.sessionStatusToString(null))
        assertNull(converters.stringToSessionStatus(null))
        assertNull(converters.mealTypeToString(null))
        assertNull(converters.stringToMealType(null))
        assertNull(converters.supplementUnitToString(null))
        assertNull(converters.stringToSupplementUnit(null))
        assertNull(converters.programTypeToString(null))
        assertNull(converters.stringToProgramType(null))
        assertNull(converters.dayHintToString(null))
        assertNull(converters.stringToDayHint(null))
    }
}
