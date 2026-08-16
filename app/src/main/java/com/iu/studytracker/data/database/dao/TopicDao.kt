package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iu.studytracker.data.database.entity.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(topic: Topic)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topics: List<Topic>)

    /** Get all topics for a module, ordered by their sequence. */
    @Query("SELECT * FROM topics WHERE moduleId = :moduleId ORDER BY orderIndex ASC")
    suspend fun getTopicsForModule(moduleId: String): List<Topic>

    /** Observe topics for a module reactively. */
    @Query("SELECT * FROM topics WHERE moduleId = :moduleId ORDER BY orderIndex ASC")
    fun observeTopicsForModule(moduleId: String): Flow<List<Topic>>

    /** Get a single topic by ID. */
    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getById(id: String): Topic?

    /** Count total topics across all modules in a month plan. */
    @Query("""
        SELECT COUNT(*) FROM topics t
        INNER JOIN modules m ON t.moduleId = m.id
        WHERE m.monthPlanId = :monthPlanId
    """)
    suspend fun countTopicsForMonth(monthPlanId: String): Int

    /** Get all topics for a month plan (across both modules). */
    @Query("""
        SELECT t.* FROM topics t
        INNER JOIN modules m ON t.moduleId = m.id
        WHERE m.monthPlanId = :monthPlanId
        ORDER BY m.orderIndex ASC, t.orderIndex ASC
    """)
    suspend fun getAllTopicsForMonth(monthPlanId: String): List<Topic>

    /** Delete all topics for a module (useful for re-entry). */
    @Query("DELETE FROM topics WHERE moduleId = :moduleId")
    suspend fun deleteTopicsByModule(moduleId: String)

    @Query("SELECT * FROM topics")
    suspend fun getAllTopics(): List<Topic>
}
