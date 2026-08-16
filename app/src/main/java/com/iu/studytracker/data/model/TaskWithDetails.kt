package com.iu.studytracker.data.model

import androidx.room.Embedded
import com.iu.studytracker.data.database.entity.Task

/**
 * Joined projection of a [Task] with its overarching [Module] and Topic details.
 */
data class TaskWithDetails(
    @Embedded
    val task: Task,

    val topicTitle: String,
    val topicOrderIndex: Int,
    val moduleName: String,
    val moduleCode: String,
    val moduleOrderIndex: Int,
    val moduleId: Long? = null,
    val resourceUri: String? = null
)
