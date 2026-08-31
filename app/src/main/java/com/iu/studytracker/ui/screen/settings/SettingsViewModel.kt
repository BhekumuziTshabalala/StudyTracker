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
import com.google.firebase.auth.FirebaseAuth

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = (application as StudyTrackerApp).userPreferences
    private val repository = (application as StudyTrackerApp).repository

    fun signOutAndClearData(onComplete: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            FirebaseAuth.getInstance().signOut()
            repository.clearAllData()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete()
            }
        }
    }

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

    fun refreshDevices() {
        viewModelScope.launch {
            syncManager.startSync()
        }
    }

    fun setFirebaseSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setFirebaseSyncEnabled(enabled)
            if (enabled) {
                userPreferences.getOrCreateDeviceId()
                syncManager.initialize() // re-initialize if enabled
            }
        }
    }
    val reminderEnabled = userPreferences.reminderEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )
    val reminderHour = userPreferences.reminderHour.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 8
    )
    val reminderMinute = userPreferences.reminderMinute.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )
    val gradingSystem = userPreferences.gradingSystem.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "GERMAN"
    )

    fun setReminderTime(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferences.setReminderTime(enabled, hour, minute)
            com.iu.studytracker.worker.ReminderScheduler.schedule(getApplication(), hour, minute, enabled)
        }
    }

    fun setGradingSystem(system: String) {
        viewModelScope.launch {
            userPreferences.setGradingSystem(system)
        }
    }
}
