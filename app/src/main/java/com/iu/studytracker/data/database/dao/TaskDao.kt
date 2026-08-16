package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Delete
import com.iu.studytracker.data.database.entity.Task
import com.iu.studytracker.data.model.TaskWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)
    
    @Query("UPDATE tasks SET actualMinutesSpent = actualMinutesSpent + :minutes WHERE id = :id")
    suspend fun incrementTaskMinutes(id: String, minutes: Int)
    
    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM tasks WHERE recurrenceRule IS NOT NULL AND recurrenceRule != '' AND isDeleted = 0")
    suspend fun getTasksWithRecurrenceSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE scheduledDate < :today AND status != 'DONE' AND isDeleted = 0 ORDER BY scheduledDate ASC")
    fun observeOverdueTasks(today: String): kotlinx.coroutines.flow.Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE scheduledDate < :today AND status != 'DONE' AND isDeleted = 0")
    suspend fun getOverdueTasksSync(today: String): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0")
    suspend fun getTaskById(id: String): Task?

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date AND isDeleted = 0 ORDER BY topicId ASC")
    suspend fun getTasksForDate(date: String): List<Task>

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date AND isDeleted = 0 ORDER BY topicId ASC")
    fun observeTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE monthPlanId = :monthPlanId AND isDeleted = 0 ORDER BY scheduledDate ASC")
    fun observeTasksForMonth(monthPlanId: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId AND isDeleted = 0")
    fun observeSubTasks(parentId: String): Flow<List<Task>>

    @Transaction
    @Query("""
        SELECT t.*, 
               tp.title AS topicTitle, 
               tp.orderIndex AS topicOrderIndex,
               m.name AS moduleName,
               '' AS moduleCode,
               m.orderIndex AS moduleOrderIndex,
               m.id AS moduleId,
               tp.resourceUri AS resourceUri
        FROM tasks t
        LEFT JOIN topics tp ON t.topicId = tp.id
        LEFT JOIN modules m ON tp.moduleId = m.id
        WHERE t.scheduledDate = :date AND t.isDeleted = 0
        ORDER BY m.orderIndex ASC, tp.orderIndex ASC
    """)
    fun observeTasksWithDetailsForDate(date: String): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("""
        SELECT t.*, 
               tp.title AS topicTitle, 
               tp.orderIndex AS topicOrderIndex,
               m.name AS moduleName,
               '' AS moduleCode,
               m.orderIndex AS moduleOrderIndex,
               m.id AS moduleId,
               tp.resourceUri AS resourceUri
        FROM tasks t
        LEFT JOIN topics tp ON t.topicId = tp.id
        LEFT JOIN modules m ON tp.moduleId = m.id
        WHERE t.status != 'DONE' AND t.isDeleted = 0 AND t.parentTaskId IS NULL
        ORDER BY t.priority DESC, t.scheduledDate ASC
    """)
    fun observeIncompleteTasksWithDetails(): Flow<List<TaskWithDetails>>
    
    @Transaction
    @Query("""
        SELECT t.*, 
               tp.title AS topicTitle, 
               tp.orderIndex AS topicOrderIndex,
               m.name AS moduleName,
               '' AS moduleCode,
               m.orderIndex AS moduleOrderIndex,
               m.id AS moduleId,
               tp.resourceUri AS resourceUri
        FROM tasks t
        LEFT JOIN topics tp ON t.topicId = tp.id
        LEFT JOIN modules m ON tp.moduleId = m.id
        WHERE t.parentTaskId = :parentId AND t.isDeleted = 0
        ORDER BY t.status ASC, t.scheduledDate ASC
    """)
    fun observeSubTasksWithDetails(parentId: String): Flow<List<TaskWithDetails>>


    @Transaction
    @Query("""
        SELECT t.*, 
               tp.title AS topicTitle, 
               tp.orderIndex AS topicOrderIndex,
               m.name AS moduleName,
               '' AS moduleCode,
               m.orderIndex AS moduleOrderIndex,
               m.id AS moduleId,
               tp.resourceUri AS resourceUri
        FROM tasks t
        LEFT JOIN topics tp ON t.topicId = tp.id
        LEFT JOIN modules m ON tp.moduleId = m.id
        WHERE t.monthPlanId = :monthPlanId AND t.isDeleted = 0
        ORDER BY m.orderIndex ASC, tp.orderIndex ASC
    """)
    fun observeAllTasksWithDetailsForMonth(monthPlanId: String): Flow<List<TaskWithDetails>>

    @Query("""
        SELECT * FROM tasks 
        WHERE monthPlanId = :monthPlanId 
          AND status != 'DONE' 
          AND scheduledDate < :today 
          AND isDeleted = 0
        ORDER BY scheduledDate ASC
    """)
    suspend fun getIncompleteTasksBeforeDate(monthPlanId: String, today: String): List<Task>

    @Query("DELETE FROM tasks WHERE monthPlanId = :monthPlanId")
    suspend fun deleteTasksForMonth(monthPlanId: String)

    @Query("SELECT * FROM tasks WHERE completedAt >= :startTimestamp AND completedAt <= :endTimestamp AND isDeleted = 0")
    fun observeTasksCompletedBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE scheduledDate >= :startDate AND scheduledDate <= :endDate AND isDeleted = 0")
    fun observeTasksScheduledBetween(startDate: String, endDate: String): Flow<List<Task>>
}
