package com.nicholasbergesen.gunsout.data.seed

import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeederMaintenancePlanTest {

    @Test fun `movement pattern backfill runs even after default program refresh is current`() {
        val plan = UserProfile(
            firstRunDone = true,
            defaultProgramRefreshVersion = DEFAULT_PROGRAM_REFRESH_VERSION,
            seededMovementPatternBackfillVersion = 0
        ).seederMaintenancePlan()

        assertFalse(plan.firstRun)
        assertFalse(plan.needsDefaultProgramRefresh)
        assertTrue(plan.needsSeededMovementPatternBackfill)
        assertTrue(plan.shouldUpdateProfile)
    }

    @Test fun `movement pattern backfill marker prevents rerun during later seeding work`() {
        val plan = UserProfile(
            firstRunDone = true,
            defaultProgramRefreshVersion = 0,
            seededMovementPatternBackfillVersion = SEEDED_MOVEMENT_PATTERN_BACKFILL_VERSION
        ).seederMaintenancePlan()

        assertTrue(plan.needsDefaultProgramRefresh)
        assertFalse(plan.needsSeededMovementPatternBackfill)
        assertTrue(plan.shouldUpdateProfile)
    }
}
