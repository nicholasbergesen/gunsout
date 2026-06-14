package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.entity.AlternateReason
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.entity.MovementPattern

/** Seeded exercises, keyed by stable seedKey strings. */
object ExerciseSeeds {

    data class Seed(val key: String, val exercise: Exercise, val alternates: List<Pair<String, AlternateReason>> = emptyList())

    val all: List<Seed> = listOf(
        // Upper Push/Pull
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
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Seated, torso parallel to floor. Light dumbbells. Raise arms wide like wings, squeeze rear delts.",
            defaultRestSec = 60,
            baselineNote = "Start with 4 to 6 kg per hand. Never ego-lift.",
            seedKey = "db_rear_delt_fly"
        )),

        // Lower Strength
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
            movementPattern = MovementPattern.ISOLATION,
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

        // Upper Hypertrophy
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
            movementPattern = MovementPattern.ISOLATION,
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
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Stand tall. Curl with control, supinate at top.",
            defaultRestSec = 60,
            seedKey = "db_bicep_curl"
        )),
        Seed("db_overhead_tricep", Exercise(
            userId = "",
            name = "Dumbbell Overhead Tricep Extension",
            primaryMuscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.DUMBBELL,
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Hold one heavy dumbbell overhead with both hands. Lower behind head, stretch triceps, press up.",
            defaultRestSec = 60,
            seedKey = "db_overhead_tricep"
        )),

        // Lower Posterior/Core
        Seed("leg_curl", Exercise(
            userId = "",
            name = "Lying or Seated Leg Curl",
            primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Pad on Achilles. Pull heels tight to glutes, squeeze hamstrings at peak.",
            defaultRestSec = 60,
            seedKey = "leg_curl"
        ), alternates = listOf("db_lying_leg_curl" to AlternateReason.HOME)),
        Seed("db_lying_leg_curl", Exercise(
            userId = "",
            name = "Dumbbell Lying Leg Curl",
            primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.DUMBBELL,
            movementPattern = MovementPattern.ISOLATION,
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
        )),

        // Expanded commercial-gym library
        Seed("barbell_bench_press", Exercise(
            userId = "",
            name = "Barbell Bench Press",
            primaryMuscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.PUSH,
            formNotes = "Shoulder blades pinned. Touch lower chest, press up and slightly back. Total bar weight includes the bar.",
            defaultRestSec = 120,
            seedKey = "barbell_bench_press"
        )),
        Seed("machine_chest_press", Exercise(
            userId = "",
            name = "Machine Chest Press",
            primaryMuscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.PUSH,
            formNotes = "Seat set so handles start around mid-chest. Press smoothly without shrugging.",
            defaultRestSec = 90,
            seedKey = "machine_chest_press"
        )),
        Seed("pec_deck_fly", Exercise(
            userId = "",
            name = "Pec Deck Fly",
            primaryMuscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Keep chest tall and elbows softly bent. Bring pads together without bouncing.",
            defaultRestSec = 60,
            seedKey = "pec_deck_fly"
        )),
        Seed("cable_chest_fly", Exercise(
            userId = "",
            name = "Cable Chest Fly",
            primaryMuscleGroup = MuscleGroup.CHEST,
            equipment = Equipment.CABLE,
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Cables set around chest height. Step forward, hug wide arcs together, control the stretch.",
            defaultRestSec = 60,
            seedKey = "cable_chest_fly"
        )),
        Seed("bodyweight_dip", Exercise(
            userId = "",
            name = "Bodyweight Dip",
            primaryMuscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.BODYWEIGHT,
            movementPattern = MovementPattern.PUSH,
            formNotes = "Use parallel bars. Lower under control until shoulders stay comfortable, press to lockout.",
            defaultRestSec = 90,
            seedKey = "bodyweight_dip"
        )),
        Seed("seated_cable_row", Exercise(
            userId = "",
            name = "Seated Cable Row",
            primaryMuscleGroup = MuscleGroup.BACK,
            equipment = Equipment.CABLE,
            movementPattern = MovementPattern.PULL,
            formNotes = "Tall torso. Pull handle to lower ribs, pause, and return until lats stretch.",
            defaultRestSec = 90,
            seedKey = "seated_cable_row"
        )),
        Seed("machine_row", Exercise(
            userId = "",
            name = "Machine Row",
            primaryMuscleGroup = MuscleGroup.BACK,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.PULL,
            formNotes = "Chest on pad if available. Pull elbows back without leaning away from the machine.",
            defaultRestSec = 90,
            seedKey = "machine_row"
        )),
        Seed("single_arm_db_row", Exercise(
            userId = "",
            name = "Single-Arm Dumbbell Row",
            primaryMuscleGroup = MuscleGroup.BACK,
            equipment = Equipment.DUMBBELL,
            movementPattern = MovementPattern.PULL,
            formNotes = "One hand on bench. Row dumbbell toward hip, keep torso quiet. Load is per hand.",
            defaultRestSec = 90,
            seedKey = "single_arm_db_row"
        )),
        Seed("face_pull", Exercise(
            userId = "",
            name = "Cable Face Pull",
            primaryMuscleGroup = MuscleGroup.SHOULDERS,
            equipment = Equipment.CABLE,
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Rope at eye height. Pull toward forehead with elbows high and shoulder blades moving.",
            defaultRestSec = 60,
            seedKey = "face_pull"
        )),
        Seed("straight_arm_pulldown", Exercise(
            userId = "",
            name = "Straight-Arm Pulldown",
            primaryMuscleGroup = MuscleGroup.BACK,
            equipment = Equipment.CABLE,
            movementPattern = MovementPattern.PULL,
            formNotes = "Arms nearly straight. Pull bar from shoulder height to thighs using lats.",
            defaultRestSec = 60,
            seedKey = "straight_arm_pulldown"
        )),
        Seed("hack_squat", Exercise(
            userId = "",
            name = "Hack Squat",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.SQUAT,
            formNotes = "Feet shoulder-width on platform. Squat deep while keeping hips and back on pads.",
            defaultRestSec = 120,
            seedKey = "hack_squat"
        )),
        Seed("smith_squat", Exercise(
            userId = "",
            name = "Smith Machine Squat",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.SQUAT,
            formNotes = "Feet slightly forward. Sit between heels, drive up evenly. Total load includes the bar path setting.",
            defaultRestSec = 120,
            seedKey = "smith_squat"
        )),
        Seed("conventional_deadlift", Exercise(
            userId = "",
            name = "Conventional Deadlift",
            primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.HINGE,
            formNotes = "Bar over mid-foot. Brace, push floor away, and keep bar close. Total load includes the bar.",
            defaultRestSec = 150,
            seedKey = "conventional_deadlift"
        )),
        Seed("trap_bar_deadlift", Exercise(
            userId = "",
            name = "Trap Bar Deadlift",
            primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.HINGE,
            formNotes = "Stand centered in trap bar. Brace, drive through the floor, and lock out tall.",
            defaultRestSec = 150,
            seedKey = "trap_bar_deadlift"
        )),
        Seed("back_extension", Exercise(
            userId = "",
            name = "Back Extension",
            primaryMuscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.BODYWEIGHT,
            movementPattern = MovementPattern.HINGE,
            formNotes = "Hips on pad. Fold at hips, squeeze glutes to return to a straight line.",
            defaultRestSec = 60,
            seedKey = "back_extension"
        )),
        Seed("cable_pull_through", Exercise(
            userId = "",
            name = "Cable Pull-Through",
            primaryMuscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.CABLE,
            movementPattern = MovementPattern.HINGE,
            formNotes = "Rope on low pulley. Hinge away from stack, then drive hips through.",
            defaultRestSec = 75,
            seedKey = "cable_pull_through"
        )),
        Seed("db_walking_lunge", Exercise(
            userId = "",
            name = "Dumbbell Walking Lunge",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.DUMBBELL,
            movementPattern = MovementPattern.LUNGE,
            formNotes = "Long smooth steps. Front knee tracks over toes, back knee lowers under control. Load is per hand.",
            defaultRestSec = 90,
            seedKey = "db_walking_lunge"
        )),
        Seed("db_reverse_lunge", Exercise(
            userId = "",
            name = "Dumbbell Reverse Lunge",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.DUMBBELL,
            movementPattern = MovementPattern.LUNGE,
            formNotes = "Step back, lower under control, and drive through the front foot. Load is per hand.",
            defaultRestSec = 90,
            seedKey = "db_reverse_lunge"
        )),
        Seed("db_step_up", Exercise(
            userId = "",
            name = "Dumbbell Step-Up",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.DUMBBELL,
            movementPattern = MovementPattern.LUNGE,
            formNotes = "Use a box around knee height. Step through the working leg without bouncing off the floor leg.",
            defaultRestSec = 90,
            seedKey = "db_step_up"
        )),
        Seed("triceps_pressdown", Exercise(
            userId = "",
            name = "Cable Triceps Pressdown",
            primaryMuscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.CABLE,
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Elbows pinned to sides. Press rope or bar down and apart, then return under control.",
            defaultRestSec = 60,
            seedKey = "triceps_pressdown"
        )),
        Seed("rope_overhead_triceps", Exercise(
            userId = "",
            name = "Rope Overhead Triceps Extension",
            primaryMuscleGroup = MuscleGroup.TRICEPS,
            equipment = Equipment.CABLE,
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Cable low behind you. Elbows high, stretch long head, extend without flaring.",
            defaultRestSec = 60,
            seedKey = "rope_overhead_triceps"
        )),
        Seed("preacher_curl", Exercise(
            userId = "",
            name = "Preacher Curl",
            primaryMuscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Upper arms fixed to pad. Curl smoothly, avoid relaxing at the bottom.",
            defaultRestSec = 60,
            seedKey = "preacher_curl"
        )),
        Seed("machine_biceps_curl", Exercise(
            userId = "",
            name = "Machine Biceps Curl",
            primaryMuscleGroup = MuscleGroup.BICEPS,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.ISOLATION,
            formNotes = "Seat adjusted so elbows align with pivot. Curl without lifting shoulders.",
            defaultRestSec = 60,
            seedKey = "machine_biceps_curl"
        )),
        Seed("seated_calf_raise", Exercise(
            userId = "",
            name = "Seated Calf Raise",
            primaryMuscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.CALVES,
            formNotes = "Pads over thighs. Full stretch at bottom, hard squeeze at top.",
            defaultRestSec = 60,
            seedKey = "seated_calf_raise"
        )),
        Seed("machine_calf_raise", Exercise(
            userId = "",
            name = "Machine Standing Calf Raise",
            primaryMuscleGroup = MuscleGroup.CALVES,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.CALVES,
            formNotes = "Knees softly unlocked. Drop heels for a full stretch, rise high onto toes.",
            defaultRestSec = 60,
            seedKey = "machine_calf_raise"
        )),
        Seed("cable_crunch", Exercise(
            userId = "",
            name = "Cable Crunch",
            primaryMuscleGroup = MuscleGroup.CORE,
            equipment = Equipment.CABLE,
            movementPattern = MovementPattern.CORE,
            formNotes = "Kneel facing stack. Curl ribs toward pelvis without pulling with arms.",
            defaultRestSec = 60,
            seedKey = "cable_crunch"
        )),
        Seed("machine_crunch", Exercise(
            userId = "",
            name = "Machine Crunch",
            primaryMuscleGroup = MuscleGroup.CORE,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.CORE,
            formNotes = "Set pads comfortably. Crunch by rounding trunk, not yanking with arms.",
            defaultRestSec = 60,
            seedKey = "machine_crunch"
        )),
        Seed("hanging_knee_raise", Exercise(
            userId = "",
            name = "Hanging Knee Raise",
            primaryMuscleGroup = MuscleGroup.CORE,
            equipment = Equipment.BODYWEIGHT,
            movementPattern = MovementPattern.CORE,
            formNotes = "Hang from bar or captain's chair. Curl knees toward chest without swinging.",
            defaultRestSec = 60,
            seedKey = "hanging_knee_raise"
        )),
        Seed("ab_wheel_rollout", Exercise(
            userId = "",
            name = "Ab Wheel Rollout",
            primaryMuscleGroup = MuscleGroup.CORE,
            equipment = Equipment.BODYWEIGHT,
            movementPattern = MovementPattern.CORE,
            formNotes = "Start from knees. Brace ribs down, roll out only as far as you can return with control.",
            defaultRestSec = 60,
            seedKey = "ab_wheel_rollout"
        ))
    )
}
