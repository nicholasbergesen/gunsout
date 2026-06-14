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
import com.nicholasbergesen.gunsout.data.entity.defaultMovementPatternFor
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

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
        val firstRun = !profile.firstRunDone
        val needsDefaultProgramRefresh = profile.defaultProgramRefreshVersion < defaultProgramRefreshVersion
        db.withTransaction {
            seedExercises(userId)
            seedAlternates(userId)
            seedPrograms(
                userId = userId,
                activateDefaultOnFirstRun = firstRun,
                refreshExistingSeededProgram = needsDefaultProgramRefresh
            )
            seedSupplements(userId)
        }
        // Re-arm any supplement reminders saved in the DB (e.g. after install on a new device or
        // after a backup-import). Boot is handled separately by SupplementBootReceiver.
        rearmReminders(userId)
        if (firstRun || needsDefaultProgramRefresh) {
            userPrefs.update(userId) {
                it.copy(
                    firstRunDone = it.firstRunDone || firstRun,
                    defaultProgramRefreshVersion = maxOf(
                        it.defaultProgramRefreshVersion,
                        defaultProgramRefreshVersion
                    )
                )
            }
        }
    }

    private suspend fun rearmReminders(userId: String) {
        supplementDao.allActiveOnce(userId).forEach { reminderScheduler.reschedule(it) }
        reminderScheduler.ensureChannel()
    }

    private suspend fun seedExercises(userId: String) {
        for (seed in ExerciseSeeds.all) {
            val seedKey = seed.exercise.seedKey!!
            val existing = exerciseDao.getBySeedKey(userId, seedKey)
            if (existing == null) {
                exerciseDao.insert(seed.exercise.copy(userId = userId))
            } else if (
                existing.primaryMuscleGroup == seed.exercise.primaryMuscleGroup &&
                existing.equipment == seed.exercise.equipment &&
                existing.movementPattern == defaultMovementPatternFor(existing.primaryMuscleGroup) &&
                existing.movementPattern != seed.exercise.movementPattern
            ) {
                exerciseDao.update(existing.copy(movementPattern = seed.exercise.movementPattern))
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
    ) {
        for (planProgram in ProgramSeeds.all) {
            seedProgram(
                userId = userId,
                planProgram = planProgram,
                activateOnFirstRun = activateDefaultOnFirstRun &&
                    planProgram.seedKey == ProgramSeeds.upperLower4Day.seedKey,
                refreshExistingSeededProgram = refreshExistingSeededProgram &&
                    planProgram.seedKey == ProgramSeeds.upperLower4Day.seedKey
            )
        }
    }

    private suspend fun seedProgram(
        userId: String,
        planProgram: ProgramSeeds.PlanProgram,
        activateOnFirstRun: Boolean,
        refreshExistingSeededProgram: Boolean
    ) {
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
        } else if (refreshExistingSeededProgram) {
            val backfilled = SeededProgramRefresh.backfillSeededTemplateMetadata(program, planProgram)
            if (backfilled != program) {
                programDao.update(backfilled)
            }
            refreshSeededProgram(userId, backfilled, planProgram)
        } else {
            val backfilled = SeededProgramRefresh.backfillSeededTemplateMetadata(program, planProgram)
            if (backfilled != program) {
                programDao.update(backfilled)
            }
        }
    }

    private suspend fun refreshSeededProgram(
        userId: String,
        program: Program,
        planProgram: ProgramSeeds.PlanProgram
    ) {
        if (!SeededProgramRefresh.shouldRefreshSeededProgramContents(program, planProgram)) return
        val daysByOrder = programDayDao.getForProgram(program.id).associateBy { it.orderIndex }
        for (planDay in planProgram.days) {
            val day = daysByOrder[planDay.orderIndex] ?: continue
            if (SeededProgramRefresh.shouldRefreshLabel(day, planDay)) {
                programDayDao.update(day.copy(label = planDay.label))
            }
        }

        for ((order, legacyExercises) in SeededProgramRefresh.refreshExercisePrescriptionsByOrder) {
            val day = daysByOrder[order] ?: continue
            val planExercises = planProgram.days.singleOrNull { it.orderIndex == order }?.exercises ?: continue
            val existingExercises = programExerciseDao.getForDay(day.id).sortedBy { it.orderIndex }
            val seedKeysByExerciseId = existingExercises.associate {
                it.exerciseId to exerciseDao.getById(it.exerciseId)?.seedKey
            }
            if (!SeededProgramRefresh.matchesLegacyProgramDay(existingExercises, seedKeysByExerciseId, legacyExercises)) {
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
            if (missingPlanExercise) continue
            val staleProgramExerciseIdsToKeep = existingExercises
                .filter { setEntryDao.countForProgramExercise(it.id) > 0 }
                .map { it.id }
                .toSet()
            val refreshPlan = SeededProgramRefresh.buildProgramExerciseRefreshPlan(
                userId = userId,
                programDayId = day.id,
                existingExercises = existingExercises,
                seedKeysByExerciseId = seedKeysByExerciseId,
                exerciseIdsBySeedKey = exerciseIdsBySeedKey,
                planExercises = planExercises,
                staleProgramExerciseIdsToKeep = staleProgramExerciseIdsToKeep
            ) ?: continue
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

    private companion object {
        const val defaultProgramRefreshVersion = 2
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
            val staleRowsToKeep: List<ProgramExercise>,
            val staleRowIdsToDelete: List<Long>
        ) {
            val rowsToUpsert: List<ProgramExercise>
                get() = plannedRows + staleRowsToKeep
        }

        fun buildProgramExerciseRefreshPlan(
            userId: String,
            programDayId: Long,
            existingExercises: List<ProgramExercise>,
            seedKeysByExerciseId: Map<Long, String?>,
            exerciseIdsBySeedKey: Map<String, Long>,
            planExercises: List<ProgramSeeds.PlanExercise>,
            staleProgramExerciseIdsToKeep: Set<Long>
        ): ProgramExerciseRefreshPlan? {
            val plannedSeedKeys = planExercises.map { it.exerciseSeedKey }.toSet()
            val existingBySeedKey = linkedMapOf<String, ProgramExercise>()
            for (existing in existingExercises.sortedBy { it.orderIndex }) {
                val seedKey = seedKeysByExerciseId[existing.exerciseId] ?: continue
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
                    protocol = planExercise.protocol
                )
            }
            val staleRows = existingExercises
                .sortedBy { it.orderIndex }
                .filter { seedKeysByExerciseId[it.exerciseId] !in plannedSeedKeys }
            val staleRowsToKeep = staleRows
                .filter { it.id in staleProgramExerciseIdsToKeep }
                .mapIndexed { offset, stale ->
                    stale.copy(orderIndex = plannedRows.size + offset)
                }
            val staleRowIdsToDelete = staleRows
                .filterNot { it.id in staleProgramExerciseIdsToKeep }
                .map { it.id }
            return ProgramExerciseRefreshPlan(plannedRows, staleRowsToKeep, staleRowIdsToDelete)
        }
    }
}
