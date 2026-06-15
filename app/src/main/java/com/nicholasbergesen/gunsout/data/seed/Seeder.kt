package com.nicholasbergesen.gunsout.data.seed

import androidx.room.withTransaction
import com.nicholasbergesen.gunsout.data.dao.ExerciseAlternateDao
import com.nicholasbergesen.gunsout.data.dao.ExerciseDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDayDao
import com.nicholasbergesen.gunsout.data.dao.ProgramExerciseDao
import com.nicholasbergesen.gunsout.data.dao.SetEntryDao
import com.nicholasbergesen.gunsout.data.dao.SupplementDao
import com.nicholasbergesen.gunsout.data.entity.ExerciseAlternate
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.Supplement
import com.nicholasbergesen.gunsout.data.entity.SupplementUnit
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

internal const val DEFAULT_PROGRAM_REFRESH_VERSION = 2

internal data class SeederMaintenancePlan(
    val firstRun: Boolean,
    val needsDefaultProgramRefresh: Boolean,
    val needsSeededMovementPatternBackfill: Boolean
) {
    val shouldUpdateProfile: Boolean
        get() = firstRun || needsDefaultProgramRefresh || needsSeededMovementPatternBackfill
}

internal fun UserProfile.seederMaintenancePlan() =
    SeederMaintenancePlan(
        firstRun = !firstRunDone,
        needsDefaultProgramRefresh = defaultProgramRefreshVersion < DEFAULT_PROGRAM_REFRESH_VERSION,
        needsSeededMovementPatternBackfill =
            seededMovementPatternBackfillVersion < SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION
    )

/**
 * Seeds the database with default programs, exercises, alternates and supplements for a single
 * userId. All inserts stamp [userId] and all `getBySeedKey` lookups are scoped to [userId] so that
 * each user receives their own copy of the catalog (rubber-duck issue #3).
 *
 * The seeding sequence is wrapped in a single Room transaction so that a partial failure rolls
 * back atomically. Without this, a retry from [com.nicholasbergesen.gunsout.feature.auth.AuthGate]'s error UI can
 * hit a half-seeded program where the parent row exists but children are missing, and
 * [seedProgram] would skip inserting the children because it short-circuits on parent presence.
 *
 * Per-user [UserPreferences] (Phase 3) makes the `firstRunDone` gate a real per-user flag:
 * each Google account that signs in gets a fresh DataStore file, defaults `firstRunDone = false`,
 * and goes through its own first-run program activation.
 */
@Singleton
class Seeder @Inject constructor(
    private val db: com.nicholasbergesen.gunsout.data.db.GunsoutDatabase,
    private val programDao: ProgramDao,
    private val programDayDao: ProgramDayDao,
    private val programExerciseDao: ProgramExerciseDao,
    private val setEntryDao: SetEntryDao,
    private val exerciseDao: ExerciseDao,
    private val alternateDao: ExerciseAlternateDao,
    private val supplementDao: SupplementDao,
    private val userPrefs: UserPreferences,
    private val reminderScheduler: com.nicholasbergesen.gunsout.feature.supplements.SupplementReminderScheduler
) {

    suspend fun seedIfNeeded(userId: String) {
        val profile = userPrefs.profile(userId).first()
        val maintenancePlan = profile.seederMaintenancePlan()
        var defaultProgramRefreshCompleted = true
        db.withTransaction {
            seedExercises(
                userId = userId,
                backfillLegacySeededMovementPatterns =
                    maintenancePlan.needsSeededMovementPatternBackfill
            )
            seedAlternates(userId)
            defaultProgramRefreshCompleted = seedPrograms(
                userId = userId,
                activateDefaultOnFirstRun = maintenancePlan.firstRun,
                refreshExistingSeededProgram = maintenancePlan.needsDefaultProgramRefresh
            )
            seedSupplements(userId)
        }
        // Re-arm any supplement reminders saved in the DB (e.g. after install on a new device or
        // after a backup-import). Boot is handled separately by SupplementBootReceiver.
        rearmReminders(userId)
        val shouldMarkDefaultProgramRefresh =
            maintenancePlan.needsDefaultProgramRefresh && defaultProgramRefreshCompleted
        val shouldMarkSeededMovementPatternBackfill = maintenancePlan.needsSeededMovementPatternBackfill
        if (maintenancePlan.firstRun || shouldMarkDefaultProgramRefresh || shouldMarkSeededMovementPatternBackfill) {
            userPrefs.update(userId) {
                it.copy(
                    firstRunDone = it.firstRunDone || maintenancePlan.firstRun,
                    defaultProgramRefreshVersion = if (shouldMarkDefaultProgramRefresh) {
                        maxOf(it.defaultProgramRefreshVersion, DEFAULT_PROGRAM_REFRESH_VERSION)
                    } else {
                        it.defaultProgramRefreshVersion
                    },
                    seededMovementPatternBackfillVersion = if (shouldMarkSeededMovementPatternBackfill) {
                        maxOf(
                            it.seededMovementPatternBackfillVersion,
                            SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION
                        )
                    } else {
                        it.seededMovementPatternBackfillVersion
                    }
                )
            }
        }
    }

    private suspend fun rearmReminders(userId: String) {
        supplementDao.allActiveOnce(userId).forEach { reminderScheduler.reschedule(it) }
        reminderScheduler.ensureChannel()
    }

    private suspend fun seedExercises(
        userId: String,
        backfillLegacySeededMovementPatterns: Boolean
    ) {
        for (seed in ExerciseSeeds.all) {
            val seedKey = seed.exercise.seedKey!!
            val existing = exerciseDao.getBySeedKey(userId, seedKey)
            if (existing == null) {
                exerciseDao.insert(seed.exercise.copy(userId = userId))
            } else {
                val normalized = existing.withSeededMovementPatternBackfill(
                    enabled = backfillLegacySeededMovementPatterns
                )
                if (normalized.movementPattern != existing.movementPattern) {
                    exerciseDao.update(normalized)
                }
            }
        }
    }

    private suspend fun seedAlternates(userId: String) {
        for (seed in ExerciseSeeds.all) {
            val parent = exerciseDao.getBySeedKey(userId, seed.exercise.seedKey!!) ?: continue
            for ((altKey, reason) in seed.alternates) {
                val alt = exerciseDao.getBySeedKey(userId, altKey) ?: continue
                alternateDao.insert(ExerciseAlternate(
                    userId = userId,
                    exerciseId = parent.id,
                    alternateExerciseId = alt.id,
                    reason = reason
                ))
            }
        }
    }

    private suspend fun seedPrograms(
        userId: String,
        activateDefaultOnFirstRun: Boolean,
        refreshExistingSeededProgram: Boolean
    ): Boolean {
        var refreshCompleted = true
        for (planProgram in ProgramSeeds.all) {
            refreshCompleted = seedProgram(
                userId = userId,
                planProgram = planProgram,
                activateOnFirstRun = activateDefaultOnFirstRun &&
                    planProgram.seedKey == ProgramSeeds.upperLower4Day.seedKey,
                refreshExistingSeededProgram = refreshExistingSeededProgram &&
                    planProgram.seedKey == ProgramSeeds.upperLower4Day.seedKey
            ) && refreshCompleted
        }
        return refreshCompleted
    }

    private suspend fun seedProgram(
        userId: String,
        planProgram: ProgramSeeds.PlanProgram,
        activateOnFirstRun: Boolean,
        refreshExistingSeededProgram: Boolean
    ): Boolean {
        var program = programDao.getBySeedKey(userId, planProgram.seedKey)
        if (program == null) {
            val newId = programDao.insert(Program(
                userId = userId,
                name = planProgram.name,
                type = planProgram.programType,
                notes = planProgram.notes,
                isActive = activateOnFirstRun,
                isTemplate = true,
                seedKey = planProgram.seedKey
            ))
            program = programDao.getById(newId)
            // Days + exercises
            for (planDay in planProgram.days) {
                val dayId = programDayDao.insert(ProgramDay(
                    userId = userId,
                    programId = newId,
                    orderIndex = planDay.orderIndex,
                    label = planDay.label,
                    preferredDayOfWeek = planDay.preferredDayOfWeek,
                    isRest = planDay.isRest
                ))
                for ((i, pe) in planDay.exercises.withIndex()) {
                    val ex = exerciseDao.getBySeedKey(userId, pe.exerciseSeedKey) ?: continue
                    programExerciseDao.insert(ProgramExercise(
                        userId = userId,
                        programDayId = dayId,
                        orderIndex = i,
                        exerciseId = ex.id,
                        sets = pe.sets,
                        repsMin = pe.repsMin,
                        repsMax = pe.repsMax,
                        restSec = pe.restSec,
                        rpeTarget = pe.rpeTarget,
                        supersetGroupId = pe.supersetGroupId,
                        protocol = pe.protocol
                    ))
                }
            }
            return true
        } else if (refreshExistingSeededProgram) {
            val backfilled = SeededProgramRefresh.backfillSeededTemplateMetadata(program, planProgram)
            if (backfilled != program) {
                programDao.update(backfilled)
            }
            activateExistingDefaultOnFirstRun(userId, backfilled, planProgram, activateOnFirstRun)
            return refreshSeededProgram(userId, backfilled, planProgram)
        } else {
            val backfilled = SeededProgramRefresh.backfillSeededTemplateMetadata(program, planProgram)
            if (backfilled != program) {
                programDao.update(backfilled)
            }
            activateExistingDefaultOnFirstRun(userId, backfilled, planProgram, activateOnFirstRun)
            return true
        }
    }

    private suspend fun activateExistingDefaultOnFirstRun(
        userId: String,
        program: Program,
        planProgram: ProgramSeeds.PlanProgram,
        activateOnFirstRun: Boolean
    ) {
        if (SeededProgramRefresh.shouldActivateExistingSeededDefaultOnFirstRun(
                program = program,
                planProgram = planProgram,
                activateOnFirstRun = activateOnFirstRun
            )
        ) {
            programDao.setActive(userId, program.id)
        }
    }

    private suspend fun refreshSeededProgram(
        userId: String,
        program: Program,
        planProgram: ProgramSeeds.PlanProgram
    ): Boolean {
        if (!SeededProgramRefresh.shouldRefreshSeededProgramContents(program, planProgram)) return true
        val daysByOrder = programDayDao.getForProgram(program.id).associateBy { it.orderIndex }
        for (planDay in planProgram.days) {
            val day = daysByOrder[planDay.orderIndex] ?: continue
            if (SeededProgramRefresh.shouldRefreshLabel(day, planDay)) {
                programDayDao.update(day.copy(label = planDay.label))
            }
        }

        var completed = true
        for (order in SeededProgramRefresh.refreshExercisePrescriptionsByOrder.keys) {
            val day = daysByOrder[order] ?: continue
            val planExercises = planProgram.days.singleOrNull { it.orderIndex == order }?.exercises ?: continue
            val existingExercises = programExerciseDao.getAllForDay(day.id).sortedBy { it.orderIndex }
            val seedKeysByExerciseId = existingExercises.associate {
                it.exerciseId to exerciseDao.getById(it.exerciseId)?.seedKey
            }
            val refreshState = SeededProgramRefresh.refreshStateForProgramDay(
                order = order,
                exercises = existingExercises.filterNot { it.isRetired },
                seedKeysByExerciseId = seedKeysByExerciseId
            )
            if (refreshState == null) {
                continue
            }
            val snapshotSeedKeysByProgramExerciseId = snapshotSeedKeysByRowId(existingExercises)
            if (
                refreshState.existingPlannedSeedKeysToReplace.isNotEmpty() &&
                existingExercises
                    .filterNot { it.isRetired }
                    .filter { seedKeysByExerciseId[it.exerciseId] in refreshState.existingPlannedSeedKeysToReplace }
                    .none { it.id in snapshotSeedKeysByProgramExerciseId }
            ) {
                continue
            }
            val exerciseIdsBySeedKey = mutableMapOf<String, Long>()
            var missingPlanExercise = false
            for (planExercise in planExercises) {
                val exercise = exerciseDao.getBySeedKey(userId, planExercise.exerciseSeedKey)
                if (exercise == null) {
                    missingPlanExercise = true
                    break
                }
                exerciseIdsBySeedKey[planExercise.exerciseSeedKey] = exercise.id
            }
            if (missingPlanExercise) {
                completed = false
                continue
            }
            val staleProgramExerciseIdsToRetire = snapshotSeedKeysByProgramExerciseId.keys
            val refreshPlan = SeededProgramRefresh.buildProgramExerciseRefreshPlan(
                userId = userId,
                programDayId = day.id,
                existingExercises = existingExercises,
                seedKeysByExerciseId = seedKeysByExerciseId,
                exerciseIdsBySeedKey = exerciseIdsBySeedKey,
                planExercises = planExercises,
                staleProgramExerciseIdsToRetire = staleProgramExerciseIdsToRetire,
                existingPlannedSeedKeysToReplace = refreshState.existingPlannedSeedKeysToReplace,
                snapshotSeedKeysByProgramExerciseId = snapshotSeedKeysByProgramExerciseId
            )
            if (refreshPlan == null) {
                completed = false
                continue
            }
            for (staleId in refreshPlan.staleRowIdsToDelete) {
                programExerciseDao.delete(staleId)
            }
            for (refreshed in refreshPlan.rowsToUpsert) {
                if (refreshed.id == 0L) {
                    programExerciseDao.insert(refreshed)
                } else {
                    programExerciseDao.update(refreshed)
                }
            }
        }
        return completed
    }

    private suspend fun snapshotSeedKeysByRowId(
        exercises: List<ProgramExercise>
    ): Map<Long, Set<String?>> {
        val result = mutableMapOf<Long, Set<String?>>()
        for (exercise in exercises) {
            val snapshotIds = setEntryDao.exerciseSnapshotIdsForProgramExercise(exercise.id)
            if (snapshotIds.isNotEmpty()) {
                result[exercise.id] = snapshotIds
                    .map { snapshotId -> exerciseDao.getById(snapshotId)?.seedKey }
                    .toSet()
            }
        }
        return result
    }

    private suspend fun seedSupplements(userId: String) {
        val key = "creatine_mono"
        if (supplementDao.getBySeedKey(userId, key) == null) {
            supplementDao.insert(Supplement(
                userId = userId,
                name = "Creatine Monohydrate",
                defaultDose = 5.0,
                unit = SupplementUnit.G,
                notes = "Daily dosing. Loading phase optional. Take with water or your smoothie.",
                takeWith = "with water or smoothie",
                reminderTime = null,
                isActive = true,
                isUserCreated = false,
                seedKey = key
            ))
        }
    }

    object SeededProgramRefresh {
        const val upperPushPullOrder = 0
        const val upperHypertrophyOrder = 3
        const val lowerPosteriorCoreOrder = 4

        private const val legacyUpperLower4DayName = "Upper / Lower 4-Day (Free Weights)"
        private const val legacyUpperLower4DayNotes =
            "Balanced free-weight strength and hypertrophy across four weekly lifting days."

        private val legacyDayLabelsByOrder = mapOf(
            0 to "Upper A",
            1 to "Lower A",
            3 to "Upper B",
            4 to "Lower B"
        )

        private val legacyUpperPushPullExercises = listOf(
            ProgramSeeds.PlanExercise("inc_db_bench", 3, 8, 10, 90),
            ProgramSeeds.PlanExercise("pull_ups", 5, 2, 3, 120, protocol = Protocol.PULL_UP_5X2_3),
            ProgramSeeds.PlanExercise("db_shoulder_press", 3, 10, 10, 60),
            ProgramSeeds.PlanExercise("db_rear_delt_fly", 3, 15, 15, 60)
        )

        private val legacyUpperHypertrophyExercises = listOf(
            ProgramSeeds.PlanExercise("chest_supported_row", 3, 8, 10, 90),
            ProgramSeeds.PlanExercise("flat_db_bench", 3, 10, 10, 90),
            ProgramSeeds.PlanExercise("db_lateral_raise", 4, 12, 15, 60),
            ProgramSeeds.PlanExercise("db_bicep_curl", 3, 10, 10, 60, supersetGroupId = 1),
            ProgramSeeds.PlanExercise("db_overhead_tricep", 3, 12, 12, 60, supersetGroupId = 1)
        )

        private val legacyLowerPosteriorCoreExercises = listOf(
            ProgramSeeds.PlanExercise("leg_curl", 3, 10, 12, 60),
            ProgramSeeds.PlanExercise("goblet_squat", 3, 10, 10, 90),
            ProgramSeeds.PlanExercise("hip_thrust", 3, 12, 12, 90),
            ProgramSeeds.PlanExercise("lying_leg_raise", 3, 15, 15, 60)
        )

        private val v1RefreshedLowerPosteriorCoreExercises = listOf(
            ProgramSeeds.PlanExercise("leg_curl", 3, 10, 12, 60),
            ProgramSeeds.PlanExercise("goblet_squat", 3, 10, 10, 90),
            ProgramSeeds.PlanExercise("trap_bar_deadlift", 3, 8, 10, 150),
            ProgramSeeds.PlanExercise("machine_crunch", 3, 10, 15, 60)
        )

        val lowerPosteriorCoreV1ReplacedSeedKeys = LowerPosteriorCoreV1PrescriptionRepair.replacedSeedKeys

        val refreshExercisePrescriptionsByOrder = mapOf(
            upperPushPullOrder to legacyUpperPushPullExercises,
            upperHypertrophyOrder to legacyUpperHypertrophyExercises,
            lowerPosteriorCoreOrder to legacyLowerPosteriorCoreExercises
        )

        fun shouldRefreshLabel(day: ProgramDay, planDay: ProgramSeeds.PlanDay): Boolean =
            day.orderIndex == planDay.orderIndex &&
                day.label == legacyDayLabelsByOrder[day.orderIndex] &&
                day.label != planDay.label

        fun shouldRefreshSeededProgramContents(program: Program, planProgram: ProgramSeeds.PlanProgram): Boolean =
            program.isTemplate &&
                program.seedKey == ProgramSeeds.upperLower4Day.seedKey &&
                planProgram.seedKey == ProgramSeeds.upperLower4Day.seedKey &&
                program.name == planProgram.name

        fun shouldActivateExistingSeededDefaultOnFirstRun(
            program: Program,
            planProgram: ProgramSeeds.PlanProgram,
            activateOnFirstRun: Boolean
        ): Boolean =
            activateOnFirstRun &&
                !program.isActive &&
                program.isTemplate &&
                program.seedKey == ProgramSeeds.upperLower4Day.seedKey &&
                planProgram.seedKey == ProgramSeeds.upperLower4Day.seedKey

        fun backfillSeededTemplateMetadata(
            program: Program,
            planProgram: ProgramSeeds.PlanProgram
        ): Program {
            if (!program.isTemplate) return program
            return program.copy(
                name = if (shouldRefreshProgramName(program, planProgram)) planProgram.name else program.name,
                type = planProgram.programType,
                notes = when {
                    shouldRefreshProgramNotes(program, planProgram) -> planProgram.notes
                    else -> program.notes ?: planProgram.notes
                }
            )
        }

        private fun shouldRefreshProgramName(program: Program, planProgram: ProgramSeeds.PlanProgram): Boolean =
            planProgram.seedKey == ProgramSeeds.upperLower4Day.seedKey &&
                program.seedKey == ProgramSeeds.upperLower4Day.seedKey &&
                program.name == legacyUpperLower4DayName

        private fun shouldRefreshProgramNotes(program: Program, planProgram: ProgramSeeds.PlanProgram): Boolean =
            planProgram.seedKey == ProgramSeeds.upperLower4Day.seedKey &&
                program.seedKey == ProgramSeeds.upperLower4Day.seedKey &&
                shouldRefreshProgramName(program, planProgram) &&
                program.notes == legacyUpperLower4DayNotes

        fun matchesLegacyUpperPushPull(
            exercises: List<ProgramExercise>,
            seedKeysByExerciseId: Map<Long, String?>
        ): Boolean = matchesLegacyProgramDay(exercises, seedKeysByExerciseId, legacyUpperPushPullExercises)

        fun matchesLegacyUpperHypertrophy(
            exercises: List<ProgramExercise>,
            seedKeysByExerciseId: Map<Long, String?>
        ): Boolean = matchesLegacyProgramDay(exercises, seedKeysByExerciseId, legacyUpperHypertrophyExercises)

        fun matchesLegacyLowerPosteriorCore(
            exercises: List<ProgramExercise>,
            seedKeysByExerciseId: Map<Long, String?>
        ): Boolean = matchesLegacyProgramDay(exercises, seedKeysByExerciseId, legacyLowerPosteriorCoreExercises)

        fun matchesV1RefreshedLowerPosteriorCore(
            exercises: List<ProgramExercise>,
            seedKeysByExerciseId: Map<Long, String?>
        ): Boolean = matchesLegacyProgramDay(exercises, seedKeysByExerciseId, v1RefreshedLowerPosteriorCoreExercises)

        data class ProgramDayRefreshState(
            val existingPlannedSeedKeysToReplace: Set<String> = emptySet()
        )

        fun refreshStateForProgramDay(
            order: Int,
            exercises: List<ProgramExercise>,
            seedKeysByExerciseId: Map<Long, String?>
        ): ProgramDayRefreshState? {
            val legacyExercises = refreshExercisePrescriptionsByOrder[order] ?: return null
            if (matchesLegacyProgramDay(exercises, seedKeysByExerciseId, legacyExercises)) {
                return ProgramDayRefreshState()
            }
            if (
                order == lowerPosteriorCoreOrder &&
                matchesV1RefreshedLowerPosteriorCore(exercises, seedKeysByExerciseId)
            ) {
                return ProgramDayRefreshState(
                    existingPlannedSeedKeysToReplace = lowerPosteriorCoreV1ReplacedSeedKeys
                )
            }
            return null
        }

        fun matchesLegacyProgramDay(
            exercises: List<ProgramExercise>,
            seedKeysByExerciseId: Map<Long, String?>,
            legacyExercises: List<ProgramSeeds.PlanExercise>
        ): Boolean {
            val sorted = exercises.sortedBy { it.orderIndex }
            if (sorted.size != legacyExercises.size) return false
            return sorted.zip(legacyExercises).withIndex().all { (index, pair) ->
                val (existing, legacy) = pair
                existing.orderIndex == index &&
                    seedKeysByExerciseId[existing.exerciseId] == legacy.exerciseSeedKey &&
                    existing.sets == legacy.sets &&
                    existing.repsMin == legacy.repsMin &&
                    existing.repsMax == legacy.repsMax &&
                    existing.restSec == legacy.restSec &&
                    existing.rpeTarget == legacy.rpeTarget &&
                    existing.supersetGroupId == legacy.supersetGroupId &&
                    existing.protocol == legacy.protocol
            }
        }

        data class ProgramExerciseRefreshPlan(
            val plannedRows: List<ProgramExercise>,
            val staleRowsToRetire: List<ProgramExercise>,
            val staleRowIdsToDelete: List<Long>
        ) {
            val rowsToUpsert: List<ProgramExercise>
                get() = plannedRows + staleRowsToRetire
        }

        fun buildProgramExerciseRefreshPlan(
            userId: String,
            programDayId: Long,
            existingExercises: List<ProgramExercise>,
            seedKeysByExerciseId: Map<Long, String?>,
            exerciseIdsBySeedKey: Map<String, Long>,
            planExercises: List<ProgramSeeds.PlanExercise>,
            staleProgramExerciseIdsToRetire: Set<Long>,
            existingPlannedSeedKeysToReplace: Set<String> = emptySet(),
            snapshotSeedKeysByProgramExerciseId: Map<Long, Set<String?>> = emptyMap()
        ): ProgramExerciseRefreshPlan? {
            val plannedSeedKeys = planExercises.map { it.exerciseSeedKey }.toSet()
            val existingBySeedKey = linkedMapOf<String, ProgramExercise>()
            for (existing in existingExercises.sortedBy { it.orderIndex }) {
                val seedKey = seedKeysByExerciseId[existing.exerciseId] ?: continue
                if (seedKey in existingPlannedSeedKeysToReplace) continue
                existingBySeedKey.putIfAbsent(seedKey, existing)
            }
            val plannedRows = mutableListOf<ProgramExercise>()
            for ((index, planExercise) in planExercises.withIndex()) {
                val exerciseId = exerciseIdsBySeedKey[planExercise.exerciseSeedKey] ?: return null
                val current = existingBySeedKey[planExercise.exerciseSeedKey]
                    ?: ProgramExercise(
                        userId = userId,
                        programDayId = programDayId,
                        orderIndex = index,
                        exerciseId = exerciseId
                    )
                plannedRows += current.copy(
                    orderIndex = index,
                    exerciseId = exerciseId,
                    sets = planExercise.sets,
                    repsMin = planExercise.repsMin,
                    repsMax = planExercise.repsMax,
                    restSec = planExercise.restSec,
                    rpeTarget = planExercise.rpeTarget,
                    supersetGroupId = planExercise.supersetGroupId,
                    protocol = planExercise.protocol,
                    isRetired = false
                )
            }
            val staleRows = existingExercises
                .sortedBy { it.orderIndex }
                .filter {
                    val seedKey = seedKeysByExerciseId[it.exerciseId]
                    seedKey !in plannedSeedKeys || seedKey in existingPlannedSeedKeysToReplace
                }
            val staleRowsToRetire = staleRows
                .filter { it.id in staleProgramExerciseIdsToRetire }
                .map { stale ->
                    val replacementSeedKey = seedKeysByExerciseId[stale.exerciseId]
                    val legacyPrescription = replacementSeedKey
                        ?.takeIf { it in existingPlannedSeedKeysToReplace }
                        ?.let {
                            LowerPosteriorCoreV1PrescriptionRepair.legacyPlanForRepair(
                                replacementSeedKey = it,
                                snapshotSeedKeys = snapshotSeedKeysByProgramExerciseId[stale.id].orEmpty()
                            )
                        }
                    stale.copy(isRetired = true).withPrescription(legacyPrescription)
                }
            val staleRowIdsToDelete = staleRows
                .filterNot { it.id in staleProgramExerciseIdsToRetire }
                .map { it.id }
            return ProgramExerciseRefreshPlan(plannedRows, staleRowsToRetire, staleRowIdsToDelete)
        }

        private fun ProgramExercise.withPrescription(
            planExercise: ProgramSeeds.PlanExercise?
        ): ProgramExercise {
            if (planExercise == null) return this
            return copy(
                sets = planExercise.sets,
                repsMin = planExercise.repsMin,
                repsMax = planExercise.repsMax,
                restSec = planExercise.restSec,
                rpeTarget = planExercise.rpeTarget,
                supersetGroupId = planExercise.supersetGroupId,
                protocol = planExercise.protocol
            )
        }
    }
}
