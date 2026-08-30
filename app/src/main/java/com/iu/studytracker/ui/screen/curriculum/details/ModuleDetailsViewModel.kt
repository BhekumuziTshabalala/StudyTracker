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
    val topics: List<com.iu.studytracker.data.database.entity.CurriculumTopic> = emptyList(),
    val tasks: List<ModuleTask> = emptyList(),
    val scheduleEvents: List<ModuleScheduleEvent> = emptyList(),
    val isTaskModalOpen: Boolean = false,
    val isEventModalOpen: Boolean = false,
    val completedTaskCount: Int = 0,
    val totalTaskCount: Int = 0,
    val allTasksDone: Boolean = false,
    val showExamDialog: Boolean = false
)

class ModuleDetailsViewModel(
    application: Application,
    private val moduleId: String
) : AndroidViewModel(application) {

    private val repository: StudyRepository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(ModuleDetailsUiState())
    val uiState: StateFlow<ModuleDetailsUiState> = combine(
        _uiState,
        repository.observeModuleById(moduleId),
        repository.observeTasksForModule(moduleId),
        repository.observeScheduleEventsForModule(moduleId),
        combine(
            repository.observeTaskCountForModule(moduleId),
            repository.observeCompletedTaskCountForModule(moduleId)
        ) { total, completed -> Pair(total, completed) }
    ) { state, module, tasks, events, counts ->
        val totalCount = counts.first
        val completedCount = counts.second
        
        val allDone = totalCount > 0 && completedCount == totalCount
        // Only show dialog if it just became all done, and hasn't been asked yet
        val shouldShowDialog = allDone && module?.examPassed == null && state.showExamDialog == false

        state.copy(
            module = module,
            tasks = tasks,
            scheduleEvents = events,
            totalTaskCount = totalCount,
            completedTaskCount = completedCount,
            allTasksDone = allDone,
            showExamDialog = if (shouldShowDialog && !state.showExamDialog && !state.allTasksDone) true else state.showExamDialog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModuleDetailsUiState())

    val gradingSystem = (application as StudyTrackerApp).userPreferences.gradingSystem.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "GERMAN"
    )

    fun setTaskModalOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isTaskModalOpen = isOpen) }
    }

    fun setEventModalOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isEventModalOpen = isOpen) }
    }
    
    fun setExamDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(showExamDialog = isOpen) }
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
            com.iu.studytracker.widget.ModuleProgressWidget.updateAllWidgets(getApplication())
        }
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateModuleTaskCompletion(taskId, isCompleted)
            
            // Re-fetch counts to trigger dialog if needed (the combine block will handle this automatically
            // once the DB updates, but we want to make sure the transition triggers the dialog).
            // We set allTasksDone to false in local state if we uncheck, so it can re-trigger when checked again.
            if (!isCompleted) {
                 _uiState.update { it.copy(allTasksDone = false) }
            }
            com.iu.studytracker.widget.ModuleProgressWidget.updateAllWidgets(getApplication())
        }
    }

    fun submitExamResult(passed: Boolean, grade: String?) {
        viewModelScope.launch {
            repository.updateExamResult(moduleId, passed, grade)
            setExamDialogOpen(false)
        }
    }

    fun deleteTask(task: ModuleTask) {
        viewModelScope.launch {
            repository.deleteModuleTask(task)
        }
    }
    
    fun updateTopicTitle(topicId: String, title: String) {
        viewModelScope.launch {
            repository.updateTopicTitle(topicId, title)
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
