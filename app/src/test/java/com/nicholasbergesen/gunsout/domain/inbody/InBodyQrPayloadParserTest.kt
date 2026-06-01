package com.nicholasbergesen.gunsout.domain.inbody

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InBodyQrPayloadParserTest {
    private val parser = InBodyQrPayloadParser()

    @Test
    fun `accepts qrcode inbody https sample and parses confirmed 270 fields`() {
        val result = parser.parse(SAMPLE_QR_URL)

        assertTrue(result is InBodyQrParseResult.Parsed)
        val draft = (result as InBodyQrParseResult.Parsed).draft
        assertEquals(LocalDate.of(2025, 10, 20), draft.date)
        assertEquals("270", draft.equipment)
        assertEquals("27379084", draft.serialNumber)
        assertEquals(67.8, draft.weightKg, 0.001)
        assertEquals(32.8, draft.muscleMassKg!!, 0.001)
        assertEquals(14.2, draft.bodyFatPct!!, 0.001)
        assertEquals(3, draft.visceralFatRating)
        assertEquals("InBody 270 #27379084 · 2025-10-20 10:40", draft.provenanceNote)
    }

    @Test
    fun `accepts https subdomains under inbody com`() {
        val result = parser.parse(SAMPLE_QR_URL.replace("qrcode.inbody.com", "regional.qrcode.inbody.com"))

        assertTrue(result is InBodyQrParseResult.Parsed)
    }

    @Test
    fun `rejects http inbody urls`() {
        val result = parser.parse(SAMPLE_QR_URL.replace("https://", "http://"))

        assertEquals(InBodyQrParseFailure.NOT_INBODY_QR, (result as InBodyQrParseResult.Failed).failure)
    }

    @Test
    fun `rejects non inbody hosts`() {
        val result = parser.parse(SAMPLE_QR_URL.replace("qrcode.inbody.com", "qrcode.example.com"))

        assertEquals(InBodyQrParseFailure.NOT_INBODY_QR, (result as InBodyQrParseResult.Failed).failure)
    }

    @Test
    fun `rejects missing ibdata`() {
        val result = parser.parse("https://qrcode.inbody.com?other=value")

        assertEquals(InBodyQrParseFailure.MISSING_IBDATA, (result as InBodyQrParseResult.Failed).failure)
    }

    @Test
    fun `empty payload fails safely`() {
        val result = parser.parse("https://qrcode.inbody.com?IBData=")

        assertEquals(InBodyQrParseFailure.INVALID_PAYLOAD, (result as InBodyQrParseResult.Failed).failure)
    }

    @Test
    fun `unknown model fails as unsupported`() {
        val result = parser.parse(SAMPLE_QR_URL.replace("IBData=270-", "IBData=570-"))

        assertEquals(InBodyQrParseFailure.UNSUPPORTED_MODEL_OR_LAYOUT, (result as InBodyQrParseResult.Failed).failure)
    }

    private companion object {
        const val SAMPLE_QR_URL =
            "https://qrcode.inbody.com?IBData=270-30F92004316!27379084!!!!!!!!!!!!!!!!!!!!17500310M20251020104037011501020124040003500428009700810162042603790463058106780573077510060328102500955022101420810678!0000!0000!0000162600770080009003490811042612000000000000000000003446337202292379231730673027018320922041PASS00113003890101042103200674022000160150008511111001111111111100820270-2DM-0416!!!!0185023000000100020010100"
    }
}
