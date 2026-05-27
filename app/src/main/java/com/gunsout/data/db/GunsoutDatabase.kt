package com.gunsout.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gunsout.data.dao.BodyMetricsLogDao
import com.gunsout.data.dao.ExerciseAlternateDao
import com.gunsout.data.dao.ExerciseDao
import com.gunsout.data.dao.FoodEntryDao
import com.gunsout.data.dao.MealTemplateDao
import com.gunsout.data.dao.ProgramDao
import com.gunsout.data.dao.ProgramDayDao
import com.gunsout.data.dao.ProgramExerciseDao
import com.gunsout.data.dao.SetEntryDao
import com.gunsout.data.dao.SupplementDao
import com.gunsout.data.dao.SupplementLogDao
import com.gunsout.data.dao.WorkoutSessionDao
import com.gunsout.data.entity.BodyMetricsLog
import com.gunsout.data.entity.Exercise
import com.gunsout.data.entity.ExerciseAlternate
import com.gunsout.data.entity.FoodEntry
import com.gunsout.data.entity.MealTemplate
import com.gunsout.data.entity.Program
import com.gunsout.data.entity.ProgramDay
import com.gunsout.data.entity.ProgramExercise
import com.gunsout.data.entity.SetEntry
import com.gunsout.data.entity.Supplement
import com.gunsout.data.entity.SupplementLog
import com.gunsout.data.entity.WorkoutSession

@Database(
    version = 3,
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
