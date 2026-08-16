package com.iu.studytracker.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iu.studytracker.data.database.entity.DegreePlan
import kotlinx.coroutines.flow.Flow

@Dao
interface DegreePlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(degreePlan: DegreePlan)

    @Query("SELECT * FROM degree_plans LIMIT 1")
    fun observeCurrentPlan(): Flow<DegreePlan?>

    @Query("SELECT * FROM degree_plans")
    suspend fun getAllDegreePlans(): List<DegreePlan>

    @Query("SELECT * FROM degree_plans LIMIT 1")
    suspend fun getCurrentPlan(): DegreePlan?
}
