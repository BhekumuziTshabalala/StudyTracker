package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "task_templates")
data class TaskTemplate(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val defaultPriority: TaskPriority = TaskPriority.MEDIUM,
    val defaultModuleId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
