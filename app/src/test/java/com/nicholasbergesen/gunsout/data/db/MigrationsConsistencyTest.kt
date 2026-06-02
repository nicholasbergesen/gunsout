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
    fun `latest schema has v5 to v6 migration registered`() {
        assertTrue(Migrations.allMigrations.any { it.startVersion == 5 && it.endVersion == 6 })
    }

    @Test
    fun `v6 movement pattern schema default matches v5 to v6 migration`() {
        val schema = listOf(
            File("schemas/com.nicholasbergesen.gunsout.data.db.GunsoutDatabase/6.json"),
            File("app/schemas/com.nicholasbergesen.gunsout.data.db.GunsoutDatabase/6.json")
        ).firstOrNull { it.exists() }

        assertTrue("Could not find exported v6 Room schema", schema != null)
        val text = schema!!.readText()
        assertTrue(
            "v6 schema must include the same movementPattern default as MIGRATION_5_6",
            text.contains("\"columnName\": \"movementPattern\"") &&
                text.contains("\"defaultValue\": \"'ISOLATION'\"")
        )
    }
}
