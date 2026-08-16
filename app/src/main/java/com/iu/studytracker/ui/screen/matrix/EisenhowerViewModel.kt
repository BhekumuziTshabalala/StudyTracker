package com.iu.studytracker.ui.screen.matrix

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.data.database.entity.TaskPriority
import com.iu.studytracker.data.model.TaskWithDetails
import com.iu.studytracker.data.repository.StudyRepository
import com.iu.studytracker.StudyTrackerApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class EisenhowerState(
    val doTasks: List<TaskWithDetails> = emptyList(),
    val scheduleTasks: List<TaskWithDetails> = emptyList(),
    val delegateTasks: List<TaskWithDetails> = emptyList(),
    val eliminateTasks: List<TaskWithDetails> = emptyList(),
    val isLoading: Boolean = true
)

class EisenhowerViewModel(
    private val repository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EisenhowerState())
    val uiState: StateFlow<EisenhowerState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeIncompleteTasksWithDetails()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .collect { tasks ->
                    _uiState.value = _uiState.value.copy(
                        doTasks = tasks.filter { it.task.priority == TaskPriority.HIGH },
                        scheduleTasks = tasks.filter { it.task.priority == TaskPriority.MEDIUM },
                        delegateTasks = tasks.filter { it.task.priority == TaskPriority.LOW },
                        eliminateTasks = tasks.filter { it.task.priority == TaskPriority.NONE },
                        isLoading = false
                    )
                }
        }
    }

    fun updateTaskPriority(taskId: String, newPriority: TaskPriority) {
        viewModelScope.launch {
            repository.updateTaskPriority(taskId, newPriority)
        }
    }
}

class EisenhowerViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EisenhowerViewModel::class.java)) {
            val repository = (application as StudyTrackerApp).repository
            @Suppress("UNCHECKED_CAST")
            return EisenhowerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
