package com.iu.studytracker.ui.screen.matrix

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.data.database.entity.TaskPriority
import com.iu.studytracker.data.model.TaskWithDetails
import com.iu.studytracker.ui.theme.StatusGreen
import com.iu.studytracker.ui.theme.StatusRed
import com.iu.studytracker.ui.theme.StatusOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EisenhowerMatrixScreen() {
    val viewModel: EisenhowerViewModel = viewModel(
        factory = EisenhowerViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
        )
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Priority Matrix", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Quadrant(
                        title = "DO",
                        subtitle = "Urgent & Important",
                        icon = Icons.Default.Bolt,
                        color = StatusRed,
                        tasks = state.doTasks,
                        modifier = Modifier.weight(1f),
                        onPriorityChange = { taskId, p -> viewModel.updateTaskPriority(taskId, p) }
                    )
                    Quadrant(
                        title = "SCHEDULE",
                        subtitle = "Not Urgent & Important",
                        icon = Icons.Default.CalendarToday,
                        color = StatusOrange,
                        tasks = state.scheduleTasks,
                        modifier = Modifier.weight(1f),
                        onPriorityChange = { taskId, p -> viewModel.updateTaskPriority(taskId, p) }
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Quadrant(
                        title = "DELEGATE",
                        subtitle = "Urgent & Not Important",
                        icon = Icons.Default.PersonAdd,
                        color = Color(0xFF3B82F6),
                        tasks = state.delegateTasks,
                        modifier = Modifier.weight(1f),
                        onPriorityChange = { taskId, p -> viewModel.updateTaskPriority(taskId, p) }
                    )
                    Quadrant(
                        title = "ELIMINATE",
                        subtitle = "Not Urgent or Important",
                        icon = Icons.Default.DeleteOutline,
                        color = Color(0xFF6B7280),
                        tasks = state.eliminateTasks,
                        modifier = Modifier.weight(1f),
                        onPriorityChange = { taskId, p -> viewModel.updateTaskPriority(taskId, p) }
                    )
                }
            }
        }
    }
}

@Composable
fun Quadrant(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    tasks: List<TaskWithDetails>,
    modifier: Modifier = Modifier,
    onPriorityChange: (String, TaskPriority) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        // Coloured header strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, color = color)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Task count badge
                if (tasks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(color.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${tasks.size}", style = MaterialTheme.typography.labelSmall,
                            color = color, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Task list or empty state
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tasks here.\nUse ··· to move tasks between quadrants.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tasks) { task ->
                    MiniTaskCard(task = task, accentColor = color, onPriorityChange = { newPriority ->
                        onPriorityChange(task.task.id, newPriority)
                    })
                }
            }
        }
    }
}

@Composable
fun MiniTaskCard(
    task: TaskWithDetails,
    accentColor: Color,
    onPriorityChange: (TaskPriority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.task.title.ifBlank { task.topicTitle },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Move",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("⚡ Move to DO") },
                        onClick = { onPriorityChange(TaskPriority.HIGH); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("📅 Move to SCHEDULE") },
                        onClick = { onPriorityChange(TaskPriority.MEDIUM); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("👤 Move to DELEGATE") },
                        onClick = { onPriorityChange(TaskPriority.LOW); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("🗑️ Move to ELIMINATE") },
                        onClick = { onPriorityChange(TaskPriority.NONE); expanded = false }
                    )
                }
            }
        }
    }
}
