package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class EventType {
    EXAM,
    TESTING_DAY,
    REINFORCEMENT,
    STUDY_BLOCK,
    BREAK
}

@Entity(
    tableName = "module_schedule_events",
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
data class ModuleScheduleEvent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val curriculumModuleId: String,
    val title: String,
    val eventType: EventType,
    val date: Long,
    val durationMinutes: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
