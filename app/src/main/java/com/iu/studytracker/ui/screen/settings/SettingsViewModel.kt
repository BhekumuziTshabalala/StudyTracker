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

    val isFirebaseSyncEnabled = userPreferences.isFirebaseSyncEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    val firebaseProjectId = userPreferences.firebaseProjectId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )
    val firebaseAppId = userPreferences.firebaseAppId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )
    val firebaseApiKey = userPreferences.firebaseApiKey.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )
    val deviceId = userPreferences.deviceId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    private val syncManager = (application as StudyTrackerApp).syncManager

    val linkedDevices = syncManager.getLinkedDevices().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun removeDevice(id: String) {
        viewModelScope.launch {
            syncManager.removeDevice(id)
        }
    }

    fun setFirebaseSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setFirebaseSyncEnabled(enabled)
            // Need to initialize ID if enabling
            if (enabled) {
                userPreferences.getOrCreateDeviceId()
            }
        }
    }

    fun saveFirebaseConfig(projectId: String, appId: String, apiKey: String) {
        viewModelScope.launch {
            userPreferences.setFirebaseConfig(projectId, appId, apiKey)
        }
    }
}
