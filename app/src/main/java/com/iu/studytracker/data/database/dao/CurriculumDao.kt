package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.data.database.entity.CurriculumTopic
import kotlinx.coroutines.flow.Flow

@Dao
interface CurriculumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculumModule(module: CurriculumModule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculumTopics(topics: List<CurriculumTopic>)

    @Query("SELECT * FROM curriculum_modules ORDER BY semester ASC, id ASC")
    fun getAllCurriculumModules(): Flow<List<CurriculumModule>>

    @Query("SELECT * FROM curriculum_modules ORDER BY semester ASC, id ASC")
    suspend fun getAllCurriculumModulesSync(): List<CurriculumModule>

    @Query("UPDATE curriculum_modules SET isCompleted = :isCompleted WHERE id = :moduleId")
    suspend fun updateModuleCompletion(moduleId: Long, isCompleted: Boolean)

    @Query("SELECT * FROM curriculum_topics WHERE curriculumModuleId = :moduleId ORDER BY id ASC")
    suspend fun getTopicsForModule(moduleId: Long): List<CurriculumTopic>
    
    @Query("SELECT * FROM curriculum_topics WHERE curriculumModuleId IN (:moduleIds) ORDER BY id ASC")
    suspend fun getTopicsForModules(moduleIds: List<Long>): List<CurriculumTopic>

    @Query("DELETE FROM curriculum_modules")
    suspend fun clearCurriculum()

    @Query("DELETE FROM curriculum_modules WHERE id = :moduleId")
    suspend fun deleteCurriculumModule(moduleId: Long)

    @Transaction
    suspend fun insertModuleWithTopics(module: CurriculumModule, topics: List<CurriculumTopic>) {
        val moduleId = insertCurriculumModule(module)
        val topicsWithIds = topics.map { it.copy(curriculumModuleId = moduleId) }
        insertCurriculumTopics(topicsWithIds)
    }
}
