package com.nicholasbergesen.gunsout.core.text

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DecimalTextTest {

    @Test fun `normalizes comma decimal input to dot`() {
        assertEquals("72.5", "72,5".normalizeDecimalInput())
    }

    @Test fun `keeps only one decimal separator`() {
        assertEquals("72.53", "72,5.3".normalizeDecimalInput())
    }

    @Test fun `formats one decimal with dot regardless of locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("37.5", formatOneDecimalOrInt(37.5))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test fun `formats near integer without decimal`() {
        assertEquals("55", formatOneDecimalOrInt(55.0000000001))
    }
}
