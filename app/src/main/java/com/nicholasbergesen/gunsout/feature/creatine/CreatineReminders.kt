package com.nicholasbergesen.gunsout.feature.creatine

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nicholasbergesen.gunsout.MainActivity
import com.nicholasbergesen.gunsout.auth.AuthSessionStore
import com.nicholasbergesen.gunsout.data.dao.CreatineDao
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import com.nicholasbergesen.gunsout.data.repo.CreatineReminderUpdater
import com.nicholasbergesen.gunsout.domain.nutrition.CreatineReminderPolicy
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreatineReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val creatineDao: CreatineDao
) : CreatineReminderUpdater {
    override fun reschedule(settings: CreatineSettings) {
        cancelForUser(settings.userId)
        val time = settings.reminderTime ?: return
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val trigger = nextCreatineReminder(now, time)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            reminderIntent(context, settings.userId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            trigger.toInstant().toEpochMilli(),
            pendingIntent
        )
    }

    suspend fun armForUser(userId: String) {
        val settings = creatineDao.getSettings(userId) ?: return
        reschedule(settings)
    }

    fun cancelForUser(@Suppress("UNUSED_PARAMETER") userId: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            reminderIntent(context, ""),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun ensureChannel() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Creatine reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily reminder for the configured creatine dose."
                }
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "supplement_reminders"
        const val ACTION_DAILY = "com.nicholasbergesen.gunsout.CREATINE_REMINDER"
        const val EXTRA_USER_ID = "user_id"
        const val REMINDER_REQUEST_CODE = 41_170
        const val REMINDER_NOTIFICATION_ID = 41_170

        fun reminderIntent(context: Context, userId: String): Intent =
            Intent(context, CreatineReminderReceiver::class.java).apply {
                action = ACTION_DAILY
                putExtra(EXTRA_USER_ID, userId)
            }
    }
}

@AndroidEntryPoint
class CreatineReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: CreatineReminderScheduler
    @Inject lateinit var creatineDao: CreatineDao
    @Inject lateinit var authSessionStore: AuthSessionStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CreatineReminderScheduler.ACTION_DAILY) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val intendedUserId =
                    intent.getStringExtra(CreatineReminderScheduler.EXTRA_USER_ID) ?: return@launch
                val currentUserId = authSessionStore.currentSignedInUserId.first()
                val settings = creatineDao.getSettings(intendedUserId) ?: return@launch
                val checkedToday =
                    creatineDao.getCheck(intendedUserId, LocalDate.now()) != null
                val reminderEnabled = settings.reminderTime != null
                val shouldNotify = CreatineReminderPolicy.shouldNotify(
                    intendedUserId = intendedUserId,
                    currentUserId = currentUserId,
                    reminderEnabled = reminderEnabled,
                    checkedToday = checkedToday
                )

                if (currentUserId == intendedUserId && reminderEnabled) {
                    // This alarm is one-shot so each occurrence stays at local wall-clock time.
                    scheduler.reschedule(settings)
                }
                if (!shouldNotify) return@launch

                scheduler.ensureChannel()
                val openApp = Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val contentIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openApp,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(
                    context,
                    CreatineReminderScheduler.CHANNEL_ID
                )
                    .setSmallIcon(android.R.drawable.ic_menu_today)
                    .setContentTitle("Take creatine")
                    .setContentText("Time for ${settings.doseGrams} g.")
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .build()
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(
                    CreatineReminderScheduler.REMINDER_NOTIFICATION_ID,
                    notification
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@AndroidEntryPoint
class CreatineReminderSystemReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: CreatineReminderScheduler
    @Inject lateinit var authSessionStore: AuthSessionStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SYSTEM_RESCHEDULE_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val userId = authSessionStore.currentSignedInUserId.first() ?: return@launch
                scheduler.armForUser(userId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val SYSTEM_RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}

internal fun nextCreatineReminder(
    now: ZonedDateTime,
    reminderTime: LocalTime
): ZonedDateTime {
    val zone = now.zone
    val today = now.toLocalDate().atTime(reminderTime).atZone(zone)
    return if (today.isAfter(now)) {
        today
    } else {
        now.toLocalDate().plusDays(1).atTime(reminderTime).atZone(zone)
    }
}
