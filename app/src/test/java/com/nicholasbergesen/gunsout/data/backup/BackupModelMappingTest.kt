package com.nicholasbergesen.gunsout.data.backup

import com.nicholasbergesen.gunsout.data.entity.AlternateReason
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.entity.CreatineCheck
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import com.nicholasbergesen.gunsout.data.entity.DayHint
import com.nicholasbergesen.gunsout.data.entity.Equipment
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.ExerciseAlternate
import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.ProgramType
import com.nicholasbergesen.gunsout.data.entity.Protocol
import com.nicholasbergesen.gunsout.data.entity.ProteinEntry
import com.nicholasbergesen.gunsout.data.entity.ProteinTargetSnapshot
import com.nicholasbergesen.gunsout.data.entity.SessionStatus
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import com.nicholasbergesen.gunsout.data.seed.SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BackupModelMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `room entities round trip through backup models for the target user`() {
        val sourceUser = "source-user"
        val targetUser = "target-user"
        val createdAt = 1_776_000_000_000L
        val sessionStartedAt = LocalDateTime.of(2026, 6, 14, 6, 30)
        val sessionCompletedAt = LocalDateTime.of(2026, 6, 14, 7, 45)
        val creatineTakenAt = LocalDateTime.of(2026, 6, 14, 8, 0)
        val foodDate = LocalDate.of(2026, 6, 14)

        val program = Program(
            id = 10,
            userId = sourceUser,
            name = "Cutting block",
            type = ProgramType.UPPER_LOWER,
            notes = "Protect knee",
            isActive = true,
            isTemplate = true,
            seedKey = "upper_lower",
            createdAt = createdAt
        )
        val day = ProgramDay(
            id = 11,
            userId = sourceUser,
            programId = program.id,
            orderIndex = 2,
            label = "Lower posterior",
            preferredDayOfWeek = DayHint.SUN,
            isRest = false
        )
        val exercise = Exercise(
            id = 12,
            userId = sourceUser,
            name = "Trap bar deadlift",
            primaryMuscleGroup = MuscleGroup.GLUTES,
            equipment = Equipment.BARBELL,
            movementPattern = MovementPattern.HINGE,
            formNotes = "Brace first",
            defaultRestSec = 150,
            baselineNote = "Start light",
            isUserCreated = false,
            isArchived = true,
            seedKey = "trap_bar_deadlift"
        )
        val alternate = ExerciseAlternate(
            userId = sourceUser,
            exerciseId = exercise.id,
            alternateExerciseId = 99,
            reason = AlternateReason.INJURY
        )
        val programExercise = ProgramExercise(
            id = 13,
            userId = sourceUser,
            programDayId = day.id,
            orderIndex = 1,
            exerciseId = exercise.id,
            sets = 4,
            repsMin = 5,
            repsMax = 8,
            restSec = 180,
            rpeTarget = 8,
            supersetGroupId = 2,
            protocol = Protocol.AMRAP
        )
        val session = WorkoutSession(
            id = 14,
            userId = sourceUser,
            date = foodDate,
            programDayId = day.id,
            programDayLabelSnapshot = day.label,
            status = SessionStatus.COMPLETED,
            notes = "Felt solid",
            kneeFeel = 2,
            startedAt = sessionStartedAt,
            completedAt = sessionCompletedAt
        )
        val set = SetEntry(
            id = 15,
            userId = sourceUser,
            sessionId = session.id,
            programExerciseId = programExercise.id,
            exerciseIdSnapshot = exercise.id,
            exerciseNameSnapshot = exercise.name,
            setIndex = 3,
            weightKg = 90.0,
            reps = 8,
            rpe = 7,
            isWarmup = true,
            completedAt = sessionCompletedAt
        )
        val proteinEntry = ProteinEntry(
            id = 16,
            userId = sourceUser,
            date = foodDate,
            grams = 45,
            label = "Rice bowl",
            loggedAt = createdAt
        )
        val targetSnapshot = ProteinTargetSnapshot(
            userId = sourceUser,
            date = foodDate,
            targetGrams = 160
        )
        val creatineSettings = CreatineSettings(
            userId = sourceUser,
            doseGrams = 5,
            reminderTime = LocalTime.of(9, 30)
        )
        val creatineCheck = CreatineCheck(
            userId = sourceUser,
            date = foodDate,
            doseGrams = 5,
            takenAt = creatineTakenAt
        )
        val bodyLog = BodyMetricsLog(
            id = 20,
            userId = sourceUser,
            date = foodDate,
            weightKg = 96.2,
            bodyFatPct = 18.4,
            muscleMassKg = 70.1,
            waterPct = 55.0,
            waterLiters = 52.9,
            boneMassKg = 3.5,
            visceralFatRating = 8,
            notes = "InBody"
        )

        assertEquals(program.copy(userId = targetUser), program.toBackup().toEntity(targetUser))
        assertEquals(day.copy(userId = targetUser), day.toBackup().toEntity(targetUser))
        assertEquals(exercise.copy(userId = targetUser), exercise.toBackup().toEntity(targetUser))
        assertEquals(alternate.copy(userId = targetUser), alternate.toBackup().toEntity(targetUser))
        assertEquals(programExercise.copy(userId = targetUser), programExercise.toBackup().toEntity(targetUser))
        assertEquals(session.copy(userId = targetUser), session.toBackup().toEntity(targetUser))
        assertEquals(set.copy(userId = targetUser), set.toBackup().toEntity(targetUser))
        assertEquals(
            proteinEntry.copy(userId = targetUser),
            proteinEntry.toBackup().toEntity(targetUser)
        )
        assertEquals(
            targetSnapshot.copy(userId = targetUser),
            targetSnapshot.toBackup().toEntity(targetUser)
        )
        assertEquals(
            creatineSettings.copy(userId = targetUser),
            creatineSettings.toBackup().toEntity(targetUser)
        )
        assertEquals(
            creatineCheck.copy(userId = targetUser),
            creatineCheck.toBackup().toEntity(targetUser)
        )
        assertEquals(bodyLog.copy(userId = targetUser), bodyLog.toBackup().toEntity(targetUser))
    }

    @Test
    fun `legacy food conversion keeps positive protein only and rounds to whole grams`() {
        val legacy = FoodEntryBackup(
            id = 7,
            date = "2026-06-14",
            mealType = "DINNER",
            name = "  Rice bowl  ",
            kcal = 650,
            proteinG = 44.6,
            carbsG = 80.0,
            fatG = 15.0,
            createdAt = 123L
        )

        val converted = legacy.toProteinEntry("target-user")!!

        assertEquals(45, converted.grams)
        assertEquals("Rice bowl", converted.label)
        assertEquals(123L, converted.loggedAt)
        assertNull(legacy.copy(proteinG = 0.0).toProteinEntry("target-user"))
    }

    @Test
    fun `legacy supplement conversion accepts seeded creatine and rejects adjacent supplements`() {
        val creatine = SupplementBackup(
            id = 1,
            name = "Creatine",
            defaultDose = 4.6,
            unit = "G",
            reminderTime = "09:30",
            isActive = true,
            isUserCreated = false,
            seedKey = LEGACY_CREATINE_SEED_KEY
        )
        val other = creatine.copy(id = 2, name = "Vitamin D", seedKey = "vitamin_d")
        val log = SupplementLogBackup(
            id = 3,
            supplementId = creatine.id,
            date = "2026-06-14",
            doseTaken = 4.6,
            unit = "G",
            takenAt = "2026-06-14T09:31"
        )

        assertEquals(5, creatine.toCreatineSettings("target-user")!!.doseGrams)
        assertEquals(LocalTime.of(9, 30), creatine.toCreatineSettings("target-user")!!.reminderTime)
        assertNull(other.toCreatineSettings("target-user"))
        assertEquals(5, log.toCreatineCheck("target-user")!!.doseGrams)
        assertNull(log.copy(unit = "MG").toCreatineCheck("target-user"))
    }

    @Test fun `explicit default movement patterns from backfilled backups are preserved on import mapping`() {
        val editedSeededExercises = listOf(
            exerciseBackup(
                seedKey = "leg_extension",
                primaryMuscleGroup = MuscleGroup.QUADS,
                equipment = Equipment.MACHINE,
                movementPattern = MovementPattern.SQUAT
            ),
            exerciseBackup(
                seedKey = "leg_curl",
                primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
                equipment = Equipment.MACHINE,
                movementPattern = MovementPattern.HINGE
            ),
            exerciseBackup(
                seedKey = "db_lying_leg_curl",
                primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
                equipment = Equipment.DUMBBELL,
                movementPattern = MovementPattern.HINGE
            ),
            exerciseBackup(
                seedKey = "db_bicep_curl",
                primaryMuscleGroup = MuscleGroup.BICEPS,
                equipment = Equipment.DUMBBELL,
                movementPattern = MovementPattern.PULL
            ),
            exerciseBackup(
                seedKey = "db_walking_lunge",
                primaryMuscleGroup = MuscleGroup.QUADS,
                equipment = Equipment.DUMBBELL,
                movementPattern = MovementPattern.SQUAT
            )
        )

        editedSeededExercises.forEach { backup ->
            val sourceBackup = backupWithExercise(
                exercise = backup,
                seededMovementPatternBackfillVersion = SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION
            )
            assertFalse(sourceBackup.needsImportedSeededMovementPatternBackfill())
            assertEquals(
                "${backup.seedKey} should preserve the explicitly exported movement pattern",
                MovementPattern.valueOf(backup.movementPattern!!),
                backup.toEntity(
                    userId = "target-user",
                    backfillLegacySeededMovementPattern =
                        sourceBackup.needsImportedSeededMovementPatternBackfill()
                ).movementPattern
            )
        }
    }

    @Test fun `schema 6 pre-marker backup repairs serialized legacy seeded isolation pattern`() {
        val backup = json.decodeFromString<GunsoutBackup>(
            """
            {
              "schemaVersion": 6,
              "exportedAtIso": "2026-06-14T00:00:00",
              "programs": [],
              "programDays": [],
              "exercises": [
                {
                  "id": 1,
                  "name": "Leg Extensions",
                  "primaryMuscleGroup": "QUADS",
                  "equipment": "MACHINE",
                  "movementPattern": "SQUAT",
                  "defaultRestSec": 60,
                  "isUserCreated": false,
                  "isArchived": false,
                  "seedKey": "leg_extension"
                }
              ],
              "exerciseAlternates": [],
              "programExercises": [],
              "sessions": [],
              "setEntries": [],
              "mealTemplates": [],
              "foodEntries": [],
              "supplements": [],
              "supplementLogs": [],
              "bodyMetricsLogs": [],
              "userProfile": {
                "currentBodyWeightKg": 100.0,
                "goalBodyWeightKg": 80.0,
                "kneeInjuryFlag": true,
                "baselineWeekActive": true,
                "firstRunDone": true
              }
            }
            """.trimIndent()
        )

        assertTrue(backup.needsImportedSeededMovementPatternBackfill())
        assertEquals(
            SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION,
            backup.seededMovementPatternBackfillVersionAfterImport()
        )
        assertEquals(
            MovementPattern.ISOLATION,
            backup.exercises.single().toEntity(
                userId = "target-user",
                backfillLegacySeededMovementPattern =
                    backup.needsImportedSeededMovementPatternBackfill()
            ).movementPattern
        )
    }

    @Test fun `schema 6 backfilled backup preserves explicit default-valued seeded edit`() {
        val backup = backupWithExercise(
            seededMovementPatternBackfillVersion = SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION,
            exercise = exerciseBackup(
                seedKey = "leg_extension",
                primaryMuscleGroup = MuscleGroup.QUADS,
                equipment = Equipment.MACHINE,
                movementPattern = MovementPattern.SQUAT
            )
        )

        assertFalse(backup.needsImportedSeededMovementPatternBackfill())
        assertEquals(
            SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION,
            backup.seededMovementPatternBackfillVersionAfterImport()
        )
        assertEquals(
            MovementPattern.SQUAT,
            backup.exercises.single().toEntity(
                userId = "target-user",
                backfillLegacySeededMovementPattern =
                    backup.needsImportedSeededMovementPatternBackfill()
            ).movementPattern
        )
    }

    @Test fun `active imported program marks first run done before seed refresh`() {
        val backup = backupWithPrograms(
            programs = listOf(programBackup(isActive = true)),
            userProfile = profileBackup(
                seededMovementPatternBackfillVersion = SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION,
                firstRunDone = false
            )
        )

        val profile = backup.userProfile!!.toUserProfile().withImportSeedState(backup)

        assertEquals(1, backup.importedActiveProgramCount())
        assertTrue(profile.firstRunDone)
    }

    @Test fun `legacy active program import without profile suppresses default activation`() {
        val backup = backupWithPrograms(
            programs = listOf(programBackup(isActive = true)),
            userProfile = null
        )

        val profile = UserProfile(firstRunDone = false)
            .withProfilelessImportSeedState(backup)

        assertEquals(1, backup.importedActiveProgramCount())
        assertTrue(profile.firstRunDone)
        assertEquals(0, profile.defaultProgramRefreshVersion)
    }

    @Test fun `legacy inactive program import without profile resets stale first run state`() {
        val backup = backupWithPrograms(
            programs = listOf(programBackup(isActive = false)),
            userProfile = null
        )

        val profile = UserProfile(firstRunDone = true, defaultProgramRefreshVersion = 2)
            .withProfilelessImportSeedState(backup)

        assertEquals(0, backup.importedActiveProgramCount())
        assertFalse(profile.firstRunDone)
        assertEquals(0, profile.defaultProgramRefreshVersion)
    }

    @Test fun `inactive program import does not mark first run done`() {
        val backup = backupWithPrograms(
            programs = listOf(programBackup(isActive = false)),
            userProfile = profileBackup(
                seededMovementPatternBackfillVersion = SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION,
                firstRunDone = false
            )
        )

        val profile = backup.userProfile!!.toUserProfile().withImportSeedState(backup)

        assertEquals(0, backup.importedActiveProgramCount())
        assertFalse(profile.firstRunDone)
    }

    @Test fun `missing movement pattern on legacy seeded isolation exercises is backfilled after fallback`() {
        val legacySeededExercises = listOf(
            exerciseBackup(
                seedKey = "leg_extension",
                primaryMuscleGroup = MuscleGroup.QUADS,
                equipment = Equipment.MACHINE,
                movementPattern = null
            ),
            exerciseBackup(
                seedKey = "leg_curl",
                primaryMuscleGroup = MuscleGroup.HAMSTRINGS,
                equipment = Equipment.MACHINE,
                movementPattern = null
            ),
            exerciseBackup(
                seedKey = "db_bicep_curl",
                primaryMuscleGroup = MuscleGroup.BICEPS,
                equipment = Equipment.DUMBBELL,
                movementPattern = null
            )
        )

        legacySeededExercises.forEach { backup ->
            assertEquals(
                "${backup.seedKey} should backfill the seeded isolation pattern only for legacy imports",
                MovementPattern.ISOLATION,
                backup.toEntity(
                    userId = "target-user",
                    backfillLegacySeededMovementPattern = true
                ).movementPattern
            )
        }
    }

    @Test fun `seeded movement pattern backfill does not over-apply to adjacent imported exercises`() {
        val customQuad = exerciseBackup(
            seedKey = "custom_leg_extension",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.MACHINE,
            movementPattern = null
        ).toEntity("target-user", backfillLegacySeededMovementPattern = true)
        val mismatchedSeed = exerciseBackup(
            seedKey = "leg_extension",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.DUMBBELL,
            movementPattern = MovementPattern.SQUAT
        ).toEntity("target-user", backfillLegacySeededMovementPattern = true)
        val intentionallyEditedSeed = exerciseBackup(
            seedKey = "leg_extension",
            primaryMuscleGroup = MuscleGroup.QUADS,
            equipment = Equipment.MACHINE,
            movementPattern = MovementPattern.PULL
        ).toEntity("target-user", backfillLegacySeededMovementPattern = true)

        assertEquals(MovementPattern.SQUAT, customQuad.movementPattern)
        assertEquals(MovementPattern.SQUAT, mismatchedSeed.movementPattern)
        assertEquals(MovementPattern.PULL, intentionallyEditedSeed.movementPattern)
    }

    private fun exerciseBackup(
        seedKey: String?,
        primaryMuscleGroup: MuscleGroup,
        equipment: Equipment,
        movementPattern: MovementPattern?
    ) = ExerciseBackup(
        id = 1,
        name = seedKey ?: "Custom exercise",
        primaryMuscleGroup = primaryMuscleGroup.name,
        equipment = equipment.name,
        movementPattern = movementPattern?.name,
        formNotes = null,
        defaultRestSec = 60,
        baselineNote = null,
        isUserCreated = seedKey == null,
        isArchived = false,
        seedKey = seedKey
    )

    private fun backupWithPrograms(
        programs: List<ProgramBackup>,
        userProfile: UserProfileBackup?
    ) = GunsoutBackup(
        schemaVersion = 6,
        exportedAtIso = "2026-06-14T00:00:00",
        programs = programs,
        programDays = emptyList(),
        exercises = emptyList(),
        exerciseAlternates = emptyList(),
        programExercises = emptyList(),
        sessions = emptyList(),
        setEntries = emptyList(),
        mealTemplates = emptyList(),
        foodEntries = emptyList(),
        supplements = emptyList(),
        supplementLogs = emptyList(),
        bodyMetricsLogs = emptyList(),
        userProfile = userProfile
    )

    private fun programBackup(isActive: Boolean) = ProgramBackup(
        id = 1,
        name = "Imported active plan",
        type = ProgramType.CUSTOM.name,
        notes = null,
        isActive = isActive,
        isTemplate = false,
        seedKey = null,
        createdAt = 1L
    )

    private fun backupWithExercise(
        exercise: ExerciseBackup,
        seededMovementPatternBackfillVersion: Int
    ) = GunsoutBackup(
        schemaVersion = 6,
        exportedAtIso = "2026-06-14T00:00:00",
        programs = emptyList(),
        programDays = emptyList(),
        exercises = listOf(exercise),
        exerciseAlternates = emptyList(),
        programExercises = emptyList(),
        sessions = emptyList(),
        setEntries = emptyList(),
        mealTemplates = emptyList(),
        foodEntries = emptyList(),
        supplements = emptyList(),
        supplementLogs = emptyList(),
        bodyMetricsLogs = emptyList(),
        userProfile = profileBackup(
            seededMovementPatternBackfillVersion = seededMovementPatternBackfillVersion
        )
    )

    private fun profileBackup(
        seededMovementPatternBackfillVersion: Int,
        firstRunDone: Boolean = true
    ) = UserProfileBackup(
        currentBodyWeightKg = 100.0,
        goalBodyWeightKg = 80.0,
        kneeInjuryFlag = true,
        baselineWeekActive = true,
        firstRunDone = firstRunDone,
        defaultProgramRefreshVersion = 1,
        seededMovementPatternBackfillVersion = seededMovementPatternBackfillVersion
    )
}
