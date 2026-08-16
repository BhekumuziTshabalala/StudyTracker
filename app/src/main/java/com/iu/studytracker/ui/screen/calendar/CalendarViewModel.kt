package com.iu.studytracker.ui.screen.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.model.TaskWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class CalendarUiState(
    val isLoading: Boolean = true,
    val year: Int = 0,
    val month: Int = 0,
    val monthName: String = "",
    val daysInMonth: Int = 0,
    val firstDayOfWeek: Int = 0, // 1=Monday ... 7=Sunday
    val tasksByDate: Map<String, List<TaskWithDetails>> = emptyMap(),
    val selectedDate: String? = null,
    val selectedDateTasks: List<TaskWithDetails> = emptyList(),
    val todayString: String = "",
    val hasSetup: Boolean = false
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as StudyTrackerApp).repository
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var tasksJob: kotlinx.coroutines.Job? = null

    init {
        loadCalendar()
    }

    private fun loadCalendar() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = LocalDate.now()
            val yearMonth = YearMonth.of(now.year, now.monthValue)
            val todayStr = now.format(dateFormatter)
            val firstDay = yearMonth.atDay(1)
            val firstDayOfWeek = firstDay.dayOfWeek.value % 7

            // Ensure plan exists first
            val initialPlan = repository.getOrCreateCurrentMonthPlan()

            repository.observeMonthPlan(now.year, now.monthValue).collect { plan ->
                if (plan == null || !plan.isSetupComplete) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasSetup = false,
                            year = now.year,
                            month = now.monthValue,
                            monthName = now.month.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                            todayString = todayStr
                        )
                    }
                    tasksJob?.cancel()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasSetup = true,
                            year = now.year,
                            month = now.monthValue,
                            monthName = now.month.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                            daysInMonth = yearMonth.lengthOfMonth(),
                            firstDayOfWeek = firstDayOfWeek,
                            todayString = todayStr
                        )
                    }

                    tasksJob?.cancel()
                    tasksJob = launch {
                        repository.observeAllTasksWithDetailsForMonth(plan.id).collect { tasks ->
                            val grouped = tasks.groupBy { it.task.scheduledDate ?: "" }
                            val selected = _uiState.value.selectedDate
                            _uiState.update {
                                it.copy(
                                    tasksByDate = grouped,
                                    selectedDateTasks = if (selected != null) grouped[selected] ?: emptyList() else emptyList()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun selectDate(day: Int) {
        val dateStr = String.format("%04d-%02d-%02d", _uiState.value.year, _uiState.value.month, day)
        val tasks = _uiState.value.tasksByDate[dateStr] ?: emptyList()
        _uiState.update {
            it.copy(
                selectedDate = dateStr,
                selectedDateTasks = tasks
            )
        }
    }

    fun refresh() {
        // Since we are observing, we don't need to manually reload, but we can trigger a re-fetch if needed.
        // For now, it's a no-op as the flow handles updates.
    }
}
