package com.iu.studytracker.ui.screen.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.database.entity.Task
import com.iu.studytracker.data.database.entity.TaskPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val focusTimePerDay: List<Pair<String, Int>> = emptyList(),
    val totalFocusTimeThisWeek: Int = 0,
    val tasksCompletedThisWeek: Int = 0,
    val tasksScheduledThisWeek: Int = 0,
    val completionRate: Float = 0f,
    val eisenhowerDistribution: Map<TaskPriority, Int> = emptyMap()
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val weekAgo = today.minusDays(6)
            
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val startDateStr = weekAgo.format(dateFormatter)
            val endDateStr = today.format(dateFormatter)
            
            val startTimestamp = weekAgo.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endTimestamp = today.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

            kotlinx.coroutines.flow.combine(
                repository.observeTasksScheduledBetween(startDateStr, endDateStr),
                repository.observeTasksCompletedBetween(startTimestamp, endTimestamp)
            ) { scheduled, completed ->
                val scheduledCount = scheduled.size
                val completedCount = completed.size
                val rate = if (scheduledCount > 0) completedCount.toFloat() / scheduledCount else 0f
                
                val focusTimeByDay = (0..6).map { i ->
                    val date = weekAgo.plusDays(i.toLong())
                    val dateStr = date.format(dateFormatter)
                    val shortDateStr = date.format(DateTimeFormatter.ofPattern("MMM d"))
                    val minutes = scheduled.filter { it.scheduledDate == dateStr }.sumOf { it.actualMinutesSpent }
                    shortDateStr to minutes
                }
                
                val totalFocusTime = scheduled.sumOf { it.actualMinutesSpent }
                val distribution = completed.groupBy { it.priority }.mapValues { it.value.size }
                
                AnalyticsUiState(
                    isLoading = false,
                    focusTimePerDay = focusTimeByDay,
                    totalFocusTimeThisWeek = totalFocusTime,
                    tasksScheduledThisWeek = scheduledCount,
                    tasksCompletedThisWeek = completedCount,
                    completionRate = rate,
                    eisenhowerDistribution = distribution
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
