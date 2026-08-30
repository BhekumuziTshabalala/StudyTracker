package com.iu.studytracker.ui.screen.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.data.database.entity.CurriculumTopic
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ManualScheduleUiState(
    val modules: List<CurriculumModule> = emptyList(),
    val topics: List<CurriculumTopic> = emptyList(),
    val topicsByDay: Map<Int, List<CurriculumTopic>> = emptyMap(),
    val isLoading: Boolean = true
)

class ManualScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(ManualScheduleUiState())
    val uiState: StateFlow<ManualScheduleUiState> = _uiState.asStateFlow()

    private var selectedModuleIds: List<String> = emptyList()

    fun init(moduleIds: List<String>) {
        if (selectedModuleIds == moduleIds) return
        selectedModuleIds = moduleIds

        viewModelScope.launch {
            val allModules = repository.getAllCurriculumModulesSync()
            val filteredModules = allModules.filter { it.id in moduleIds }

            // Auto-allocate unscheduled topics
            val topicsToAllocate = repository.getTopicsForModulesSync(moduleIds)
            var currentDay = 1 // Start at Monday
            var madeChanges = false
            topicsToAllocate.forEach { topic ->
                if (topic.scheduledDay == null) {
                    repository.updateCurriculumTopicSchedule(
                        topicId = topic.id,
                        day = currentDay,
                        time = "12:00 PM", // Default Noon
                        category = "NOON"
                    )
                    madeChanges = true
                    currentDay = if (currentDay >= 7) 1 else currentDay + 1
                }
            }

            // Observe the topics for these modules
            repository.observeCurriculumTopicsForModules(moduleIds).collect { topics ->
                val grouped = topics.groupBy { it.scheduledDay ?: -1 }
                _uiState.update { 
                    it.copy(
                        modules = filteredModules,
                        topics = topics,
                        topicsByDay = grouped,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateTopicSchedule(topicId: String, dayOfWeek: Int?, time: String?, category: String?) {
        viewModelScope.launch {
            repository.updateCurriculumTopicSchedule(topicId, dayOfWeek, time, category)
        }
    }

    fun finishSetup(onComplete: () -> Unit) {
        viewModelScope.launch {
            val now = LocalDate.now()
            // We just need to mark the month as set up without actually generating dates.
            // SetupMonthWithCurriculumModules generates tasks automatically in StudyRepository.kt.
            // But if we use manual scheduling, maybe we should skip auto-generating tasks for this month?
            // Actually, we can just call it - but since we don't use TopicScheduler for daily topics, it doesn't hurt.
            // However, the Dashboard uses those generated tasks. 
            // We will modify Dashboard to show BOTH auto-generated Tasks and manually scheduled CurriculumTopics.
            repository.setupMonthWithCurriculumModules(
                year = now.year, 
                month = now.monthValue, 
                moduleIds = selectedModuleIds
            )
            onComplete()
        }
    }
}
