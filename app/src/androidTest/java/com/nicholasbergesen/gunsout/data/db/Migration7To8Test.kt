package com.nicholasbergesen.gunsout.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration7To8Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GunsoutDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration_preservesProteinAndCreatineWhileDroppingObsoleteNutritionData() {
        helper.createDatabase(TEST_DATABASE, 7).apply {
            execSQL(
                """
                INSERT INTO food_entry
                    (id, userId, date, mealType, name, kcal, proteinG, carbsG, fatG, sourceTemplateId, createdAt)
                VALUES
                    (1, 'user', '2026-07-25', 'DINNER', '  Rice bowl  ', 600, 44.6, 70, 10, NULL, 1234),
                    (2, 'user', '2026-07-25', 'SNACK', 'No protein', 100, 0, 20, 2, NULL, 1235)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO supplement
                    (id, userId, name, defaultDose, unit, notes, takeWith, reminderTime, isActive, isUserCreated, seedKey)
                VALUES
                    (10, 'user', 'Creatine', 4.6, 'G', NULL, NULL, '09:30', 1, 0, 'creatine_mono'),
                    (11, 'user', 'Vitamin D', 1, 'CAPSULE', NULL, NULL, NULL, 1, 1, 'vitamin_d')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO supplement_log
                    (id, userId, supplementId, date, doseTaken, unit, takenAt)
                VALUES
                    (20, 'user', 10, '2026-07-25', 4.6, 'G', '2026-07-25T09:31'),
                    (21, 'user', 11, '2026-07-25', 1, 'CAPSULE', '2026-07-25T09:32')
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            8,
            true,
            *Migrations.allMigrations
        )

        migrated.query(
            "SELECT grams, label, loggedAt FROM protein_entry WHERE userId = 'user'"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(45, cursor.getInt(0))
            assertEquals("Rice bowl", cursor.getString(1))
            assertEquals(1234L, cursor.getLong(2))
        }
        migrated.query(
            "SELECT doseGrams, reminderTime FROM creatine_settings WHERE userId = 'user'"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(5, cursor.getInt(0))
            assertEquals("09:30", cursor.getString(1))
        }
        migrated.query(
            "SELECT doseGrams, takenAt FROM creatine_check WHERE userId = 'user'"
        ).use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(5, cursor.getInt(0))
            assertEquals("2026-07-25T09:31", cursor.getString(1))
        }
        migrated.query("SELECT * FROM protein_target_snapshot").use { cursor ->
            assertFalse(cursor.moveToFirst())
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-7-8"
    }
}
