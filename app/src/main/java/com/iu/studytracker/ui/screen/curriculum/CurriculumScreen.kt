package com.iu.studytracker.ui.screen.curriculum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToModuleDetails: (String) -> Unit,
    viewModel: CurriculumViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Curriculum Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { viewModel.setImportModalOpen(true) }) {
                        Icon(Icons.Default.IntegrationInstructions, "Import JSON")
                    }
                    IconButton(onClick = { viewModel.setManualEntryModalOpen(true) }) {
                        Icon(Icons.Default.Add, "Add Module")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.modules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No Curriculum Data",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.setImportModalOpen(true) }) {
                        Text("Import JSON")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { viewModel.setManualEntryModalOpen(true) }) {
                        Text("Add Manually")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val activeModules = state.modules.filter { !it.isCompleted }
                val completedModules = state.modules.filter { it.isCompleted }
                
                item {
                    val totalModules = state.modules.size
                    val progress = if (totalModules > 0) completedModules.size.toFloat() / totalModules else 0f
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Degree Roadmap",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = Module2Color,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${completedModules.size} Completed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$totalModules Total",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                val grouped = activeModules.groupBy { it.semester }
                grouped.forEach { (semester, modules) ->
                    item {
                        val totalCredits = modules.size * 5
                        val completedCredits = modules.count { it.isCompleted } * 5
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Semester $semester",
                                style = MaterialTheme.typography.titleMedium,
                                color = OceanBlueLight
                            )
                            Text(
                                text = "$completedCredits / $totalCredits ECTS",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (completedCredits == totalCredits && totalCredits > 0) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(modules) { module ->
                        CurriculumModuleItem(
                            module = module,
                            onClick = { onNavigateToModuleDetails(module.id) },
                            onDelete = { viewModel.deleteModule(module.id) },
                            onToggleCompletion = { isCompleted ->
                                viewModel.toggleModuleCompletion(module.id, isCompleted)
                            }
                        )
                    }
                }
                
                if (completedModules.isNotEmpty()) {
                    item {
                        Text(
                            text = "Completed Modules",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(completedModules) { module ->
                        CurriculumModuleItem(
                            module = module,
                            onClick = { onNavigateToModuleDetails(module.id) },
                            onDelete = { viewModel.deleteModule(module.id) },
                            onToggleCompletion = { isCompleted ->
                                viewModel.toggleModuleCompletion(module.id, isCompleted)
                            }
                        )
                    }
                }
            }
        }
    }

    if (state.isImportModalOpen) {
        ImportJsonModal(
            jsonText = state.importJsonText,
            onJsonChange = viewModel::updateImportJsonText,
            error = state.error,
            onDismiss = { viewModel.setImportModalOpen(false) },
            onImport = viewModel::importJson
        )
    }

    if (state.isManualEntryModalOpen) {
        ManualEntryModal(
            semester = state.manualSemester,
            code = state.manualModuleCode,
            name = state.manualModuleName,
            assessment = state.manualModuleAssessment,
            totalUnits = state.manualTotalUnits,
            onFieldsChange = viewModel::updateManualFields,
            topics = state.manualTopics,
            onTopicNameChange = viewModel::updateTopicName,
            error = state.error,
            onDismiss = { viewModel.setManualEntryModalOpen(false) },
            onSave = viewModel::saveManualModule
        )
    }
}

@Composable
fun CurriculumModuleItem(
    module: CurriculumModule,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleCompletion: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (module.isCompleted) StatusGreen else OceanBlue)
            )
            Row(
                modifier = Modifier.padding(16.dp).weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Checkbox(
                checked = module.isCompleted,
                onCheckedChange = onToggleCompletion,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.code,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (module.assessment.isNotBlank()) {
                    Text(
                        text = module.assessment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (module.finalGrade != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Grade: ${module.finalGrade}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
}

@Composable
fun ImportJsonModal(
    jsonText: String,
    onJsonChange: (String) -> Unit,
    error: String?,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Curriculum JSON") },
        text = {
            Column {
                Text(
                    text = "Expected JSON format:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "{\n  \"programme\": \"...\",\n  \"curriculum\": [\n    {\n      \"semester\": 1,\n      \"modules\": [\n        {\n          \"code\": \"...\",\n          \"name\": \"...\",\n          \"assessment\": \"...\",\n          \"core_topics\": [\"...\"]\n        }\n      ]\n    }\n  ]\n}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = onJsonChange,
                    label = { Text("Paste JSON here") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10,
                    shape = RoundedCornerShape(12.dp)
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, color = StatusRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onImport) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ManualEntryModal(
    semester: String,
    code: String,
    name: String,
    assessment: String,
    totalUnits: String,
    onFieldsChange: (String, String, String, String, String) -> Unit,
    topics: List<String>,
    onTopicNameChange: (Int, String) -> Unit,
    error: String?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Module") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = semester,
                        onValueChange = { onFieldsChange(it, code, name, assessment, totalUnits) },
                        label = { Text("Sem") },
                        modifier = Modifier.weight(0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { onFieldsChange(semester, it, name, assessment, totalUnits) },
                        label = { Text("Code") },
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { onFieldsChange(semester, code, it, assessment, totalUnits) },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = assessment,
                    onValueChange = { onFieldsChange(semester, code, name, it, totalUnits) },
                    label = { Text("Assessment") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = totalUnits,
                    onValueChange = { onFieldsChange(semester, code, name, assessment, it) },
                    label = { Text("Total Number of Units") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                
                if (topics.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    Text("Unit Topics (${topics.size})", style = MaterialTheme.typography.labelMedium)
                    
                    // Show a list of fields to let user rename the units
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(topics.size) { index ->
                                OutlinedTextField(
                                    value = topics[index],
                                    onValueChange = { onTopicNameChange(index, it) },
                                    label = { Text("Unit ${index + 1} Name") },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                if (error != null) {
                    Text(text = error, color = StatusRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
