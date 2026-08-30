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
    suspend fun insertCurriculumModule(module: CurriculumModule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurriculumTopics(topics: List<CurriculumTopic>)

    @Query("SELECT * FROM curriculum_modules ORDER BY semester ASC, id ASC")
    fun getAllCurriculumModules(): Flow<List<CurriculumModule>>

    @Query("SELECT * FROM curriculum_modules ORDER BY semester ASC, id ASC")
    suspend fun getAllCurriculumModulesSync(): List<CurriculumModule>

    @Query("UPDATE curriculum_modules SET isCompleted = :isCompleted WHERE id = :moduleId")
    suspend fun updateModuleCompletion(moduleId: String, isCompleted: Boolean)
    
    @Query("UPDATE curriculum_topics SET isCompleted = :isCompleted WHERE id = :topicId")
    suspend fun updateTopicCompletion(topicId: String, isCompleted: Boolean)
    
    @Query("UPDATE curriculum_modules SET examPassed = :examPassed, finalGrade = :finalGrade WHERE id = :moduleId")
    suspend fun updateExamResult(moduleId: String, examPassed: Boolean?, finalGrade: String?)

    @Query("SELECT * FROM curriculum_modules WHERE id = :moduleId")
    fun observeModuleById(moduleId: String): Flow<CurriculumModule?>

    @Query("SELECT * FROM curriculum_topics WHERE curriculumModuleId = :moduleId ORDER BY id ASC")
    suspend fun getTopicsForModule(moduleId: String): List<CurriculumTopic>
    
    @Query("SELECT * FROM curriculum_topics WHERE curriculumModuleId = :moduleId ORDER BY id ASC")
    fun observeTopicsForModule(moduleId: String): Flow<List<CurriculumTopic>>
    
    @Query("SELECT * FROM curriculum_topics WHERE curriculumModuleId IN (:moduleIds) ORDER BY id ASC")
    suspend fun getTopicsForModules(moduleIds: List<String>): List<CurriculumTopic>


    @Query("SELECT * FROM curriculum_topics")
    suspend fun getAllCurriculumTopics(): List<CurriculumTopic>
    
    @Query("SELECT * FROM curriculum_topics")
    fun observeAllCurriculumTopics(): Flow<List<CurriculumTopic>>

    @Query("DELETE FROM curriculum_modules")
    suspend fun clearCurriculum()

    @Query("DELETE FROM curriculum_modules WHERE id = :moduleId")
    suspend fun deleteCurriculumModule(moduleId: String)

    @Query("""
        SELECT ct.* FROM curriculum_topics ct
        INNER JOIN curriculum_modules cm ON ct.curriculumModuleId = cm.id
        WHERE ct.curriculumModuleId IN (:moduleIds)
        ORDER BY cm.semester ASC, ct.id ASC
    """)
    fun observeTopicsForModules(moduleIds: List<String>): Flow<List<CurriculumTopic>>

    /** Update the day-of-week assignment for a single topic (1=Mon..7=Sun, null=unscheduled) */
    @Query("UPDATE curriculum_topics SET scheduledDay = :day, scheduledTime = :time, timeSlotCategory = :category WHERE id = :topicId")
    suspend fun updateTopicSchedule(topicId: String, day: Int?, time: String?, category: String?)

    @Query("UPDATE curriculum_topics SET title = :title WHERE id = :topicId")
    suspend fun updateTopicTitle(topicId: String, title: String)

    /** All topics whose scheduledDay matches the given weekday (1=Mon..7=Sun) */
    @Query("SELECT * FROM curriculum_topics WHERE scheduledDay = :dayOfWeek ORDER BY id ASC")
    suspend fun getTopicsForDay(dayOfWeek: Int): List<CurriculumTopic>

    /** Observe topics for a given weekday */
    @Query("SELECT * FROM curriculum_topics WHERE scheduledDay = :dayOfWeek ORDER BY id ASC")
    fun observeTopicsForDay(dayOfWeek: Int): Flow<List<CurriculumTopic>>

    @Transaction
    suspend fun insertModuleWithTopics(module: CurriculumModule, topics: List<CurriculumTopic>) {
        insertCurriculumModule(module)
        val topicsWithIds = topics.map { it.copy(curriculumModuleId = module.id) }
        insertCurriculumTopics(topicsWithIds)
    }
}
