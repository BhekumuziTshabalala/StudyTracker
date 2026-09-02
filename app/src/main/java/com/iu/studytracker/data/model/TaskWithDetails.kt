package com.iu.studytracker.data.model

import androidx.room.ColumnInfo
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
    @ColumnInfo(name = "associatedModuleId")
    val associatedModuleId: String? = null,
    val resourceUri: String? = null
)
