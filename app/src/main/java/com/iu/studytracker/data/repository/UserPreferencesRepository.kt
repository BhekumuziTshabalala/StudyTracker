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
}
