package com.nicholasbergesen.gunsout.feature.body

import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.repo.BodyRepository
import com.nicholasbergesen.gunsout.domain.inbody.InBodyCsvImportDraft
import com.nicholasbergesen.gunsout.domain.inbody.InBodyCsvParseResult
import com.nicholasbergesen.gunsout.domain.inbody.InBodyCsvPayloadParser
import java.time.LocalDate
import javax.inject.Inject

data class InBodyCsvImportUndo(
    val rowUndos: List<InBodyQrImportUndo>
)

sealed interface InBodyCsvImportResult {
    data class Imported(
        val importedRows: Int,
        val undo: InBodyCsvImportUndo,
        val message: String
    ) : InBodyCsvImportResult

    data class Failed(val message: String) : InBodyCsvImportResult
}

class InBodyCsvImportUseCase @Inject constructor(
    private val parser: InBodyCsvPayloadParser,
    private val bodyRepository: BodyRepository
) {
    suspend fun import(userId: String, rawCsv: String): InBodyCsvImportResult {
        val parsed = when (val result = parser.parse(rawCsv)) {
            is InBodyCsvParseResult.Failed -> return InBodyCsvImportResult.Failed(result.message)
            is InBodyCsvParseResult.Parsed -> result.drafts
        }

        val originalsByDate = linkedMapOf<LocalDate, BodyMetricsLog?>()
        val importedIdsByDate = linkedMapOf<LocalDate, Long>()
        parsed.forEach { draft ->
            if (!originalsByDate.containsKey(draft.date)) {
                originalsByDate[draft.date] = bodyRepository.getOnDate(userId, draft.date)
            }
            importedIdsByDate[draft.date] = importDraft(userId, draft, originalsByDate[draft.date] != null)
        }

        val rowUndos = originalsByDate.map { (date, previousLog) ->
            InBodyQrImportUndo(
                insertedLogId = if (previousLog == null) importedIdsByDate[date] else null,
                previousLog = previousLog
            )
        }

        val rowWord = if (parsed.size == 1) "measurement" else "measurements"
        return InBodyCsvImportResult.Imported(
            importedRows = parsed.size,
            undo = InBodyCsvImportUndo(rowUndos),
            message = "Imported ${parsed.size} InBody CSV $rowWord"
        )
    }

    suspend fun undo(undo: InBodyCsvImportUndo) {
        undo.rowUndos.asReversed().forEach { rowUndo ->
            val previousLog = rowUndo.previousLog
            if (previousLog == null) {
                rowUndo.insertedLogId?.let { bodyRepository.delete(it) }
            } else {
                bodyRepository.restore(previousLog)
            }
        }
    }

    private suspend fun importDraft(
        userId: String,
        draft: InBodyCsvImportDraft,
        hadExistingLogAtStart: Boolean
    ): Long =
        bodyRepository.log(
            userId = userId,
            date = draft.date,
            weightKg = draft.weightKg,
            bodyFatPct = draft.bodyFatPct,
            muscleMassKg = draft.muscleMassKg,
            waterLiters = draft.waterLiters,
            boneMassKg = draft.boneMassKg,
            visceralFatRating = draft.visceralFatRating,
            notes = if (hadExistingLogAtStart) null else draft.provenanceNote
        )
}
