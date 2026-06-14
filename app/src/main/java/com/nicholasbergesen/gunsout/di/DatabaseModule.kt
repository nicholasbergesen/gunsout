package com.nicholasbergesen.gunsout.di

import android.content.Context
import androidx.room.Room
import com.nicholasbergesen.gunsout.data.db.GunsoutDatabase
import com.nicholasbergesen.gunsout.data.db.Migrations
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
        // v2/v3 -> v4 transitions wipe the on-device data because this PR drops the Ingredient,
        // MealTemplateIngredient and MealPlan tables and adds per-user scoping to every remaining
        // entity, and writing a real Room migration through that shape change is not worth the
        // risk for a personal-use app. Scoped to the legacy versions explicitly via
        // fallbackToDestructiveMigrationFrom so a future schema bump (v4 -> v5) that forgets to
        // add a Migration object will throw at startup instead of silently wiping the database.
        // The destructive version list is sourced from Migrations.destructiveFallbackFromVersions
        // so the same constant is shared with the regression test that guards the no-overlap
        // invariant between explicit migrations and the destructive fallback list.
        return Room.databaseBuilder(context, GunsoutDatabase::class.java, "gunsout.db")
            .addMigrations(*Migrations.allMigrations)
            .fallbackToDestructiveMigrationFrom(
                dropAllTables = true,
                *Migrations.destructiveFallbackFromVersions
            )
            .build()
    }

    @Provides fun provideProgramDao(db: GunsoutDatabase) = db.programDao()
    @Provides fun provideProgramDayDao(db: GunsoutDatabase) = db.programDayDao()
    @Provides fun provideExerciseDao(db: GunsoutDatabase) = db.exerciseDao()
    @Provides fun provideExerciseAlternateDao(db: GunsoutDatabase) = db.exerciseAlternateDao()
    @Provides fun provideProgramExerciseDao(db: GunsoutDatabase) = db.programExerciseDao()
    @Provides fun provideWorkoutSessionDao(db: GunsoutDatabase) = db.workoutSessionDao()
    @Provides fun provideSetEntryDao(db: GunsoutDatabase) = db.setEntryDao()
    @Provides fun provideMealTemplateDao(db: GunsoutDatabase) = db.mealTemplateDao()
    @Provides fun provideFoodEntryDao(db: GunsoutDatabase) = db.foodEntryDao()
    @Provides fun provideSupplementDao(db: GunsoutDatabase) = db.supplementDao()
    @Provides fun provideSupplementLogDao(db: GunsoutDatabase) = db.supplementLogDao()
    @Provides fun provideBodyMetricsLogDao(db: GunsoutDatabase) = db.bodyMetricsLogDao()
}
