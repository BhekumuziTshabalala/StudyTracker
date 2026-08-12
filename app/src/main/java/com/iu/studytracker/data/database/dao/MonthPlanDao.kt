package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.iu.studytracker.data.database.entity.MonthPlan
import com.iu.studytracker.data.database.relation.MonthPlanWithModules
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(monthPlan: MonthPlan): Long

    /** Get a plan for a specific calendar month. */
    @Query("SELECT * FROM month_plans WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getByYearAndMonth(year: Int, month: Int): MonthPlan?

    /** Observe a plan for a specific calendar month reactively. */
    @Query("SELECT * FROM month_plans WHERE year = :year AND month = :month LIMIT 1")
    fun observeByYearAndMonth(year: Int, month: Int): Flow<MonthPlan?>

    /** Get the plan by its primary key. */
    @Query("SELECT * FROM month_plans WHERE id = :id")
    suspend fun getById(id: Long): MonthPlan?

    /** Mark setup as complete after modules and topics have been entered. */
    @Query("UPDATE month_plans SET isSetupComplete = 1 WHERE id = :id")
    suspend fun markSetupComplete(id: Long)

    /** Get all month plans ordered by most recent first. */
    @Query("SELECT * FROM month_plans ORDER BY year DESC, month DESC")
    fun observeAll(): Flow<List<MonthPlan>>

    /** Get month plan with its modules (relationship query). */
    @Transaction
    @Query("SELECT * FROM month_plans WHERE id = :id")
    suspend fun getWithModules(id: Long): MonthPlanWithModules?

    /** Delete a specific month plan (cascades to modules, topics, tasks). */
    @Query("DELETE FROM month_plans WHERE id = :id")
    suspend fun deleteById(id: Long)
}
