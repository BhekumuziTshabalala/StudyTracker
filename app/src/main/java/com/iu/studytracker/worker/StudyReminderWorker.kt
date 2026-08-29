package com.iu.studytracker.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.iu.studytracker.MainActivity
import com.iu.studytracker.StudyTrackerApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that sends daily study reminders.
 *
 * Fires once per day and checks how many incomplete tasks
 * are scheduled for today, then posts a notification.
 */
class StudyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as StudyTrackerApp
        val repository = app.repository

        val incompleteCount = repository.getIncompleteCountForToday()

        if (incompleteCount > 0) {
            // Find active module progress
            val activeModules = repository.getAllCurriculumModulesSync().filter { !it.isCompleted }
            val moduleProgressList = mutableListOf<String>()
            
            for (module in activeModules) {
                val totalTasks = repository.observeTaskCountForModule(module.id).first()
                val completedTasks = repository.observeCompletedTaskCountForModule(module.id).first()
                val remaining = totalTasks - completedTasks
                if (remaining > 0) {
                    moduleProgressList.add("${module.name}: $remaining tasks left")
                }
            }

            val moduleText = if (moduleProgressList.isNotEmpty()) {
                "\n" + moduleProgressList.joinToString("\n")
            } else {
                ""
            }

            sendNotification(
                title = "📚 Study Time!",
                message = "You have $incompleteCount topic${if (incompleteCount > 1) "s" else ""} to study today.$moduleText"
            )
        } else {
            // All done or rest day
            sendNotification(
                title = "✅ Great job!",
                message = "All caught up for today. Keep the streak going!"
            )
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, StudyTrackerApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val WORK_NAME = "daily_study_reminder"

        /**
         * Schedules a daily repeating reminder.
         * Uses ExistingPeriodicWorkPolicy.KEEP to avoid re-scheduling
         * if already active.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
