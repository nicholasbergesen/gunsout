package com.nicholasbergesen.gunsout.feature.workout

import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.SetEntry

internal data class SessionExerciseIdentity(
    val exerciseId: Long,
    val fallbackName: String? = null
)

internal fun resolvedSessionExerciseIdentity(
    programExercise: ProgramExercise,
    currentSets: List<SetEntry>,
    overrideExerciseId: Long? = null
): SessionExerciseIdentity {
    if (overrideExerciseId != null) return SessionExerciseIdentity(overrideExerciseId)

    val snapshot = currentSets.consistentExerciseSnapshot()
    // V1-repaired retired rows may now point at replacement exercises; the set snapshot is the
    // in-progress session's source of truth when every set in that slot agrees.
    return if (programExercise.isRetired && snapshot != null) {
        snapshot
    } else {
        SessionExerciseIdentity(programExercise.exerciseId)
    }
}

private fun List<SetEntry>.consistentExerciseSnapshot(): SessionExerciseIdentity? {
    val first = firstOrNull() ?: return null
    if (any { it.exerciseIdSnapshot != first.exerciseIdSnapshot }) return null

    return SessionExerciseIdentity(
        exerciseId = first.exerciseIdSnapshot,
        fallbackName = first.exerciseNameSnapshot.takeIf { it.isNotBlank() }
    )
}
