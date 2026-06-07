package com.nicholasbergesen.gunsout.data.analysis

import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession
import kotlinx.serialization.Serializable

@Serializable
data class ExerciseSessionsExport(
    val schemaVersion: Int,
    val exportedAtIso: String,
    val format: String,
    val sessions: List<ExerciseSessionExport>
)

@Serializable
data class ExerciseSessionExport(
    val sourceSessionId: Long,
    val date: String,
    val dayLabel: String,
    val sessionType: String,
    val startedAt: String,
    val completedAt: String?,
    val kneeFeel: Int?,
    val notes: String?,
    val exercises: List<ExerciseSessionExerciseExport>
)

@Serializable
data class ExerciseSessionExerciseExport(
    val sourceExerciseId: Long,
    val name: String,
    val sets: List<ExerciseSessionSetExport>
)

@Serializable
data class ExerciseSessionSetExport(
    val sourceSetId: Long,
    val setIndex: Int,
    val weightKg: Double?,
    val reps: Int?,
    val rpe: Int?,
    val isWarmup: Boolean,
    val completedAt: String?
)

fun buildExerciseSessionsExport(
    exportedAtIso: String,
    sessions: List<Pair<WorkoutSession, List<SetEntry>>>
): ExerciseSessionsExport =
    ExerciseSessionsExport(
        schemaVersion = 1,
        exportedAtIso = exportedAtIso,
        format = "gunsout-exercise-sessions",
        sessions = sessions.map { (session, sets) -> session.toExerciseSessionExport(sets) }
    )

private fun WorkoutSession.toExerciseSessionExport(sets: List<SetEntry>): ExerciseSessionExport =
    ExerciseSessionExport(
        sourceSessionId = id,
        date = date.toString(),
        dayLabel = if (sets.isEmpty()) "Rest" else programDayLabelSnapshot,
        sessionType = if (sets.isEmpty()) "REST" else "WORKOUT",
        startedAt = startedAt.toString(),
        completedAt = completedAt?.toString(),
        kneeFeel = kneeFeel,
        notes = notes,
        exercises = sets
            .groupBy { it.exerciseIdSnapshot to it.exerciseNameSnapshot }
            .map { (exercise, exerciseSets) ->
                ExerciseSessionExerciseExport(
                    sourceExerciseId = exercise.first,
                    name = exercise.second,
                    sets = exerciseSets
                        .sortedWith(compareBy<SetEntry> { it.setIndex }.thenBy { it.isWarmup }.thenBy { it.id })
                        .map { it.toExerciseSessionSetExport() }
                )
            }
            .sortedBy { it.name }
    )

private fun SetEntry.toExerciseSessionSetExport(): ExerciseSessionSetExport =
    ExerciseSessionSetExport(
        sourceSetId = id,
        setIndex = setIndex,
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        isWarmup = isWarmup,
        completedAt = completedAt?.toString()
    )
