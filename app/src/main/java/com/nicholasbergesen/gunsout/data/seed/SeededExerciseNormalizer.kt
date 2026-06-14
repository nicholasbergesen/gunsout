package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.defaultMovementPatternFor

private val seededExercisesByKey = ExerciseSeeds.all.associateBy { it.key }

internal fun Exercise.withSeededMovementPatternBackfill(): Exercise {
    val seedExercise = seedKey
        ?.let(seededExercisesByKey::get)
        ?.exercise
        ?: return this
    if (primaryMuscleGroup != seedExercise.primaryMuscleGroup || equipment != seedExercise.equipment) {
        return this
    }
    val legacyDefault = defaultMovementPatternFor(primaryMuscleGroup)
    if (movementPattern != legacyDefault || movementPattern == seedExercise.movementPattern) {
        return this
    }
    return copy(movementPattern = seedExercise.movementPattern)
}
