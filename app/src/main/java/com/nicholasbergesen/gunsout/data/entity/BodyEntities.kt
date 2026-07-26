package com.nicholasbergesen.gunsout.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "body_metrics_log",
    indices = [Index("date"), Index("userId")]
)
data class BodyMetricsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: LocalDate,
    val weightKg: Double,
    val bodyFatPct: Double? = null,
    val muscleMassKg: Double? = null,
    val waterPct: Double? = null,
    val waterLiters: Double? = null,
    val boneMassKg: Double? = null,
    val visceralFatRating: Int? = null,
    val notes: String? = null
)
