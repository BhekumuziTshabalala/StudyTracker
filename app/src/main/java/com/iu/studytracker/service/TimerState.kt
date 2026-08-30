package com.iu.studytracker.service

import kotlinx.coroutines.flow.MutableStateFlow

enum class TimerTheme(val displayName: String) {
    MODERN_RING("Modern Ring"),
    FLIP_CLOCK("Flip Clock")
}

enum class SessionState {
    IDLE, FOCUSING, BREAK, FINISHED
}

object TimerState {
    val remainingMillis = MutableStateFlow(0L)
    val totalMillis = MutableStateFlow(0L)
    val isRunning = MutableStateFlow(false)
    val sessionState = MutableStateFlow(SessionState.IDLE)
    
    val currentTaskId = MutableStateFlow<String?>(null)
    val currentTaskTitle = MutableStateFlow<String?>(null)
    
    val selectedTheme = MutableStateFlow(TimerTheme.MODERN_RING)
    val soundEnabled = MutableStateFlow(true)

    fun reset() {
        remainingMillis.value = 0L
        totalMillis.value = 0L
        isRunning.value = false
        sessionState.value = SessionState.IDLE
        currentTaskId.value = null
        currentTaskTitle.value = null
    }

    fun setTheme(context: android.content.Context, theme: TimerTheme) {
        selectedTheme.value = theme
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme.name).apply()
    }

    fun setSoundEnabled(context: android.content.Context, enabled: Boolean) {
        soundEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    const val PREFS_NAME = "focus_timer_prefs"
    const val KEY_SESSION_END = "KEY_SESSION_END"
    const val KEY_TASK_ID = "KEY_TASK_ID"
    const val KEY_TASK_TITLE = "KEY_TASK_TITLE"
    const val KEY_TOTAL_MILLIS = "KEY_TOTAL_MILLIS"
    const val KEY_IS_RUNNING = "KEY_IS_RUNNING"
    const val KEY_REMAINING_MILLIS = "KEY_REMAINING_MILLIS"
    const val KEY_THEME = "KEY_THEME"
    const val KEY_SOUND = "KEY_SOUND"
    const val KEY_SESSION_STATE = "KEY_SESSION_STATE"

    fun restore(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        
        val isTimerRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val sessionEnd = prefs.getLong(KEY_SESSION_END, 0L)
        val storedRemaining = prefs.getLong(KEY_REMAINING_MILLIS, 0L)
        val stateString = prefs.getString(KEY_SESSION_STATE, SessionState.IDLE.name)
        sessionState.value = try {
            SessionState.valueOf(stateString ?: SessionState.IDLE.name)
        } catch (e: Exception) {
            SessionState.IDLE
        }
        
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
        
        val themeString = prefs.getString(KEY_THEME, TimerTheme.MODERN_RING.name)
        selectedTheme.value = try {
            TimerTheme.valueOf(themeString ?: TimerTheme.MODERN_RING.name)
        } catch (e: Exception) {
            TimerTheme.MODERN_RING
        }
        
        soundEnabled.value = prefs.getBoolean(KEY_SOUND, true)
    }
}
