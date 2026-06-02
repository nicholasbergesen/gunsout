package com.nicholasbergesen.gunsout.feature.body

import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.repo.BodyRepository
import com.nicholasbergesen.gunsout.domain.inbody.InBodyQrImportDraft
import com.nicholasbergesen.gunsout.domain.inbody.InBodyQrParseFailure
import com.nicholasbergesen.gunsout.domain.inbody.InBodyQrParseResult
import com.nicholasbergesen.gunsout.domain.inbody.InBodyQrPayloadParser
import java.util.Locale
import javax.inject.Inject

data class InBodyQrImportUndo(
    val insertedLogId: Long?,
    val previousLog: BodyMetricsLog?
)

sealed interface InBodyQrImportResult {
    data class Imported(
        val draft: InBodyQrImportDraft,
        val logId: Long,
        val undo: InBodyQrImportUndo,
        val message: String
    ) : InBodyQrImportResult

    data class Failed(
        val failure: InBodyQrParseFailure,
        val message: String
    ) : InBodyQrImportResult
}

class InBodyQrImportUseCase @Inject constructor(
    private val parser: InBodyQrPayloadParser,
    private val bodyRepository: BodyRepository
) {
    suspend fun import(userId: String, rawQrValue: String): InBodyQrImportResult {
        return when (val parsed = parser.parse(rawQrValue)) {
            is InBodyQrParseResult.Failed -> InBodyQrImportResult.Failed(
                failure = parsed.failure,
                message = parsed.failure.userMessage
            )
            is InBodyQrParseResult.Parsed -> importDraft(userId, parsed.draft)
        }
    }

    suspend fun undo(undo: InBodyQrImportUndo) {
        val previousLog = undo.previousLog
        if (previousLog == null) {
            undo.insertedLogId?.let { bodyRepository.delete(it) }
        } else {
            bodyRepository.restore(previousLog)
        }
    }

    private suspend fun importDraft(userId: String, draft: InBodyQrImportDraft): InBodyQrImportResult.Imported {
        val previousLog = bodyRepository.getOnDate(userId, draft.date)
        val logId = bodyRepository.log(
            userId = userId,
            date = draft.date,
            weightKg = draft.weightKg,
            bodyFatPct = draft.bodyFatPct,
            muscleMassKg = draft.muscleMassKg,
            waterLiters = null,
            boneMassKg = null,
            visceralFatRating = draft.visceralFatRating,
            notes = if (previousLog == null) draft.provenanceNote else null
        )

        return InBodyQrImportResult.Imported(
            draft = draft,
            logId = logId,
            undo = InBodyQrImportUndo(
                insertedLogId = if (previousLog == null) logId else null,
                previousLog = previousLog
            ),
            message = "Imported InBody ${draft.equipment}: ${formatOneDecimal(draft.weightKg)} kg, " +
                "${formatOneDecimal(draft.bodyFatPct)}% fat, " +
                "${formatOneDecimal(draft.muscleMassKg)} kg muscle" +
                (draft.visceralFatRating?.let { ", visceral $it" } ?: "")
        )
    }

    private fun formatOneDecimal(value: Double?): String =
        value?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
}
