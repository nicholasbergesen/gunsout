package com.nicholasbergesen.gunsout.domain.inbody

import java.net.URI
import java.net.URLDecoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.floor

data class InBodyQrImportDraft(
    val testedAt: LocalDateTime,
    val equipment: String,
    val serialNumber: String?,
    val weightKg: Double,
    val bodyFatPct: Double?,
    val muscleMassKg: Double?,
    val visceralFatRating: Int?,
    val bodyFatMassKg: Double? = null,
    val bmi: Double? = null,
    val heightCm: Double? = null,
    val age: Int? = null,
    val gender: String? = null
) {
    val date: LocalDate = testedAt.toLocalDate()

    val provenanceNote: String
        get() = buildString {
            append("InBody ")
            append(equipment)
            serialNumber?.takeIf { it.isNotBlank() }?.let {
                append(" #")
                append(it)
            }
            append(" · ")
            append(testedAt.format(PROVENANCE_TIME_FORMATTER))
        }
}

sealed interface InBodyQrParseResult {
    data class Parsed(val draft: InBodyQrImportDraft) : InBodyQrParseResult
    data class Failed(val failure: InBodyQrParseFailure) : InBodyQrParseResult
}

enum class InBodyQrParseFailure(val userMessage: String) {
    NOT_INBODY_QR("That doesn't look like an InBody QR code"),
    MISSING_IBDATA("The InBody QR could not be read"),
    UNSUPPORTED_MODEL_OR_LAYOUT("This InBody QR is from InBody, but this layout is not supported yet"),
    INVALID_PAYLOAD("The InBody QR could not be read")
}

class InBodyQrPayloadParser @Inject constructor() {

    fun parse(rawValue: String): InBodyQrParseResult {
        val uri = parseUri(rawValue.trim()) ?: return InBodyQrParseResult.Failed(InBodyQrParseFailure.NOT_INBODY_QR)
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            return InBodyQrParseResult.Failed(InBodyQrParseFailure.NOT_INBODY_QR)
        }

        val host = uri.host?.lowercase(Locale.US)
        if (host == null || !isAllowedInBodyHost(host)) {
            return InBodyQrParseResult.Failed(InBodyQrParseFailure.NOT_INBODY_QR)
        }

        val payload = runCatching { queryParameter(uri.rawQuery, "IBData") }
            .getOrElse { return InBodyQrParseResult.Failed(InBodyQrParseFailure.INVALID_PAYLOAD) }
            ?: return InBodyQrParseResult.Failed(InBodyQrParseFailure.MISSING_IBDATA)
        if (payload.isBlank()) {
            return InBodyQrParseResult.Failed(InBodyQrParseFailure.INVALID_PAYLOAD)
        }

        return parseIbData(payload)
    }

    private fun parseIbData(payload: String): InBodyQrParseResult {
        val segments = payload.split('!')
        val header = segments.firstOrNull().orEmpty()
        val model = header.substringBefore("-").takeIf { it.isNotBlank() }
            ?: return InBodyQrParseResult.Failed(InBodyQrParseFailure.INVALID_PAYLOAD)

        if (model != SUPPORTED_EQUIPMENT) {
            return InBodyQrParseResult.Failed(InBodyQrParseFailure.UNSUPPORTED_MODEL_OR_LAYOUT)
        }

        val mainSegment = segments.firstOrNull { it.length == MODEL_270_MAIN_SEGMENT_LENGTH && isModel270MainSegment(it) }
            ?: return InBodyQrParseResult.Failed(InBodyQrParseFailure.UNSUPPORTED_MODEL_OR_LAYOUT)
        val hasKnownLayoutMarker = segments.any { it.contains(MODEL_270_LAYOUT_MARKER) }
        if (!hasKnownLayoutMarker) {
            return InBodyQrParseResult.Failed(InBodyQrParseFailure.UNSUPPORTED_MODEL_OR_LAYOUT)
        }

        return try {
            val testedAt = LocalDateTime.parse(mainSegment.substring(9, 23), RAW_TIME_FORMATTER)
            val weightKg = requiredScaled(mainSegment, 75, 4)
            val muscleMassKg = requiredScaled(mainSegment, 91, 4)
            val bodyFatMassKg = optionalScaled(mainSegment, 47, 4)
            val bmi = optionalScaled(mainSegment, 104, 4)
            val bodyFatPct = requiredScaled(mainSegment, 108, 4)
            val visceralFatAreaCm2 = requiredScaled(mainSegment, 39, 4)
            val visceralFatRating = floor(visceralFatAreaCm2 / 10.0).toInt().takeIf { it > 0 }
            val heightCm = optionalScaled(mainSegment, 0, 4)
            val age = mainSegment.substring(4, 7).toIntOrNull()
            val gender = when (mainSegment[8].uppercaseChar()) {
                'M' -> "Male"
                'F' -> "Female"
                else -> null
            }

            InBodyQrParseResult.Parsed(
                InBodyQrImportDraft(
                    testedAt = testedAt,
                    equipment = model,
                    serialNumber = segments.getOrNull(1)?.takeIf { it.isNotBlank() },
                    weightKg = weightKg,
                    bodyFatPct = bodyFatPct,
                    muscleMassKg = muscleMassKg,
                    visceralFatRating = visceralFatRating,
                    bodyFatMassKg = bodyFatMassKg,
                    bmi = bmi,
                    heightCm = heightCm,
                    age = age,
                    gender = gender
                )
            )
        } catch (_: RuntimeException) {
            InBodyQrParseResult.Failed(InBodyQrParseFailure.INVALID_PAYLOAD)
        }
    }

    private fun isModel270MainSegment(segment: String): Boolean {
        val gender = segment[8].uppercaseChar()
        return (gender == 'M' || gender == 'F') && segment.substring(9, 23).all(Char::isDigit)
    }

    private fun requiredScaled(value: String, start: Int, length: Int): Double {
        val raw = value.substring(start, start + length)
        require(raw.all(Char::isDigit)) { "Expected numeric field" }
        return raw.toInt() / 10.0
    }

    private fun optionalScaled(value: String, start: Int, length: Int): Double? {
        val raw = value.substring(start, start + length)
        return raw.takeIf { it.all(Char::isDigit) }?.toInt()?.div(10.0)
    }

    private fun queryParameter(rawQuery: String?, name: String): String? {
        if (rawQuery == null) return null
        return rawQuery.split('&')
            .asSequence()
            .mapNotNull { part ->
                val equals = part.indexOf('=')
                if (equals < 0) return@mapNotNull null
                val key = decode(part.substring(0, equals))
                val value = decode(part.substring(equals + 1))
                key to value
            }
            .firstOrNull { it.first == name }
            ?.second
    }

    private fun decode(value: String): String =
        URLDecoder.decode(value, Charsets.UTF_8.name())

    private fun parseUri(value: String): URI? =
        runCatching { URI(value) }.getOrNull()

    private fun isAllowedInBodyHost(host: String): Boolean =
        host == "inbody.com" || host.endsWith(".inbody.com")

    private companion object {
        const val SUPPORTED_EQUIPMENT = "270"
        const val MODEL_270_MAIN_SEGMENT_LENGTH = 119
        const val MODEL_270_LAYOUT_MARKER = "270-2DM-0416"
        val RAW_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}

private val PROVENANCE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
