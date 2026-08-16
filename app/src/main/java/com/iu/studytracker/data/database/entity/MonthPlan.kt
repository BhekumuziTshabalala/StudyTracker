package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import java.util.UUID

/**
 * Represents a single month's study plan.
 *
 * Each MonthPlan maps to a specific year+month combination and tracks
 * whether the user has completed the initial setup (entering two modules
 * and their topics). The unique index on (year, month) prevents duplicate
 * plans for the same calendar month.
 */
@Entity(
    tableName = "month_plans",
    indices = [Index(value = ["year", "month"], unique = true)]
)
data class MonthPlan(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /** Calendar year (e.g. 2026) */
    val year: Int,

    /** Calendar month 1–12 */
    val month: Int,

    /** True once the user has entered both modules and their topics */
    val isSetupComplete: Boolean = false,

    /** Epoch millis when this plan was first created */
    val createdAt: Long = System.currentTimeMillis(),

    /** Optional reference to the macro-level degree plan */
    val degreePlanId: String? = null,
    
    val updatedAt: Long = System.currentTimeMillis()
)
