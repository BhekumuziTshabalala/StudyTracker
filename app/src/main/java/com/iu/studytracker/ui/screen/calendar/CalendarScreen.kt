package com.iu.studytracker.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Purple80)
        }
        return
    }

    if (!uiState.hasSetup) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please complete the setup first.", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isTablet = maxWidth >= 720.dp

            if (isTablet) {
                // ── Tablet Side-by-Side Layout ───────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Pane: Calendar Grid (55% width)
                    Card(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "${uiState.monthName} ${uiState.year}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Day of week headers
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                val days = listOf("S", "M", "T", "W", "T", "F", "S")
                                days.forEach { day ->
                                    Text(
                                        text = day,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Calendar grid
                            CalendarGridContent(
                                uiState = uiState,
                                onSelectDay = { day -> viewModel.selectDate(day) }
                            )
                        }
                    }

                    // Right Pane: Tasks for Selected Date (45% width)
                    Card(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = if (uiState.selectedDate != null) "Tasks for ${uiState.selectedDate}" else "Select a Day",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            if (uiState.selectedDateTasks.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (uiState.selectedDate != null) "No tasks scheduled for this day." else "Tap a date on the calendar to view tasks.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    uiState.selectedDateTasks.forEach { task ->
                                        CalendarTaskItem(taskWithDetails = task)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Phone Single-Column Layout ───────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "${uiState.monthName} ${uiState.year}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Day of week headers
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        val days = listOf("S", "M", "T", "W", "T", "F", "S")
                        days.forEach { day ->
                            Text(
                                text = day,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar grid
                    CalendarGridContent(
                        uiState = uiState,
                        onSelectDay = { day -> viewModel.selectDate(day) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Task Panel
                    if (uiState.selectedDate != null) {
                        Text(
                            text = "Tasks for ${uiState.selectedDate}",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (uiState.selectedDateTasks.isEmpty()) {
                            Text("No tasks scheduled for this day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            uiState.selectedDateTasks.forEach { task ->
                                CalendarTaskItem(taskWithDetails = task)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGridContent(
    uiState: CalendarUiState,
    onSelectDay: (Int) -> Unit
) {
    val daysInMonth = uiState.daysInMonth
    val firstDayOfWeek = uiState.firstDayOfWeek
    var currentDay = 1

    Column(modifier = Modifier.fillMaxWidth()) {
        for (week in 0..5) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (dayOfWeek in 0..6) {
                    if (week == 0 && dayOfWeek < firstDayOfWeek || currentDay > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val day = currentDay
                        val dateStr = String.format("%04d-%02d-%02d", uiState.year, uiState.month, day)
                        val isSelected = uiState.selectedDate == dateStr
                        val isToday = uiState.todayString == dateStr
                        val tasks = uiState.tasksByDate[dateStr] ?: emptyList()
                        val allCompleted = tasks.isNotEmpty() && tasks.all { it.task.isCompleted }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Purple60.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    width = if (isToday) 2.dp else 0.dp,
                                    color = if (isToday) Purple60 else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onSelectDay(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = day.toString(),
                                    color = if (isToday || isSelected) Purple80 else MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp
                                )
                                if (tasks.isNotEmpty()) {
                                    if (allCompleted) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = StatusGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else {
                                        Row(horizontalArrangement = Arrangement.Center) {
                                            val moduleIndices = tasks.map { it.moduleOrderIndex }.distinct().sorted()
                                            moduleIndices.forEachIndexed { idx, modIndex ->
                                                val color = when (modIndex % 3) {
                                                    0 -> Module1Color
                                                    1 -> Module2Color
                                                    else -> StatusOrange
                                                }
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                                                if (idx < moduleIndices.size - 1) {
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        currentDay++
                    }
                }
            }
            if (currentDay > daysInMonth) break
        }
    }
}

@Composable
private fun CalendarTaskItem(taskWithDetails: com.iu.studytracker.data.model.TaskWithDetails) {
    val task = taskWithDetails.task
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = "Task Status",
                tint = if (task.isCompleted) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = taskWithDetails.topicTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = taskWithDetails.moduleName,
                    color = when (taskWithDetails.moduleOrderIndex % 3) {
                        0 -> Module1Color
                        1 -> Module2Color
                        else -> StatusOrange
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
