package com.iu.studytracker.ui.screen.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.database.entity.Module
import com.iu.studytracker.data.model.DailyTaskWithDetails
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

data class DashboardUiState(
    val isLoading: Boolean = true,
    val hasMonthPlan: Boolean = false,
    val isSetupComplete: Boolean = false,
    val monthPlanId: Long = 0,
    val todayFormatted: String = "",
    val dayOfWeek: String = "",
    val monthName: String = "",
    val tasks: List<DailyTaskWithDetails> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val modules: List<Module> = emptyList(),
    val rankTitle: String = "Bronze",
    val xp: Int = 0,
    val programmeName: String = "",
    val curriculumModulesCompleted: Int = 0,
    val curriculumModulesTotal: Int = 0,
    val completedEcts: Int = 0,
    val totalEcts: Int = 180
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = LocalDate.now()
            val todayFormatted = now.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            val dayOfWeek = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            val monthName = now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

            val plan = repository.getOrCreateCurrentMonthPlan()

            if (!plan.isSetupComplete) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasMonthPlan = true,
                        isSetupComplete = false,
                        monthPlanId = plan.id,
                        todayFormatted = todayFormatted,
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
                        _uiState.update { it.copy(totalEcts = plan.totalCreditsRequired) }
                    }
                }
            }

            launch {
                repository.observeAllCurriculumModules().collect { currModules ->
                    val completed = currModules.count { m -> m.isCompleted }
                    _uiState.update {
                        it.copy(
                            curriculumModulesTotal = currModules.size,
                            curriculumModulesCompleted = completed,
                            completedEcts = completed * 5
                        )
                    }
                }
            }

            // Observe today's tasks reactively
            repository.observeTodaysTasksWithDetails().collect { tasks ->
                _uiState.update {
                    it.copy(
                        tasks = tasks,
                        completedCount = tasks.count { t -> t.isCompleted },
                        totalCount = tasks.size
                    )
                }
            }
        }
    }

    fun toggleTask(taskId: Long, isCurrentlyCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleTaskCompletion(taskId, isCurrentlyCompleted)
            updateRankStats(_uiState.value.monthPlanId)
        }
    }

    private fun updateRankStats(monthPlanId: Long) {
        if (monthPlanId == 0L) return
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
        if (planId == 0L) return
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
}
