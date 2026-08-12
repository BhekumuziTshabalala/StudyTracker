package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the overall degree progression to provide a macro-level roadmap.
 */
@Entity(tableName = "degree_plans")
data class DegreePlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Target graduation date, e.g. "July 2027" */
    val targetGraduation: String = "July 2027",

    /** Total ECTS/credits required for graduation */
    val totalCreditsRequired: Int = 180,

    /** Current accumulated credits */
    val completedCredits: Int = 0
)
