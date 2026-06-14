package com.nicholasbergesen.gunsout.domain.recommendation

import com.nicholasbergesen.gunsout.core.text.formatOneDecimalOrInt
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.prefs.Sex
import com.nicholasbergesen.gunsout.data.prefs.TrainingExperience
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.roundToInt

enum class RecommendationTarget { WEIGHT_KG, REPS }

data class ExerciseRecommendation(
    val target: RecommendationTarget,
    val weightKg: Double? = null,
    val setWeightKg: List<Double> = emptyList(),
    val reps: Int? = null,
    val displayText: String,
    val explanation: String,
    val confidence: RecommendationConfidence
) {
    fun weightKgForSet(setIndex: Int): Double? =
        setWeightKg.getOrNull(setIndex - 1) ?: weightKg
}

enum class RecommendationConfidence { HIGH, MEDIUM, LOW }

data class ExerciseFormula(
    val seedKey: String,
    val target: RecommendationTarget,
    val bodyWeightRatio: Double = 0.0,
    val minWeightKg: Double = 0.0,
    val roundingKg: Double = 2.5,
    val progressionStepKg: Double = roundingKg,
    val assistanceSetting: Boolean = false,
    val beginnerReps: Int = 8,
    val noviceReps: Int = 10,
    val intermediateReps: Int = 12,
    val advancedReps: Int = 15
)

object ExerciseFormulaCatalog {
    private val formulas = listOf(
        weighted("inc_db_bench", 0.18, 6.0, 1.0),
        reps("pull_ups", 2, 3, 4, 5),
        weighted("lat_pulldown", 0.45, 20.0, 5.0),
        weighted("assisted_pullup", 0.35, 15.0, 5.0, assistanceSetting = true),
        weighted("db_shoulder_press", 0.13, 4.0, 1.0),
        weighted("db_rear_delt_fly", 0.05, 2.0, 1.0, progressionStepKg = 1.0),
        weighted("leg_press", 0.75, 20.0, 5.0),
        weighted("back_squat", 0.45, 20.0, 2.5),
        weighted("bulgarian_split_squat", 0.10, 4.0, 1.0),
        weighted("rdl", 0.20, 8.0, 2.0),
        weighted("leg_extension", 0.25, 10.0, 5.0),
        weighted("goblet_cyclist_squat", 0.16, 6.0, 2.0),
        weighted("standing_calf_raise", 0.14, 6.0, 2.0),
        weighted("chest_supported_row", 0.18, 6.0, 2.0),
        weighted("barbell_row", 0.40, 20.0, 2.5),
        weighted("flat_db_bench", 0.20, 6.0, 1.0),
        weighted("db_lateral_raise", 0.05, 2.0, 1.0, progressionStepKg = 1.0),
        weighted("db_bicep_curl", 0.08, 3.0, 1.0, progressionStepKg = 1.0),
        weighted("db_overhead_tricep", 0.12, 5.0, 1.0),
        weighted("leg_curl", 0.22, 10.0, 5.0),
        weighted("db_lying_leg_curl", 0.07, 3.0, 1.0, progressionStepKg = 1.0),
        weighted("goblet_squat", 0.18, 6.0, 2.0),
        weighted("hip_thrust", 0.45, 20.0, 2.5),
        reps("lying_leg_raise", 8, 10, 12, 15),
        weighted("barbell_bench_press", 0.45, 20.0, 2.5),
        weighted("machine_chest_press", 0.40, 15.0, 5.0),
        weighted("pec_deck_fly", 0.18, 10.0, 5.0),
        weighted("cable_chest_fly", 0.10, 5.0, 5.0),
        reps("bodyweight_dip", 3, 5, 8, 10),
        weighted("seated_cable_row", 0.42, 15.0, 5.0),
        weighted("machine_row", 0.45, 15.0, 5.0),
        weighted("single_arm_db_row", 0.22, 8.0, 2.0),
        weighted("face_pull", 0.12, 5.0, 5.0),
        weighted("straight_arm_pulldown", 0.14, 5.0, 5.0),
        weighted("hack_squat", 0.55, 20.0, 5.0),
        weighted("smith_squat", 0.40, 20.0, 2.5),
        weighted("conventional_deadlift", 0.55, 30.0, 2.5),
        weighted("trap_bar_deadlift", 0.60, 30.0, 2.5),
        reps("back_extension", 8, 10, 12, 15),
        weighted("cable_pull_through", 0.25, 10.0, 5.0),
        weighted("db_walking_lunge", 0.10, 4.0, 1.0),
        weighted("db_reverse_lunge", 0.10, 4.0, 1.0),
        weighted("db_step_up", 0.09, 4.0, 1.0),
        weighted("triceps_pressdown", 0.18, 5.0, 5.0),
        weighted("rope_overhead_triceps", 0.14, 5.0, 5.0),
        weighted("preacher_curl", 0.12, 5.0, 2.5),
        weighted("machine_biceps_curl", 0.16, 5.0, 5.0),
        weighted("seated_calf_raise", 0.25, 10.0, 5.0),
        weighted("machine_calf_raise", 0.45, 15.0, 5.0),
        weighted("cable_crunch", 0.20, 5.0, 5.0),
        weighted("machine_crunch", 0.25, 10.0, 5.0),
        reps("hanging_knee_raise", 6, 8, 10, 12),
        reps("ab_wheel_rollout", 4, 6, 8, 10)
    ).associateBy { it.seedKey }

    val knownSeedKeys: Set<String> = formulas.keys

    fun forExercise(exercise: Exercise): Pair<ExerciseFormula, RecommendationConfidence> {
        val seedKey = exercise.seedKey
        if (seedKey != null) {
            formulas[seedKey]?.let { return it to RecommendationConfidence.HIGH }
        }
        return fallbackFormula(exercise) to RecommendationConfidence.LOW
    }

    private fun fallbackFormula(exercise: Exercise): ExerciseFormula {
        if (exercise.equipment == Equipment.BODYWEIGHT) {
            return reps("fallback_bodyweight", 6, 8, 10, 12)
        }
        val ratio = when (exercise.movementPattern) {
            MovementPattern.PUSH -> 0.16
            MovementPattern.PULL -> 0.18
            MovementPattern.SQUAT -> 0.28
            MovementPattern.HINGE -> 0.30
            MovementPattern.LUNGE -> 0.10
            MovementPattern.ISOLATION -> 0.08
            MovementPattern.CALVES -> 0.14
            MovementPattern.CORE -> 0.12
        }
        val rounding = when (exercise.equipment) {
            Equipment.DUMBBELL -> 1.0
            Equipment.BARBELL -> 2.5
            Equipment.MACHINE, Equipment.CABLE -> 5.0
            else -> 2.5
        }
        return weighted("fallback_weighted", ratio, rounding * 2.0, rounding, progressionStepKg = rounding)
    }

    private fun weighted(
        seedKey: String,
        bodyWeightRatio: Double,
        minWeightKg: Double,
        roundingKg: Double,
        progressionStepKg: Double = roundingKg,
        assistanceSetting: Boolean = false
    ) = ExerciseFormula(
        seedKey = seedKey,
        target = RecommendationTarget.WEIGHT_KG,
        bodyWeightRatio = bodyWeightRatio,
        minWeightKg = minWeightKg,
        roundingKg = roundingKg,
        progressionStepKg = progressionStepKg,
        assistanceSetting = assistanceSetting
    )

    private fun reps(
        seedKey: String,
        beginner: Int,
        novice: Int,
        intermediate: Int,
        advanced: Int
    ) = ExerciseFormula(
        seedKey = seedKey,
        target = RecommendationTarget.REPS,
        beginnerReps = beginner,
        noviceReps = novice,
        intermediateReps = intermediate,
        advancedReps = advanced
    )
}

class ExerciseRecommendationEngine {

    fun recommend(
        prescription: ProgramExercise,
        exercise: Exercise,
        previousWorkingSets: List<SetEntry>,
        baselineWeekActive: Boolean,
        profile: UserProfile,
        latestBodyLog: BodyMetricsLog?,
        recentBodyLogs: List<BodyMetricsLog>
    ): ExerciseRecommendation? {
        val (formula, confidence) = ExerciseFormulaCatalog.forExercise(exercise)
        val working = previousWorkingSets.filter { !it.isWarmup }
        return when (formula.target) {
            RecommendationTarget.REPS -> recommendReps(
                prescription = prescription,
                formula = formula,
                working = working,
                baselineWeekActive = baselineWeekActive,
                profile = profile,
                confidence = confidence
            )
            RecommendationTarget.WEIGHT_KG -> recommendWeight(
                prescription = prescription,
                exercise = exercise,
                formula = formula,
                working = working,
                baselineWeekActive = baselineWeekActive,
                profile = profile,
                latestBodyLog = latestBodyLog,
                recentBodyLogs = recentBodyLogs,
                confidence = confidence
            )
        }
    }

    private fun recommendReps(
        prescription: ProgramExercise,
        formula: ExerciseFormula,
        working: List<SetEntry>,
        baselineWeekActive: Boolean,
        profile: UserProfile,
        confidence: RecommendationConfidence
    ): ExerciseRecommendation {
        val target = if (working.isEmpty()) {
            startingReps(formula, profile.trainingExperience)
        } else {
            val reps = working.mapNotNull { it.reps }
            if (reps.isEmpty() || baselineWeekActive) {
                startingReps(formula, profile.trainingExperience)
            } else if (prescription.protocol == Protocol.PULL_UP_5X2_3) {
                val total = reps.sum()
                when {
                    total >= 18 -> prescription.repsMax
                    total < 9 -> 2
                    else -> 3
                }
            } else {
                when {
                    reps.all { it >= prescription.repsMax } -> ((reps.maxOrNull() ?: prescription.repsMax) + 1).coerceAtMost(25)
                    reps.any { it < prescription.repsMin } -> (reps.minOrNull() ?: prescription.repsMin).coerceAtLeast(1)
                    else -> (reps.maxOrNull() ?: prescription.repsMin).coerceAtLeast(prescription.repsMin)
                }
            }
        }
        return ExerciseRecommendation(
            target = RecommendationTarget.REPS,
            reps = target,
            displayText = "Target $target reps per set",
            explanation = "Rep target uses the local bodyweight formula and recent performance.",
            confidence = confidence
        )
    }

    private fun recommendWeight(
        prescription: ProgramExercise,
        exercise: Exercise,
        formula: ExerciseFormula,
        working: List<SetEntry>,
        baselineWeekActive: Boolean,
        profile: UserProfile,
        latestBodyLog: BodyMetricsLog?,
        recentBodyLogs: List<BodyMetricsLog>,
        confidence: RecommendationConfidence
    ): ExerciseRecommendation? {
        val previousWeights = working.mapNotNull { it.weightKg }
        val previousWeight = if (formula.assistanceSetting) {
            previousWeights.minOrNull()
        } else {
            previousWeights.maxOrNull()
        }
        val target = if (previousWeight == null) {
            val bodyWeight = latestBodyLog?.weightKg ?: profile.currentBodyWeightKg.takeIf { it > 0.0 } ?: return null
            startingWeight(formula, bodyWeight, profile, latestBodyLog)
        } else {
            progressedWeight(
                prescription = prescription,
                formula = formula,
                working = working,
                previousWeight = previousWeight,
                baselineWeekActive = baselineWeekActive,
                recentBodyLogs = recentBodyLogs
            )
        }
        val label = if (previousWeight == null) "Start with" else "Next target"
        return ExerciseRecommendation(
            target = RecommendationTarget.WEIGHT_KG,
            weightKg = target,
            setWeightKg = if (formula.assistanceSetting) {
                List(prescription.sets.coerceAtLeast(0)) { target }
            } else {
                rampedSetWeights(
                    topWeightKg = target,
                    setCount = prescription.sets,
                    incrementKg = formula.roundingKg
                )
            },
            displayText = "$label ${formatOneDecimalOrInt(target)} kg",
            explanation = "${exercise.name} target uses local formula, prior sets, and recent body metrics.",
            confidence = confidence
        )
    }

    private fun startingWeight(
        formula: ExerciseFormula,
        bodyWeightKg: Double,
        profile: UserProfile,
        latestBodyLog: BodyMetricsLog?
    ): Double {
        val sexFactor = when (profile.sex) {
            Sex.MALE -> 1.05
            Sex.FEMALE -> 0.85
            null -> 0.95
        }
        val experienceFactor = when (profile.trainingExperience) {
            TrainingExperience.BEGINNER -> 0.80
            TrainingExperience.NOVICE -> 0.95
            TrainingExperience.INTERMEDIATE -> 1.10
            TrainingExperience.ADVANCED -> 1.25
        }
        val ageFactor = when {
            profile.age == null -> 1.0
            profile.age >= 55 -> 0.90
            profile.age >= 45 -> 0.95
            else -> 1.0
        }
        val leanMassFactor = latestBodyLog?.let { log ->
            if (log.weightKg <= 0.0 || !log.weightKg.isFinite()) {
                null
            } else {
                val leanKg = log.muscleMassKg ?: log.bodyFatPct?.let { bodyFat -> log.weightKg * (1 - bodyFat / 100.0) }
                val ratio = leanKg?.let { it / (log.weightKg * 0.72) }
                ratio?.takeIf { it.isFinite() }?.coerceIn(0.85, 1.10)
            }
        } ?: 1.0
        val raw = bodyWeightKg * formula.bodyWeightRatio * sexFactor * experienceFactor * ageFactor * leanMassFactor
        return roundDown(raw.coerceAtLeast(formula.minWeightKg), formula.roundingKg)
    }

    private fun progressedWeight(
        prescription: ProgramExercise,
        formula: ExerciseFormula,
        working: List<SetEntry>,
        previousWeight: Double,
        baselineWeekActive: Boolean,
        recentBodyLogs: List<BodyMetricsLog>
    ): Double {
        if (baselineWeekActive) return roundDown(previousWeight, formula.roundingKg)
        val reps = working.mapNotNull { it.reps }
        if (reps.isEmpty()) return roundDown(previousWeight, formula.roundingKg)
        val trend = BodyTrend.from(recentBodyLogs)
        val rpes = working.mapNotNull { it.rpe }
        val effortTooHigh = rpes.isNotEmpty() && rpes.any { it >= 9 }
        val hitTop = reps.all { it >= prescription.repsMax }
        val missedBottom = reps.any { it < prescription.repsMin }
        val target = when {
            missedBottom && formula.assistanceSetting -> previousWeight + formula.progressionStepKg
            missedBottom -> previousWeight * 0.95
            trend.muscleMassDown -> previousWeight
            hitTop && !effortTooHigh && !trend.bodyWeightDroppingFast && formula.assistanceSetting ->
                previousWeight - formula.progressionStepKg
            hitTop && !effortTooHigh && !trend.bodyWeightDroppingFast -> previousWeight + formula.progressionStepKg
            else -> previousWeight
        }
        return roundDown(target.coerceAtLeast(formula.minWeightKg), formula.roundingKg)
    }

    private fun startingReps(formula: ExerciseFormula, experience: TrainingExperience): Int = when (experience) {
        TrainingExperience.BEGINNER -> formula.beginnerReps
        TrainingExperience.NOVICE -> formula.noviceReps
        TrainingExperience.INTERMEDIATE -> formula.intermediateReps
        TrainingExperience.ADVANCED -> formula.advancedReps
    }

    private data class BodyTrend(
        val bodyWeightDroppingFast: Boolean,
        val muscleMassDown: Boolean
    ) {
        companion object {
            fun from(logs: List<BodyMetricsLog>): BodyTrend {
                val sorted = logs.sortedBy { it.date }
                val weightLogs = sorted.filter { it.weightKg > 0.0 }
                val droppingFast = if (weightLogs.size >= 2) {
                    val first = weightLogs.first()
                    val last = weightLogs.last()
                    val days = ChronoUnit.DAYS.between(first.date, last.date).coerceAtLeast(1)
                    val pctPerWeek = ((last.weightKg - first.weightKg) / first.weightKg) * 100.0 / (days / 7.0)
                    pctPerWeek < -0.75
                } else {
                    false
                }
                val muscleLogs = sorted.filter { it.muscleMassKg != null }
                val muscleDown = muscleLogs.size >= 2 &&
                    muscleLogs.last().muscleMassKg!! < muscleLogs.first().muscleMassKg!! - 0.1
                return BodyTrend(droppingFast, muscleDown)
            }
        }
    }

    private fun roundDown(value: Double, increment: Double): Double {
        if (increment <= 0.0) return value
        val rounded = floor(value / increment) * increment
        return (rounded * 10.0).roundToInt() / 10.0
    }

    private fun rampedSetWeights(
        topWeightKg: Double,
        setCount: Int,
        incrementKg: Double
    ): List<Double> {
        if (setCount <= 0) return emptyList()
        if (setCount == 1) return listOf(topWeightKg)
        val startFactor = when {
            setCount >= 4 -> 0.80
            setCount == 3 -> 0.85
            else -> 0.90
        }
        val minimum = minOf(topWeightKg, incrementKg.takeIf { it > 0.0 } ?: topWeightKg)
        var previous = minimum
        return (1..setCount).map { setIndex ->
            if (setIndex == setCount) {
                topWeightKg
            } else {
                val progress = (setIndex - 1).toDouble() / (setCount - 1).toDouble()
                val factor = startFactor + ((1.0 - startFactor) * progress)
                val rounded = roundDown(topWeightKg * factor, incrementKg)
                    .coerceAtLeast(minimum)
                    .coerceAtMost(topWeightKg)
                    .coerceAtLeast(previous)
                previous = rounded
                rounded
            }
        }
    }

}
