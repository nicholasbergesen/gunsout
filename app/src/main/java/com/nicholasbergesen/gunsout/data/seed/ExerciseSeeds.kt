package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.AlternateReason
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup

/** Seeded exercises, keyed by stable seedKey strings. */
object ExerciseSeeds {

    data class Seed(val key: String, val exercise: Exercise, val alternates: List<Pair<String, AlternateReason>> = emptyList())

    val all: List<Seed> = listOf(
        // Upper A
        Seed("inc_db_bench", Exercise(
            userId = "",
            name = "Incline Dumbbell Bench Press",
            primaryMuscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.DUMBBELL,
            formNotes = "Bench set to 30 degrees. Press straight over eyes. Lower until deep chest stretch. Elbows at 45 degrees to torso.",
            defaultRestSec = 90,
            baselineNote = "Start with 14 to 16 kg per hand. Heavy at your size is 24 kg plus.",
            seedKey = "inc_db_bench"
        )),
        Seed("pull_ups", Exercise(
            userId = "",
            name = "Pull-Ups (5x2-3 Protocol)",
            primaryMuscleGroup = MuscleGroup.BACK,
            equipment = Equipment.BODYWEIGHT,
            formNotes = "Dead hang start. Pull chest to bar, drive elbows down to ribs. Slow negative. 5 sets of 2-3 with 1 rep in the tank. Optional 5-second negative on last set.",
            defaultRestSec = 120,
            baselineNote = "Aim for 12-14 total quality reps across all sets.",
            seedKey = "pull_ups"
        ), alternates = listOf(
            "lat_pulldown" to AlternateReason.STRENGTH_LOW,
            "assisted_pullup" to AlternateReason.STRENGTH_LOW
        )),
        Seed("lat_pulldown", Exercise(
            userId = "",
            name = "Lat Pulldown",
            primaryMuscleGroup = MuscleGroup.BACK,
            equipment = Equipment.CABLE,
            formNotes = "Wide grip just outside shoulders. Chest up, slight lean back. Pull to collarbone, elbows down to back pockets.",
            defaultRestSec = 90,
            seedKey = "lat_pulldown"
        )),
        Seed("assisted_pullup", Exercise(
            userId = "",
            name = "Assisted Pull-Up (Band or Machine)",
            primaryMuscleGroup = MuscleGroup.BACK,
            equipment = Equipment.MACHINE,
            formNotes = "Counterbalance reduces effective bodyweight. Set assistance so you can do 6-8 clean reps.",
            defaultRestSec = 90,
            seedKey = "assisted_pullup"
        )),
        Seed("db_shoulder_press", Exercise(
            userId = "",
            name = "Seated Dumbbell Shoulder Press",
            primaryMuscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Vertical back support. Palms forward. Press straight up to lockout, lower to ear level.",
            defaultRestSec = 60,
            baselineNote = "Start with 10 to 12 kg per hand.",
            seedKey = "db_shoulder_press"
        )),
        Seed("db_rear_delt_fly", Exercise(
            userId = "",
            name = "Dumbbell Rear Delt Flyes",
            primaryMuscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Seated, torso parallel to floor. Light dumbbells. Raise arms wide like wings, squeeze rear delts.",
            defaultRestSec = 60,
            baselineNote = "Start with 4 to 6 kg per hand. Never ego-lift.",
            seedKey = "db_rear_delt_fly"
        )),

        // Lower A
        Seed("leg_press", Exercise(
            userId = "",
            name = "Leg Press",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.MACHINE,
            formNotes = "Feet high and hip-width. Lower to 90 degrees, do not let lower back lift off pad. Drive through heels. KNEE SAFETY: stop on any pain or click.",
            defaultRestSec = 120,
            baselineNote = "Start with sled-only or one 10 kg per side. Knee comes first.",
            seedKey = "leg_press"
        ), alternates = listOf(
            "back_squat" to AlternateReason.PREFERENCE,
            "bulgarian_split_squat" to AlternateReason.HOME
        )),
        Seed("back_squat", Exercise(
            userId = "",
            name = "Barbell Back Squat",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.BARBELL,
            formNotes = "Bar on upper traps. Squat to parallel or below. Knees track over toes. KNEE SAFETY caution.",
            defaultRestSec = 120,
            seedKey = "back_squat"
        )),
        Seed("bulgarian_split_squat", Exercise(
            userId = "",
            name = "Bulgarian Split Squat",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Rear foot on bench, ~2 feet ahead. Lower until back knee almost touches. Drive through front heel.",
            defaultRestSec = 90,
            seedKey = "bulgarian_split_squat"
        )),
        Seed("rdl", Exercise(
            userId = "",
            name = "Romanian Deadlift",
            primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Spine flat, hips back to the wall behind you. Lower down shins until deep hamstring stretch. Drive hips forward to stand.",
            defaultRestSec = 90,
            baselineNote = "Start with 16 kg per hand. Hip hinge protects knees.",
            seedKey = "rdl"
        )),
        Seed("leg_extension", Exercise(
            userId = "",
            name = "Leg Extensions",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.MACHINE,
            formNotes = "Pad on lower shins. Extend, squeeze quads hard for 1 second at top, lower slowly.",
            defaultRestSec = 60,
            seedKey = "leg_extension"
        ), alternates = listOf(
            "goblet_cyclist_squat" to AlternateReason.HOME
        )),
        Seed("goblet_cyclist_squat", Exercise(
            userId = "",
            name = "Goblet Cyclist Squat",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Heels elevated on plates or block. Dumbbell vertical at chest. Squat deep, loads quads.",
            defaultRestSec = 60,
            seedKey = "goblet_cyclist_squat"
        )),
        Seed("standing_calf_raise", Exercise(
            userId = "",
            name = "Standing Dumbbell Calf Raises",
            primaryMuscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.DUMBBELL,
            formNotes = "Stand on a step or block. Heels drop low for stretch, explode up onto toes.",
            defaultRestSec = 60,
            seedKey = "standing_calf_raise"
        )),

        // Upper B
        Seed("chest_supported_row", Exercise(
            userId = "",
            name = "Chest-Supported Dumbbell Row",
            primaryMuscleGroup = MuscleGroup.BACK,
            equipment = Equipment.DUMBBELL,
            formNotes = "Lie face down on incline bench. Row dumbbells past torso, squeeze shoulder blades, 1-second pause.",
            defaultRestSec = 90,
            seedKey = "chest_supported_row"
        ), alternates = listOf("barbell_row" to AlternateReason.PREFERENCE)),
        Seed("barbell_row", Exercise(
            userId = "",
            name = "Barbell Row",
            primaryMuscleGroup = MuscleGroup.BACK,
            equipment = Equipment.BARBELL,
            formNotes = "Hinge at hips, flat back. Pull bar to belly button, elbows to the ceiling.",
            defaultRestSec = 90,
            seedKey = "barbell_row"
        )),
        Seed("flat_db_bench", Exercise(
            userId = "",
            name = "Flat Dumbbell Bench Press",
            primaryMuscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.DUMBBELL,
            formNotes = "Press straight up from chest level. Control descent.",
            defaultRestSec = 90,
            baselineNote = "Start with 14 to 16 kg per hand.",
            seedKey = "flat_db_bench"
        )),
        Seed("db_lateral_raise", Exercise(
            userId = "",
            name = "Dumbbell Lateral Raises",
            primaryMuscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Slight bend in elbows. Raise arms wide to parallel with floor. Lower slowly.",
            defaultRestSec = 60,
            baselineNote = "Start with 4 to 6 kg per hand.",
            seedKey = "db_lateral_raise"
        )),
        Seed("db_bicep_curl", Exercise(
            userId = "",
            name = "Dumbbell Bicep Curl",
            primaryMuscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Stand tall. Curl with control, supinate at top.",
            defaultRestSec = 60,
            seedKey = "db_bicep_curl"
        )),
        Seed("db_overhead_tricep", Exercise(
            userId = "",
            name = "Dumbbell Overhead Tricep Extension",
            primaryMuscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Hold one heavy dumbbell overhead with both hands. Lower behind head, stretch triceps, press up.",
            defaultRestSec = 60,
            seedKey = "db_overhead_tricep"
        )),

        // Lower B
        Seed("leg_curl", Exercise(
            userId = "",
            name = "Lying or Seated Leg Curl",
            primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.MACHINE,
            formNotes = "Pad on Achilles. Pull heels tight to glutes, squeeze hamstrings at peak.",
            defaultRestSec = 60,
            seedKey = "leg_curl"
        ), alternates = listOf("db_lying_leg_curl" to AlternateReason.HOME)),
        Seed("db_lying_leg_curl", Exercise(
            userId = "",
            name = "Dumbbell Lying Leg Curl",
            primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Face-down on bench, knees at edge. Squeeze dumbbell vertically between feet. Curl up.",
            defaultRestSec = 60,
            seedKey = "db_lying_leg_curl"
        )),
        Seed("goblet_squat", Exercise(
            userId = "",
            name = "Goblet Squat",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.DUMBBELL,
            formNotes = "Dumbbell vertical at chest. Squat deep, chest proud. Drive through heels.",
            defaultRestSec = 90,
            baselineNote = "Start with 12 to 14 kg.",
            seedKey = "goblet_squat"
        )),
        Seed("hip_thrust", Exercise(
            userId = "",
            name = "Hip Thrusts",
            primaryMuscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.BARBELL,
            formNotes = "Upper back on bench. Barbell or dumbbell across hips. Drive feet down, push hips up to a straight line.",
            defaultRestSec = 90,
            seedKey = "hip_thrust"
        )),
        Seed("lying_leg_raise", Exercise(
            userId = "",
            name = "Lying Leg Raises",
            primaryMuscleGroup = MuscleGroup.CORE,
            equipment = Equipment.BODYWEIGHT,
            formNotes = "Flat on bench, grip behind head. Raise legs to 90 degrees, lower slowly without arching back.",
            defaultRestSec = 60,
            seedKey = "lying_leg_raise"
        ))
    )
}
