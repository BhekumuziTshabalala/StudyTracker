package com.iu.studytracker.ui.screen.studynow

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
                    title = { Text("Study Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
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
                .padding(24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
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
                        onStyleSelected = { viewModel.setStyle(it) }
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
                                        if (mins > 0) viewModel.setCustomSettings(mins, uiState.customBreakMinutes)
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
                                        if (mins > 0) viewModel.setCustomSettings(uiState.customFocusMinutes, mins)
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

            val selectedTheme by com.iu.studytracker.service.TimerState.selectedTheme.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current

            // Timer Display
            TimerDisplay(
                uiState = uiState,
                selectedTheme = selectedTheme,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Controls
            TimerControls(
                timerState = uiState.timerState,
                onToggle = { viewModel.toggleTimer() },
                onStop = { viewModel.stopTimer() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Post-Focus Session Flow
            AnimatedVisibility(
                visible = uiState.showPostSessionDialog,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Did you complete this unit?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.markTopicDone() }) {
                            Text("Mark Done")
                        }
                        Button(onClick = { viewModel.takeBreak() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                            Text("Take Break")
                        }
                        OutlinedButton(onClick = { viewModel.scheduleForLater() }) {
                            Text("Schedule for Later")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.dismissPostSession() }) {
                        Text("Dismiss")
                    }
                }
            }
            
            // Paused Session Flow
            AnimatedVisibility(
                visible = uiState.timerState == TimerState.PAUSED,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Session Paused",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { viewModel.toggleTimer() }) {
                            Text("Resume")
                        }
                        OutlinedButton(onClick = { viewModel.endPausedSession() }) {
                            Text("End Session")
                        }
                    }
                }
            }

            // Theme Selector
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    com.iu.studytracker.service.TimerTheme.entries.forEach { theme ->
                        val isSelected = theme == selectedTheme
                        Surface(
                            onClick = { com.iu.studytracker.service.TimerState.setTheme(context, theme) },
                            shape = RoundedCornerShape(50.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        ) {
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
        
        if (uiState.showTopicSelectionDialog) {
            TopicSelectionDialog(
                modules = uiState.modules,
                topics = uiState.topics,
                onDismiss = { viewModel.onDismissTopicSelection() },
                onTopicSelected = { moduleId, topicId -> viewModel.onTopicSelected(moduleId, topicId) }
            )
        }
        
        if (uiState.showRescheduleDialog) {
            RescheduleDialog(
                onDismiss = { viewModel.onDismissReschedule() },
                onSave = { day, time, category -> viewModel.onReschedule(day, time, category) }
            )
        }
    }
}

@Composable
fun TopicSelectionDialog(
    modules: List<com.iu.studytracker.data.database.entity.CurriculumModule>,
    topics: List<com.iu.studytracker.data.database.entity.CurriculumTopic>,
    onDismiss: () -> Unit,
    onTopicSelected: (String, String) -> Unit
) {
    var selectedModuleId by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth().heightIn(max = 400.dp)
            ) {
                Text(
                    text = if (selectedModuleId == null) "Select Module" else "Select Unit",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (selectedModuleId == null) {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(modules.size) { index ->
                            val module = modules[index]
                            Text(
                                text = module.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedModuleId = module.id }
                                    .padding(vertical = 12.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }
                } else {
                    val moduleTopics = topics.filter { it.curriculumModuleId == selectedModuleId && !it.isCompleted }
                    if (moduleTopics.isEmpty()) {
                        Text("No pending units for this module.")
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(moduleTopics.size) { index ->
                                val topic = moduleTopics[index]
                                Text(
                                    text = topic.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTopicSelected(selectedModuleId!!, topic.id) }
                                        .padding(vertical = 12.dp)
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { selectedModuleId = null }) {
                        Text("Back to Modules")
                    }
                }
            }
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
                style = MaterialTheme.typography.titleMedium,
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
                            style = MaterialTheme.typography.labelMedium,
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
    selectedTheme: com.iu.studytracker.service.TimerTheme,
    modifier: Modifier = Modifier
) {
    val minutes = uiState.timeRemainingSeconds / 60
    val seconds = uiState.timeRemainingSeconds % 60
    val timeString = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

    val progress = when (uiState.timerState) {
        TimerState.FOCUSING, TimerState.PAUSED, TimerState.IDLE -> {
            1f - (uiState.timeRemainingSeconds.toFloat() / (uiState.currentFocusMinutes * 60f))
        }
        TimerState.BREAK -> {
            1f - (uiState.timeRemainingSeconds.toFloat() / (uiState.currentBreakMinutes * 60f))
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
        if (selectedTheme == com.iu.studytracker.service.TimerTheme.FLIP_CLOCK) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stateText,
                    color = ringColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                com.iu.studytracker.ui.screen.focus.FlipClockTimer(timeRemainingSeconds = uiState.timeRemainingSeconds)
            }
        } else {
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = timeString,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
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

@Composable
fun RescheduleDialog(
    onDismiss: () -> Unit,
    onSave: (Int, String, String) -> Unit
) {
    var selectedDay by remember { mutableStateOf(1) } // Monday
    var selectedCategory by remember { mutableStateOf("MORNING") }
    
    val timeSlots = listOf(
        Triple("MORNING", "Morning", "08:00 AM"),
        Triple("NOON", "Noon", "12:00 PM"),
        Triple("NIGHT", "Night", "06:00 PM")
    )
    val weekdays = listOf(
        1 to "Monday", 2 to "Tuesday", 3 to "Wednesday", 4 to "Thursday",
        5 to "Friday", 6 to "Saturday", 7 to "Sunday"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth()
            ) {
                Text(
                    text = "Reschedule Unit",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Select Day", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weekdays.forEach { (index, name) ->
                        val isSelected = selectedDay == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedDay = index }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(name.take(3), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Select Time Slot", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeSlots.forEach { (cat, label, time) ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { 
                        val time = timeSlots.find { it.first == selectedCategory }?.third ?: "08:00 AM"
                        onSave(selectedDay, time, selectedCategory)
                    }) { 
                        Text("Save") 
                    }
                }
            }
        }
    }
}
