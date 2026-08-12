package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK -> curriculum_modules.id */
    val curriculumModuleId: Long = 0,
    
    val title: String
)
