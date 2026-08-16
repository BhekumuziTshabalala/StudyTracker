package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TaskType {
    ASSIGNMENT,
    PROJECT,
    GENERAL
}

@Entity(
    tableName = "module_tasks",
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
data class ModuleTask(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val curriculumModuleId: String,
    val title: String,
    val description: String,
    val type: TaskType,
    val dueDate: Long?,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
