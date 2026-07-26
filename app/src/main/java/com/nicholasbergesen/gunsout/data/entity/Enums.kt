package com.nicholasbergesen.gunsout.data.entity

enum class Equipment { DUMBBELL, BARBELL, MACHINE, BODYWEIGHT, BENCH, CABLE, OTHER }

enum class MuscleGroup { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, QUADS, HAMSTRINGS, GLUTES, CALVES, CORE, FULL_BODY, OTHER }

enum class MovementPattern { PUSH, PULL, SQUAT, HINGE, LUNGE, ISOLATION, CALVES, CORE }

fun defaultMovementPatternFor(muscleGroup: MuscleGroup): MovementPattern = when (muscleGroup) {
    MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS -> MovementPattern.PUSH
    MuscleGroup.BACK, MuscleGroup.BICEPS -> MovementPattern.PULL
    MuscleGroup.FOREARMS -> MovementPattern.ISOLATION
    MuscleGroup.QUADS -> MovementPattern.SQUAT
    MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES -> MovementPattern.HINGE
    MuscleGroup.CALVES -> MovementPattern.CALVES
    MuscleGroup.CORE -> MovementPattern.CORE
    MuscleGroup.FULL_BODY, MuscleGroup.OTHER -> MovementPattern.ISOLATION
}

enum class AlternateReason { HOME, INJURY, NO_CABLE, STRENGTH_LOW, PREFERENCE }

enum class Protocol { STANDARD, PULL_UP_5X2_3, AMRAP }

enum class SessionStatus { PLANNED, IN_PROGRESS, COMPLETED, SKIPPED }

enum class ProgramType { UPPER_LOWER, PPL, FULL_BODY, CUSTOM }

enum class DayHint { MON, TUE, WED, THU, FRI, SAT, SUN }
