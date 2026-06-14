package com.nicholasbergesen.gunsout.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Place future Room migrations here as the schema evolves once data preservation matters.
 *
 * Versions 1, 2, and 3 are intentionally destructive on upgrade (see
 * [destructiveFallbackFromVersions]); the multi-user PR drops the Ingredient,
 * MealTemplateIngredient and MealPlan tables and adds per-user scoping to every remaining entity,
 * which cannot be migrated meaningfully. From version 4 onward, every schema bump must add an
 * explicit migration here so data survives the update.
 *
 * To add a new migration:
 *   1. Bump `GunsoutDatabase.version`.
 *   2. Add `MIGRATION_X_Y` below with the SQL needed to transform the old schema to the new.
 *   3. Append it to [allMigrations].
 *   4. Run a build so Room exports the new schema JSON under `app/schemas/`.
 *
 * Invariant: no migration's start or end version may appear in
 * [destructiveFallbackFromVersions]; Room rejects such overlap at build() with
 * IllegalArgumentException. The invariant is asserted by `MigrationsConsistencyTest`.
 */
object Migrations {

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE body_metrics_log ADD COLUMN waterLiters REAL")
            db.execSQL(
                """
                UPDATE body_metrics_log
                SET waterLiters = weightKg * waterPct / 100.0
                WHERE waterPct IS NOT NULL
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE exercise ADD COLUMN movementPattern TEXT NOT NULL DEFAULT 'ISOLATION'")
            db.execSQL(
                """
                UPDATE exercise
                SET movementPattern = CASE
                    WHEN primaryMuscleGroup IN ('CHEST', 'SHOULDERS', 'TRICEPS') THEN 'PUSH'
                    WHEN primaryMuscleGroup IN ('BACK', 'BICEPS') THEN 'PULL'
                    WHEN primaryMuscleGroup = 'QUADS' THEN 'SQUAT'
                    WHEN primaryMuscleGroup IN ('HAMSTRINGS', 'GLUTES') THEN 'HINGE'
                    WHEN primaryMuscleGroup = 'CALVES' THEN 'CALVES'
                    WHEN primaryMuscleGroup = 'CORE' THEN 'CORE'
                    ELSE 'ISOLATION'
                END
                """.trimIndent()
            )
        }
    }

    val allMigrations: Array<Migration> = arrayOf(MIGRATION_4_5, MIGRATION_5_6)

    val destructiveFallbackFromVersions: IntArray = intArrayOf(1, 2, 3)
}
