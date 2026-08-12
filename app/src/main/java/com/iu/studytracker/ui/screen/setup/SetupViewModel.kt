package com.iu.studytracker.ui.screen.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.scheduler.TopicScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SetupUiState(
    val module1Name: String = "",
    val module2Name: String = "",
    val module1Topics: List<String> = emptyList(),
    val module2Topics: List<String> = emptyList(),
    val module1NewTopic: String = "",
    val module2NewTopic: String = "",
    val isGenerating: Boolean = false,
    val scheduleSummary: TopicScheduler.ScheduleSummary? = null,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    // ── Module 1 ────────────────────────────────────────────────

    fun updateModule1Name(name: String) {
        _uiState.update { it.copy(module1Name = name) }
    }

    fun updateModule1NewTopic(topic: String) {
        _uiState.update { it.copy(module1NewTopic = topic) }
    }

    fun addModule1Topic() {
        val topic = _uiState.value.module1NewTopic.trim()
        if (topic.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    module1Topics = it.module1Topics + topic,
                    module1NewTopic = ""
                )
            }
        }
    }

    fun removeModule1Topic(index: Int) {
        _uiState.update {
            it.copy(module1Topics = it.module1Topics.toMutableList().apply { removeAt(index) })
        }
    }

    // ── Module 2 ────────────────────────────────────────────────

    fun updateModule2Name(name: String) {
        _uiState.update { it.copy(module2Name = name) }
    }

    fun updateModule2NewTopic(topic: String) {
        _uiState.update { it.copy(module2NewTopic = topic) }
    }

    fun addModule2Topic() {
        val topic = _uiState.value.module2NewTopic.trim()
        if (topic.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    module2Topics = it.module2Topics + topic,
                    module2NewTopic = ""
                )
            }
        }
    }

    fun removeModule2Topic(index: Int) {
        _uiState.update {
            it.copy(module2Topics = it.module2Topics.toMutableList().apply { removeAt(index) })
        }
    }

    // ── Generate Schedule ────────────────────────────────────────

    fun canGenerate(): Boolean {
        val state = _uiState.value
        return state.module1Name.isNotBlank() &&
                state.module2Name.isNotBlank() &&
                state.module1Topics.isNotEmpty() &&
                state.module2Topics.isNotEmpty() &&
                !state.isGenerating
    }

    fun generateSchedule() {
        if (!canGenerate()) return

        val state = _uiState.value
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = LocalDate.now()
                val (_, result) = repository.setupMonthAndGenerateSchedule(
                    year = now.year,
                    month = now.monthValue,
                    module1Name = state.module1Name.trim(),
                    module1Topics = state.module1Topics,
                    module2Name = state.module2Name.trim(),
                    module2Topics = state.module2Topics,
                    startFrom = now
                )

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        scheduleSummary = result?.summary(),
                        isComplete = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Failed to generate schedule"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
