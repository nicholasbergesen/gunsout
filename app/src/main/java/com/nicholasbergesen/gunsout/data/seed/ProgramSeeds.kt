package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.DayHint
import com.nicholasbergesen.gunsout.data.entity.Protocol

object ProgramSeeds {

    /** A planned exercise inside a program day, referencing exercises by their seed key. */
    data class PlanExercise(
        val exerciseSeedKey: String,
        val sets: Int,
        val repsMin: Int,
        val repsMax: Int,
        val restSec: Int,
        val rpeTarget: Int? = null,
        val protocol: Protocol = Protocol.STANDARD,
        val supersetGroupId: Int? = null
    )

    data class PlanDay(
        val orderIndex: Int,
        val label: String,
        val preferredDayOfWeek: DayHint?,
        val isRest: Boolean = false,
        val exercises: List<PlanExercise> = emptyList()
    )

    data class PlanProgram(
        val seedKey: String,
        val name: String,
        val days: List<PlanDay>
    )

    val upperLower4Day = PlanProgram(
        seedKey = "upper_lower_4day",
        name = "Upper / Lower 4-Day (Free Weights)",
        days = listOf(
            PlanDay(0, "Upper A", DayHint.MON, exercises = listOf(
                PlanExercise("inc_db_bench", 3, 8, 10, 90),
                PlanExercise("pull_ups", 5, 2, 3, 120, protocol = Protocol.PULL_UP_5X2_3),
                PlanExercise("db_shoulder_press", 3, 10, 10, 60),
                PlanExercise("db_rear_delt_fly", 3, 15, 15, 60)
            )),
            PlanDay(1, "Lower A", DayHint.TUE, exercises = listOf(
                PlanExercise("leg_press", 4, 8, 8, 120),
                PlanExercise("rdl", 3, 10, 10, 90),
                PlanExercise("leg_extension", 3, 12, 15, 60),
                PlanExercise("standing_calf_raise", 3, 15, 15, 60)
            )),
            PlanDay(2, "Rest", DayHint.WED, isRest = true),
            PlanDay(3, "Upper B", DayHint.THU, exercises = listOf(
                PlanExercise("chest_supported_row", 3, 8, 10, 90),
                PlanExercise("flat_db_bench", 3, 10, 10, 90),
                PlanExercise("db_lateral_raise", 4, 12, 15, 60),
                PlanExercise("db_bicep_curl", 3, 10, 10, 60, supersetGroupId = 1),
                PlanExercise("db_overhead_tricep", 3, 12, 12, 60, supersetGroupId = 1)
            )),
            PlanDay(4, "Lower B", DayHint.FRI, exercises = listOf(
                PlanExercise("leg_curl", 3, 10, 12, 60),
                PlanExercise("goblet_squat", 3, 10, 10, 90),
                PlanExercise("hip_thrust", 3, 12, 12, 90),
                PlanExercise("lying_leg_raise", 3, 15, 15, 60)
            )),
            PlanDay(5, "Rest", DayHint.SAT, isRest = true),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )
}
