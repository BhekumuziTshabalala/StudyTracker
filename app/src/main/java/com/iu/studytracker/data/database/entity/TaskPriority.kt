package com.iu.studytracker.data.database.entity

enum class TaskPriority(val value: Int, val isImportant: Boolean, val isUrgent: Boolean) {
    NONE(0, false, false),   // Eliminate
    LOW(1, false, true),     // Delegate
    MEDIUM(2, true, false),  // Schedule
    HIGH(3, true, true)      // Do
}
