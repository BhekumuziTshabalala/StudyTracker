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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
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
                title = { Text(module?.code ?: "Module Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            var showFabMenu by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showFabMenu,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(expandFrom = Alignment.Bottom),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Bottom)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
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
            
            // Progress Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Module Progress",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (module?.finalGrade != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Grade: ${module.finalGrade}", 
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val progress = if (state.totalTaskCount > 0) state.completedTaskCount.toFloat() / state.totalTaskCount else 0f
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                strokeWidth = 6.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "${(progress * 100).toInt()}% Completed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${state.completedTaskCount} of ${state.totalTaskCount} tasks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Syllabus Section
            item {
                Text(
                    text = "Syllabus (Total Units: ${module?.totalUnits ?: 0})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (module?.syllabus?.isNotBlank() == true) {
                    Text(
                        text = module.syllabus,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("No syllabus available for this module.", style = MaterialTheme.typography.bodyMedium)
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
    
    if (state.showExamDialog) {
        val gradingSystem by viewModel.gradingSystem.collectAsState()
        ExamResultDialog(
            gradingSystem = gradingSystem,
            onDismiss = { viewModel.setExamDialogOpen(false) },
            onSubmit = { passed, grade -> viewModel.submitExamResult(passed, grade) }
        )
    }
}

@Composable
fun TaskItem(task: ModuleTask, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(com.iu.studytracker.ui.theme.Module1Color)
            )
            Row(
                modifier = Modifier.padding(16.dp).weight(1f),
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
}

@Composable
fun EventItem(event: ModuleScheduleEvent, onDelete: () -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val dateStr = Instant.ofEpochMilli(event.date).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(com.iu.studytracker.ui.theme.Module2Color)
            )
            Row(
                modifier = Modifier.padding(16.dp).weight(1f),
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
                Text("Type", style = MaterialTheme.typography.labelMedium)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    TaskType.values().forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            RadioButton(
                                selected = type == t,
                                onClick = { type = t }
                            )
                            Text(
                                t.name.lowercase().replaceFirstChar { 
                                    if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() 
                                }
                            )
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
                LazyColumn(modifier = Modifier.height(150.dp)) {
                    items(EventType.values()) { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = type == t,
                                onClick = { type = t }
                            )
                                Text(
                                    t.name.lowercase().replaceFirstChar { 
                                        if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() 
                                    }
                                )
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

@Composable
fun ExamResultDialog(
    gradingSystem: String,
    onDismiss: () -> Unit,
    onSubmit: (Boolean, String?) -> Unit
) {
    var passed by remember { mutableStateOf<Boolean?>(null) }
    var grade by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    
    val validGrades = when (gradingSystem) {
        "GERMAN" -> listOf("1.0", "1.3", "1.7", "2.0", "2.3", "2.7", "3.0", "3.3", "3.7", "4.0", "5.0")
        "LETTER" -> listOf("A", "B", "C", "D", "E", "F")
        else -> emptyList() // No strict predefined list for percentages
    }

    val validateGrade = { g: String ->
        when (gradingSystem) {
            "GERMAN", "LETTER" -> validGrades.contains(g)
            "PERCENTAGE" -> g.toIntOrNull()?.let { it in 0..100 } == true
            else -> true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Module Complete! 🎉") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "You've completed all tasks for this module. Have you passed your final exam?",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(
                        selected = passed == true,
                        onClick = { passed = true },
                        label = { Text("Yes, passed") }
                    )
                    FilterChip(
                        selected = passed == false,
                        onClick = { passed = false },
                        label = { Text("No, failed") }
                    )
                }
                
                androidx.compose.animation.AnimatedVisibility(visible = passed != null) {
                    Column {
                        val labelText = when (gradingSystem) {
                            "GERMAN" -> "Final Grade (German Scale)"
                            "LETTER" -> "Final Grade (Letter)"
                            "PERCENTAGE" -> "Final Grade (Percentage %)"
                            else -> "Final Grade"
                        }
                        OutlinedTextField(
                            value = grade,
                            onValueChange = { 
                                grade = it
                                isError = it.isNotEmpty() && !validateGrade(it)
                            },
                            label = { Text(labelText) },
                            isError = isError,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                if (isError) {
                                    val errText = when (gradingSystem) {
                                        "GERMAN" -> "Invalid grade. Use 1.0, 1.3, 1.7... 4.0, or 5.0"
                                        "LETTER" -> "Invalid grade. Use A, B, C, D, E, or F"
                                        "PERCENTAGE" -> "Invalid grade. Enter a number between 0 and 100"
                                        else -> "Invalid grade"
                                    }
                                    Text(errText)
                                } else {
                                    val helperText = when (gradingSystem) {
                                        "GERMAN" -> "e.g., 1.0 (Sehr gut) to 5.0 (Nicht bestanden)"
                                        "LETTER" -> "e.g., A (Excellent) to F (Fail)"
                                        "PERCENTAGE" -> "e.g., 85"
                                        else -> "Enter grade"
                                    }
                                    Text(helperText)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(passed!!, grade.takeIf { it.isNotBlank() }) },
                enabled = passed != null && !isError
            ) { Text("Save Result") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Skip for now") }
        }
    )
}

@Composable
fun TopicEditItem(
    topic: com.iu.studytracker.data.database.entity.CurriculumTopic,
    onTitleChange: (String) -> Unit
) {
    var title by remember { mutableStateOf(topic.title) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { 
                    title = it
                    onTitleChange(it)
                },
                modifier = Modifier.weight(1f),
                label = { Text("Topic Description") },
                singleLine = true
            )
        }
    }
}
