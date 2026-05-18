package com.gunsout.feature.supplements

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gunsout.MainActivity
import com.gunsout.data.dao.SupplementDao
import com.gunsout.data.entity.Supplement
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and posts daily supplement intake reminders. Uses [AlarmManager.setInexactRepeating]
 * so we never need the [android.Manifest.permission.SCHEDULE_EXACT_ALARM] permission. Reminders
 * survive reboot via [SupplementBootReceiver].
 */
@Singleton
class SupplementReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun reschedule(supplement: Supplement) {
        cancel(supplement.id)
        val time = supplement.reminderTime
        if (!supplement.isActive || time == null) return

        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var trigger = now.with(time)
        if (trigger.isBefore(now)) trigger = trigger.plusDays(1)

        val intent = buildIntent(supplement.id, supplement.name, supplement.defaultDose, supplement.unit.name)
        val pi = PendingIntent.getBroadcast(
            context, supplement.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            pi
        )
    }

    fun cancel(supplementId: Long) {
        val pi = PendingIntent.getBroadcast(
            context, supplementId.toInt(), buildIntent(supplementId, "", 0.0, "G"),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi)
        pi.cancel()
    }

    private fun buildIntent(id: Long, name: String, dose: Double, unit: String): Intent =
        Intent(context, SupplementReminderReceiver::class.java).apply {
            action = SupplementReminderReceiver.ACTION_DAILY
            putExtra(SupplementReminderReceiver.EXTRA_ID, id)
            putExtra(SupplementReminderReceiver.EXTRA_NAME, name)
            putExtra(SupplementReminderReceiver.EXTRA_DOSE, dose)
            putExtra(SupplementReminderReceiver.EXTRA_UNIT, unit)
        }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(NotificationChannel(
                    CHANNEL_ID, "Supplement reminders", NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Daily reminder for active supplements."
                })
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "supplement_reminders"
    }
}

/** Posts a daily reminder notification. */
@AndroidEntryPoint
class SupplementReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: SupplementReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DAILY) return
        scheduler.ensureChannel()
        val id = intent.getLongExtra(EXTRA_ID, -1)
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val dose = intent.getDoubleExtra(EXTRA_DOSE, 0.0)
        val unit = intent.getStringExtra(EXTRA_UNIT).orEmpty()

        val openApp = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val contentPi = PendingIntent.getActivity(
            context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, SupplementReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle(name.ifBlank { "Take your supplement" })
            .setContentText(if (dose > 0.0) "Time for ${dose} ${unit.lowercase()}." else "Daily reminder.")
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify((id and 0x7FFFFFFFL).toInt(), notification)
    }

    companion object {
        const val ACTION_DAILY = "com.gunsout.SUPPLEMENT_REMINDER"
        const val EXTRA_ID = "id"
        const val EXTRA_NAME = "name"
        const val EXTRA_DOSE = "dose"
        const val EXTRA_UNIT = "unit"
    }
}

/** Re-schedules every active supplement after device reboot. */
@AndroidEntryPoint
class SupplementBootReceiver : BroadcastReceiver() {

    @Inject lateinit var supplementDao: SupplementDao

    @Inject lateinit var scheduler: SupplementReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val supplements = supplementDao.allActiveOnce()
                supplements.forEach { scheduler.reschedule(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
