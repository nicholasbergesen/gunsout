package com.nicholasbergesen.gunsout.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(
    tableName = "protein_entry",
    indices = [Index(value = ["userId", "date"])]
)
data class ProteinEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: LocalDate,
    val grams: Int,
    val label: String? = null,
    val loggedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "protein_target_snapshot",
    primaryKeys = ["userId", "date"]
)
data class ProteinTargetSnapshot(
    val userId: String,
    val date: LocalDate,
    val targetGrams: Int
)

@Entity(tableName = "creatine_settings")
data class CreatineSettings(
    @PrimaryKey val userId: String,
    val doseGrams: Int = DEFAULT_CREATINE_DOSE_GRAMS,
    val reminderTime: LocalTime? = null
)

@Entity(
    tableName = "creatine_check",
    primaryKeys = ["userId", "date"]
)
data class CreatineCheck(
    val userId: String,
    val date: LocalDate,
    val doseGrams: Int,
    val takenAt: LocalDateTime = LocalDateTime.now()
)

const val DEFAULT_CREATINE_DOSE_GRAMS = 5
