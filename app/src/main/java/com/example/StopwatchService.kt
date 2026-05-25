package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.viewmodel.StopwatchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class StopwatchService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceCompat()
        startTicker()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cronômetros - Fluxo Driver",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação de controle dos cronômetros de trabalho e almoço."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_WORK -> {
                StopwatchState.isLunchStopwatchRunning.value = false
                StopwatchState.isStopwatchRunning.value = true
            }
            ACTION_PAUSE_WORK -> {
                StopwatchState.isStopwatchRunning.value = false
            }
            ACTION_START_LUNCH -> {
                StopwatchState.isStopwatchRunning.value = false
                StopwatchState.isLunchStopwatchRunning.value = true
            }
            ACTION_PAUSE_LUNCH -> {
                StopwatchState.isLunchStopwatchRunning.value = false
            }
            ACTION_RESET_ALL -> {
                StopwatchState.isStopwatchRunning.value = false
                StopwatchState.isLunchStopwatchRunning.value = false
                StopwatchState.stopwatchSeconds.value = 0L
                StopwatchState.lunchStopwatchSeconds.value = 0L
                stopSelf()
                return START_NOT_STICKY
            }
        }
        
        // Ensure notification matched immediately!
        updateNotification()
        
        // If everything is paused/stopped, and we didn't explicitly reset, we should still stay alive
        // unless they reset.
        return START_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                var updated = false
                if (StopwatchState.isStopwatchRunning.value) {
                    StopwatchState.stopwatchSeconds.value += 1
                    updated = true
                }
                if (StopwatchState.isLunchStopwatchRunning.value) {
                    StopwatchState.lunchStopwatchSeconds.value += 1
                    updated = true
                }
                // Update notification whenever there is a tick, or periodically
                if (updated || !StopwatchState.isStopwatchRunning.value && !StopwatchState.isLunchStopwatchRunning.value) {
                    updateNotification()
                }
            }
        }
    }

    private fun updateNotification() {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(): Notification {
        val isWorkRunning = StopwatchState.isStopwatchRunning.value
        val isLunchRunning = StopwatchState.isLunchStopwatchRunning.value
        
        val workSec = StopwatchState.stopwatchSeconds.value
        val lunchSec = StopwatchState.lunchStopwatchSeconds.value

        val title = when {
            isWorkRunning -> "⏱️ Fluxo Driver - Em Atividade"
            isLunchRunning -> "☕ Fluxo Driver - Almoço"
            else -> "⏸️ Fluxo Driver - Pausados"
        }

        val contentText = "Trabalho: ${formatTime(workSec)} | Almoço: ${formatTime(lunchSec)}"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // Standard system play icon used for small badge
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)

        // Work Action Button
        val workActionIntent = if (isWorkRunning) ACTION_PAUSE_WORK else ACTION_START_WORK
        val workActionLabel = if (isWorkRunning) "Pausar Trabalho" else "Iniciar Trabalho"
        builder.addAction(0, workActionLabel, createActionPendingIntent(workActionIntent))

        // Lunch Action Button
        val lunchActionIntent = if (isLunchRunning) ACTION_PAUSE_LUNCH else ACTION_START_LUNCH
        val lunchActionLabel = if (isLunchRunning) "Pausar Almoço" else "Iniciar Almoço"
        builder.addAction(0, lunchActionLabel, createActionPendingIntent(lunchActionIntent))

        return builder.build()
    }

    private fun createActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, StopwatchService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "stopwatch_channel"
        const val NOTIFICATION_ID = 4132

        const val ACTION_START_WORK = "ACTION_START_WORK"
        const val ACTION_PAUSE_WORK = "ACTION_PAUSE_WORK"
        const val ACTION_START_LUNCH = "ACTION_START_LUNCH"
        const val ACTION_PAUSE_LUNCH = "ACTION_PAUSE_LUNCH"
        const val ACTION_RESET_ALL = "ACTION_RESET_ALL"
    }
}
