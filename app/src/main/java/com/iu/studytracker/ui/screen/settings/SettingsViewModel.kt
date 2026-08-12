package com.iu.studytracker.ui.screen.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.repository.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = (application as StudyTrackerApp).userPreferences
    private val repository = (application as StudyTrackerApp).repository

    val themeMode: StateFlow<ThemeMode> = userPreferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    val degreePlan = repository.observeCurrentDegreePlan().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun updateGraduationDate(date: String) {
        viewModelScope.launch {
            val current = repository.getCurrentDegreePlan() ?: com.iu.studytracker.data.database.entity.DegreePlan()
            repository.insertDegreePlan(current.copy(targetGraduation = date))
        }
    }

    fun updateTotalEcts(ects: Int) {
        viewModelScope.launch {
            val current = repository.getCurrentDegreePlan() ?: com.iu.studytracker.data.database.entity.DegreePlan()
            repository.insertDegreePlan(current.copy(totalCreditsRequired = ects))
        }
    }
}
