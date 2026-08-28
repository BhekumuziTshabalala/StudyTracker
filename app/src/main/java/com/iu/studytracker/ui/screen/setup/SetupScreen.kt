package com.iu.studytracker.ui.screen.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.data.database.entity.CurriculumModule
import com.iu.studytracker.ui.theme.*

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isTablet = maxWidth >= 720.dp

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

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Monthly Setup",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Select 1 to 3 modules for this month (Recommended: 2)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Error Message ───────────────────────────────────
                state.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusRed.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = StatusRed,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::dismissError) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = StatusRed)
                            }
                        }
                    }
                }

                // ── Curriculum Module Selection ───────────────────────
                if (state.curriculumModules.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No curriculum found. Please import your curriculum in the Curriculum tab first.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val grouped = state.curriculumModules.groupBy { it.semester }
                        grouped.forEach { (semester, modules) ->
                            item {
                                Text(
                                    text = "Semester $semester",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = OceanBlueLight,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(modules) { module ->
                                val isSelected = state.selectedModuleIds.contains(module.id)
                                ModuleSelectionItem(
                                    module = module,
                                    isSelected = isSelected,
                                    onClick = { viewModel.toggleModuleSelection(module.id) }
                                )
                            }
                        }
                    }
                }

                // ── Bottom Action Area ──────────────────────────────
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        if (!state.isComplete) {
                            Button(
                                onClick = viewModel::generateSchedule,
                                enabled = viewModel.canGenerate(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OceanBlue,
                                    disabledContainerColor = OceanBlue.copy(alpha = 0.3f)
                                )
                            ) {
                                if (state.isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Generating...")
                                } else {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Generate Schedule (${state.selectedModuleIds.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // ── Schedule Summary (animated) ─────────────────────
                        AnimatedVisibility(
                            visible = state.isComplete && state.scheduleSummary != null,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            state.scheduleSummary?.let { summary ->
                                Column {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = StatusGreen.copy(alpha = 0.1f)
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            width = 1.dp,
                                            brush = Brush.linearGradient(listOf(StatusGreen.copy(alpha = 0.3f), StatusGreen.copy(alpha = 0.1f)))
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = StatusGreen,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    "Schedule Generated!",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = StatusGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Stats grid
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                StatItem("Topics", "${summary.totalTopics}", OceanBlueLight)
                                                StatItem("Study Days", "${summary.totalDays - summary.restDays}", Cyan80)
                                                StatItem("Rest Days", "${summary.restDays}", StatusGreen)
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Avg ${String.format(java.util.Locale.US, "%.1f", summary.avgTopicsPerStudyDay)} topics per study day",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = onSetupComplete,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StatusGreen
                                        )
                                    ) {
                                        Text(
                                            "Go to Dashboard",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModuleSelectionItem(
    module: CurriculumModule,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Purple40.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isSelected) OceanBlue else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 2.dp, brush = Brush.linearGradient(listOf(borderColor, borderColor)))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = module.code,
                    style = MaterialTheme.typography.labelMedium,
                    color = OceanBlueLight
                )
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = OceanBlue)
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
