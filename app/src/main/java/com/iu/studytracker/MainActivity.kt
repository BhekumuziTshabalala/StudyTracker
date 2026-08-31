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
import com.iu.studytracker.ui.theme.DolphinPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Restore focus timer state from SharedPreferences
        com.iu.studytracker.service.TimerState.restore(this)
        if (com.iu.studytracker.service.TimerState.isRunning.value) {
            val intent = android.content.Intent(this, com.iu.studytracker.service.FocusTimerService::class.java).apply {
                action = com.iu.studytracker.service.FocusTimerService.ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
        
        enableEdgeToEdge()
        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { _ -> }
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            
            val app = application as StudyTrackerApp
            val themeMode by app.userPreferences.themeMode.collectAsState(initial = com.iu.studytracker.data.repository.ThemeMode.SYSTEM)
            
            val isDarkTheme = when (themeMode) {
                com.iu.studytracker.data.repository.ThemeMode.LIGHT -> false
                com.iu.studytracker.data.repository.ThemeMode.DARK -> true
                com.iu.studytracker.data.repository.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            DolphinPlannerTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    val startDestination = if (auth.currentUser == null) {
                        com.iu.studytracker.ui.navigation.Screen.Login.route
                    } else {
                        com.iu.studytracker.ui.navigation.Screen.Dashboard.route
                    }
                    StudyTrackerNavGraph(startDestination = startDestination)
                }
            }
        }
    }
}
