package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iu.studytracker.data.database.entity.DailyTask
import com.iu.studytracker.data.model.DailyTaskWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DailyTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<DailyTask>)

    /** Get all tasks for a specific calendar date. */
    @Query("SELECT * FROM daily_tasks WHERE scheduledDate = :date ORDER BY id ASC")
    suspend fun getTasksForDate(date: String): List<DailyTask>

    /** Observe today's tasks reactively (for the dashboard). */
    @Query("SELECT * FROM daily_tasks WHERE scheduledDate = :date ORDER BY id ASC")
    fun observeTasksForDate(date: String): Flow<List<DailyTask>>

    /** Get all tasks for a month plan (for calendar/progress views). */
    @Query("SELECT * FROM daily_tasks WHERE monthPlanId = :monthPlanId ORDER BY scheduledDate ASC, id ASC")
    fun observeTasksForMonth(monthPlanId: Long): Flow<List<DailyTask>>

    /** Mark a task as completed. */
    @Query("UPDATE daily_tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :taskId")
    suspend fun markComplete(taskId: Long, completedAt: Long = System.currentTimeMillis())

    /** Mark a task as incomplete (undo). */
    @Query("UPDATE daily_tasks SET isCompleted = 0, completedAt = NULL WHERE id = :taskId")
    suspend fun markIncomplete(taskId: Long)

    /** Get completion stats for a month: total tasks and completed count. */
    @Query("""
        SELECT COUNT(*) FROM daily_tasks WHERE monthPlanId = :monthPlanId
    """)
    suspend fun getTotalTaskCount(monthPlanId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM daily_tasks 
        WHERE monthPlanId = :monthPlanId AND isCompleted = 1
    """)
    suspend fun getCompletedTaskCount(monthPlanId: Long): Int

    /** Count incomplete tasks for a specific date (used by notification worker). */
    @Query("""
        SELECT COUNT(*) FROM daily_tasks 
        WHERE scheduledDate = :date AND isCompleted = 0
    """)
    suspend fun getIncompleteCountForDate(date: String): Int

    /** Delete all generated tasks for a month plan (for schedule regeneration). */
    @Query("DELETE FROM daily_tasks WHERE monthPlanId = :monthPlanId")
    suspend fun deleteTasksForMonth(monthPlanId: Long)

    /** Get distinct scheduled dates for a month plan (for calendar view). */
    @Query("""
        SELECT DISTINCT scheduledDate FROM daily_tasks 
        WHERE monthPlanId = :monthPlanId 
        ORDER BY scheduledDate ASC
    """)
    suspend fun getScheduledDatesForMonth(monthPlanId: Long): List<String>

    /** Get tasks with topic and module details for a specific date (dashboard). */
    @Query("""
        SELECT dt.id AS taskId, dt.scheduledDate, dt.isCompleted, dt.completedAt,
               t.title AS topicTitle, m.name AS moduleName, m.orderIndex AS moduleOrderIndex
        FROM daily_tasks dt
        INNER JOIN topics t ON dt.topicId = t.id
        INNER JOIN modules m ON t.moduleId = m.id
        WHERE dt.scheduledDate = :date
        ORDER BY m.orderIndex ASC, t.orderIndex ASC
    """)
    fun observeTasksWithDetailsForDate(date: String): Flow<List<DailyTaskWithDetails>>

    /** Get all tasks with details for a month plan (calendar & progress). */
    @Query("""
        SELECT dt.id AS taskId, dt.scheduledDate, dt.isCompleted, dt.completedAt,
               t.title AS topicTitle, m.name AS moduleName, m.orderIndex AS moduleOrderIndex
        FROM daily_tasks dt
        INNER JOIN topics t ON dt.topicId = t.id
        INNER JOIN modules m ON t.moduleId = m.id
        WHERE dt.monthPlanId = :monthPlanId
        ORDER BY dt.scheduledDate ASC, m.orderIndex ASC, t.orderIndex ASC
    """)
    fun observeAllTasksWithDetailsForMonth(monthPlanId: Long): Flow<List<DailyTaskWithDetails>>
}
