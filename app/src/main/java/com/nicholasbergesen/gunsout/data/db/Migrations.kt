package com.nicholasbergesen.gunsout.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

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

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE program_exercise ADD COLUMN isRetired INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val migration7To8Statements = listOf(
        """
        CREATE TABLE IF NOT EXISTS protein_entry (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            userId TEXT NOT NULL,
            date TEXT NOT NULL,
            grams INTEGER NOT NULL,
            label TEXT,
            loggedAt INTEGER NOT NULL
        )
        """.trimIndent(),
        """
        CREATE INDEX IF NOT EXISTS index_protein_entry_userId_date
        ON protein_entry (userId, date)
        """.trimIndent(),
        """
        INSERT INTO protein_entry (id, userId, date, grams, label, loggedAt)
        SELECT
            id,
            userId,
            date,
            MAX(1, CAST(ROUND(proteinG) AS INTEGER)),
            NULLIF(TRIM(name), ''),
            createdAt
        FROM food_entry
        WHERE proteinG > 0
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS protein_target_snapshot (
            userId TEXT NOT NULL,
            date TEXT NOT NULL,
            targetGrams INTEGER NOT NULL,
            PRIMARY KEY (userId, date)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS creatine_settings (
            userId TEXT NOT NULL PRIMARY KEY,
            doseGrams INTEGER NOT NULL,
            reminderTime TEXT
        )
        """.trimIndent(),
        """
        INSERT OR REPLACE INTO creatine_settings (userId, doseGrams, reminderTime)
        SELECT
            userId,
            CASE
                WHEN defaultDose > 0 THEN MAX(1, CAST(ROUND(defaultDose) AS INTEGER))
                ELSE 5
            END,
            reminderTime
        FROM supplement
        WHERE seedKey = 'creatine_mono' AND unit = 'G'
        """.trimIndent(),
        """
        INSERT OR IGNORE INTO creatine_settings (userId, doseGrams, reminderTime)
        SELECT DISTINCT userId, 5, NULL
        FROM program
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS creatine_check (
            userId TEXT NOT NULL,
            date TEXT NOT NULL,
            doseGrams INTEGER NOT NULL,
            takenAt TEXT NOT NULL,
            PRIMARY KEY (userId, date)
        )
        """.trimIndent(),
        """
        INSERT OR IGNORE INTO creatine_check (userId, date, doseGrams, takenAt)
        SELECT
            logs.userId,
            logs.date,
            MAX(1, CAST(ROUND(logs.doseTaken) AS INTEGER)),
            logs.takenAt
        FROM supplement_log AS logs
        INNER JOIN supplement AS supplements ON supplements.id = logs.supplementId
        WHERE supplements.seedKey = 'creatine_mono' AND logs.unit = 'G'
        ORDER BY logs.takenAt ASC
        """.trimIndent(),
        "DROP TABLE supplement_log",
        "DROP TABLE supplement",
        "DROP TABLE food_entry",
        "DROP TABLE meal_template"
    )

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migration7To8Statements.forEach(db::execSQL)
        }

        override fun migrate(connection: SQLiteConnection) {
            migration7To8Statements.forEach(connection::execSQL)
        }
    }

    val allMigrations: Array<Migration> =
        arrayOf(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)

    val destructiveFallbackFromVersions: IntArray = intArrayOf(1, 2, 3)
}
