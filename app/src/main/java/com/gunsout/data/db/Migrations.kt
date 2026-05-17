package com.gunsout.data.db

import androidx.room.migration.Migration

/**
 * Place future Room migrations here as the schema evolves. Always add explicit migrations rather
 * than relying on `fallbackToDestructiveMigration`; data must survive app updates.
 *
 * To add a new migration:
 *   1. Bump `GunsoutDatabase.version`.
 *   2. Add `MIGRATION_X_Y` here with the SQL needed to transform the old schema to the new.
 *   3. Append it to [allMigrations].
 */
object Migrations {
    val allMigrations: Array<Migration> = arrayOf(
        // Example template (uncomment when needed):
        // object : Migration(1, 2) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE workout_session ADD COLUMN newField TEXT")
        //     }
        // }
    )
}
