package com.nicholasbergesen.gunsout.domain.nutrition

object CreatineReminderPolicy {
    fun shouldNotify(
        intendedUserId: String,
        currentUserId: String?,
        reminderEnabled: Boolean,
        checkedToday: Boolean
    ): Boolean =
        intendedUserId.isNotBlank() &&
            currentUserId == intendedUserId &&
            reminderEnabled &&
            !checkedToday
}
