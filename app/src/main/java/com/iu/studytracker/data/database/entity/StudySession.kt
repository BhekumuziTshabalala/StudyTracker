package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "study_sessions",
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
data class StudySession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val curriculumModuleId: String = "",
    val unitNumber: Int = 0,
    val scheduledDay: Int? = null,
    val scheduledTime: String? = null,
    val timeSlotCategory: String? = null,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
