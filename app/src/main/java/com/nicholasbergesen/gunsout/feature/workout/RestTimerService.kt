package com.nicholasbergesen.gunsout.feature.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that runs a rest-timer notification. The service auto-stops when the timer
 * hits zero. Designed for short countdowns (60-180 seconds) so it runs under the SPECIAL_USE
 * foreground service type allowed for short-form background work on Android 14+.
 */
class RestTimerService : Service() {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val totalSeconds = intent?.getIntExtra(EXTRA_SECONDS, 90)?.coerceAtLeast(1) ?: 90
        val exerciseLabel = intent?.getStringExtra(EXTRA_LABEL).orEmpty()
        ensureChannel()
        startForegroundInternal(totalSeconds, totalSeconds, exerciseLabel)
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1_000)
                remaining--
                updateNotification(remaining, totalSeconds, exerciseLabel)
            }
            stopForegroundCompat()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundInternal(remaining: Int, total: Int, label: String) {
        val notification = buildNotification(remaining, total, label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(remaining: Int, total: Int, label: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(remaining, total, label))
    }

    private fun buildNotification(remaining: Int, total: Int, label: String): Notification {
        val title = if (remaining == 0) "Rest complete" else "Rest: ${remaining}s"
        val body = if (label.isBlank()) "Rest timer" else "After: $label"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(title)
            .setContentText(body)
            .setOnlyAlertOnce(true)
            .setProgress(total, total - remaining, false)
            .setOngoing(remaining > 0)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(NotificationChannel(
                    CHANNEL_ID, "Rest timer", NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows remaining rest between sets."
                })
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    companion object {
        private const val CHANNEL_ID = "rest_timer"
        private const val NOTIFICATION_ID = 7345
        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_LABEL = "extra_label"

        fun start(context: Context, seconds: Int, label: String) {
            val intent = Intent(context, RestTimerService::class.java).apply {
                putExtra(EXTRA_SECONDS, seconds)
                putExtra(EXTRA_LABEL, label)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RestTimerService::class.java))
        }
    }
}
