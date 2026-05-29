package com.nicholasbergesen.gunsout.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Place future Room migrations here as the schema evolves. Always add explicit migrations rather
 * than relying on `fallbackToDestructiveMigration`; data must survive app updates.
 *
 * To add a new migration:
 *   1. Bump `GunsoutDatabase.version`.
 *   2. Add `MIGRATION_X_Y` below with the SQL needed to transform the old schema to the new.
 *   3. Append it to [allMigrations].
 *   4. Run a build so Room exports the new schema JSON under `app/schemas/`.
 */
object Migrations {

    /**
     * v2: Add the unique index that prevents duplicate set rows for the same
     * (sessionId, programExerciseId, setIndex, isWarmup) tuple. Existing rows that already
     * violate it are deduplicated, keeping the most recent (highest id) row per slot.
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                DELETE FROM set_entry
                WHERE id NOT IN (
                    SELECT MAX(id) FROM set_entry
                    GROUP BY sessionId,
                             COALESCE(programExerciseId, -1),
                             setIndex,
                             isWarmup
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS idx_set_entry_unique_slot
                ON set_entry(sessionId, programExerciseId, setIndex, isWarmup)
                """.trimIndent()
            )
        }
    }

    val allMigrations: Array<Migration> = arrayOf(MIGRATION_1_2)
}
