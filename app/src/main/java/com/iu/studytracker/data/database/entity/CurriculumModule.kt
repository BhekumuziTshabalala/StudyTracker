package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

import java.util.UUID

/**
 * A module that belongs to the overall degree curriculum.
 * This is used as a template from which the user selects modules to schedule for a specific month.
 */
@Entity(tableName = "curriculum_modules")
data class CurriculumModule(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val semester: Int = 0,
    val code: String = "",
    val name: String = "",
    val assessment: String = "",
    val isCompleted: Boolean = false,
    val examPassed: Boolean? = null,
    val finalGrade: String? = null,
    
    val syllabus: String = "",
    val totalUnits: Int = 0,
    
    val updatedAt: Long = System.currentTimeMillis()
)
