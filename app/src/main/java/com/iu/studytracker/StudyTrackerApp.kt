package com.iu.studytracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.iu.studytracker.data.database.StudyTrackerDatabase
import com.iu.studytracker.data.repository.StudyRepository
import com.iu.studytracker.worker.StudyReminderWorker
import kotlinx.coroutines.launch

class StudyTrackerApp : Application() {

    // Lazy-init the database singleton
    val database: StudyTrackerDatabase by lazy {
        StudyTrackerDatabase.getInstance(this)
    }

    val repository: StudyRepository by lazy {
        StudyRepository(
            database = database,
            monthPlanDao = database.monthPlanDao(),
            moduleDao = database.moduleDao(),
            topicDao = database.topicDao(),
            taskDao = database.taskDao(),
            degreePlanDao = database.degreePlanDao(),
            curriculumDao = database.curriculumDao(),
            moduleDetailsDao = database.moduleDetailsDao(),
            taskTemplateDao = database.taskTemplateDao()
        )
    }

    val userPreferences: com.iu.studytracker.data.repository.UserPreferencesRepository by lazy {
        com.iu.studytracker.data.repository.UserPreferencesRepository(this)
    }

    val syncManager: com.iu.studytracker.data.repository.FirebaseSyncManager by lazy {
        com.iu.studytracker.data.repository.FirebaseSyncManager(this, database, userPreferences)
    }
    private val applicationScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        StudyReminderWorker.schedule(this)
        com.iu.studytracker.worker.TaskRecurrenceWorker.schedule(this)

        // Initialize Firebase Sync
        applicationScope.launch {
            syncManager.initialize()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Study Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily study goal reminders"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "study_reminder_channel"
    }
}
