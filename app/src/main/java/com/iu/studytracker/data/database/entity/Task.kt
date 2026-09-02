package com.iu.studytracker.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A generalized Task entity that replaces the old DailyTask paradigm.
 * Supports standalone tasks, nested sub-tasks, markdown descriptions,
 * string UUIDs for cloud sync, priorities, and complex scheduling rules.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = MonthPlan::class,
            parentColumns = ["id"],
            childColumns = ["monthPlanId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("monthPlanId"),
        Index("topicId"),
        Index("parentTaskId"),
        Index("status"),
        Index("scheduledDate") // Legacy support index
    ]
)
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /** Self-referential FK for nested sub-tasks */
    val parentTaskId: String? = null,

    /** FK -> month_plans.id (nullable if task is not part of a curriculum month plan) */
    val monthPlanId: String? = null,

    /** FK -> topics.id (nullable if standalone task) */
    val topicId: String? = null,
    
    /** FK -> modules.id (nullable if standalone task or legacy topic-based) */
    val moduleId: String? = null,

    /** NEW unit-based scheduling property */
    val unitNumber: Int? = null,

    /** Task title */
    val title: String = "",

    /** Markdown content */
    val description: String = "",

    /** Task Status (TODO, IN_PROGRESS, DONE) */
    val status: TaskStatus = TaskStatus.TODO,

    /** Task Priority (NONE, LOW, MEDIUM, HIGH) */
    val priority: TaskPriority = TaskPriority.NONE,

    /** Calendar date in "yyyy-MM-dd" format for legacy queries */
    val scheduledDate: String? = null,

    /** Epoch millis when the task starts */
    val startDate: Long? = null,

    /** Epoch millis when the task ends/is due */
    val endDate: Long? = null,

    /** Epoch millis when completed, null if not yet done */
    val completedAt: Long? = null,

    /** RFC 5545 RRULE format for recurring tasks */
    val recurrenceRule: String? = null,

    /** Expected time to complete in minutes (default 25 for one Pomodoro) */
    val estimatedMinutes: Int = 25,

    /** Actual time spent focusing on this task */
    val actualMinutesSpent: Int = 0,

    /** Sync metadata: Last updated timestamp */
    val updatedAt: Long = System.currentTimeMillis(),

    /** Sync metadata: Soft deletion flag for Firestore sync */
    val isDeleted: Boolean = false
) {
    val isCompleted: Boolean
        get() = status == TaskStatus.DONE
}
