package com.nicholasbergesen.gunsout.domain.recommendation

import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.prefs.Sex
import com.nicholasbergesen.gunsout.data.prefs.TrainingExperience
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import com.nicholasbergesen.gunsout.data.seed.ExerciseSeeds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExerciseRecommendationEngineTest {

    private val engine = ExerciseRecommendationEngine()

    @Test fun `every seeded exercise has a local formula`() {
        val missing = ExerciseSeeds.all.map { it.key }.toSet() - ExerciseFormulaCatalog.knownSeedKeys

        assertTrue("Missing formulas for $missing", missing.isEmpty())
    }

    @Test fun `first time weighted exercise returns exact rounded kg`() {
        val recommendation = engine.recommend(
            prescription = pe(),
            exercise = exercise(seedKey = "flat_db_bench", equipment = Equipment.DUMBBELL),
            previousWorkingSets = emptyList(),
            baselineWeekActive = false,
            profile = profile(),
            latestBodyLog = BodyMetricsLog(userId = "u", date = LocalDate.now(), weightKg = 100.0, muscleMassKg = 70.0),
            recentBodyLogs = emptyList()
        )

        assertEquals(RecommendationTarget.WEIGHT_KG, recommendation!!.target)
        assertEquals(16.0, recommendation.weightKg!!, 0.001)
        assertEquals("Start with 16 kg", recommendation.displayText)
    }

    @Test fun `pure bodyweight exercise returns exact reps`() {
        val recommendation = engine.recommend(
            prescription = pe(protocol = Protocol.PULL_UP_5X2_3, sets = 5, repsMin = 2, repsMax = 3),
            exercise = exercise(seedKey = "pull_ups", equipment = Equipment.BODYWEIGHT),
            previousWorkingSets = emptyList(),
            baselineWeekActive = false,
            profile = profile(trainingExperience = TrainingExperience.BEGINNER),
            latestBodyLog = BodyMetricsLog(userId = "u", date = LocalDate.now(), weightKg = 100.0),
            recentBodyLogs = emptyList()
        )

        assertEquals(RecommendationTarget.REPS, recommendation!!.target)
        assertEquals(2, recommendation.reps)
        assertEquals("Target 2 reps per set", recommendation.displayText)
    }

    @Test fun `pull up graduation does not overfill old five set prescription`() {
        val recommendation = engine.recommend(
            prescription = pe(protocol = Protocol.PULL_UP_5X2_3, sets = 5, repsMin = 2, repsMax = 3),
            exercise = exercise(seedKey = "pull_ups", equipment = Equipment.BODYWEIGHT),
            previousWorkingSets = listOf(
                set(null, 4),
                set(null, 4),
                set(null, 4),
                set(null, 3),
                set(null, 3)
            ),
            baselineWeekActive = false,
            profile = profile(trainingExperience = TrainingExperience.BEGINNER),
            latestBodyLog = BodyMetricsLog(userId = "u", date = LocalDate.now(), weightKg = 100.0),
            recentBodyLogs = emptyList()
        )

        assertEquals(3, recommendation!!.reps)
    }

    @Test fun `custom unmapped exercise gets low confidence fallback`() {
        val recommendation = engine.recommend(
            prescription = pe(),
            exercise = exercise(seedKey = null, equipment = Equipment.CABLE, movementPattern = MovementPattern.ISOLATION),
            previousWorkingSets = emptyList(),
            baselineWeekActive = false,
            profile = profile(),
            latestBodyLog = BodyMetricsLog(userId = "u", date = LocalDate.now(), weightKg = 100.0),
            recentBodyLogs = emptyList()
        )

        assertNotNull(recommendation?.weightKg)
        assertEquals(RecommendationConfidence.LOW, recommendation!!.confidence)
    }

    @Test fun `fast bodyweight drop caps progression increase`() {
        val recommendation = engine.recommend(
            prescription = pe(repsMin = 8, repsMax = 10),
            exercise = exercise(seedKey = "leg_press", equipment = Equipment.MACHINE, movementPattern = MovementPattern.SQUAT),
            previousWorkingSets = listOf(set(100.0, 10, 8), set(100.0, 10, 8), set(100.0, 10, 8)),
            baselineWeekActive = false,
            profile = profile(),
            latestBodyLog = null,
            recentBodyLogs = listOf(
                BodyMetricsLog(userId = "u", date = LocalDate.now().minusDays(14), weightKg = 100.0),
                BodyMetricsLog(userId = "u", date = LocalDate.now(), weightKg = 98.0)
            )
        )

        assertEquals(100.0, recommendation!!.weightKg!!, 0.001)
    }

    @Test fun `muscle mass loss holds instead of increasing`() {
        val recommendation = engine.recommend(
            prescription = pe(repsMin = 8, repsMax = 10),
            exercise = exercise(seedKey = "leg_press", equipment = Equipment.MACHINE, movementPattern = MovementPattern.SQUAT),
            previousWorkingSets = listOf(set(100.0, 10, 7), set(100.0, 10, 7), set(100.0, 10, 7)),
            baselineWeekActive = false,
            profile = profile(),
            latestBodyLog = null,
            recentBodyLogs = listOf(
                BodyMetricsLog(userId = "u", date = LocalDate.now().minusDays(14), weightKg = 100.0, muscleMassKg = 70.0),
                BodyMetricsLog(userId = "u", date = LocalDate.now(), weightKg = 100.0, muscleMassKg = 69.7)
            )
        )

        assertEquals(100.0, recommendation!!.weightKg!!, 0.001)
    }

    @Test fun `missing sex age and body logs are neutral enough to suggest from profile weight`() {
        val recommendation = engine.recommend(
            prescription = pe(),
            exercise = exercise(seedKey = "lat_pulldown", equipment = Equipment.CABLE),
            previousWorkingSets = emptyList(),
            baselineWeekActive = false,
            profile = UserProfile(currentBodyWeightKg = 100.0),
            latestBodyLog = null,
            recentBodyLogs = emptyList()
        )

        assertEquals(RecommendationTarget.WEIGHT_KG, recommendation!!.target)
        assertNotNull(recommendation.weightKg)
    }

    @Test fun `missing RPE does not block performance increase when reps hit top`() {
        val recommendation = engine.recommend(
            prescription = pe(repsMin = 8, repsMax = 10),
            exercise = exercise(seedKey = "lat_pulldown", equipment = Equipment.CABLE),
            previousWorkingSets = listOf(set(50.0, 10), set(50.0, 10), set(50.0, 10)),
            baselineWeekActive = false,
            profile = profile(),
            latestBodyLog = null,
            recentBodyLogs = emptyList()
        )

        assertEquals(55.0, recommendation!!.weightKg!!, 0.001)
        assertEquals("Next target 55 kg", recommendation.displayText)
    }

    @Test fun `assisted pull up reduces assistance after strong performance`() {
        val recommendation = engine.recommend(
            prescription = pe(repsMin = 6, repsMax = 8),
            exercise = exercise(seedKey = "assisted_pullup", equipment = Equipment.MACHINE),
            previousWorkingSets = listOf(set(40.0, 8), set(40.0, 8), set(40.0, 8)),
            baselineWeekActive = false,
            profile = profile(),
            latestBodyLog = null,
            recentBodyLogs = emptyList()
        )

        assertEquals(35.0, recommendation!!.weightKg!!, 0.001)
    }

    private fun pe(
        repsMin: Int = 8,
        repsMax: Int = 10,
        protocol: Protocol = Protocol.STANDARD,
        sets: Int = 3
    ) = ProgramExercise(
        userId = "u",
        programDayId = 1,
        orderIndex = 0,
        exerciseId = 1,
        sets = sets,
        repsMin = repsMin,
        repsMax = repsMax,
        restSec = 90,
        protocol = protocol
    )

    private fun exercise(
        seedKey: String?,
        equipment: Equipment,
        movementPattern: MovementPattern = MovementPattern.PULL
    ) = Exercise(
        userId = "u",
        name = seedKey ?: "Custom",
        primaryMuscleGroup = MuscleGroup.BACK,
        equipment = equipment,
        movementPattern = movementPattern,
        seedKey = seedKey
    )

    private fun set(weightKg: Double?, reps: Int?, rpe: Int? = null) = SetEntry(
        userId = "u",
        sessionId = 1,
        programExerciseId = 1,
        exerciseIdSnapshot = 1,
        exerciseNameSnapshot = "Exercise",
        setIndex = 1,
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        isWarmup = false
    )

    private fun profile(
        trainingExperience: TrainingExperience = TrainingExperience.BEGINNER
    ) = UserProfile(
        currentBodyWeightKg = 100.0,
        goalBodyWeightKg = 80.0,
        age = 35,
        sex = Sex.MALE,
        trainingExperience = trainingExperience
    )
}
