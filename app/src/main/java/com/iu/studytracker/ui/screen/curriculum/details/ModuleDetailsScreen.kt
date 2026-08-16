package com.iu.studytracker.ui.screen.curriculum.details

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.data.database.entity.EventType
import com.iu.studytracker.data.database.entity.ModuleScheduleEvent
import com.iu.studytracker.data.database.entity.ModuleTask
import com.iu.studytracker.data.database.entity.TaskType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailsScreen(
    moduleId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: ModuleDetailsViewModel = viewModel(
        factory = ModuleDetailsViewModelFactory(application, moduleId)
    )
    val state by viewModel.uiState.collectAsState()
    val module = state.module

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(module?.code ?: "Module Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            // A simple FAB that opens a dialog to choose what to add
            var showFabMenu by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            viewModel.setTaskModalOpen(true)
                        },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("Add Task") },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExtendedFloatingActionButton(
                        onClick = {
                            showFabMenu = false
                            viewModel.setEventModalOpen(true)
                        },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("Add Schedule Event") },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }) {
                    Icon(Icons.Default.Add, "Add Item")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = module?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (module?.assessment?.isNotBlank() == true) {
                    Text(
                        text = "Assessment: ${module.assessment}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Tasks Section
            item {
                Text(
                    text = "Tasks & Assignments",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (state.tasks.isEmpty()) {
                    Text("No tasks added yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(state.tasks) { task ->
                TaskItem(
                    task = task,
                    onToggle = { isCompleted -> viewModel.toggleTaskCompletion(task.id, isCompleted) },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }

            // Schedule Events Section
            item {
                Text(
                    text = "Schedule & Events",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (state.scheduleEvents.isEmpty()) {
                    Text("No schedule events added yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(state.scheduleEvents) { event ->
                EventItem(
                    event = event,
                    onDelete = { viewModel.deleteEvent(event) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (state.isTaskModalOpen) {
        AddTaskDialog(
            onDismiss = { viewModel.setTaskModalOpen(false) },
            onSave = { title, desc, type, dueDate ->
                viewModel.addTask(title, desc, type, dueDate)
            }
        )
    }

    if (state.isEventModalOpen) {
        AddEventDialog(
            onDismiss = { viewModel.setEventModalOpen(false) },
            onSave = { title, type, date, duration ->
                viewModel.addEvent(title, type, date, duration)
            }
        )
    }
}

@Composable
fun TaskItem(task: ModuleTask, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggle
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                if (task.description.isNotBlank()) {
                    Text(text = task.description, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = task.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}

@Composable
fun EventItem(event: ModuleScheduleEvent, onDelete: () -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val dateStr = Instant.ofEpochMilli(event.date).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$dateStr • ${event.eventType.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (event.durationMinutes != null) {
                    Text(text = "Duration: ${event.durationMinutes} min", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, TaskType, Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TaskType.ASSIGNMENT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Simplified type selection for brevity
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row {
                    TaskType.values().forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = type == t,
                                onClick = { type = t }
                            )
                            Text(t.name.lowercase().capitalize())
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, description, type, null) },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onSave: (String, EventType, Long, Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(EventType.EXAM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Schedule Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Event Type", style = MaterialTheme.typography.labelMedium)
                // Using a simple list due to lack of standard dropdown in basic M3 without more code
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(EventType.values()) { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = type == t,
                                onClick = { type = t }
                            )
                            Text(t.name.lowercase().capitalize())
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, type, System.currentTimeMillis(), null) },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
