package com.nicholasbergesen.gunsout.domain.nutrition

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatineReminderPolicyTest {
    @Test
    fun `reminder posts only for the signed in owner when enabled and incomplete`() {
        assertTrue(
            CreatineReminderPolicy.shouldNotify(
                intendedUserId = "user",
                currentUserId = "user",
                reminderEnabled = true,
                checkedToday = false
            )
        )
        assertFalse(
            CreatineReminderPolicy.shouldNotify("user", null, true, false)
        )
        assertFalse(
            CreatineReminderPolicy.shouldNotify("user", "other-user", true, false)
        )
        assertFalse(
            CreatineReminderPolicy.shouldNotify("user", "user", false, false)
        )
        assertFalse(
            CreatineReminderPolicy.shouldNotify("user", "user", true, true)
        )
        assertFalse(
            CreatineReminderPolicy.shouldNotify("", "", true, false)
        )
    }
}
