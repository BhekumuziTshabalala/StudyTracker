package com.iu.studytracker.ui.screen.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimerUiState(
    val taskId: Long? = null,
    val taskTitle: String = "",
    val timeRemainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val totalTimeSpentSeconds: Int = 0
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun setTask(taskId: Long, taskTitle: String, estimatedMinutes: Int = 25) {
        timerJob?.cancel()
        _uiState.update { 
            TimerUiState(
                taskId = taskId,
                taskTitle = taskTitle,
                timeRemainingSeconds = estimatedMinutes * 60
            ) 
        }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (_uiState.value.timeRemainingSeconds <= 0) return
        
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemainingSeconds > 0) {
                delay(1000L)
                _uiState.update { 
                    it.copy(
                        timeRemainingSeconds = it.timeRemainingSeconds - 1,
                        totalTimeSpentSeconds = it.totalTimeSpentSeconds + 1
                    ) 
                }
            }
            // Finished
            _uiState.update { it.copy(isRunning = false, isFinished = true) }
            saveTimeSpent()
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    fun finishEarly() {
        timerJob?.cancel()
        _uiState.update { it.copy(isRunning = false, isFinished = true) }
        saveTimeSpent()
    }

    private fun saveTimeSpent() {
        val taskId = _uiState.value.taskId ?: return
        val minutes = _uiState.value.totalTimeSpentSeconds / 60
        if (minutes > 0) {
            viewModelScope.launch {
                repository.incrementTimeSpent(taskId, minutes)
            }
        }
    }
}
