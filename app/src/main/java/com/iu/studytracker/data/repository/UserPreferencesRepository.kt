package com.iu.studytracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class UserPreferencesRepository(private val context: Context) {
    private val THEME_KEY = intPreferencesKey("theme_mode")
    private val PROGRAMME_NAME_KEY = stringPreferencesKey("programme_name")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[THEME_KEY]) {
            0 -> ThemeMode.SYSTEM
            1 -> ThemeMode.LIGHT
            2 -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val programmeName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PROGRAMME_NAME_KEY]
    }

    private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    private val FIREBASE_PROJECT_ID_KEY = stringPreferencesKey("firebase_project_id")
    private val FIREBASE_APP_ID_KEY = stringPreferencesKey("firebase_app_id")
    private val FIREBASE_API_KEY_KEY = stringPreferencesKey("firebase_api_key")
    private val FIREBASE_SYNC_ENABLED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("firebase_sync_enabled")

    val deviceId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_ID_KEY] ?: java.util.UUID.randomUUID().toString().also { newId ->
            // Note: Since we are in a map block, we can't write back synchronously safely here,
            // but we can generate it. We should ideally generate it in a side-effect or let the consumer save it.
            // For safety, let's just return a default and let a setup method handle it.
        }
    }
    
    suspend fun getOrCreateDeviceId(): String {
        var currentId = ""
        context.dataStore.edit { preferences ->
            currentId = preferences[DEVICE_ID_KEY] ?: ""
            if (currentId.isEmpty()) {
                currentId = java.util.UUID.randomUUID().toString()
                preferences[DEVICE_ID_KEY] = currentId
            }
        }
        return currentId
    }

    val firebaseProjectId: Flow<String?> = context.dataStore.data.map { it[FIREBASE_PROJECT_ID_KEY] }
    val firebaseAppId: Flow<String?> = context.dataStore.data.map { it[FIREBASE_APP_ID_KEY] }
    val firebaseApiKey: Flow<String?> = context.dataStore.data.map { it[FIREBASE_API_KEY_KEY] }
    val isFirebaseSyncEnabled: Flow<Boolean> = context.dataStore.data.map { it[FIREBASE_SYNC_ENABLED_KEY] ?: false }

    private val REMINDER_ENABLED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("reminder_enabled")
    private val REMINDER_HOUR_KEY = androidx.datastore.preferences.core.intPreferencesKey("reminder_hour")
    private val REMINDER_MINUTE_KEY = androidx.datastore.preferences.core.intPreferencesKey("reminder_minute")
    private val GRADING_SYSTEM_KEY = androidx.datastore.preferences.core.stringPreferencesKey("grading_system")

    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[REMINDER_ENABLED_KEY] ?: true }
    val reminderHour: Flow<Int> = context.dataStore.data.map { it[REMINDER_HOUR_KEY] ?: 8 }
    val reminderMinute: Flow<Int> = context.dataStore.data.map { it[REMINDER_MINUTE_KEY] ?: 0 }
    val gradingSystem: Flow<String> = context.dataStore.data.map { it[GRADING_SYSTEM_KEY] ?: "GERMAN" }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.ordinal
        }
    }

    suspend fun setProgrammeName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PROGRAMME_NAME_KEY] = name
        }
    }

    suspend fun setFirebaseConfig(projectId: String, appId: String, apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[FIREBASE_PROJECT_ID_KEY] = projectId
            preferences[FIREBASE_APP_ID_KEY] = appId
            preferences[FIREBASE_API_KEY_KEY] = apiKey
        }
    }

    suspend fun setFirebaseSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIREBASE_SYNC_ENABLED_KEY] = enabled
        }
    }

    suspend fun setReminderTime(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_ENABLED_KEY] = enabled
            preferences[REMINDER_HOUR_KEY] = hour
            preferences[REMINDER_MINUTE_KEY] = minute
        }
    }

    suspend fun setGradingSystem(system: String) {
        context.dataStore.edit { preferences ->
            preferences[GRADING_SYSTEM_KEY] = system
        }
    }
}
