package com.nicholasbergesen.gunsout.domain.inbody

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class InBodyCsvPayloadParserTest {

    private val parser = InBodyCsvPayloadParser()

    @Test
    fun `parses InBody app csv export row`() {
        val result = parser.parse(SAMPLE_CSV)

        assertTrue(result is InBodyCsvParseResult.Parsed)
        val draft = (result as InBodyCsvParseResult.Parsed).drafts.single()
        assertEquals(LocalDateTime.of(2026, 6, 7, 15, 58, 19), draft.testedAt)
        assertEquals(LocalDate.of(2026, 6, 7), draft.date)
        assertEquals("270", draft.equipment)
        assertEquals(80.4, draft.weightKg, 0.001)
        assertEquals(36.2, draft.muscleMassKg!!, 0.001)
        assertEquals(18.4, draft.bodyFatPct!!, 0.001)
        assertEquals(43.8, draft.waterLiters!!, 0.001)
        assertEquals(8, draft.visceralFatRating)
        assertEquals("InBody 270 CSV - 2026-06-07 15:58", draft.provenanceNote)
    }

    @Test
    fun `missing optional csv values remain null`() {
        val result = parser.parse(
            "\uFEFFDate,Measurement device.,Weight(kg),Skeletal Muscle Mass(kg),Percent Body Fat(%),Total Body Water(L),Visceral Fat Level(Level)\n" +
                "20260607155819,270,80.4,-,-,-,-\n"
        )

        val draft = (result as InBodyCsvParseResult.Parsed).drafts.single()
        assertEquals(80.4, draft.weightKg, 0.001)
        assertEquals(null, draft.muscleMassKg)
        assertEquals(null, draft.bodyFatPct)
        assertEquals(null, draft.waterLiters)
        assertEquals(null, draft.visceralFatRating)
    }

    @Test
    fun `invalid date reports row failure`() {
        val result = parser.parse(
            "\uFEFFDate,Weight(kg)\n" +
                "not-a-date,80.4\n"
        )

        assertEquals(
            "The InBody CSV has an invalid date on row 2",
            (result as InBodyCsvParseResult.Failed).message
        )
    }

    private companion object {
        const val SAMPLE_CSV =
            "\uFEFFDate,Measurement device.,Weight(kg),Skeletal Muscle Mass(kg),Soft Lean Mass(kg),Body Fat Mass(kg),Percent Body Fat(%),Basal Metabolic Rate(kcal),InBody Age,InBody Score,Visceral Fat Level(Level),Total Body Water(L),Bone Mineral Content(kg)\n" +
                "20260607155819,270,80.4,36.2,-,14.8,18.4,1780,-,91.0,8.0,43.8,-\n"
    }
}
