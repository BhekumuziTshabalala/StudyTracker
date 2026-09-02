package com.iu.studytracker.ui.screen.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.data.database.entity.StudySession
import com.iu.studytracker.ui.theme.GradientEnd
import com.iu.studytracker.ui.theme.GradientStart
import com.iu.studytracker.ui.theme.Purple40

val WEEKDAYS = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScheduleScreen(
    moduleIds: List<String>,
    onSetupComplete: () -> Unit,
    viewModel: ManualScheduleViewModel = viewModel()
) {
    LaunchedEffect(moduleIds) {
        viewModel.init(moduleIds)
    }

    val state by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Container with max-width for tablet ergonomics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1100.dp)
                    .weight(1f)
            ) {
                // ── Gradient Header ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Purple40.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
                ) {
                    Column {
                        // Close button row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = onSetupComplete) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "Plan My Week",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Assign topics to specific days of the week.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        WEEKDAYS.forEach { (dayIndex, dayName) ->
                            val dayTopics = state.sessionsByDay[dayIndex] ?: emptyList()
                            
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (dayTopics.isNotEmpty()) {
                                        Text("${dayTopics.size} Units", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (dayTopics.isEmpty()) {
                                item {
                                    Text("Free day", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                items(dayTopics) { topic ->
                                    val module = state.modules.find { it.id == topic.curriculumModuleId }
                                    TopicScheduleCard(
                                        session = topic,
                                        moduleName = module?.name ?: "Unknown Module",
                                        onScheduleChange = { newDay, newTime, newCategory ->
                                            viewModel.updateSessionSchedule(topic.id, newDay, newTime, newCategory)
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Unscheduled topics if any
                        val unscheduled = state.sessionsByDay[-1] ?: emptyList()
                        if (unscheduled.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Unscheduled Units",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            items(unscheduled) { topic ->
                                val module = state.modules.find { it.id == topic.curriculumModuleId }
                                TopicScheduleCard(
                                    session = topic,
                                    moduleName = module?.name ?: "Unknown Module",
                                    onScheduleChange = { newDay, newTime, newCategory ->
                                        viewModel.updateSessionSchedule(topic.id, newDay, newTime, newCategory)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { viewModel.finishSetup(onSetupComplete) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Go to Dashboard", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

val TIME_SLOTS = listOf(
    Triple("MORNING", "Morning", "08:00 AM"),
    Triple("NOON", "Noon", "12:00 PM"),
    Triple("NIGHT", "Night", "06:00 PM")
)

@Composable
fun TopicScheduleCard(
    session: StudySession,
    moduleName: String,
    onScheduleChange: (Int?, String?, String?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = moduleName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Unit ${session.unitNumber}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Select Day", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WEEKDAYS.forEach { (dayIndex, dayName) ->
                    val isSelected = session.scheduledDay == dayIndex
                    val bgColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        label = "dayBgColor"
                    )
                    val textColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        label = "dayTextColor"
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable { onScheduleChange(if (isSelected) null else dayIndex, session.scheduledTime, session.timeSlotCategory) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName.take(3),
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor
                        )
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
                TIME_SLOTS.forEach { (category, label, time) ->
                    val isSelected = session.timeSlotCategory == category
                    val bgColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                        label = "timeBgColor"
                    )
                    val textColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                        label = "timeTextColor"
                    )
                    val subTextColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        label = "timeSubTextColor"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable { 
                                onScheduleChange(session.scheduledDay, if (isSelected) null else time, if (isSelected) null else category) 
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = time,
                                style = MaterialTheme.typography.bodySmall,
                                color = subTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}
