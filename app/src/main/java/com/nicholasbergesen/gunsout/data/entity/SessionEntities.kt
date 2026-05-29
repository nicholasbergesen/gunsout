package com.nicholasbergesen.gunsout.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "workout_session",
    foreignKeys = [ForeignKey(
        entity = ProgramDay::class,
        parentColumns = ["id"],
        childColumns = ["programDayId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("programDayId"), Index("date"), Index("userId")]
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: LocalDate,
    val programDayId: Long?,
    val programDayLabelSnapshot: String,
    val status: SessionStatus = SessionStatus.IN_PROGRESS,
    val notes: String? = null,
    val kneeFeel: Int? = null,
    val startedAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null
)

@Entity(
    tableName = "set_entry",
    foreignKeys = [ForeignKey(
        entity = WorkoutSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("sessionId"),
        Index("exerciseIdSnapshot"),
        Index("userId"),
        Index(
            value = ["sessionId", "programExerciseId", "setIndex", "isWarmup"],
            unique = true,
            name = "idx_set_entry_unique_slot"
        )
    ]
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val sessionId: Long,
    val programExerciseId: Long?,
    val exerciseIdSnapshot: Long,
    val exerciseNameSnapshot: String,
    val setIndex: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val rpe: Int? = null,
    val isWarmup: Boolean = false,
    val completedAt: LocalDateTime? = null
)
