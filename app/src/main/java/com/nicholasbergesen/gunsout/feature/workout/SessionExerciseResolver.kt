package com.nicholasbergesen.gunsout.feature.workout

import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.seed.LowerPosteriorCoreV1PrescriptionRepair

internal enum class SessionExerciseIdentitySource {
    PROGRAM_EXERCISE,
    SET_SNAPSHOT,
    OVERRIDE
}

internal data class SessionExerciseIdentity(
    val exerciseId: Long,
    val fallbackName: String? = null,
    val source: SessionExerciseIdentitySource = SessionExerciseIdentitySource.PROGRAM_EXERCISE
)

internal fun resolvedSessionExerciseIdentity(
    programExercise: ProgramExercise,
    currentSets: List<SetEntry>,
    overrideExerciseId: Long? = null
): SessionExerciseIdentity {
    if (overrideExerciseId != null) {
        return SessionExerciseIdentity(overrideExerciseId, source = SessionExerciseIdentitySource.OVERRIDE)
    }

    val snapshot = currentSets.consistentExerciseSnapshot()
    // V1-repaired retired rows may now point at replacement exercises; the set snapshot is the
    // in-progress session's source of truth when every set in that slot agrees.
    return if (programExercise.isRetired && snapshot != null) {
        snapshot
    } else {
        SessionExerciseIdentity(programExercise.exerciseId)
    }
}

internal fun resolvedSessionExercisePrescription(
    programExercise: ProgramExercise,
    identity: SessionExerciseIdentity,
    rowExerciseSeedKey: String?,
    snapshotExerciseSeedKey: String?
): ProgramExercise {
    if (!programExercise.isRetired || identity.source != SessionExerciseIdentitySource.SET_SNAPSHOT) {
        return programExercise
    }
    val repair = LowerPosteriorCoreV1PrescriptionRepair.legacyPlanForRepair(
        replacementSeedKey = rowExerciseSeedKey,
        snapshotSeedKey = snapshotExerciseSeedKey
    ) ?: return programExercise
    return programExercise.copy(
        exerciseId = identity.exerciseId,
        sets = repair.sets,
        repsMin = repair.repsMin,
        repsMax = repair.repsMax,
        restSec = repair.restSec,
        rpeTarget = repair.rpeTarget,
        supersetGroupId = repair.supersetGroupId,
        protocol = repair.protocol
    )
}

private fun List<SetEntry>.consistentExerciseSnapshot(): SessionExerciseIdentity? {
    val first = firstOrNull() ?: return null
    if (any { it.exerciseIdSnapshot != first.exerciseIdSnapshot }) return null

    return SessionExerciseIdentity(
        exerciseId = first.exerciseIdSnapshot,
        fallbackName = first.exerciseNameSnapshot.takeIf { it.isNotBlank() },
        source = SessionExerciseIdentitySource.SET_SNAPSHOT
    )
}
