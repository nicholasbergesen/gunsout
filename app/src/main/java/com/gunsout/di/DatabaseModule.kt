package com.gunsout.di

import android.content.Context
import androidx.room.Room
import com.gunsout.BuildConfig
import com.gunsout.data.db.GunsoutDatabase
import com.gunsout.data.db.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GunsoutDatabase {
        val builder = Room.databaseBuilder(context, GunsoutDatabase::class.java, "gunsout.db")
            .addMigrations(*Migrations.allMigrations)
        // Only allow destructive fallback in debug builds. In release we'd rather crash on a
        // missing migration than wipe the user's history.
        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration()
        }
        return builder.build()
    }

    @Provides fun provideProgramDao(db: GunsoutDatabase) = db.programDao()
    @Provides fun provideProgramDayDao(db: GunsoutDatabase) = db.programDayDao()
    @Provides fun provideExerciseDao(db: GunsoutDatabase) = db.exerciseDao()
    @Provides fun provideExerciseAlternateDao(db: GunsoutDatabase) = db.exerciseAlternateDao()
    @Provides fun provideProgramExerciseDao(db: GunsoutDatabase) = db.programExerciseDao()
    @Provides fun provideWorkoutSessionDao(db: GunsoutDatabase) = db.workoutSessionDao()
    @Provides fun provideSetEntryDao(db: GunsoutDatabase) = db.setEntryDao()
    @Provides fun provideMealPlanDao(db: GunsoutDatabase) = db.mealPlanDao()
    @Provides fun provideMealTemplateDao(db: GunsoutDatabase) = db.mealTemplateDao()
    @Provides fun provideIngredientDao(db: GunsoutDatabase) = db.ingredientDao()
    @Provides fun provideMealTemplateIngredientDao(db: GunsoutDatabase) = db.mealTemplateIngredientDao()
    @Provides fun provideFoodEntryDao(db: GunsoutDatabase) = db.foodEntryDao()
    @Provides fun provideSupplementDao(db: GunsoutDatabase) = db.supplementDao()
    @Provides fun provideSupplementLogDao(db: GunsoutDatabase) = db.supplementLogDao()
    @Provides fun provideBodyMetricsLogDao(db: GunsoutDatabase) = db.bodyMetricsLogDao()
}
