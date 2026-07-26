package com.nicholasbergesen.gunsout.domain.nutrition

import com.nicholasbergesen.gunsout.data.prefs.ActivityLevel
import com.nicholasbergesen.gunsout.data.prefs.GoalType
import com.nicholasbergesen.gunsout.data.prefs.Sex
import com.nicholasbergesen.gunsout.data.prefs.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionTargetCalculatorsTest {
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

    @Test
    fun `calorie suggestion preserves canonical Mifflin St Jeor result`() {
        assertEquals(2759, CalorieTargetCalculator.suggest(maleProfile()))
    }

    @Test
    fun `calorie suggestion requires every guidance profile input in range`() {
        assertNull(CalorieTargetCalculator.suggest(maleProfile(age = null)))
        assertNull(CalorieTargetCalculator.suggest(maleProfile(sex = null)))
        assertNull(CalorieTargetCalculator.suggest(maleProfile(heightCm = null)))
        assertNull(CalorieTargetCalculator.suggest(maleProfile(heightCm = 50)))
        assertNull(CalorieTargetCalculator.suggest(maleProfile(age = 150)))
        assertNull(CalorieTargetCalculator.suggest(maleProfile(currentKg = 10.0)))
        assertNull(CalorieTargetCalculator.suggest(maleProfile(goalKg = 400.0)))
    }

    @Test
    fun `calorie suggestion applies activity goal delta and floor`() {
        val femaleCut = UserProfile(
            currentBodyWeightKg = 65.0,
            goalBodyWeightKg = 60.0,
            heightCm = 165,
            age = 35,
            sex = Sex.FEMALE,
            activityLevel = ActivityLevel.LIGHT,
            goalType = GoalType.CUT
        )
        val lowCalorieCut = UserProfile(
            currentBodyWeightKg = 40.0,
            goalBodyWeightKg = 40.0,
            heightCm = 150,
            age = 50,
            sex = Sex.FEMALE,
            activityLevel = ActivityLevel.SEDENTARY,
            goalType = GoalType.CUT
        )

        assertEquals(1349, CalorieTargetCalculator.suggest(femaleCut))
        assertEquals(1200, CalorieTargetCalculator.suggest(lowCalorieCut))
        assertTrue(
            CalorieTargetCalculator.suggest(
                maleProfile(activity = ActivityLevel.VERY_ACTIVE, goal = GoalType.BULK)
            )!! > CalorieTargetCalculator.suggest(
                maleProfile(activity = ActivityLevel.SEDENTARY)
            )!! + 800
        )
    }

    @Test
    fun `calorie override works without a suggestion and is independent`() {
        val incomplete = maleProfile(age = null)

        assertNull(CalorieTargetCalculator.effective(incomplete, null))
        assertEquals(
            CalorieTarget(2500, TargetSource.OVERRIDDEN),
            CalorieTargetCalculator.effective(incomplete, 2500)
        )
        assertEquals(
            CalorieTarget(2759, TargetSource.SUGGESTED),
            CalorieTargetCalculator.effective(maleProfile(), null)
        )
    }

    @Test
    fun `protein suggestion uses only goal weight and rounds to whole grams`() {
        val otherwiseIncomplete = maleProfile(
            age = null,
            sex = null,
            heightCm = null,
            goalKg = 80.4
        )

        assertEquals(161, ProteinTargetCalculator.suggest(otherwiseIncomplete))
    }

    @Test
    fun `protein suggestion rejects invalid goal weight but override still works`() {
        val invalidGoal = maleProfile(goalKg = 0.0)

        assertNull(ProteinTargetCalculator.suggest(invalidGoal))
        assertNull(ProteinTargetCalculator.effective(invalidGoal, null))
        assertEquals(
            ProteinTarget(175, TargetSource.OVERRIDDEN),
            ProteinTargetCalculator.effective(invalidGoal, 175)
        )
    }
}
