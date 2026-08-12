package com.iu.studytracker.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.ui.theme.Module1Color

@Composable
fun FocusTimerDialog(
    taskId: Long,
    taskTitle: String,
    onDismiss: () -> Unit,
    viewModel: TimerViewModel = viewModel()
) {
    // Only set the task once when opening
    androidx.compose.runtime.LaunchedEffect(taskId) {
        viewModel.setTask(taskId, taskTitle)
    }

    val uiState by viewModel.uiState.collectAsState()

    Dialog(onDismissRequest = { 
        viewModel.finishEarly()
        onDismiss() 
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Timer",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { 
                        viewModel.finishEarly()
                        onDismiss() 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.taskTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Timer Display
                val minutes = uiState.timeRemainingSeconds / 60
                val seconds = uiState.timeRemainingSeconds % 60
                val timeString = String.format("%02d:%02d", minutes, seconds)

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(Module1Color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeString,
                        color = Module1Color,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.toggleTimer() },
                        containerColor = Module1Color,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (uiState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isRunning) "Pause" else "Play"
                        )
                    }

                    if (uiState.isRunning || uiState.timeRemainingSeconds < 25 * 60) {
                        FloatingActionButton(
                            onClick = { 
                                viewModel.finishEarly()
                                onDismiss() 
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                        }
                    }
                }
            }
        }
    }
}
