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
        const val ACTION_START_BREAK = "ACTION_START_BREAK"
        const val ACTION_CONTINUE_SESSION = "ACTION_CONTINUE_SESSION"
        
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_MINUTES = "EXTRA_MINUTES"
        
        const val NOTIFICATION_CHANNEL_ID = "focus_timer_channel"
        const val NOTIFICATION_ID = 1001
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
                        TimerState.sessionState.value = SessionState.FOCUSING
                        
                        getSharedPreferences(TimerState.PREFS_NAME, MODE_PRIVATE)
                            .edit()
                            .putInt("KEY_LAST_FOCUS_MINUTES", minutes)
                            .apply()
                    }
                } else {
                    // Resume action: if we don't have a current task, we can't resume
                    if (TimerState.currentTaskId.value == null) {
                        startForeground(NOTIFICATION_ID, buildNotification(null))
                        stopSelf()
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
            ACTION_START_BREAK -> {
                val breakMinutes = intent?.getIntExtra(EXTRA_MINUTES, 5) ?: 5
                TimerState.totalMillis.value = breakMinutes * 60 * 1000L
                TimerState.remainingMillis.value = TimerState.totalMillis.value
                TimerState.sessionState.value = SessionState.BREAK
                startTimer()
            }
            ACTION_CONTINUE_SESSION -> {
                // Return to focusing
                TimerState.sessionState.value = SessionState.FOCUSING
                // We keep the same task ID and title, but restore the focus minutes
                val minutes = intent?.getIntExtra(EXTRA_MINUTES, 25) ?: 25
                TimerState.totalMillis.value = minutes * 60 * 1000L
                TimerState.remainingMillis.value = TimerState.totalMillis.value
                startTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        
        TimerState.isRunning.value = true
        PauseReminderReceiver.cancelReminder(this)
        
        // If we have a saved session end, use it; otherwise compute it.
        val prefs = getSharedPreferences(TimerState.PREFS_NAME, MODE_PRIVATE)
        val savedSessionEnd = prefs.getLong(TimerState.KEY_SESSION_END, 0L)
        val sessionEndEpochMillis = if (savedSessionEnd > 0) savedSessionEnd else (System.currentTimeMillis() + TimerState.remainingMillis.value)
        
        // Save to SharedPrefs
        prefs.edit()
            .putLong(TimerState.KEY_SESSION_END, sessionEndEpochMillis)
            .putString(TimerState.KEY_TASK_ID, TimerState.currentTaskId.value)
            .putString(TimerState.KEY_TASK_TITLE, TimerState.currentTaskTitle.value)
            .putLong(TimerState.KEY_TOTAL_MILLIS, TimerState.totalMillis.value)
            .putLong(TimerState.KEY_REMAINING_MILLIS, TimerState.remainingMillis.value)
            .putString(TimerState.KEY_SESSION_STATE, TimerState.sessionState.value.name)
            .putBoolean(TimerState.KEY_IS_RUNNING, true)
            .apply()
            
        startForeground(NOTIFICATION_ID, buildNotification(sessionEndEpochMillis))
        
        timerJob = serviceScope.launch {
            while (TimerState.isRunning.value) {
                val now = System.currentTimeMillis()
                val rem = max(0L, sessionEndEpochMillis - now)
                TimerState.remainingMillis.value = rem
                
                if (rem == 0L) {
                    onTimerFinished()
                    break
                }
                
                delay(1000L)
            }
        }
    }

    private fun pauseTimer() {
        TimerState.isRunning.value = false
        timerJob?.cancel()
        
        // Update SharedPrefs
        val prefs = getSharedPreferences(TimerState.PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putLong(TimerState.KEY_SESSION_END, 0L)
            .putLong(TimerState.KEY_REMAINING_MILLIS, TimerState.remainingMillis.value)
            .putBoolean(TimerState.KEY_IS_RUNNING, false)
            .apply()
            
        updateNotification()
        
        // Schedule 5-minute pause reminder
        PauseReminderReceiver.scheduleNextReminder(this)
    }

    private fun stopTimer() {
        val currentState = TimerState.sessionState.value
        if (currentState == SessionState.FOCUSING || currentState == SessionState.BREAK) {
            // Premature termination
            finishSession(premature = true)
        } else {
            // Hard stop
            clearTimerState()
        }
    }

    private fun onTimerFinished() {
        finishSession(premature = false)
    }
    
    private fun finishSession(premature: Boolean) {
        pauseTimer()
        PauseReminderReceiver.cancelReminder(this)
        val prefs = getSharedPreferences(TimerState.PREFS_NAME, MODE_PRIVATE)
        
        if (!premature && TimerState.soundEnabled.value) {
            try {
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, uri)
                ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val currentState = TimerState.sessionState.value
        val taskId = TimerState.currentTaskId.value
        val millisSpent = TimerState.totalMillis.value - TimerState.remainingMillis.value
        val minutesSpent = (millisSpent / (60 * 1000L)).toInt()
        
        if (taskId != null && minutesSpent > 0 && currentState == SessionState.FOCUSING) {
            serviceScope.launch(Dispatchers.IO) {
                val repository = (applicationContext as StudyTrackerApp).repository
                repository.incrementTaskMinutes(taskId, minutesSpent)
            }
        }

        if (currentState == SessionState.FOCUSING) {
            // Go to FINISHED state to trigger the post-session UI
            TimerState.sessionState.value = SessionState.FINISHED
            prefs.edit().putString(TimerState.KEY_SESSION_STATE, SessionState.FINISHED.name)
                .remove(TimerState.KEY_SESSION_END)
                .putBoolean(TimerState.KEY_IS_RUNNING, false)
                .apply()
                
            if (!premature) {
                showTimerFinishedNotification(isBreak = false)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else if (currentState == SessionState.BREAK) {
            // If it's a break, whether premature or not, just finish it.
            // But we shouldn't show a notification if they manually stopped it early.
            TimerState.sessionState.value = SessionState.FINISHED
            prefs.edit().putString(TimerState.KEY_SESSION_STATE, SessionState.FINISHED.name)
                .remove(TimerState.KEY_SESSION_END)
                .putBoolean(TimerState.KEY_IS_RUNNING, false)
                .apply()
                
            if (!premature) {
                showTimerFinishedNotification(isBreak = true)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            clearTimerState()
        }
    }
    
    private fun clearTimerState() {
        PauseReminderReceiver.cancelReminder(this)
        getSharedPreferences(TimerState.PREFS_NAME, MODE_PRIVATE).edit()
            .remove(TimerState.KEY_SESSION_END)
            .remove(TimerState.KEY_TASK_ID)
            .remove(TimerState.KEY_TASK_TITLE)
            .remove(TimerState.KEY_TOTAL_MILLIS)
            .remove(TimerState.KEY_IS_RUNNING)
            .remove(TimerState.KEY_REMAINING_MILLIS)
            .remove(TimerState.KEY_SESSION_STATE)
            .apply()
        
        TimerState.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun showTimerFinishedNotification(isBreak: Boolean) {
        val title = if (isBreak) "Break Over!" else "Focus Session Complete!"
        val text = if (isBreak) "Time to get back to studying." else "Great job! Time for a break or next unit."
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            
        if (isBreak) {
            val prefs = getSharedPreferences(TimerState.PREFS_NAME, MODE_PRIVATE)
            val lastFocusMinutes = prefs.getInt("KEY_LAST_FOCUS_MINUTES", 25)
            
            val continueIntent = Intent(this, FocusTimerService::class.java).apply {
                action = ACTION_CONTINUE_SESSION
                putExtra(EXTRA_MINUTES, lastFocusMinutes)
            }
            val continuePendingIntent = PendingIntent.getService(
                this, 1, continueIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(0, "Continue Session", continuePendingIntent)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, builder.build())
    }

    private fun buildNotification(sessionEndMillis: Long? = null): Notification {
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

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(TimerState.currentTaskTitle.value ?: "Focus Timer")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default for now
            .setContentIntent(pendingIntent)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setOngoing(TimerState.isRunning.value)
            .setOnlyAlertOnce(true)

        if (sessionEndMillis != null && TimerState.isRunning.value) {
            // Android's Chronometer expects SystemClock.elapsedRealtime() as the base, not currentTimeMillis.
            val remainingMillis = kotlin.math.max(0L, sessionEndMillis - System.currentTimeMillis())
            val chronometerBase = android.os.SystemClock.elapsedRealtime() + remainingMillis
            
            builder.setWhen(chronometerBase)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setContentText("Focus session in progress")
        } else {
            val minutes = (TimerState.remainingMillis.value / 1000) / 60
            val seconds = (TimerState.remainingMillis.value / 1000) % 60
            val timeString = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
            builder.setContentText(timeString)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        // If not running, don't pass sessionEndMillis to just show static text
        notificationManager.notify(NOTIFICATION_ID, buildNotification(null))
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
