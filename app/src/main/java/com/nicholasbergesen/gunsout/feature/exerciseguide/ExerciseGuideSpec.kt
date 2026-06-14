package com.nicholasbergesen.gunsout.feature.exerciseguide

import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup

internal enum class BodyMuscleRegion {
    CHEST,
    UPPER_BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    QUADS,
    HAMSTRINGS,
    GLUTES,
    CALVES,
    CORE
}

internal enum class ExerciseGuideMotion {
    PUSH,
    PULL,
    SQUAT,
    HINGE,
    LUNGE,
    ISOLATION,
    CALVES,
    CORE
}

internal data class ExerciseGuideSpec(
    val targetLabel: String,
    val targetCue: String,
    val highlightedRegions: Set<BodyMuscleRegion>,
    val movementLabel: String,
    val movementCue: String,
    val motion: ExerciseGuideMotion
)

private data class MuscleGuide(
    val label: String,
    val cue: String,
    val regions: Set<BodyMuscleRegion>
)

private data class MovementGuide(
    val label: String,
    val cue: String,
    val motion: ExerciseGuideMotion
)

internal fun exerciseGuideSpecFor(
    muscleGroup: MuscleGroup,
    movementPattern: MovementPattern
): ExerciseGuideSpec {
    val muscleGuide = muscleGuideFor(muscleGroup)
    val movementGuide = movementGuideFor(movementPattern)
    return ExerciseGuideSpec(
        targetLabel = muscleGuide.label,
        targetCue = muscleGuide.cue,
        highlightedRegions = muscleGuide.regions,
        movementLabel = movementGuide.label,
        movementCue = movementGuide.cue,
        motion = movementGuide.motion
    )
}

@Suppress("REDUNDANT_ELSE_IN_WHEN")
private fun muscleGuideFor(muscleGroup: MuscleGroup): MuscleGuide = when (muscleGroup) {
    MuscleGroup.CHEST -> MuscleGuide(
        label = "Chest",
        cue = "Keep shoulder blades steady while the chest drives the press.",
        regions = setOf(BodyMuscleRegion.CHEST)
    )
    MuscleGroup.BACK -> MuscleGuide(
        label = "Back",
        cue = "Lead with the elbows and squeeze the upper back at the finish.",
        regions = setOf(BodyMuscleRegion.UPPER_BACK)
    )
    MuscleGroup.SHOULDERS -> MuscleGuide(
        label = "Shoulders",
        cue = "Move under control and avoid shrugging through the neck.",
        regions = setOf(BodyMuscleRegion.SHOULDERS)
    )
    MuscleGroup.BICEPS -> MuscleGuide(
        label = "Biceps",
        cue = "Keep upper arms quiet so the biceps do the work.",
        regions = setOf(BodyMuscleRegion.BICEPS)
    )
    MuscleGroup.TRICEPS -> MuscleGuide(
        label = "Triceps",
        cue = "Lock the upper arms in place and extend through the elbow.",
        regions = setOf(BodyMuscleRegion.TRICEPS)
    )
    MuscleGroup.QUADS -> MuscleGuide(
        label = "Quads",
        cue = "Track knees over toes and drive through the middle of the foot.",
        regions = setOf(BodyMuscleRegion.QUADS)
    )
    MuscleGroup.HAMSTRINGS -> MuscleGuide(
        label = "Hamstrings",
        cue = "Push hips back and feel the stretch behind the thighs.",
        regions = setOf(BodyMuscleRegion.HAMSTRINGS)
    )
    MuscleGroup.GLUTES -> MuscleGuide(
        label = "Glutes",
        cue = "Drive hips through and finish tall without arching the back.",
        regions = setOf(BodyMuscleRegion.GLUTES)
    )
    MuscleGroup.CALVES -> MuscleGuide(
        label = "Calves",
        cue = "Rise high onto the toes, pause, then lower with control.",
        regions = setOf(BodyMuscleRegion.CALVES)
    )
    MuscleGroup.CORE -> MuscleGuide(
        label = "Core",
        cue = "Brace first and keep the ribs stacked over the hips.",
        regions = setOf(BodyMuscleRegion.CORE)
    )
    MuscleGroup.FULL_BODY -> MuscleGuide(
        label = "Full body",
        cue = "Move as one connected chain while keeping the main joints stacked.",
        regions = BodyMuscleRegion.entries.toSet()
    )
    MuscleGroup.OTHER -> generalMuscleGuide()
    else -> generalMuscleGuide()
}

private fun generalMuscleGuide(): MuscleGuide = MuscleGuide(
    label = "General",
    cue = "Use the form notes for exercise-specific setup and tempo.",
    regions = BodyMuscleRegion.entries.toSet()
)

private fun movementGuideFor(movementPattern: MovementPattern): MovementGuide = when (movementPattern) {
    MovementPattern.PUSH -> MovementGuide(
        label = "Push",
        cue = "Press away under control, pause briefly, then return slowly.",
        motion = ExerciseGuideMotion.PUSH
    )
    MovementPattern.PULL -> MovementGuide(
        label = "Pull",
        cue = "Pull toward the body, squeeze, then extend without losing tension.",
        motion = ExerciseGuideMotion.PULL
    )
    MovementPattern.SQUAT -> MovementGuide(
        label = "Squat",
        cue = "Sit down between the knees, stay balanced, then drive back up.",
        motion = ExerciseGuideMotion.SQUAT
    )
    MovementPattern.HINGE -> MovementGuide(
        label = "Hinge",
        cue = "Send hips back with a flat back, then stand tall through the hips.",
        motion = ExerciseGuideMotion.HINGE
    )
    MovementPattern.LUNGE -> MovementGuide(
        label = "Lunge",
        cue = "Lower straight down in split stance, then drive through the lead leg.",
        motion = ExerciseGuideMotion.LUNGE
    )
    MovementPattern.ISOLATION -> MovementGuide(
        label = "Isolation",
        cue = "Keep the body still and move only the working joint.",
        motion = ExerciseGuideMotion.ISOLATION
    )
    MovementPattern.CALVES -> MovementGuide(
        label = "Calves",
        cue = "Lift the heels, squeeze at the top, then lower through the full range.",
        motion = ExerciseGuideMotion.CALVES
    )
    MovementPattern.CORE -> MovementGuide(
        label = "Core",
        cue = "Brace, move slowly, and keep the lower back from arching.",
        motion = ExerciseGuideMotion.CORE
    )
}
