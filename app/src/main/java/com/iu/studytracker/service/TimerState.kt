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
}
