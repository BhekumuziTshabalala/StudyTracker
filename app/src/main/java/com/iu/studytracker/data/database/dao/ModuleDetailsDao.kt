package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iu.studytracker.data.database.entity.ModuleScheduleEvent
import com.iu.studytracker.data.database.entity.ModuleTask
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDetailsDao {

    // --- Tasks ---

    @Query("SELECT * FROM module_tasks WHERE curriculumModuleId = :moduleId ORDER BY dueDate ASC, id ASC")
    fun getTasksForModule(moduleId: String): Flow<List<ModuleTask>>

    @Insert
    suspend fun insertTask(task: ModuleTask)

    @Update
    suspend fun updateTask(task: ModuleTask)

    @Delete
    suspend fun deleteTask(task: ModuleTask)

    @Query("UPDATE module_tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean)

    // --- Schedule Events ---

    @Query("SELECT * FROM module_schedule_events WHERE curriculumModuleId = :moduleId ORDER BY date ASC")
    fun getScheduleEventsForModule(moduleId: String): Flow<List<ModuleScheduleEvent>>

    @Insert
    suspend fun insertScheduleEvent(event: ModuleScheduleEvent)

    @Update
    suspend fun updateScheduleEvent(event: ModuleScheduleEvent)

    @Delete
    suspend fun deleteScheduleEvent(event: ModuleScheduleEvent)
}
