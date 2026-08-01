package com.nicholasbergesen.gunsout.data.db

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the runtime invariant that Room enforces inside `RoomDatabase.Builder.build()`:
 * no explicit Migration may start or end on a version that also appears in
 * `fallbackToDestructiveMigrationFrom(...)`. Hitting this invariant in production crashes the
 * app on first DB construction with `IllegalArgumentException: Inconsistency detected. A
 * Migration was supplied ... that has a start or end version equal to a start version supplied
 * to fallbackToDestructiveMigrationFrom(...)`, which is what shipped in an earlier revision of
 * this PR before the v2/v3 -> v4 destructive-wipe regime replaced MIGRATION_1_2 as dead code.
 *
 * This test runs as a pure-Kotlin JVM unit test (no Android instrumentation) so the regression
 * is caught before the APK is ever assembled.
 */
class MigrationsConsistencyTest {

    @Test
    fun `no migration overlaps with the destructive fallback version list`() {
        val destructive = Migrations.destructiveFallbackFromVersions.toSet()
        val migrationEndpoints = Migrations.allMigrations
            .flatMap { listOf(it.startVersion, it.endVersion) }
            .toSet()
        val overlap = migrationEndpoints.intersect(destructive)
        assertTrue(
            "Migrations $migrationEndpoints overlap with destructive fallback $destructive; " +
                "Room.databaseBuilder().build() will throw IllegalArgumentException at runtime.",
            overlap.isEmpty()
        )
    }

    @Test
    fun `latest schema has v7 to v8 migration registered`() {
        assertTrue(Migrations.allMigrations.any { it.startVersion == 7 && it.endVersion == 8 })
    }

    @Test
    fun `v8 schema contains protein and creatine tables without legacy nutrition tables`() {
        val schema = listOf(
            File("schemas/com.nicholasbergesen.gunsout.data.db.GunsoutDatabase/8.json"),
            File("app/schemas/com.nicholasbergesen.gunsout.data.db.GunsoutDatabase/8.json")
        ).firstOrNull { it.exists() }

        assertTrue("Could not find exported v8 Room schema", schema != null)
        val text = schema!!.readText()
        assertTrue(text.contains("\"tableName\": \"protein_entry\""))
        assertTrue(text.contains("\"tableName\": \"protein_target_snapshot\""))
        assertTrue(text.contains("\"tableName\": \"creatine_settings\""))
        assertTrue(text.contains("\"tableName\": \"creatine_check\""))
        assertTrue(!text.contains("\"tableName\": \"food_entry\""))
        assertTrue(!text.contains("\"tableName\": \"supplement\""))
    }
}
