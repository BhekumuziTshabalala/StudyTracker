package com.iu.studytracker.ui.screen.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.scheduler.TopicScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class SetupUiState(
    val curriculumModules: List<CurriculumModule> = emptyList(),
    val selectedModuleIds: Set<String> = emptySet(),
    val isGenerating: Boolean = false,
    val scheduleSummary: TopicScheduler.ScheduleSummary? = null,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllCurriculumModules().collect { modules ->
                val incompleteModules = modules.filter { !it.isCompleted }
                _uiState.update { it.copy(curriculumModules = incompleteModules) }
            }
        }
    }

    fun toggleModuleSelection(moduleId: String) {
        _uiState.update { state ->
            val selected = state.selectedModuleIds.toMutableSet()
            if (selected.contains(moduleId)) {
                selected.remove(moduleId)
            } else {
                if (selected.size < 3) {
                    selected.add(moduleId)
                } else {
                    return@update state.copy(errorMessage = "You can select up to 3 modules maximum.")
                }
            }
            state.copy(selectedModuleIds = selected)
        }
    }

    fun canGenerate(): Boolean {
        val state = _uiState.value
        return state.selectedModuleIds.isNotEmpty() && !state.isGenerating
    }

    fun generateSchedule() {
        if (!canGenerate()) return

        val state = _uiState.value
        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = LocalDate.now()
                val selectedIds = state.selectedModuleIds.toList()
                val (_, result) = repository.setupMonthWithCurriculumModules(
                    year = now.year,
                    month = now.monthValue,
                    moduleIds = selectedIds,
                    startFrom = now
                )

                if (result != null && result.tasks.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            scheduleSummary = result.summary(),
                            isComplete = true
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = "Failed to generate schedule (no topics found)"
                        )
                    }
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
