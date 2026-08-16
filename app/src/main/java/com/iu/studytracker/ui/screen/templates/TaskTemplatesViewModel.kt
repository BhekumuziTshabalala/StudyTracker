package com.iu.studytracker.ui.screen.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.data.database.entity.TaskTemplate
import com.iu.studytracker.data.database.entity.TaskPriority
import com.iu.studytracker.data.repository.StudyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskTemplatesViewModel(
    private val repository: StudyRepository
) : ViewModel() {

    val templates: StateFlow<List<TaskTemplate>> = repository.observeAllTaskTemplates()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTemplate(title: String, priority: TaskPriority) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTaskTemplate(TaskTemplate(title = title, defaultPriority = priority))
        }
    }

    fun deleteTemplate(template: TaskTemplate) {
        viewModelScope.launch {
            repository.deleteTaskTemplate(template)
        }
    }

    fun applyTemplateToToday(template: TaskTemplate) {
        viewModelScope.launch {
            val task = com.iu.studytracker.data.database.entity.Task(
                title = template.title,
                priority = template.defaultPriority,
                scheduledDate = repository.todayString()
            )
            repository.insertTask(task)
        }
    }
}

class TaskTemplatesViewModelFactory(
    private val repository: StudyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskTemplatesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskTemplatesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
