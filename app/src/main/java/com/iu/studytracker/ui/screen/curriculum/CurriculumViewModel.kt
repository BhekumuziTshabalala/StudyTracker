package com.iu.studytracker.ui.screen.curriculum

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iu.studytracker.StudyTrackerApp
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.data.database.entity.CurriculumTopic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.iu.studytracker.data.database.entity.DegreePlan

data class CurriculumUiState(
    val modules: List<CurriculumModule> = emptyList(),
    val degreePlan: DegreePlan? = null,
    val isImportModalOpen: Boolean = false,
    val isManualEntryModalOpen: Boolean = false,
    val importJsonText: String = "",
    val manualSemester: String = "",
    val manualModuleCode: String = "",
    val manualModuleName: String = "",
    val manualModuleAssessment: String = "",
    val manualTopics: List<String> = emptyList(),
    val newTopicText: String = "",
    val error: String? = null
)

class CurriculumViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as StudyTrackerApp).repository

    private val _uiState = MutableStateFlow(CurriculumUiState())
    val uiState: StateFlow<CurriculumUiState> = _uiState.asStateFlow()

    init {
        loadModules()
    }

    private fun loadModules() {
        viewModelScope.launch {
            repository.observeAllCurriculumModules().collect { modules ->
                _uiState.update { it.copy(modules = modules) }
            }
        }
        viewModelScope.launch {
            repository.observeCurrentDegreePlan().collect { plan ->
                _uiState.update { it.copy(degreePlan = plan) }
            }
        }
    }

    fun setImportModalOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isImportModalOpen = isOpen, importJsonText = "", error = null) }
    }

    fun setManualEntryModalOpen(isOpen: Boolean) {
        _uiState.update { 
            it.copy(
                isManualEntryModalOpen = isOpen,
                manualSemester = "",
                manualModuleCode = "",
                manualModuleName = "",
                manualModuleAssessment = "",
                manualTopics = emptyList(),
                newTopicText = "",
                error = null
            ) 
        }
    }

    fun updateImportJsonText(text: String) {
        _uiState.update { it.copy(importJsonText = text) }
    }

    private val userPrefs = (application as StudyTrackerApp).userPreferences

    fun importJson() {
        val json = _uiState.value.importJsonText
        if (json.isBlank()) {
            _uiState.update { it.copy(error = "JSON cannot be empty") }
            return
        }
        viewModelScope.launch {
            val programmeName = repository.importCurriculumFromJson(json)
            if (programmeName != null) {
                userPrefs.setProgrammeName(programmeName)
                setImportModalOpen(false)
            } else {
                _uiState.update { it.copy(error = "Invalid JSON format or missing fields") }
            }
        }
    }

    fun toggleModuleCompletion(moduleId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateCurriculumModuleCompletion(moduleId, isCompleted)
        }
    }

    fun updateManualFields(semester: String, code: String, name: String, assessment: String) {
        _uiState.update { 
            it.copy(
                manualSemester = semester,
                manualModuleCode = code,
                manualModuleName = name,
                manualModuleAssessment = assessment
            )
        }
    }

    fun updateNewTopicText(text: String) {
        _uiState.update { it.copy(newTopicText = text) }
    }

    fun addManualTopic() {
        val topic = _uiState.value.newTopicText.trim()
        if (topic.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    manualTopics = it.manualTopics + topic,
                    newTopicText = ""
                )
            }
        }
    }

    fun removeManualTopic(index: Int) {
        _uiState.update {
            val updated = it.manualTopics.toMutableList().apply { removeAt(index) }
            it.copy(manualTopics = updated)
        }
    }

    fun saveManualModule() {
        val state = _uiState.value
        val semesterInt = state.manualSemester.toIntOrNull() ?: 1
        
        if (state.manualModuleName.isBlank() || state.manualModuleCode.isBlank()) {
            _uiState.update { it.copy(error = "Module Code and Name are required") }
            return
        }
        
        val module = CurriculumModule(
            semester = semesterInt,
            code = state.manualModuleCode,
            name = state.manualModuleName,
            assessment = state.manualModuleAssessment
        )
        val topics = state.manualTopics.map { CurriculumTopic(title = it) }

        viewModelScope.launch {
            repository.insertCurriculumModuleManually(module, topics)
            setManualEntryModalOpen(false)
        }
    }

    fun deleteModule(moduleId: String) {
        viewModelScope.launch {
            repository.deleteCurriculumModule(moduleId)
        }
    }
}
