package com.iu.studytracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.iu.studytracker.MainActivity
import com.iu.studytracker.R
import com.iu.studytracker.StudyTrackerApp
import kotlinx.coroutines.*
import kotlin.math.max

class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_MINUTES = "EXTRA_MINUTES"
        
        private const val NOTIFICATION_CHANNEL_ID = "focus_timer_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                if (taskId != null) {
                    val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Focus Time"
                    val minutes = intent.getIntExtra(EXTRA_MINUTES, 25)
                    
                    if (TimerState.currentTaskId.value != taskId) {
                        TimerState.currentTaskId.value = taskId
                        TimerState.currentTaskTitle.value = title
                        TimerState.totalMillis.value = minutes * 60 * 1000L
                        TimerState.remainingMillis.value = TimerState.totalMillis.value
                    }
                } else {
                    // Resume action: if we don't have a current task, we can't resume
                    if (TimerState.currentTaskId.value == null) {
                        return START_NOT_STICKY
                    }
                }
                
                startTimer()
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        if (TimerState.isRunning.value) return
        
        TimerState.isRunning.value = true
        startForeground(NOTIFICATION_ID, buildNotification())
        
        timerJob = serviceScope.launch {
            while (TimerState.remainingMillis.value > 0 && TimerState.isRunning.value) {
                delay(1000)
                TimerState.remainingMillis.value = max(0, TimerState.remainingMillis.value - 1000)
                updateNotification()
            }
            
            if (TimerState.remainingMillis.value == 0L) {
                // Timer finished!
                onTimerFinished()
            }
        }
    }

    private fun pauseTimer() {
        TimerState.isRunning.value = false
        timerJob?.cancel()
        updateNotification()
    }

    private fun stopTimer() {
        pauseTimer()
        TimerState.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerFinished() {
        TimerState.isRunning.value = false
        val taskId = TimerState.currentTaskId.value
        val minutesSpent = (TimerState.totalMillis.value / (60 * 1000L)).toInt()
        
        // Update database
        if (taskId != null) {
            serviceScope.launch(Dispatchers.IO) {
                val repository = (applicationContext as StudyTrackerApp).repository
                // We'll add an increment method to the repository
                repository.incrementTaskMinutes(taskId, minutesSpent)
            }
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        TimerState.reset()
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Can pass extras to navigate straight to the focus screen
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseAction = if (TimerState.isRunning.value) {
            val pauseIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", pausePending)
        } else {
            val playIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_START }
            val playPending = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Resume", playPending)
        }
        
        val stopIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopAction = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)

        val minutes = (TimerState.remainingMillis.value / 1000) / 60
        val seconds = (TimerState.remainingMillis.value / 1000) % 60
        val timeString = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(TimerState.currentTaskTitle.value ?: "Focus Timer")
            .setContentText(timeString)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default for now
            .setContentIntent(pendingIntent)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setOngoing(TimerState.isRunning.value)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Focus Timer",
                NotificationManager.IMPORTANCE_LOW // Low importance so it doesn't ring every second
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
