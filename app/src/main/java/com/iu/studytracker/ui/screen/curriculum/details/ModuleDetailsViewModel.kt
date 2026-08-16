package com.iu.studytracker.ui.screen.curriculum.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.data.database.entity.EventType
import com.iu.studytracker.data.database.entity.ModuleScheduleEvent
import com.iu.studytracker.data.database.entity.ModuleTask
import com.iu.studytracker.data.database.entity.TaskType
import com.iu.studytracker.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModuleDetailsUiState(
    val module: CurriculumModule? = null,
    val tasks: List<ModuleTask> = emptyList(),
    val scheduleEvents: List<ModuleScheduleEvent> = emptyList(),
    val isTaskModalOpen: Boolean = false,
    val isEventModalOpen: Boolean = false
)

class ModuleDetailsViewModel(
    application: Application,
    private val moduleId: String
) : AndroidViewModel(application) {

    private val repository: StudyRepository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(ModuleDetailsUiState())
    val uiState: StateFlow<ModuleDetailsUiState> = combine(
        _uiState,
        repository.observeAllCurriculumModules(),
        repository.observeTasksForModule(moduleId),
        repository.observeScheduleEventsForModule(moduleId)
    ) { state, modules, tasks, events ->
        state.copy(
            module = modules.find { it.id == moduleId },
            tasks = tasks,
            scheduleEvents = events
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModuleDetailsUiState())

    fun setTaskModalOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isTaskModalOpen = isOpen) }
    }

    fun setEventModalOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isEventModalOpen = isOpen) }
    }

    fun addTask(title: String, description: String, type: TaskType, dueDate: Long?) {
        viewModelScope.launch {
            repository.insertModuleTask(
                ModuleTask(
                    curriculumModuleId = moduleId,
                    title = title,
                    description = description,
                    type = type,
                    dueDate = dueDate
                )
            )
            setTaskModalOpen(false)
        }
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateModuleTaskCompletion(taskId, isCompleted)
        }
    }

    fun deleteTask(task: ModuleTask) {
        viewModelScope.launch {
            repository.deleteModuleTask(task)
        }
    }

    fun addEvent(title: String, type: EventType, date: Long, durationMinutes: Int?) {
        viewModelScope.launch {
            repository.insertScheduleEvent(
                ModuleScheduleEvent(
                    curriculumModuleId = moduleId,
                    title = title,
                    eventType = type,
                    date = date,
                    durationMinutes = durationMinutes
                )
            )
            setEventModalOpen(false)
        }
    }

    fun deleteEvent(event: ModuleScheduleEvent) {
        viewModelScope.launch {
            repository.deleteScheduleEvent(event)
        }
    }
}

class ModuleDetailsViewModelFactory(
    private val application: Application,
    private val moduleId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModuleDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ModuleDetailsViewModel(application, moduleId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
