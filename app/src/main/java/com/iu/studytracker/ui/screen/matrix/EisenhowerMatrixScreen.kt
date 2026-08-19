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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        factory = EisenhowerViewModelFactory(androidx.compose.ui.platform.LocalContext.current.applicationContext as Application)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eisenhower Matrix") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Quadrant(
                        title = "DO",
                        subtitle = "Urgent & Important",
                        color = StatusRed,
                        tasks = state.doTasks,
                        modifier = Modifier.weight(1f).padding(end = 8.dp, bottom = 8.dp),
                        onPriorityChange = { taskId, priority -> viewModel.updateTaskPriority(taskId, priority) }
                    )
                    Quadrant(
                        title = "SCHEDULE",
                        subtitle = "Not Urgent & Important",
                        color = StatusOrange,
                        tasks = state.scheduleTasks,
                        modifier = Modifier.weight(1f).padding(start = 8.dp, bottom = 8.dp),
                        onPriorityChange = { taskId, priority -> viewModel.updateTaskPriority(taskId, priority) }
                    )
                }
                Row(modifier = Modifier.weight(1f)) {
                    Quadrant(
                        title = "DELEGATE",
                        subtitle = "Urgent & Not Important",
                        color = StatusOrange,
                        tasks = state.delegateTasks,
                        modifier = Modifier.weight(1f).padding(end = 8.dp, top = 8.dp),
                        onPriorityChange = { taskId, priority -> viewModel.updateTaskPriority(taskId, priority) }
                    )
                    Quadrant(
                        title = "ELIMINATE",
                        subtitle = "Not Urgent & Not Important",
                        color = StatusGreen,
                        tasks = state.eliminateTasks,
                        modifier = Modifier.weight(1f).padding(start = 8.dp, top = 8.dp),
                        onPriorityChange = { taskId, priority -> viewModel.updateTaskPriority(taskId, priority) }
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
    color: Color,
    tasks: List<TaskWithDetails>,
    modifier: Modifier = Modifier,
    onPriorityChange: (String, TaskPriority) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks) { task ->
                MiniTaskCard(task = task, onPriorityChange = { newPriority ->
                    onPriorityChange(task.task.id, newPriority)
                })
            }
        }
    }
}

@Composable
fun MiniTaskCard(
    task: TaskWithDetails,
    onPriorityChange: (TaskPriority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.task.title.ifBlank { task.topicTitle },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Move", modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Move to DO") },
                        onClick = { onPriorityChange(TaskPriority.HIGH); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to SCHEDULE") },
                        onClick = { onPriorityChange(TaskPriority.MEDIUM); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to DELEGATE") },
                        onClick = { onPriorityChange(TaskPriority.LOW); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to ELIMINATE") },
                        onClick = { onPriorityChange(TaskPriority.NONE); expanded = false }
                    )
                }
            }
        }
    }
}
