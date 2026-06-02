package com.nicholasbergesen.gunsout.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program",
    indices = [Index("userId")]
)
data class Program(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val type: ProgramType = ProgramType.CUSTOM,
    val notes: String? = null,
    val isActive: Boolean = false,
    val isTemplate: Boolean = false,
    val seedKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "program_day",
    foreignKeys = [ForeignKey(
        entity = Program::class,
        parentColumns = ["id"],
        childColumns = ["programId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("programId"), Index("userId")]
)
data class ProgramDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val programId: Long,
    val orderIndex: Int,
    val label: String,
    val preferredDayOfWeek: DayHint? = null,
    val isRest: Boolean = false
)

@Entity(
    tableName = "exercise",
    indices = [Index("userId")]
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val primaryMuscleGroup: MuscleGroup,
    val equipment: Equipment,
    val movementPattern: MovementPattern = defaultMovementPatternFor(primaryMuscleGroup),
    val formNotes: String? = null,
    val defaultRestSec: Int = 90,
    val baselineNote: String? = null,
    val isUserCreated: Boolean = false,
    val isArchived: Boolean = false,
    val seedKey: String? = null
)

@Entity(
    tableName = "exercise_alternate",
    primaryKeys = ["exerciseId", "alternateExerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["alternateExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("alternateExerciseId"), Index("userId")]
)
data class ExerciseAlternate(
    val exerciseId: Long,
    val alternateExerciseId: Long,
    val userId: String,
    val reason: AlternateReason
)

@Entity(
    tableName = "program_exercise",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDay::class,
            parentColumns = ["id"],
            childColumns = ["programDayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("programDayId"), Index("exerciseId"), Index("userId")]
)
data class ProgramExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val programDayId: Long,
    val orderIndex: Int,
    val exerciseId: Long,
    val sets: Int = 3,
    val repsMin: Int = 8,
    val repsMax: Int = 10,
    val restSec: Int = 90,
    val rpeTarget: Int? = null,
    val supersetGroupId: Int? = null,
    val protocol: Protocol = Protocol.STANDARD
)
