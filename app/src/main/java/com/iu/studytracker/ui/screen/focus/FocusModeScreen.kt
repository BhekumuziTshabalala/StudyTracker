package com.iu.studytracker.ui.screen.focus

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iu.studytracker.service.FocusTimerService
import com.iu.studytracker.service.TimerState
import com.iu.studytracker.ui.theme.Module1Color
import com.iu.studytracker.ui.theme.Module2Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    
    val remainingMillis by TimerState.remainingMillis.collectAsState()
    val totalMillis by TimerState.totalMillis.collectAsState()
    val isRunning by TimerState.isRunning.collectAsState()
    val currentTaskTitle by TimerState.currentTaskTitle.collectAsState()
    val currentTaskId by TimerState.currentTaskId.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTaskTitle ?: "No active task",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // Timer Circle
            val progress = if (totalMillis > 0) remainingMillis.toFloat() / totalMillis else 0f
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(1000, easing = LinearEasing),
                label = "timerProgress"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(300.dp)
            ) {
                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.size(250.dp)) {
                    drawCircle(
                        color = surfaceVariantColor,
                        style = Stroke(width = 24.dp.toPx())
                    )
                    drawArc(
                        color = Module1Color,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                val minutes = (remainingMillis / 1000) / 60
                val seconds = (remainingMillis / 1000) % 60
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRunning) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, FocusTimerService::class.java).apply {
                                action = FocusTimerService.ACTION_PAUSE
                            }
                            context.startService(intent)
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(40.dp))
                    }
                } else {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, FocusTimerService::class.java).apply {
                                action = FocusTimerService.ACTION_START
                            }
                            context.startService(intent)
                        },
                        containerColor = Module2Color,
                        contentColor = Color.White,
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                FloatingActionButton(
                    onClick = {
                        val intent = Intent(context, FocusTimerService::class.java).apply {
                            action = FocusTimerService.ACTION_STOP
                        }
                        context.startService(intent)
                        onNavigateBack()
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}
