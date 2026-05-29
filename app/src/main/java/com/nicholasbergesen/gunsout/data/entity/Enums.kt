package com.nicholasbergesen.gunsout.data.entity

enum class Equipment { DUMBBELL, BARBELL, MACHINE, BODYWEIGHT, BENCH, CABLE, OTHER }

enum class MuscleGroup { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, QUADS, HAMSTRINGS, GLUTES, CALVES, CORE, FULL_BODY, OTHER }

enum class AlternateReason { HOME, INJURY, NO_CABLE, STRENGTH_LOW, PREFERENCE }

enum class Protocol { STANDARD, PULL_UP_5X2_3, AMRAP }

enum class SessionStatus { PLANNED, IN_PROGRESS, COMPLETED, SKIPPED }

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK, SMOOTHIE }

enum class SupplementUnit { MG, G, ML, CAPSULE }

enum class ProgramType { UPPER_LOWER, PPL, FULL_BODY, CUSTOM }

enum class DayHint { MON, TUE, WED, THU, FRI, SAT, SUN }
