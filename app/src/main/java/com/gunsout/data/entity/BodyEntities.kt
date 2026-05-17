package com.gunsout.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "supplement")
data class Supplement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultDose: Double,
    val unit: SupplementUnit,
    val notes: String? = null,
    val takeWith: String? = null,
    val reminderTime: LocalTime? = null,
    val isActive: Boolean = true,
    val isUserCreated: Boolean = false,
    val seedKey: String? = null
)

@Entity(
    tableName = "supplement_log",
    foreignKeys = [ForeignKey(
        entity = Supplement::class,
        parentColumns = ["id"],
        childColumns = ["supplementId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("supplementId"), Index("date")]
)
data class SupplementLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplementId: Long,
    val date: LocalDate,
    val doseTaken: Double,
    val unit: SupplementUnit,
    val takenAt: LocalDateTime = LocalDateTime.now()
)

@Entity(
    tableName = "body_metrics_log",
    indices = [Index("date")]
)
data class BodyMetricsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val weightKg: Double,
    val bodyFatPct: Double? = null,
    val muscleMassKg: Double? = null,
    val waterPct: Double? = null,
    val boneMassKg: Double? = null,
    val visceralFatRating: Int? = null,
    val notes: String? = null
)
