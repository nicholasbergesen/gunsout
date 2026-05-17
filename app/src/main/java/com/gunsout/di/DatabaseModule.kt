package com.gunsout.di

import android.content.Context
import androidx.room.Room
import com.gunsout.data.db.GunsoutDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): GunsoutDatabase =
        Room.databaseBuilder(context, GunsoutDatabase::class.java, "gunsout.db")
            .fallbackToDestructiveMigration()
            .build()

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
