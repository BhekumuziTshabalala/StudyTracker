package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A module that belongs to the overall degree curriculum.
 * This is used as a template from which the user selects modules to schedule for a specific month.
 */
@Entity(tableName = "curriculum_modules")
data class CurriculumModule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val semester: Int,
    val code: String,
    val name: String,
    val assessment: String,
    val isCompleted: Boolean = false
)
