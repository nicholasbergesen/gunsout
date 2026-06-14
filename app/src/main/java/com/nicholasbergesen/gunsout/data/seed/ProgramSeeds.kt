package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.DayHint
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.ProgramType

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
        val programType: ProgramType = ProgramType.CUSTOM,
        val notes: String? = null,
        val days: List<PlanDay>
    )

    val upperLower4Day = PlanProgram(
        seedKey = "upper_lower_4day",
        name = "Upper / Lower 4-Day",
        programType = ProgramType.UPPER_LOWER,
        notes = "Balanced strength and hypertrophy across four weekly lifting days, with cable and dumbbell accessories.",
        days = listOf(
            PlanDay(0, "Upper Push/Pull", DayHint.MON, exercises = listOf(
                PlanExercise("inc_db_bench", 3, 8, 10, 90),
                PlanExercise("pull_ups", 5, 2, 3, 120, protocol = Protocol.PULL_UP_5X2_3),
                PlanExercise("lat_pulldown", 3, 8, 10, 90),
                PlanExercise("db_shoulder_press", 3, 10, 10, 60),
                PlanExercise("db_rear_delt_fly", 3, 15, 15, 60)
            )),
            PlanDay(1, "Lower Strength", DayHint.TUE, exercises = listOf(
                PlanExercise("leg_press", 4, 8, 8, 120),
                PlanExercise("rdl", 3, 10, 10, 90),
                PlanExercise("leg_extension", 3, 12, 15, 60),
                PlanExercise("standing_calf_raise", 3, 15, 15, 60)
            )),
            PlanDay(2, "Rest", DayHint.WED, isRest = true),
            PlanDay(3, "Upper Hypertrophy", DayHint.THU, exercises = listOf(
                PlanExercise("chest_supported_row", 3, 8, 10, 90),
                PlanExercise("flat_db_bench", 3, 10, 10, 90),
                PlanExercise("db_lateral_raise", 4, 12, 15, 60),
                PlanExercise("db_bicep_curl", 3, 10, 10, 60, supersetGroupId = 1),
                PlanExercise("db_overhead_tricep", 3, 12, 12, 60, supersetGroupId = 1),
                PlanExercise("db_wrist_curl", 2, 12, 15, 45),
                PlanExercise("db_reverse_wrist_curl", 2, 12, 15, 45)
            )),
            PlanDay(4, "Lower Posterior/Core", DayHint.FRI, exercises = listOf(
                PlanExercise("leg_curl", 3, 10, 12, 60),
                PlanExercise("goblet_squat", 3, 10, 10, 90),
                PlanExercise("trap_bar_deadlift", 3, 8, 10, 150),
                PlanExercise("machine_crunch", 3, 10, 15, 60)
            )),
            PlanDay(5, "Rest", DayHint.SAT, isRest = true),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val runnerStrength2Day = PlanProgram(
        seedKey = "runner_strength_2day",
        name = "Runner Strength 2-Day",
        programType = ProgramType.FULL_BODY,
        notes = "Two lower-impact strength sessions for runners who need durable hips, hamstrings, calves, and trunk control.",
        days = listOf(
            PlanDay(0, "Stride Strength", DayHint.MON, exercises = listOf(
                PlanExercise("db_reverse_lunge", 3, 8, 10, 90),
                PlanExercise("leg_curl", 3, 10, 12, 60),
                PlanExercise("standing_calf_raise", 4, 12, 15, 60),
                PlanExercise("ab_wheel_rollout", 3, 6, 10, 60)
            )),
            PlanDay(1, "Rest", DayHint.TUE, isRest = true),
            PlanDay(2, "Hill Support", DayHint.WED, exercises = listOf(
                PlanExercise("goblet_squat", 3, 8, 10, 90),
                PlanExercise("rdl", 3, 8, 10, 90),
                PlanExercise("db_step_up", 3, 8, 10, 90),
                PlanExercise("hanging_knee_raise", 3, 10, 12, 60)
            )),
            PlanDay(3, "Rest", DayHint.THU, isRest = true),
            PlanDay(4, "Rest", DayHint.FRI, isRest = true),
            PlanDay(5, "Rest", DayHint.SAT, isRest = true),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val runnerRacePrep3Day = PlanProgram(
        seedKey = "runner_race_prep_3day",
        name = "Runner Race Prep 3-Day",
        programType = ProgramType.FULL_BODY,
        notes = "A three-day plan for runners who want strength, posture, and core work around a race block.",
        days = listOf(
            PlanDay(0, "Lower Power", DayHint.MON, exercises = listOf(
                PlanExercise("trap_bar_deadlift", 3, 5, 6, 150),
                PlanExercise("bulgarian_split_squat", 3, 8, 8, 90),
                PlanExercise("seated_calf_raise", 4, 10, 12, 60),
                PlanExercise("cable_crunch", 3, 10, 15, 60)
            )),
            PlanDay(1, "Rest", DayHint.TUE, isRest = true),
            PlanDay(2, "Upper Posture", DayHint.WED, exercises = listOf(
                PlanExercise("seated_cable_row", 3, 10, 12, 75),
                PlanExercise("face_pull", 3, 12, 15, 60),
                PlanExercise("machine_chest_press", 3, 8, 10, 75),
                PlanExercise("db_lateral_raise", 3, 12, 15, 60)
            )),
            PlanDay(3, "Rest", DayHint.THU, isRest = true),
            PlanDay(4, "Single-Leg Control", DayHint.FRI, exercises = listOf(
                PlanExercise("db_walking_lunge", 3, 10, 12, 90),
                PlanExercise("back_extension", 3, 10, 12, 60),
                PlanExercise("goblet_cyclist_squat", 3, 10, 12, 75),
                PlanExercise("lying_leg_raise", 3, 12, 15, 60)
            )),
            PlanDay(5, "Rest", DayHint.SAT, isRest = true),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val cyclistStrength2Day = PlanProgram(
        seedKey = "cyclist_strength_2day",
        name = "Cyclist Strength 2-Day",
        programType = ProgramType.FULL_BODY,
        notes = "Two heavy but compact gym sessions for cyclists who need force production and posterior-chain balance.",
        days = listOf(
            PlanDay(0, "Pedal Force", DayHint.MON, exercises = listOf(
                PlanExercise("leg_press", 4, 6, 8, 150),
                PlanExercise("rdl", 3, 8, 10, 120),
                PlanExercise("seated_calf_raise", 4, 10, 12, 60),
                PlanExercise("machine_crunch", 3, 10, 15, 60)
            )),
            PlanDay(1, "Rest", DayHint.TUE, isRest = true),
            PlanDay(2, "Stability and Pull", DayHint.WED, exercises = listOf(
                PlanExercise("bulgarian_split_squat", 3, 8, 10, 90),
                PlanExercise("leg_curl", 3, 10, 12, 60),
                PlanExercise("seated_cable_row", 3, 10, 12, 75),
                PlanExercise("face_pull", 3, 12, 15, 60)
            )),
            PlanDay(3, "Rest", DayHint.THU, isRest = true),
            PlanDay(4, "Rest", DayHint.FRI, isRest = true),
            PlanDay(5, "Rest", DayHint.SAT, isRest = true),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val maleStrength4Day = PlanProgram(
        seedKey = "male_strength_4day",
        name = "Male Strength 4-Day",
        programType = ProgramType.UPPER_LOWER,
        notes = "Four strength-focused sessions built around heavy barbell and machine compounds.",
        days = listOf(
            PlanDay(0, "Squat and Bench", DayHint.MON, exercises = listOf(
                PlanExercise("back_squat", 4, 3, 5, 180),
                PlanExercise("barbell_bench_press", 4, 3, 5, 180),
                PlanExercise("barbell_row", 4, 5, 6, 150),
                PlanExercise("cable_crunch", 3, 10, 12, 60)
            )),
            PlanDay(1, "Rest", DayHint.TUE, isRest = true),
            PlanDay(2, "Deadlift and Press", DayHint.WED, exercises = listOf(
                PlanExercise("conventional_deadlift", 3, 3, 5, 180),
                PlanExercise("db_shoulder_press", 4, 5, 8, 120),
                PlanExercise("pull_ups", 5, 2, 3, 120, protocol = Protocol.PULL_UP_5X2_3),
                PlanExercise("machine_calf_raise", 3, 10, 12, 60)
            )),
            PlanDay(3, "Rest", DayHint.THU, isRest = true),
            PlanDay(4, "Upper Volume", DayHint.FRI, exercises = listOf(
                PlanExercise("inc_db_bench", 4, 8, 10, 90),
                PlanExercise("seated_cable_row", 4, 8, 10, 90),
                PlanExercise("db_lateral_raise", 3, 12, 15, 60),
                PlanExercise("triceps_pressdown", 3, 10, 12, 60)
            )),
            PlanDay(5, "Lower Volume", DayHint.SAT, exercises = listOf(
                PlanExercise("hack_squat", 4, 8, 10, 120),
                PlanExercise("leg_curl", 3, 10, 12, 60),
                PlanExercise("hip_thrust", 3, 8, 10, 90),
                PlanExercise("ab_wheel_rollout", 3, 6, 10, 60)
            )),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val femaleStrength4Day = PlanProgram(
        seedKey = "female_strength_4day",
        name = "Female Strength 4-Day",
        programType = ProgramType.UPPER_LOWER,
        notes = "Four strength sessions emphasizing glutes, legs, upper-body strength, and core balance.",
        days = listOf(
            PlanDay(0, "Lower Strength", DayHint.MON, exercises = listOf(
                PlanExercise("hip_thrust", 4, 5, 8, 150),
                PlanExercise("hack_squat", 4, 6, 8, 150),
                PlanExercise("leg_curl", 3, 8, 10, 75),
                PlanExercise("machine_calf_raise", 3, 10, 12, 60)
            )),
            PlanDay(1, "Upper Strength", DayHint.TUE, exercises = listOf(
                PlanExercise("flat_db_bench", 4, 6, 8, 120),
                PlanExercise("lat_pulldown", 4, 6, 8, 120),
                PlanExercise("db_shoulder_press", 3, 8, 10, 90),
                PlanExercise("face_pull", 3, 12, 15, 60)
            )),
            PlanDay(2, "Rest", DayHint.WED, isRest = true),
            PlanDay(3, "Glutes and Posterior", DayHint.THU, exercises = listOf(
                PlanExercise("rdl", 4, 8, 10, 120),
                PlanExercise("db_walking_lunge", 3, 10, 12, 90),
                PlanExercise("cable_pull_through", 3, 10, 12, 75),
                PlanExercise("ab_wheel_rollout", 3, 6, 10, 60)
            )),
            PlanDay(4, "Rest", DayHint.FRI, isRest = true),
            PlanDay(5, "Upper and Core", DayHint.SAT, exercises = listOf(
                PlanExercise("machine_chest_press", 3, 8, 10, 90),
                PlanExercise("seated_cable_row", 3, 8, 10, 90),
                PlanExercise("db_lateral_raise", 3, 12, 15, 60),
                PlanExercise("cable_crunch", 3, 10, 15, 60)
            )),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val beginnerFullBody3Day = PlanProgram(
        seedKey = "beginner_full_body_3day",
        name = "Beginner Full Body 3-Day",
        programType = ProgramType.FULL_BODY,
        notes = "Simple full-body practice with machines and dumbbells for newer lifters.",
        days = listOf(
            PlanDay(0, "Full Body A", DayHint.MON, exercises = listOf(
                PlanExercise("leg_press", 3, 8, 10, 90),
                PlanExercise("machine_chest_press", 3, 8, 10, 90),
                PlanExercise("lat_pulldown", 3, 8, 10, 90),
                PlanExercise("machine_crunch", 2, 10, 15, 60)
            )),
            PlanDay(1, "Rest", DayHint.TUE, isRest = true),
            PlanDay(2, "Full Body B", DayHint.WED, exercises = listOf(
                PlanExercise("goblet_squat", 3, 8, 10, 90),
                PlanExercise("seated_cable_row", 3, 8, 10, 90),
                PlanExercise("db_shoulder_press", 3, 8, 10, 90),
                PlanExercise("standing_calf_raise", 2, 12, 15, 60)
            )),
            PlanDay(3, "Rest", DayHint.THU, isRest = true),
            PlanDay(4, "Full Body C", DayHint.FRI, exercises = listOf(
                PlanExercise("rdl", 3, 8, 10, 90),
                PlanExercise("flat_db_bench", 3, 8, 10, 90),
                PlanExercise("machine_row", 3, 8, 10, 90),
                PlanExercise("lying_leg_raise", 2, 10, 12, 60)
            )),
            PlanDay(5, "Rest", DayHint.SAT, isRest = true),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val hypertrophyPpl6Day = PlanProgram(
        seedKey = "hypertrophy_ppl_6day",
        name = "Hypertrophy PPL 6-Day",
        programType = ProgramType.PPL,
        notes = "A higher-volume push, pull, legs split for muscle gain with one weekly rest day.",
        days = listOf(
            PlanDay(0, "Push A", DayHint.MON, exercises = listOf(
                PlanExercise("barbell_bench_press", 4, 6, 8, 120),
                PlanExercise("db_shoulder_press", 3, 8, 10, 90),
                PlanExercise("pec_deck_fly", 3, 12, 15, 60),
                PlanExercise("triceps_pressdown", 3, 10, 12, 60)
            )),
            PlanDay(1, "Pull A", DayHint.TUE, exercises = listOf(
                PlanExercise("lat_pulldown", 4, 8, 10, 90),
                PlanExercise("seated_cable_row", 4, 8, 10, 90),
                PlanExercise("face_pull", 3, 12, 15, 60),
                PlanExercise("preacher_curl", 3, 10, 12, 60)
            )),
            PlanDay(2, "Legs A", DayHint.WED, exercises = listOf(
                PlanExercise("hack_squat", 4, 8, 10, 120),
                PlanExercise("leg_curl", 3, 10, 12, 60),
                PlanExercise("leg_extension", 3, 12, 15, 60),
                PlanExercise("standing_calf_raise", 4, 12, 15, 60)
            )),
            PlanDay(3, "Push B", DayHint.THU, exercises = listOf(
                PlanExercise("inc_db_bench", 4, 8, 10, 90),
                PlanExercise("db_lateral_raise", 4, 12, 15, 60),
                PlanExercise("cable_chest_fly", 3, 12, 15, 60),
                PlanExercise("rope_overhead_triceps", 3, 10, 12, 60)
            )),
            PlanDay(4, "Pull B", DayHint.FRI, exercises = listOf(
                PlanExercise("machine_row", 4, 8, 10, 90),
                PlanExercise("straight_arm_pulldown", 3, 12, 15, 60),
                PlanExercise("single_arm_db_row", 3, 10, 12, 75),
                PlanExercise("machine_biceps_curl", 3, 10, 12, 60)
            )),
            PlanDay(5, "Legs B", DayHint.SAT, exercises = listOf(
                PlanExercise("leg_press", 4, 10, 12, 120),
                PlanExercise("rdl", 3, 8, 10, 90),
                PlanExercise("db_walking_lunge", 3, 10, 12, 90),
                PlanExercise("seated_calf_raise", 4, 12, 15, 60)
            )),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val bodyRecomposition3Day = PlanProgram(
        seedKey = "body_recomposition_3day",
        name = "Body Recomposition 3-Day",
        programType = ProgramType.FULL_BODY,
        notes = "Three efficient full-body sessions for fat loss support while preserving muscle.",
        days = listOf(
            PlanDay(0, "Strength Density A", DayHint.MON, exercises = listOf(
                PlanExercise("leg_press", 3, 8, 10, 90),
                PlanExercise("flat_db_bench", 3, 8, 10, 90),
                PlanExercise("lat_pulldown", 3, 8, 10, 90),
                PlanExercise("machine_crunch", 3, 10, 15, 60)
            )),
            PlanDay(1, "Rest", DayHint.TUE, isRest = true),
            PlanDay(2, "Strength Density B", DayHint.WED, exercises = listOf(
                PlanExercise("rdl", 3, 8, 10, 90),
                PlanExercise("machine_chest_press", 3, 10, 12, 75),
                PlanExercise("seated_cable_row", 3, 10, 12, 75),
                PlanExercise("standing_calf_raise", 3, 12, 15, 60)
            )),
            PlanDay(3, "Rest", DayHint.THU, isRest = true),
            PlanDay(4, "Strength Density C", DayHint.FRI, exercises = listOf(
                PlanExercise("goblet_squat", 3, 10, 12, 90),
                PlanExercise("inc_db_bench", 3, 8, 10, 90),
                PlanExercise("chest_supported_row", 3, 8, 10, 90),
                PlanExercise("hanging_knee_raise", 3, 10, 12, 60)
            )),
            PlanDay(5, "Rest", DayHint.SAT, isRest = true),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val climberAntagonist3Day = PlanProgram(
        seedKey = "climber_antagonist_3day",
        name = "Climber Strength and Antagonists 3-Day",
        programType = ProgramType.FULL_BODY,
        notes = "Pulling strength, shoulder balance, pushing volume, and lower-body support for climbers.",
        days = listOf(
            PlanDay(0, "Pull Strength", DayHint.MON, exercises = listOf(
                PlanExercise("pull_ups", 5, 2, 3, 120, protocol = Protocol.PULL_UP_5X2_3),
                PlanExercise("chest_supported_row", 4, 8, 10, 90),
                PlanExercise("face_pull", 3, 12, 15, 60),
                PlanExercise("hanging_knee_raise", 3, 10, 12, 60)
            )),
            PlanDay(1, "Rest", DayHint.TUE, isRest = true),
            PlanDay(2, "Antagonist Push", DayHint.WED, exercises = listOf(
                PlanExercise("flat_db_bench", 4, 8, 10, 90),
                PlanExercise("db_shoulder_press", 3, 8, 10, 90),
                PlanExercise("db_rear_delt_fly", 3, 12, 15, 60),
                PlanExercise("triceps_pressdown", 3, 10, 12, 60)
            )),
            PlanDay(3, "Rest", DayHint.THU, isRest = true),
            PlanDay(4, "Lower and Core", DayHint.FRI, exercises = listOf(
                PlanExercise("goblet_squat", 3, 8, 10, 90),
                PlanExercise("rdl", 3, 8, 10, 90),
                PlanExercise("standing_calf_raise", 3, 12, 15, 60),
                PlanExercise("ab_wheel_rollout", 3, 6, 10, 60)
            )),
            PlanDay(5, "Rest", DayHint.SAT, isRest = true),
            PlanDay(6, "Rest", DayHint.SUN, isRest = true)
        )
    )

    val all = listOf(
        upperLower4Day,
        runnerStrength2Day,
        runnerRacePrep3Day,
        cyclistStrength2Day,
        maleStrength4Day,
        femaleStrength4Day,
        beginnerFullBody3Day,
        hypertrophyPpl6Day,
        bodyRecomposition3Day,
        climberAntagonist3Day
    )
}
