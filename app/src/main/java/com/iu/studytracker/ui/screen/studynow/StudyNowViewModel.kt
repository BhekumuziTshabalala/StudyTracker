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
    val totalFocusTimeSpentSeconds: Int = 0
) {
    val currentFocusMinutes: Int
        get() = if (selectedStyle == PomodoroStyle.CUSTOM) customFocusMinutes else selectedStyle.focusMinutes
    val currentBreakMinutes: Int
        get() = if (selectedStyle == PomodoroStyle.CUSTOM) customBreakMinutes else selectedStyle.breakMinutes
}

class StudyNowViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(StudyNowUiState())
    val uiState: StateFlow<StudyNowUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    // keep track of what state it was in before pause
    private var prePauseState: TimerState = TimerState.FOCUSING

    fun selectStyle(style: PomodoroStyle) {
        if (_uiState.value.timerState != TimerState.IDLE && _uiState.value.timerState != TimerState.FINISHED) return
        _uiState.update { 
            val updatedState = it.copy(selectedStyle = style)
            updatedState.copy(
                timeRemainingSeconds = updatedState.currentFocusMinutes * 60,
                timerState = TimerState.IDLE
            ) 
        }
    }

    fun updateCustomTime(focusMins: Int, breakMins: Int) {
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
            TimerState.IDLE -> startFocusTimer()
            TimerState.FOCUSING -> pauseTimer()
            TimerState.BREAK -> pauseTimer()
            TimerState.PAUSED -> resumeTimer()
            TimerState.FINISHED -> resetTimer()
        }
    }

    private fun resumeTimer() {
        _uiState.update { it.copy(timerState = prePauseState) }
        startTimerCountdown()
    }

    private fun startFocusTimer() {
        _uiState.update { 
            it.copy(
                timerState = TimerState.FOCUSING,
                timeRemainingSeconds = it.currentFocusMinutes * 60
            ) 
        }
        startTimerCountdown()
    }
    
    private fun startBreakTimer() {
        _uiState.update { 
            it.copy(
                timerState = TimerState.BREAK,
                timeRemainingSeconds = it.currentBreakMinutes * 60
            ) 
        }
        startTimerCountdown()
    }

    private fun startTimerCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemainingSeconds > 0) {
                delay(1000L)
                _uiState.update { 
                    it.copy(
                        timeRemainingSeconds = it.timeRemainingSeconds - 1,
                        totalFocusTimeSpentSeconds = if (it.timerState == TimerState.FOCUSING) it.totalFocusTimeSpentSeconds + 1 else it.totalFocusTimeSpentSeconds
                    ) 
                }
            }
            
            // Timer finished
            val currentState = _uiState.value.timerState
            if (currentState == TimerState.FOCUSING) {
                startBreakTimer()
            } else if (currentState == TimerState.BREAK) {
                _uiState.update { it.copy(timerState = TimerState.FINISHED) }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        prePauseState = _uiState.value.timerState
        _uiState.update { it.copy(timerState = TimerState.PAUSED) }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _uiState.update { 
            it.copy(
                timerState = TimerState.IDLE,
                timeRemainingSeconds = it.currentFocusMinutes * 60
            ) 
        }
    }
    
    private fun resetTimer() {
        stopTimer()
    }
}
