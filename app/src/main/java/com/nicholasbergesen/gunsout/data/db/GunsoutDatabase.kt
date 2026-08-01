package com.nicholasbergesen.gunsout.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nicholasbergesen.gunsout.data.dao.BodyMetricsLogDao
import com.nicholasbergesen.gunsout.data.dao.CreatineDao
import com.nicholasbergesen.gunsout.data.dao.ExerciseAlternateDao
import com.nicholasbergesen.gunsout.data.dao.ExerciseDao
import com.nicholasbergesen.gunsout.data.dao.ProteinEntryDao
import com.nicholasbergesen.gunsout.data.dao.ProteinTargetSnapshotDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDayDao
import com.nicholasbergesen.gunsout.data.dao.ProgramExerciseDao
import com.nicholasbergesen.gunsout.data.dao.SetEntryDao
import com.nicholasbergesen.gunsout.data.dao.WorkoutSessionDao
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.entity.CreatineCheck
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.ExerciseAlternate
import com.nicholasbergesen.gunsout.data.entity.ProteinEntry
import com.nicholasbergesen.gunsout.data.entity.ProteinTargetSnapshot
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession

@Database(
    version = 8,
    exportSchema = true,
    entities = [
        Program::class,
        ProgramDay::class,
        Exercise::class,
        ExerciseAlternate::class,
        ProgramExercise::class,
        WorkoutSession::class,
        SetEntry::class,
        ProteinEntry::class,
        ProteinTargetSnapshot::class,
        CreatineSettings::class,
        CreatineCheck::class,
        BodyMetricsLog::class
    ]
)
@TypeConverters(Converters::class)
abstract class GunsoutDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun programDayDao(): ProgramDayDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseAlternateDao(): ExerciseAlternateDao
    abstract fun programExerciseDao(): ProgramExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setEntryDao(): SetEntryDao
    abstract fun proteinEntryDao(): ProteinEntryDao
    abstract fun proteinTargetSnapshotDao(): ProteinTargetSnapshotDao
    abstract fun creatineDao(): CreatineDao
    abstract fun bodyMetricsLogDao(): BodyMetricsLogDao
}
