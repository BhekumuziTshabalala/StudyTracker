package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.database.relation.ModuleWithTopics
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(module: Module)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modules: List<Module>)

    @Update
    suspend fun update(module: Module)

    /** Get the two modules belonging to a month plan. */
    @Query("SELECT * FROM modules WHERE monthPlanId = :monthPlanId ORDER BY orderIndex ASC")
    suspend fun getModulesForMonth(monthPlanId: String): List<Module>

    /** Observe the two modules belonging to a month plan reactively. */
    @Query("SELECT * FROM modules WHERE monthPlanId = :monthPlanId ORDER BY orderIndex ASC")
    fun observeModulesForMonth(monthPlanId: String): Flow<List<Module>>

    /** Get a single module by ID. */
    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getById(id: String): Module?

    /** Get a module with all its topics (relationship query). */
    @Transaction
    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getWithTopics(id: String): ModuleWithTopics?

    /** Get both modules with their topics for a month plan. */
    @Transaction
    @Query("SELECT * FROM modules WHERE monthPlanId = :monthPlanId ORDER BY orderIndex ASC")
    suspend fun getModulesWithTopicsForMonth(monthPlanId: String): List<ModuleWithTopics>
}
