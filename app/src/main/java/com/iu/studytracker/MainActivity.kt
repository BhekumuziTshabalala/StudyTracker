package com.iu.studytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.iu.studytracker.ui.navigation.StudyTrackerNavGraph
import com.iu.studytracker.ui.theme.DarkBackground
import com.iu.studytracker.ui.theme.StudyTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as StudyTrackerApp
            val themeMode by app.userPreferences.themeMode.collectAsState(initial = com.iu.studytracker.data.repository.ThemeMode.SYSTEM)
            
            val isDarkTheme = when (themeMode) {
                com.iu.studytracker.data.repository.ThemeMode.LIGHT -> false
                com.iu.studytracker.data.repository.ThemeMode.DARK -> true
                com.iu.studytracker.data.repository.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            StudyTrackerTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    StudyTrackerNavGraph()
                }
            }
        }
    }
}
