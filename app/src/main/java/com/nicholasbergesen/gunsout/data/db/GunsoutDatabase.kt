package com.nicholasbergesen.gunsout.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nicholasbergesen.gunsout.data.dao.BodyMetricsLogDao
import com.nicholasbergesen.gunsout.data.dao.ExerciseAlternateDao
import com.nicholasbergesen.gunsout.data.dao.ExerciseDao
import com.nicholasbergesen.gunsout.data.dao.FoodEntryDao
import com.nicholasbergesen.gunsout.data.dao.MealTemplateDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDao
import com.nicholasbergesen.gunsout.data.dao.ProgramDayDao
import com.nicholasbergesen.gunsout.data.dao.ProgramExerciseDao
import com.nicholasbergesen.gunsout.data.dao.SetEntryDao
import com.nicholasbergesen.gunsout.data.dao.SupplementDao
import com.nicholasbergesen.gunsout.data.dao.SupplementLogDao
import com.nicholasbergesen.gunsout.data.dao.WorkoutSessionDao
import com.nicholasbergesen.gunsout.data.entity.BodyMetricsLog
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.ExerciseAlternate
import com.nicholasbergesen.gunsout.data.entity.FoodEntry
import com.nicholasbergesen.gunsout.data.entity.MealTemplate
import com.nicholasbergesen.gunsout.data.entity.Program
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.Supplement
import com.nicholasbergesen.gunsout.data.entity.SupplementLog
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession

@Database(
    version = 6,
    exportSchema = true,
    entities = [
        Program::class,
        ProgramDay::class,
        Exercise::class,
        ExerciseAlternate::class,
        ProgramExercise::class,
        WorkoutSession::class,
        SetEntry::class,
        MealTemplate::class,
        FoodEntry::class,
        Supplement::class,
        SupplementLog::class,
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
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun supplementDao(): SupplementDao
    abstract fun supplementLogDao(): SupplementLogDao
    abstract fun bodyMetricsLogDao(): BodyMetricsLogDao
}
