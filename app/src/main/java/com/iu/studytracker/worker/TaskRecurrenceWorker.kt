package com.iu.studytracker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.database.entity.Task
import com.iu.studytracker.util.RecurrenceHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class TaskRecurrenceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as StudyTrackerApp).repository
        
        // Find tasks with RRULE that aren't deleted
        // Wait, we need a query in TaskDao for this. Let's assume we fetch all active tasks 
        // with recurrenceRule != null and isCompleted = false.
        
        // To be safe, let's fetch ALL tasks and filter for ones that are recurring.
        // Actually, we can just use a specific query in TaskDao.
        
        return try {
            val tasks = repository.getTasksWithRecurrenceSync()
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val tasksToInsert = mutableListOf<Task>()
            
            for (task in tasks) {
                if (task.isCompleted && task.recurrenceRule != null && task.scheduledDate != null) {
                    val nextDate = RecurrenceHelper.calculateNextOccurrence(task.scheduledDate, task.recurrenceRule)
                    if (nextDate != null) {
                        val existingTasks = repository.getTasksForDate(nextDate)
                        if (existingTasks.none { it.title == task.title }) {
                            val nextTask = task.copy(
                                id = UUID.randomUUID().toString(),
                                scheduledDate = nextDate,
                                status = com.iu.studytracker.data.database.entity.TaskStatus.TODO,
                                actualMinutesSpent = 0
                            )
                            tasksToInsert.add(nextTask)
                        }
                    }
                }
            }
            
            if (tasksToInsert.isNotEmpty()) {
                repository.insertTasks(tasksToInsert)
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "task_recurrence_worker"

        fun schedule(context: Context) {
            val request = androidx.work.PeriodicWorkRequestBuilder<TaskRecurrenceWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.DAYS
            ).build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
