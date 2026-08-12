package com.iu.studytracker.ui.screen.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.model.DailyTaskWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class ModuleProgress(
    val name: String,
    val completed: Int,
    val total: Int,
    val orderIndex: Int
)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val hasSetup: Boolean = false,
    val overallCompleted: Int = 0,
    val overallTotal: Int = 0,
    val overallPercentage: Float = 0f,
    val modulesProgress: List<ModuleProgress> = emptyList(),
    val daysRemaining: Int = 0,
    val studyStreak: Int = 0,
    val monthName: String = ""
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    private fun loadProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = LocalDate.now()
            val plan = repository.getOrCreateCurrentMonthPlan()
            val monthName = now.month.name.lowercase().replaceFirstChar { it.uppercase() }

            if (!plan.isSetupComplete) {
                _uiState.update { it.copy(isLoading = false, hasSetup = false, monthName = monthName) }
                return@launch
            }

            val modules = repository.getModulesForMonth(plan.id)
            val daysRemaining = YearMonth.of(now.year, now.monthValue).lengthOfMonth() - now.dayOfMonth

            // Observe all tasks for progress calculation
            repository.observeAllTasksWithDetailsForMonth(plan.id).collect { tasks ->
                val totalCompleted = tasks.count { it.isCompleted }
                val total = tasks.size
                
                val modulesProgress = modules.map { mod ->
                    val modTasks = tasks.filter { it.moduleOrderIndex == mod.orderIndex }
                    ModuleProgress(
                        name = mod.name,
                        completed = modTasks.count { it.isCompleted },
                        total = modTasks.size,
                        orderIndex = mod.orderIndex
                    )
                }

                // Calculate streak: consecutive days with all tasks completed (backwards from yesterday)
                val streak = calculateStreak(tasks, now)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasSetup = true,
                        overallCompleted = totalCompleted,
                        overallTotal = total,
                        overallPercentage = if (total > 0) totalCompleted.toFloat() / total else 0f,
                        modulesProgress = modulesProgress,
                        daysRemaining = daysRemaining,
                        monthName = monthName,
                        studyStreak = streak
                    )
                }
            }
        }
    }

    fun refresh() {
        loadProgress()
    }

    private fun calculateStreak(tasks: List<DailyTaskWithDetails>, now: LocalDate): Int {
        var streak = 0
        var checkDate = now.minusDays(1) // Start from yesterday
        val tasksByDate = tasks.groupBy { it.scheduledDate }

        while (true) {
            val dateStr = checkDate.toString()
            val dayTasks = tasksByDate[dateStr]

            if (dayTasks == null || dayTasks.isEmpty()) {
                // No tasks scheduled — skip this day (rest day)
                // But only look back within the current month
                if (checkDate.monthValue != now.monthValue) break
                checkDate = checkDate.minusDays(1)
                continue
            }

            if (dayTasks.all { it.isCompleted }) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }
}
