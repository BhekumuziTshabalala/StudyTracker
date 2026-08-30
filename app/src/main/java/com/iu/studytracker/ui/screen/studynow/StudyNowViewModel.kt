package com.iu.studytracker.ui.screen.studynow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PomodoroStyle(val title: String, val focusMinutes: Int, val breakMinutes: Int) {
    CLASSIC("Classic (25/5)", 25, 5),
    EXTENDED("Extended (50/10)", 50, 10),
    DEEP_WORK("Deep Work (90/15)", 90, 15),
    CUSTOM("Custom", 25, 5)
}

enum class TimerState {
    IDLE, FOCUSING, BREAK, FINISHED, PAUSED
}

data class StudyNowUiState(
    val selectedStyle: PomodoroStyle = PomodoroStyle.CLASSIC,
    val customFocusMinutes: Int = 25,
    val customBreakMinutes: Int = 5,
    val timerState: TimerState = TimerState.IDLE,
    val timeRemainingSeconds: Int = PomodoroStyle.CLASSIC.focusMinutes * 60,
    val totalFocusTimeSpentSeconds: Int = 0,
    val modules: List<com.iu.studytracker.data.database.entity.CurriculumModule> = emptyList(),
    val topics: List<com.iu.studytracker.data.database.entity.CurriculumTopic> = emptyList(),
    val showTopicSelectionDialog: Boolean = false,
    val showRescheduleDialog: Boolean = false,
    val showPostSessionDialog: Boolean = false,
    val selectedModuleId: String? = null,
    val selectedTopicId: String? = null
) {
    val currentFocusMinutes: Int
        get() = if (selectedStyle == PomodoroStyle.CUSTOM) customFocusMinutes else selectedStyle.focusMinutes
    val currentBreakMinutes: Int
        get() = if (selectedStyle == PomodoroStyle.CUSTOM) customBreakMinutes else selectedStyle.breakMinutes
}

class StudyNowViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as com.iu.studytracker.StudyTrackerApp).repository
    
    private val _uiState = MutableStateFlow(StudyNowUiState())
    val uiState: StateFlow<StudyNowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllCurriculumModules().collect { modules ->
                _uiState.update { it.copy(modules = modules) }
            }
        }
        viewModelScope.launch {
            repository.observeAllCurriculumTopics().collect { topics ->
                _uiState.update { it.copy(topics = topics) }
            }
        }
        
        // Observe global timer state
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                com.iu.studytracker.service.TimerState.isRunning,
                com.iu.studytracker.service.TimerState.remainingMillis,
                com.iu.studytracker.service.TimerState.sessionState
            ) { isRunning, remaining, sessionState ->
                Triple(isRunning, remaining, sessionState)
            }.collect { (isRunning, remaining, sessionState) ->
                val newTimerState = when {
                    sessionState == com.iu.studytracker.service.SessionState.FINISHED -> TimerState.FINISHED
                    // Never show PAUSED with 0 remaining — it's a stale state from a dead session
                    remaining <= 0L && !isRunning -> TimerState.IDLE
                    sessionState == com.iu.studytracker.service.SessionState.FOCUSING && isRunning -> TimerState.FOCUSING
                    sessionState == com.iu.studytracker.service.SessionState.BREAK && isRunning -> TimerState.BREAK
                    (sessionState == com.iu.studytracker.service.SessionState.FOCUSING || sessionState == com.iu.studytracker.service.SessionState.BREAK) && !isRunning && remaining > 0L -> TimerState.PAUSED
                    else -> TimerState.IDLE
                }
                
                _uiState.update { 
                    it.copy(
                        timerState = newTimerState,
                        showPostSessionDialog = newTimerState == TimerState.FINISHED,
                        timeRemainingSeconds = if (newTimerState != TimerState.IDLE) (remaining / 1000).toInt() else it.currentFocusMinutes * 60
                    )
                }
            }
        }
    }

    fun setStyle(style: PomodoroStyle) {
        if (_uiState.value.timerState != TimerState.IDLE && _uiState.value.timerState != TimerState.FINISHED) return
        _uiState.update { 
            it.copy(
                selectedStyle = style,
                timeRemainingSeconds = if (style != PomodoroStyle.CUSTOM) style.focusMinutes * 60 else it.customFocusMinutes * 60
            ) 
        }
    }

    fun setCustomSettings(focusMins: Int, breakMins: Int) {
        if (_uiState.value.timerState != TimerState.IDLE && _uiState.value.timerState != TimerState.FINISHED) return
        _uiState.update { 
            val updatedState = it.copy(customFocusMinutes = focusMins, customBreakMinutes = breakMins)
            if (updatedState.selectedStyle == PomodoroStyle.CUSTOM) {
                updatedState.copy(timeRemainingSeconds = updatedState.currentFocusMinutes * 60)
            } else {
                updatedState
            }
        }
    }

    fun toggleTimer() {
        val currentState = _uiState.value.timerState
        when (currentState) {
            TimerState.IDLE -> {
                if (_uiState.value.selectedTopicId == null) {
                    _uiState.update { it.copy(showTopicSelectionDialog = true) }
                } else {
                    startFocusTimer()
                }
            }
            TimerState.FOCUSING, TimerState.BREAK -> pauseTimer()
            TimerState.PAUSED -> resumeTimer()
            TimerState.FINISHED -> resetTimer()
        }
    }
    
    fun onTopicSelected(moduleId: String, topicId: String) {
        _uiState.update { 
            it.copy(
                selectedModuleId = moduleId,
                selectedTopicId = topicId,
                showTopicSelectionDialog = false
            )
        }
        startFocusTimer()
    }
    
    fun onDismissTopicSelection() {
        _uiState.update { it.copy(showTopicSelectionDialog = false) }
    }

    private fun resumeTimer() {
        val intent = android.content.Intent(getApplication(), com.iu.studytracker.service.FocusTimerService::class.java).apply {
            action = com.iu.studytracker.service.FocusTimerService.ACTION_START
        }
        getApplication<Application>().startService(intent)
    }

    private fun startFocusTimer() {
        val selectedTopic = _uiState.value.topics.find { it.id == _uiState.value.selectedTopicId }
        val title = selectedTopic?.title ?: "Focus Time"
        val intent = android.content.Intent(getApplication(), com.iu.studytracker.service.FocusTimerService::class.java).apply {
            action = com.iu.studytracker.service.FocusTimerService.ACTION_START
            putExtra(com.iu.studytracker.service.FocusTimerService.EXTRA_TASK_ID, _uiState.value.selectedTopicId)
            putExtra(com.iu.studytracker.service.FocusTimerService.EXTRA_TASK_TITLE, title)
            putExtra(com.iu.studytracker.service.FocusTimerService.EXTRA_MINUTES, _uiState.value.currentFocusMinutes)
        }
        getApplication<Application>().startService(intent)
    }
    
    private fun startBreakTimer() {
        val intent = android.content.Intent(getApplication(), com.iu.studytracker.service.FocusTimerService::class.java).apply {
            action = com.iu.studytracker.service.FocusTimerService.ACTION_START_BREAK
            putExtra(com.iu.studytracker.service.FocusTimerService.EXTRA_MINUTES, _uiState.value.currentBreakMinutes)
        }
        getApplication<Application>().startService(intent)
    }

    private fun pauseTimer() {
        val intent = android.content.Intent(getApplication(), com.iu.studytracker.service.FocusTimerService::class.java).apply {
            action = com.iu.studytracker.service.FocusTimerService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
    }

    fun stopTimer() {
        val intent = android.content.Intent(getApplication(), com.iu.studytracker.service.FocusTimerService::class.java).apply {
            action = com.iu.studytracker.service.FocusTimerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        // Note: We intentionally do not clear selectedTopicId here, 
        // so that the post-session dialog retains its context.
    }
    
    private fun hardResetTimer() {
        val intent = android.content.Intent(getApplication(), com.iu.studytracker.service.FocusTimerService::class.java).apply {
            action = com.iu.studytracker.service.FocusTimerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        _uiState.update { 
            it.copy(
                selectedTopicId = null,
                selectedModuleId = null,
                showPostSessionDialog = false
            ) 
        }
    }
    
    private fun resetTimer() {
        hardResetTimer()
    }
    
    fun endPausedSession() {
        stopTimer()
    }
    
    fun dismissPostSession() {
        hardResetTimer()
    }
    
    fun markTopicDone() {
        val topicId = _uiState.value.selectedTopicId
        if (topicId != null) {
            viewModelScope.launch {
                repository.updateCurriculumTopicCompletion(topicId, true)
            }
        }
        hardResetTimer()
    }
    
    fun takeBreak() {
        // Taking a break closes the post-session state.
        hardResetTimer()
        startBreakTimer()
    }
    
    fun scheduleForLater() {
        _uiState.update { it.copy(showRescheduleDialog = true) }
    }
    
    fun onReschedule(dayOfWeek: Int, time: String, category: String) {
        val topicId = _uiState.value.selectedTopicId
        if (topicId != null) {
            viewModelScope.launch {
                repository.updateCurriculumTopicSchedule(topicId, dayOfWeek, time, category)
            }
        }
        _uiState.update { it.copy(showRescheduleDialog = false) }
        hardResetTimer()
    }
    
    fun onDismissReschedule() {
        _uiState.update { it.copy(showRescheduleDialog = false) }
    }
}
