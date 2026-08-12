package com.iu.studytracker.data.model

/**
 * Joined projection of a [DailyTask] with its [Topic] title
 * and parent [Module] name. Used by the Dashboard and Calendar
 * screens to display tasks without extra lookups.
 *
 * Field names must exactly match the column aliases in the
 * corresponding DAO @Query.
 */
data class DailyTaskWithDetails(
    val taskId: Long,
    val scheduledDate: String,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val topicTitle: String,
    val moduleName: String,
    val moduleOrderIndex: Int,
    val actualMinutesSpent: Int,
    val resourceUri: String?,
    val pageRange: String?
)
