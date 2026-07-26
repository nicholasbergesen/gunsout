package com.nicholasbergesen.gunsout.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class Migration7To8Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        databaseFile(),
        BundledSQLiteDriver(),
        GunsoutDatabase::class,
        { GunsoutDatabase_Impl() },
        emptyList()
    )

    @Test
    fun migration_preservesProteinAndCreatineWhileDroppingObsoleteNutritionData() {
        helper.createDatabase(7).use { database ->
            database.execSQL(
                """
                INSERT INTO food_entry
                    (id, userId, date, mealType, name, kcal, proteinG, carbsG, fatG, sourceTemplateId, createdAt)
                VALUES
                    (1, 'user', '2026-07-25', 'DINNER', '  Rice bowl  ', 600, 44.6, 70, 10, NULL, 1234),
                    (2, 'user', '2026-07-25', 'SNACK', 'No protein', 100, 0, 20, 2, NULL, 1235)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO supplement
                    (id, userId, name, defaultDose, unit, notes, takeWith, reminderTime, isActive, isUserCreated, seedKey)
                VALUES
                    (10, 'user', 'Creatine', 4.6, 'G', NULL, NULL, '09:30', 1, 0, 'creatine_mono'),
                    (11, 'user', 'Vitamin D', 1, 'CAPSULE', NULL, NULL, NULL, 1, 1, 'vitamin_d')
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO supplement_log
                    (id, userId, supplementId, date, doseTaken, unit, takenAt)
                VALUES
                    (20, 'user', 10, '2026-07-25', 4.6, 'G', '2026-07-25T09:31'),
                    (21, 'user', 11, '2026-07-25', 1, 'CAPSULE', '2026-07-25T09:32')
                """.trimIndent()
            )
        }

        val migrated = helper.runMigrationsAndValidate(8, Migrations.allMigrations.toList())

        migrated.assertSingleRow(
            "SELECT grams, label, loggedAt FROM protein_entry WHERE userId = 'user'"
        ) { statement ->
            assertEquals(45, statement.getInt(0))
            assertEquals("Rice bowl", statement.getText(1))
            assertEquals(1234L, statement.getLong(2))
        }
        migrated.assertSingleRow(
            "SELECT doseGrams, reminderTime FROM creatine_settings WHERE userId = 'user'"
        ) { statement ->
            assertEquals(5, statement.getInt(0))
            assertEquals("09:30", statement.getText(1))
        }
        migrated.assertSingleRow(
            "SELECT doseGrams, takenAt FROM creatine_check WHERE userId = 'user'"
        ) { statement ->
            assertEquals(5, statement.getInt(0))
            assertEquals("2026-07-25T09:31", statement.getText(1))
        }
        migrated.prepare("SELECT * FROM protein_target_snapshot").use { statement ->
            assertFalse(statement.step())
        }
        migrated.close()
    }

    private fun SQLiteConnection.assertSingleRow(
        sql: String,
        assertions: (androidx.sqlite.SQLiteStatement) -> Unit
    ) {
        prepare(sql).use { statement ->
            assertTrue(statement.step())
            assertions(statement)
            assertFalse(statement.step())
        }
    }

    private fun databaseFile(): File =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getDatabasePath("migration-7-8")
            .also { file ->
                val parent = file.parentFile
                require(parent != null && (parent.isDirectory || parent.mkdirs())) {
                    "Could not create migration test database directory"
                }
                require(!file.exists() || file.delete()) {
                    "Could not reset migration test database"
                }
            }
}
