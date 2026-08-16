package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.iu.studytracker.data.database.entity.TaskTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskTemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TaskTemplate)

    @Update
    suspend fun update(template: TaskTemplate)

    @Delete
    suspend fun delete(template: TaskTemplate)

    @Query("SELECT * FROM task_templates ORDER BY title ASC")
    fun observeAll(): Flow<List<TaskTemplate>>
}
