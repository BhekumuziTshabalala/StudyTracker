package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import java.util.UUID

/**
 * A topic that belongs to a specific [CurriculumModule].
 */
@Entity(
    tableName = "curriculum_topics",
    foreignKeys = [
        ForeignKey(
            entity = CurriculumModule::class,
            parentColumns = ["id"],
            childColumns = ["curriculumModuleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["curriculumModuleId"])]
)
data class CurriculumTopic(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /** FK -> curriculum_modules.id */
    val curriculumModuleId: String = "",
    
    val title: String,
    
    /**
     * User-assigned day of week: 1=Monday, 2=Tuesday, … 7=Sunday, null=unscheduled.
     * Used by the Manual Schedule planner so the Dashboard can surface topics for today.
     */
    val scheduledDay: Int? = null,
    
    val updatedAt: Long = System.currentTimeMillis()
)
