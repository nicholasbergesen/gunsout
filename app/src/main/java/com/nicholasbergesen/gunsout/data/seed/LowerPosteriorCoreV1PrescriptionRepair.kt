package com.nicholasbergesen.gunsout.data.seed

internal object LowerPosteriorCoreV1PrescriptionRepair {
    private const val trapBarDeadliftSeedKey = "trap_bar_deadlift"
    private const val machineCrunchSeedKey = "machine_crunch"
    private const val hipThrustSeedKey = "hip_thrust"
    private const val lyingLegRaiseSeedKey = "lying_leg_raise"

    val replacedSeedKeys: Set<String> = setOf(trapBarDeadliftSeedKey, machineCrunchSeedKey)

    private val legacyPrescriptionsByReplacementSeedKey = mapOf(
        trapBarDeadliftSeedKey to ProgramSeeds.PlanExercise(hipThrustSeedKey, 3, 12, 12, 90),
        machineCrunchSeedKey to ProgramSeeds.PlanExercise(lyingLegRaiseSeedKey, 3, 15, 15, 60)
    )

    fun legacyPlanForReplacement(replacementSeedKey: String?): ProgramSeeds.PlanExercise? =
        legacyPrescriptionsByReplacementSeedKey[replacementSeedKey]

    fun legacyPlanForRepair(
        replacementSeedKey: String?,
        snapshotSeedKey: String?
    ): ProgramSeeds.PlanExercise? =
        legacyPlanForReplacement(replacementSeedKey)
            ?.takeIf { it.exerciseSeedKey == snapshotSeedKey }
}
