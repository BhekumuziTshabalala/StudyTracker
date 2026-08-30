package com.iu.studytracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.AlarmManager
import androidx.core.app.NotificationCompat
import com.iu.studytracker.MainActivity
import com.iu.studytracker.R

class PauseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // If timer is not paused, ignore
        if (TimerState.isRunning.value || TimerState.sessionState.value == SessionState.IDLE || TimerState.sessionState.value == SessionState.FINISHED) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val resumeIntent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
        }
        val resumePendingIntent = PendingIntent.getService(
            context, 2, resumeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val endIntent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        }
        val endPendingIntent = PendingIntent.getService(
            context, 3, endIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(context, FocusTimerService.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Session Paused")
            .setContentText("Your study session has been paused for a while. Ready to continue?")
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Resume", resumePendingIntent)
            .addAction(0, "End Session", endPendingIntent)
            
        notificationManager.notify(FocusTimerService.NOTIFICATION_ID + 2, builder.build())

        // Schedule next reminder in 5 minutes
        scheduleNextReminder(context)
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 999
        private const val REMINDER_INTERVAL_MILLIS = 5 * 60 * 1000L // 5 minutes

        fun scheduleNextReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PauseReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            
            val triggerTime = System.currentTimeMillis() + REMINDER_INTERVAL_MILLIS
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        fun cancelReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, PauseReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
            
            // Also cancel any showing pause notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(FocusTimerService.NOTIFICATION_ID + 2)
        }
    }
}
