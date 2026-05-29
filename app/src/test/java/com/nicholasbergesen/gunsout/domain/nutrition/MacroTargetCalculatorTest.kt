package com.nicholasbergesen.gunsout.domain.nutrition

import com.nicholasbergesen.gunsout.data.prefs.ActivityLevel
import com.nicholasbergesen.gunsout.data.prefs.GoalType
import com.nicholasbergesen.gunsout.data.prefs.MacroOverrides
import com.nicholasbergesen.gunsout.data.prefs.Sex
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroTargetCalculatorTest {

    private fun maleProfile(
        age: Int? = 30,
        sex: Sex? = Sex.MALE,
        heightCm: Int? = 180,
        currentKg: Double = 80.0,
        goalKg: Double = 80.0,
        activity: ActivityLevel = ActivityLevel.MODERATE,
        goal: GoalType = GoalType.MAINTAIN
    ) = UserProfile(
        currentBodyWeightKg = currentKg,
        goalBodyWeightKg = goalKg,
        heightCm = heightCm,
        age = age,
        sex = sex,
        activityLevel = activity,
        goalType = goal
    )

    @Test fun suggest_nullWhenAgeMissing() {
        assertNull(MacroTargetCalculator.suggest(maleProfile(age = null)))
    }

    @Test fun suggest_nullWhenSexMissing() {
        assertNull(MacroTargetCalculator.suggest(maleProfile(sex = null)))
    }

    @Test fun suggest_nullWhenHeightMissing() {
        assertNull(MacroTargetCalculator.suggest(maleProfile(heightCm = null)))
    }

    @Test fun suggest_nullWhenHeightOutOfRange() {
        assertNull(MacroTargetCalculator.suggest(maleProfile(heightCm = 50)))
        assertNull(MacroTargetCalculator.suggest(maleProfile(heightCm = 300)))
    }

    @Test fun suggest_nullWhenAgeOutOfRange() {
        assertNull(MacroTargetCalculator.suggest(maleProfile(age = 5)))
        assertNull(MacroTargetCalculator.suggest(maleProfile(age = 150)))
    }

    @Test fun suggest_nullWhenWeightOutOfRange() {
        assertNull(MacroTargetCalculator.suggest(maleProfile(currentKg = 10.0)))
        assertNull(MacroTargetCalculator.suggest(maleProfile(goalKg = 400.0)))
    }

    @Test fun suggest_maleModerateMaintain_canonical() {
        // BMR = 10*80 + 6.25*180 - 5*30 + 5 = 800 + 1125 - 150 + 5 = 1780
        // Maintenance = 1780 * 1.55 = 2759
        // kcal = 2759
        // Protein = 2.0 * 80 = 160g
        // Fat = 0.9 * 80 = 72g
        // Carbs = (2759 - 160*4 - 72*9) / 4 = (2759 - 640 - 648) / 4 = 1471/4 = 367
        val s = MacroTargetCalculator.suggest(maleProfile())!!
        assertEquals(2759, s.kcal)
        assertEquals(160, s.proteinG)
        assertEquals(72, s.fatG)
        assertEquals(367, s.carbsG)
    }

    @Test fun suggest_femaleLightCut_appliesDeltas() {
        // Female: BMR = 10*65 + 6.25*165 - 5*35 - 161 = 650 + 1031.25 - 175 - 161 = 1345.25
        // LIGHT = 1.375; maintenance = 1849.7
        // CUT -500 => 1349.7 -> kcal 1349 (truncate to Int)
        val profile = UserProfile(
            currentBodyWeightKg = 65.0,
            goalBodyWeightKg = 60.0,
            heightCm = 165,
            age = 35,
            sex = Sex.FEMALE,
            activityLevel = ActivityLevel.LIGHT,
            goalType = GoalType.CUT
        )
        val s = MacroTargetCalculator.suggest(profile)!!
        assertEquals(1349, s.kcal)
        assertEquals(120, s.proteinG) // 2.0 * 60 = 120
        assertEquals(54, s.fatG)      // 0.9 * 60 = 54
    }

    @Test fun suggest_kcalFlooredAt1200() {
        // Tiny profile to force kcal below 1200
        val profile = UserProfile(
            currentBodyWeightKg = 40.0,
            goalBodyWeightKg = 40.0,
            heightCm = 150,
            age = 50,
            sex = Sex.FEMALE,
            activityLevel = ActivityLevel.SEDENTARY,
            goalType = GoalType.CUT
        )
        val s = MacroTargetCalculator.suggest(profile)!!
        assertTrue("kcal should floor at 1200, got ${s.kcal}", s.kcal >= 1200)
    }

    @Test fun suggest_veryActiveBulk_increasesKcal() {
        val sedentary = MacroTargetCalculator.suggest(
            maleProfile(activity = ActivityLevel.SEDENTARY, goal = GoalType.MAINTAIN)
        )!!
        val veryActiveBulk = MacroTargetCalculator.suggest(
            maleProfile(activity = ActivityLevel.VERY_ACTIVE, goal = GoalType.BULK)
        )!!
        assertTrue(veryActiveBulk.kcal > sedentary.kcal + 800)
    }

    @Test fun effectiveTarget_nullWhenSuggestNullAndNoOverrides() {
        val incomplete = maleProfile(age = null)
        val target = MacroTargetCalculator.effectiveTarget(incomplete, MacroOverrides())
        assertNull(target)
    }

    @Test fun effectiveTarget_nullWhenSuggestNullAndPartialOverrides() {
        val incomplete = maleProfile(age = null)
        val target = MacroTargetCalculator.effectiveTarget(
            incomplete,
            MacroOverrides(kcal = 2500, proteinG = 180)
        )
        assertNull(target)
    }

    @Test fun effectiveTarget_fullOverridesSurviveWhenSuggestNull() {
        val incomplete = maleProfile(age = null)
        val target = MacroTargetCalculator.effectiveTarget(
            incomplete,
            MacroOverrides(kcal = 2500, proteinG = 180, carbsG = 250, fatG = 80)
        )
        assertNotNull(target)
        assertEquals(2500, target!!.kcal)
        assertEquals(180, target.proteinG)
        assertEquals(MacroTarget.Source.OVERRIDDEN, target.source)
    }

    @Test fun effectiveTarget_suggestionWhenNoOverrides() {
        val target = MacroTargetCalculator.effectiveTarget(maleProfile(), MacroOverrides())!!
        assertEquals(2759, target.kcal)
        assertEquals(MacroTarget.Source.SUGGESTED, target.source)
    }

    @Test fun effectiveTarget_partialOverrideMergesWithSuggestion() {
        val target = MacroTargetCalculator.effectiveTarget(
            maleProfile(),
            MacroOverrides(kcal = 3000)
        )!!
        assertEquals(3000, target.kcal)
        assertEquals(160, target.proteinG) // suggested
        assertEquals(MacroTarget.Source.OVERRIDDEN, target.source)
    }
}
