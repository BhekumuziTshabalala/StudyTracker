package com.iu.studytracker.service

import kotlinx.coroutines.flow.MutableStateFlow

object TimerState {
    val remainingMillis = MutableStateFlow(0L)
    val totalMillis = MutableStateFlow(0L)
    val isRunning = MutableStateFlow(false)
    val currentTaskId = MutableStateFlow<String?>(null)
    val currentTaskTitle = MutableStateFlow<String?>(null)

    fun reset() {
        remainingMillis.value = 0L
        totalMillis.value = 0L
        isRunning.value = false
        currentTaskId.value = null
        currentTaskTitle.value = null
    }

    const val PREFS_NAME = "focus_timer_prefs"
    const val KEY_SESSION_END = "KEY_SESSION_END"
    const val KEY_TASK_ID = "KEY_TASK_ID"
    const val KEY_TASK_TITLE = "KEY_TASK_TITLE"
    const val KEY_TOTAL_MILLIS = "KEY_TOTAL_MILLIS"
    const val KEY_IS_RUNNING = "KEY_IS_RUNNING"
    const val KEY_REMAINING_MILLIS = "KEY_REMAINING_MILLIS"

    fun restore(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        
        val isTimerRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val sessionEnd = prefs.getLong(KEY_SESSION_END, 0L)
        val storedRemaining = prefs.getLong(KEY_REMAINING_MILLIS, 0L)
        
        currentTaskId.value = prefs.getString(KEY_TASK_ID, null)
        currentTaskTitle.value = prefs.getString(KEY_TASK_TITLE, null)
        totalMillis.value = prefs.getLong(KEY_TOTAL_MILLIS, 0L)
        
        if (isTimerRunning && sessionEnd > 0) {
            val now = System.currentTimeMillis()
            val rem = kotlin.math.max(0L, sessionEnd - now)
            remainingMillis.value = rem
            isRunning.value = true
        } else {
            remainingMillis.value = storedRemaining
            isRunning.value = false
        }
    }
}
