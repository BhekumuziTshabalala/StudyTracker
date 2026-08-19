package com.iu.studytracker.ui.screen.studynow

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.ui.theme.Module1Color
import com.iu.studytracker.ui.theme.Module2Color
import com.iu.studytracker.ui.theme.OceanBlueLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyNowScreen(
    viewModel: StudyNowViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isFullscreen by remember { mutableStateOf(false) }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                StudyNowContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreen = false }
                )
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Study Now") },
                    actions = {
                        IconButton(onClick = { isFullscreen = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Full Screen")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            StudyNowContent(
                uiState = uiState,
                viewModel = viewModel,
                isFullscreen = false,
                onToggleFullscreen = { isFullscreen = true },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun StudyNowContent(
    uiState: StudyNowUiState,
    viewModel: StudyNowViewModel,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isFullscreen) Modifier.systemBarsPadding() else Modifier
            )
    ) {
        if (isFullscreen) {
            IconButton(
                onClick = onToggleFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Full Screen")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isFullscreen) {
                Spacer(modifier = Modifier.height(48.dp))
            }

            // Style Selector (Only show when IDLE)
            AnimatedVisibility(
                visible = uiState.timerState == TimerState.IDLE || uiState.timerState == TimerState.FINISHED,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StyleSelector(
                        selectedStyle = uiState.selectedStyle,
                        onStyleSelected = { viewModel.selectStyle(it) }
                    )
                    
                    if (uiState.selectedStyle == PomodoroStyle.CUSTOM) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var focusText by remember(uiState.customFocusMinutes) { mutableStateOf(uiState.customFocusMinutes.toString()) }
                            var breakText by remember(uiState.customBreakMinutes) { mutableStateOf(uiState.customBreakMinutes.toString()) }
                            
                            OutlinedTextField(
                                value = focusText,
                                onValueChange = { 
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        focusText = it
                                        val mins = it.toIntOrNull() ?: 0
                                        if (mins > 0) viewModel.updateCustomTime(mins, uiState.customBreakMinutes)
                                    }
                                },
                                label = { Text("Focus (min)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = breakText,
                                onValueChange = { 
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        breakText = it
                                        val mins = it.toIntOrNull() ?: 0
                                        if (mins > 0) viewModel.updateCustomTime(uiState.customFocusMinutes, mins)
                                    }
                                },
                                label = { Text("Break (min)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Timer Display
            TimerDisplay(
                uiState = uiState,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Controls
            TimerControls(
                timerState = uiState.timerState,
                onToggle = { viewModel.toggleTimer() },
                onStop = { viewModel.stopTimer() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StyleSelector(
    selectedStyle: PomodoroStyle,
    onStyleSelected: (PomodoroStyle) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Study Mode",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PomodoroStyle.values().forEach { style ->
                    val isSelected = style == selectedStyle
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) Module1Color.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Module1Color else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onStyleSelected(style) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.title,
                            color = if (isSelected) Module1Color else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimerDisplay(
    uiState: StudyNowUiState,
    modifier: Modifier = Modifier
) {
    val minutes = uiState.timeRemainingSeconds / 60
    val seconds = uiState.timeRemainingSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    val progress = when (uiState.timerState) {
        TimerState.FOCUSING, TimerState.PAUSED, TimerState.IDLE -> {
            1f - (uiState.timeRemainingSeconds.toFloat() / (uiState.selectedStyle.focusMinutes * 60f))
        }
        TimerState.BREAK -> {
            1f - (uiState.timeRemainingSeconds.toFloat() / (uiState.selectedStyle.breakMinutes * 60f))
        }
        TimerState.FINISHED -> 1f
    }

    val stateText = when (uiState.timerState) {
        TimerState.IDLE -> "Ready to Focus"
        TimerState.FOCUSING -> "Focus Time"
        TimerState.PAUSED -> "Paused"
        TimerState.BREAK -> "Break Time"
        TimerState.FINISHED -> "Session Complete!"
    }
    
    val ringColor = when (uiState.timerState) {
        TimerState.BREAK -> Module2Color
        else -> Module1Color
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing ring
        Box(
            modifier = Modifier
                .size(320.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Timer Circle
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(260.dp),
            color = ringColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            strokeWidth = 8.dp,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stateText,
                color = ringColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = timeString,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun TimerControls(
    timerState: TimerState,
    onToggle: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Toggle Button (Play/Pause)
        val isRunning = timerState == TimerState.FOCUSING || timerState == TimerState.BREAK
        
        FloatingActionButton(
            onClick = onToggle,
            containerColor = Module1Color,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(80.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isRunning) "Pause" else "Play",
                modifier = Modifier.size(36.dp)
            )
        }

        // Stop Button
        AnimatedVisibility(
            visible = timerState != TimerState.IDLE,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            FloatingActionButton(
                onClick = onStop,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
