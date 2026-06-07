package com.nicholasbergesen.gunsout.domain.inbody

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private val INBODY_CSV_PROVENANCE_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

data class InBodyCsvImportDraft(
    val testedAt: LocalDateTime,
    val equipment: String?,
    val weightKg: Double,
    val bodyFatPct: Double?,
    val muscleMassKg: Double?,
    val waterLiters: Double?,
    val boneMassKg: Double?,
    val visceralFatRating: Int?
) {
    val date: LocalDate = testedAt.toLocalDate()

    val provenanceNote: String
        get() = buildString {
            append("InBody")
            equipment?.takeIf { it.isNotBlank() }?.let {
                append(" ")
                append(it)
            }
            append(" CSV - ")
            append(testedAt.format(INBODY_CSV_PROVENANCE_TIME_FORMATTER))
        }
}

sealed interface InBodyCsvParseResult {
    data class Parsed(val drafts: List<InBodyCsvImportDraft>) : InBodyCsvParseResult
    data class Failed(val message: String) : InBodyCsvParseResult
}

class InBodyCsvPayloadParser @Inject constructor() {

    fun parse(rawCsv: String): InBodyCsvParseResult {
        val rows = parseCsvRows(rawCsv)
        if (rows.isEmpty()) {
            return InBodyCsvParseResult.Failed("The InBody CSV is empty")
        }

        val headers = rows.first().mapIndexed { index, header -> headerKey(header) to index }.toMap()
        val missing = REQUIRED_HEADERS.filter { it !in headers }
        if (missing.isNotEmpty()) {
            return InBodyCsvParseResult.Failed("The InBody CSV is missing required columns")
        }

        val drafts = mutableListOf<InBodyCsvImportDraft>()
        rows.drop(1).forEachIndexed { rowIndex, row ->
            if (row.all { it.isBlank() }) return@forEachIndexed
            val displayRow = rowIndex + 2
            val testedAt = parseTestedAt(cell(row, headers, DATE_HEADER))
                ?: return InBodyCsvParseResult.Failed("The InBody CSV has an invalid date on row $displayRow")
            val weightKg = parseDecimal(cell(row, headers, WEIGHT_HEADER))
                ?: return InBodyCsvParseResult.Failed("The InBody CSV has an invalid weight on row $displayRow")

            drafts += InBodyCsvImportDraft(
                testedAt = testedAt,
                equipment = cell(row, headers, DEVICE_HEADER).takeUnlessMissing(),
                weightKg = weightKg,
                bodyFatPct = parseDecimal(cell(row, headers, BODY_FAT_PERCENT_HEADER)),
                muscleMassKg = parseDecimal(cell(row, headers, SKELETAL_MUSCLE_HEADER)),
                waterLiters = parseDecimal(cell(row, headers, TOTAL_BODY_WATER_HEADER)),
                boneMassKg = parseDecimal(cell(row, headers, BONE_MINERAL_CONTENT_HEADER)),
                visceralFatRating = parseDecimal(cell(row, headers, VISCERAL_FAT_LEVEL_HEADER))?.toInt()
            )
        }

        if (drafts.isEmpty()) {
            return InBodyCsvParseResult.Failed("The InBody CSV did not contain any measurements")
        }

        return InBodyCsvParseResult.Parsed(drafts.sortedBy { it.testedAt })
    }

    private fun cell(row: List<String>, headers: Map<String, Int>, key: String): String =
        headers[key]?.let { row.getOrNull(it) }.orEmpty()

    private fun parseTestedAt(value: String): LocalDateTime? {
        val normalized = value.takeUnlessMissing() ?: return null
        return DATE_FORMATTERS.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDateTime.parse(normalized, formatter) }.getOrNull()
        }
    }

    private fun parseDecimal(value: String): Double? =
        value.takeUnlessMissing()?.toDoubleOrNull()

    private fun String.takeUnlessMissing(): String? =
        trim().takeUnless { it.isBlank() || it == "-" }

    private fun headerKey(value: String): String =
        value
            .trim()
            .trimStart('\uFEFF')
            .lowercase(Locale.US)
            .filter { it.isLetterOrDigit() || it == '%' }

    private fun parseCsvRows(rawCsv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < rawCsv.length) {
            val char = rawCsv[index]
            when {
                inQuotes && char == '"' && rawCsv.getOrNull(index + 1) == '"' -> {
                    cell.append('"')
                    index += 1
                }
                char == '"' -> inQuotes = !inQuotes
                !inQuotes && char == ',' -> {
                    row += cell.toString()
                    cell.clear()
                }
                !inQuotes && (char == '\n' || char == '\r') -> {
                    row += cell.toString()
                    cell.clear()
                    if (row.any { it.isNotBlank() }) rows += row.toList()
                    row.clear()
                    if (char == '\r' && rawCsv.getOrNull(index + 1) == '\n') index += 1
                }
                else -> cell.append(char)
            }
            index += 1
        }
        row += cell.toString()
        if (row.any { it.isNotBlank() }) rows += row.toList()
        return rows
    }

    private companion object {
        const val DATE_HEADER = "date"
        const val DEVICE_HEADER = "measurementdevice"
        const val WEIGHT_HEADER = "weightkg"
        const val SKELETAL_MUSCLE_HEADER = "skeletalmusclemasskg"
        const val BODY_FAT_PERCENT_HEADER = "percentbodyfat%"
        const val TOTAL_BODY_WATER_HEADER = "totalbodywaterl"
        const val BONE_MINERAL_CONTENT_HEADER = "bonemineralcontentkg"
        const val VISCERAL_FAT_LEVEL_HEADER = "visceralfatlevellevel"
        val REQUIRED_HEADERS = setOf(DATE_HEADER, WEIGHT_HEADER)
        val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        )
    }
}
