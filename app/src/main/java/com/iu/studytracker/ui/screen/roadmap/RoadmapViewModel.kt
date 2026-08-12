package com.iu.studytracker.ui.screen.roadmap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.database.entity.DegreePlan
import com.iu.studytracker.data.database.entity.MonthPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoadmapUiState(
    val isLoading: Boolean = true,
    val degreePlan: DegreePlan? = null,
    val monthPlans: List<MonthPlan> = emptyList()
)

class RoadmapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(RoadmapUiState())
    val uiState: StateFlow<RoadmapUiState> = _uiState.asStateFlow()

    init {
        loadRoadmap()
    }

    private fun loadRoadmap() {
        viewModelScope.launch(Dispatchers.IO) {
            var plan = repository.getCurrentDegreePlan()
            if (plan == null) {
                plan = DegreePlan()
                repository.insertDegreePlan(plan)
            } else if (plan.totalCreditsRequired == 1801) {
                // Self-healing: fix the "1801" issue caused by citation regex bug
                plan = plan.copy(totalCreditsRequired = 180)
                repository.insertDegreePlan(plan)
            }
            
            launch {
                kotlinx.coroutines.flow.combine(
                    repository.observeCurrentDegreePlan(),
                    repository.observeAllCurriculumModules()
                ) { currentPlan, curriculumModules ->
                    val completed = curriculumModules.count { it.isCompleted }
                    val newCompletedCredits = completed * 5
                    
                    val basePlan = currentPlan ?: plan
                    basePlan.copy(completedCredits = newCompletedCredits)
                }.collect { updatedPlan ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            degreePlan = updatedPlan
                        )
                    }
                }
            }

            launch {
                repository.observeAllMonthPlans().collect { months ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            monthPlans = months.sortedWith(compareBy({ it.year }, { it.month }))
                        ) 
                    }
                }
            }
        }
    }
}
