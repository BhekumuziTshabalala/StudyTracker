package com.iu.studytracker.ui.screen.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.model.TaskWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import com.iu.studytracker.data.database.entity.MonthPlan

data class SemesterProgress(
    val semester: Int,
    val completedCredits: Int,
    val totalCredits: Int
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val hasMonthPlan: Boolean = false,
    val isSetupComplete: Boolean = false,
    val monthPlanId: String = "",
    val todayFormatted: String = "",
    val todayDateString: String = "",
    val dayOfWeek: String = "",
    val monthName: String = "",
    val tasks: List<TaskWithDetails> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val modules: List<Module> = emptyList(),
    val rankTitle: String = "Bronze",
    val xp: Int = 0,
    val programmeName: String = "",
    val curriculumModulesCompleted: Int = 0,
    val curriculumModulesTotal: Int = 0,
    val completedEcts: Int = 0,
    val totalEcts: Int = 180,
    val monthPlans: List<MonthPlan> = emptyList(),
    val targetGraduation: String = "",
    val semesterProgress: List<SemesterProgress> = emptyList(),
    val overdueTasks: List<com.iu.studytracker.data.database.entity.Task> = emptyList(),
    val isSyncing: Boolean = false,
    val manualSessions: List<com.iu.studytracker.data.database.entity.StudySession> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as StudyTrackerApp).repository
    private val syncManager = (application as StudyTrackerApp).syncManager

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        viewModelScope.launch {
            syncManager.isSyncing.collect { syncing ->
                _uiState.update { it.copy(isSyncing = syncing) }
            }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            syncManager.triggerManualSync()
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = LocalDate.now()
            val todayFormatted = now.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            val dayOfWeek = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val monthName = now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

            val todayDateString = repository.todayString()

            val plan = repository.getOrCreateCurrentMonthPlan()

            if (!plan.isSetupComplete) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasMonthPlan = true,
                        isSetupComplete = false,
                        monthPlanId = plan.id,
                        todayFormatted = todayFormatted,
                        todayDateString = todayDateString,
                        dayOfWeek = dayOfWeek,
                        monthName = monthName
                    )
                }
                return@launch
            }

            // Load modules
            val modules = repository.getModulesForMonth(plan.id)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasMonthPlan = true,
                    isSetupComplete = true,
                    monthPlanId = plan.id,
                    todayFormatted = todayFormatted,
                    todayDateString = todayDateString,
                    dayOfWeek = dayOfWeek,
                    monthName = monthName,
                    modules = modules
                )
            }

            updateRankStats(plan.id)

            val userPrefs = (getApplication<Application>() as StudyTrackerApp).userPreferences
            
            launch {
                userPrefs.programmeName.collect { name ->
                    _uiState.update { it.copy(programmeName = name ?: "") }
                }
            }

            launch {
                repository.observeCurrentDegreePlan().collect { plan ->
                    if (plan != null) {
                        _uiState.update { it.copy(totalEcts = plan.totalCreditsRequired, targetGraduation = plan.targetGraduation) }
                    }
                }
            }

            launch {
                repository.observeAllMonthPlans().collect { months ->
                    _uiState.update { 
                        it.copy(
                            monthPlans = months.sortedWith(compareBy({ it.year }, { it.month }))
                        ) 
                    }
                }
            }

            launch {
                repository.observeAllCurriculumModules().collect { currModules ->
                    val completed = currModules.count { m -> m.isCompleted }
                    
                    val semesterProgress = currModules.groupBy { it.semester }.map { (sem, mods) ->
                        SemesterProgress(
                            semester = sem,
                            completedCredits = mods.count { it.isCompleted } * 5,
                            totalCredits = mods.size * 5
                        )
                    }.sortedBy { it.semester }

                    _uiState.update {
                        it.copy(
                            curriculumModulesTotal = currModules.size,
                            curriculumModulesCompleted = completed,
                            completedEcts = completed * 5,
                            semesterProgress = semesterProgress
                        )
                    }
                }
            }

            // Observe today's tasks reactively
            repository.observeTodaysTasksWithDetails().collect { tasks ->
                _uiState.update {
                    it.copy(
                        tasks = tasks,
                        completedCount = tasks.count { t -> t.task.isCompleted },
                        totalCount = tasks.size
                    )
                }
            }
        }
            
        viewModelScope.launch(Dispatchers.IO) {
            val now = LocalDate.now()
            repository.observeStudySessionsForDay(now.dayOfWeek.value).collect { sessions ->
                _uiState.update { it.copy(manualSessions = sessions) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Observe overdue tasks
            launch {
                repository.observeOverdueTasks().collect { overdueTasks ->
                    _uiState.update {
                        it.copy(overdueTasks = overdueTasks)
                    }
                }
            }
        }
    }

    fun rescheduleOverdueTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.rescheduleOverdueTasksToToday()
        }
    }

    fun toggleTaskCompletion(taskId: String, isCurrentlyCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleTaskCompletion(taskId, isCurrentlyCompleted)
            updateRankStats(_uiState.value.monthPlanId)
        }
    }

    private fun updateRankStats(monthPlanId: String) {
        if (monthPlanId.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val (completed, _) = repository.getCompletionStats(monthPlanId)
            val xp = completed * 10
            val rankTitle = when {
                xp >= 200 -> "Gold"
                xp >= 100 -> "Silver"
                else -> "Bronze"
            }
            _uiState.update { it.copy(xp = xp, rankTitle = rankTitle) }
        }
    }

    fun rebalanceSchedule() {
        val planId = _uiState.value.monthPlanId
        if (planId.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val wasRebalanced = repository.rebalanceSchedule(planId)
            if (wasRebalanced) {
                // If it was rebalanced, we just need to ensure the observers refresh, which they do automatically.
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadDashboard()
    }

    fun observeSubTasks(parentId: String): kotlinx.coroutines.flow.Flow<List<TaskWithDetails>> {
        return repository.observeSubTasksWithDetails(parentId)
    }

    fun addSubTask(parentId: String, inputTitle: String) {
        viewModelScope.launch {
            val parentTask = repository.getTasksForDate(repository.todayString()).find { it.id == parentId }
                ?: return@launch
            
            val parsed = com.iu.studytracker.util.TaskParser.parse(inputTitle)

            val newTask = com.iu.studytracker.data.database.entity.Task(
                title = parsed.cleanTitle,
                parentTaskId = parentId,
                monthPlanId = parentTask.monthPlanId,
                topicId = parentTask.topicId,
                scheduledDate = parsed.dateString ?: parentTask.scheduledDate
            )
            repository.insertTask(newTask)
        }
    }
}
