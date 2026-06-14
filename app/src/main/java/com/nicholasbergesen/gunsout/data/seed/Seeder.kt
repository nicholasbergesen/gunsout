package com.nicholasbergesen.gunsout.data.seed

import androidx.room.withTransaction
import com.nicholasbergesen.gunsout.data.dao.ExerciseAlternateDao
import com.nicholasbergesen.gunsout.data.dao.ExerciseDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDayDao
import com.nicholasbergesen.gunsout.data.dao.ProgramExerciseDao
import com.nicholasbergesen.gunsout.data.dao.SupplementDao
import com.nicholasbergesen.gunsout.data.entity.ExerciseAlternate
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.Supplement
import com.nicholasbergesen.gunsout.data.entity.SupplementUnit
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
        val needsSeededMovementPatternBackfill =
            profile.seededMovementPatternBackfillVersion < SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION &&
                needsDefaultProgramRefresh
        db.withTransaction {
            seedExercises(
                userId = userId,
                backfillLegacySeededMovementPatterns = needsSeededMovementPatternBackfill
            )
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
        if (firstRun || needsDefaultProgramRefresh || needsSeededMovementPatternBackfill) {
            userPrefs.update(userId) {
                it.copy(
                    firstRunDone = it.firstRunDone || firstRun,
                    defaultProgramRefreshVersion = maxOf(
                        it.defaultProgramRefreshVersion,
                        defaultProgramRefreshVersion
                    ),
                    seededMovementPatternBackfillVersion = maxOf(
                        it.seededMovementPatternBackfillVersion,
                        SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION
                    )
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
        val daysByOrder = programDayDao.getForProgram(program.id).associateBy { it.orderIndex }
        for (planDay in planProgram.days) {
            val day = daysByOrder[planDay.orderIndex] ?: continue
            if (SeededProgramRefresh.shouldRefreshLabel(day, planDay)) {
                programDayDao.update(day.copy(label = planDay.label))
            }
        }

        val lowerDay = daysByOrder[SeededProgramRefresh.lowerPosteriorCoreOrder] ?: return
        val existingExercises = programExerciseDao.getForDay(lowerDay.id).sortedBy { it.orderIndex }
        val seedKeysByExerciseId = existingExercises.associate {
            it.exerciseId to exerciseDao.getById(it.exerciseId)?.seedKey
        }
        if (!SeededProgramRefresh.matchesLegacyLowerPosteriorCore(existingExercises, seedKeysByExerciseId)) {
            return
        }

        val planExercises = planProgram.days
            .single { it.orderIndex == SeededProgramRefresh.lowerPosteriorCoreOrder }
            .exercises
        for ((index, planExercise) in planExercises.withIndex()) {
            val current = existingExercises.getOrNull(index) ?: return
            val exercise = exerciseDao.getBySeedKey(userId, planExercise.exerciseSeedKey) ?: return
            programExerciseDao.update(current.copy(
                orderIndex = index,
                exerciseId = exercise.id,
                sets = planExercise.sets,
                repsMin = planExercise.repsMin,
                repsMax = planExercise.repsMax,
                restSec = planExercise.restSec,
                rpeTarget = planExercise.rpeTarget,
                supersetGroupId = planExercise.supersetGroupId,
                protocol = planExercise.protocol
            ))
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
        const val defaultProgramRefreshVersion = 1
    }

    object SeededProgramRefresh {
        const val lowerPosteriorCoreOrder = 4

        private val legacyDayLabelsByOrder = mapOf(
            0 to "Upper A",
            1 to "Lower A",
            3 to "Upper B",
            4 to "Lower B"
        )

        private val legacyLowerPosteriorCoreExercises = listOf(
            ProgramSeeds.PlanExercise("leg_curl", 3, 10, 12, 60),
            ProgramSeeds.PlanExercise("goblet_squat", 3, 10, 10, 90),
            ProgramSeeds.PlanExercise("hip_thrust", 3, 12, 12, 90),
            ProgramSeeds.PlanExercise("lying_leg_raise", 3, 15, 15, 60)
        )

        fun shouldRefreshLabel(day: ProgramDay, planDay: ProgramSeeds.PlanDay): Boolean =
            day.orderIndex == planDay.orderIndex &&
                day.label == legacyDayLabelsByOrder[day.orderIndex] &&
                day.label != planDay.label

        fun backfillSeededTemplateMetadata(
            program: Program,
            planProgram: ProgramSeeds.PlanProgram
        ): Program {
            if (!program.isTemplate) return program
            return program.copy(
                type = planProgram.programType,
                notes = program.notes ?: planProgram.notes
            )
        }

        fun matchesLegacyLowerPosteriorCore(
            exercises: List<ProgramExercise>,
            seedKeysByExerciseId: Map<Long, String?>
        ): Boolean {
            val sorted = exercises.sortedBy { it.orderIndex }
            if (sorted.size != legacyLowerPosteriorCoreExercises.size) return false
            return sorted.zip(legacyLowerPosteriorCoreExercises).withIndex().all { (index, pair) ->
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
    }
}
