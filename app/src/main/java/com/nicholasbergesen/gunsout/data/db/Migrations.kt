package com.nicholasbergesen.gunsout.data.db

import androidx.room.migration.Migration

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

    val allMigrations: Array<Migration> = emptyArray()

    val destructiveFallbackFromVersions: IntArray = intArrayOf(1, 2, 3)
}
