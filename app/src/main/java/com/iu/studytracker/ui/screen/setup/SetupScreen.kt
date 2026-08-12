package com.iu.studytracker.ui.screen.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.ui.theme.*

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // If complete and user has seen summary, navigate
    // (we'll let the user tap a "Go to Dashboard" button)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val isTablet = maxWidth >= 720.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Container with max-width for tablet ergonomics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 1100.dp)
            ) {
                // ── Gradient Header ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Purple40.copy(alpha = 0.3f),
                                    DarkBackground
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
                                    tint = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Monthly Setup",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Enter your two modules and their topics for this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Module Cards Section ────────────────────────────
                if (isTablet) {
                    // Side-by-Side Dual Column on Tablet
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ModuleCard(
                                moduleNumber = 1,
                                accentColor = Module1Color,
                                moduleName = state.module1Name,
                                onModuleNameChange = viewModel::updateModule1Name,
                                topics = state.module1Topics,
                                onRemoveTopic = viewModel::removeModule1Topic,
                                newTopic = state.module1NewTopic,
                                onNewTopicChange = viewModel::updateModule1NewTopic,
                                onAddTopic = viewModel::addModule1Topic,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            ModuleCard(
                                moduleNumber = 2,
                                accentColor = Module2Color,
                                moduleName = state.module2Name,
                                onModuleNameChange = viewModel::updateModule2Name,
                                topics = state.module2Topics,
                                onRemoveTopic = viewModel::removeModule2Topic,
                                newTopic = state.module2NewTopic,
                                onNewTopicChange = viewModel::updateModule2NewTopic,
                                onAddTopic = viewModel::addModule2Topic,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Stacked Single Column on Phone
                    ModuleCard(
                        moduleNumber = 1,
                        accentColor = Module1Color,
                        moduleName = state.module1Name,
                        onModuleNameChange = viewModel::updateModule1Name,
                        topics = state.module1Topics,
                        onRemoveTopic = viewModel::removeModule1Topic,
                        newTopic = state.module1NewTopic,
                        onNewTopicChange = viewModel::updateModule1NewTopic,
                        onAddTopic = viewModel::addModule1Topic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ModuleCard(
                        moduleNumber = 2,
                        accentColor = Module2Color,
                        moduleName = state.module2Name,
                        onModuleNameChange = viewModel::updateModule2Name,
                        topics = state.module2Topics,
                        onRemoveTopic = viewModel::removeModule2Topic,
                        newTopic = state.module2NewTopic,
                        onNewTopicChange = viewModel::updateModule2NewTopic,
                        onAddTopic = viewModel::addModule2Topic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Error Message ───────────────────────────────────
                state.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Generate Button ─────────────────────────────────
                if (!state.isComplete) {
                    Button(
                        onClick = viewModel::generateSchedule,
                        enabled = viewModel.canGenerate(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple60,
                            disabledContainerColor = Purple60.copy(alpha = 0.3f)
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
                                "Generate Study Schedule",
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
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
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
                                        StatItem("Topics", "${summary.totalTopics}", Purple80)
                                        StatItem("Study Days", "${summary.totalDays - summary.restDays}", Cyan80)
                                        StatItem("Rest Days", "${summary.restDays}", StatusGreen)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Avg ${String.format("%.1f", summary.avgTopicsPerStudyDay)} topics per study day",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Go to Dashboard button
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ModuleCard(
    moduleNumber: Int,
    accentColor: Color,
    moduleName: String,
    onModuleNameChange: (String) -> Unit,
    topics: List<String>,
    onRemoveTopic: (Int) -> Unit,
    newTopic: String,
    onNewTopicChange: (String) -> Unit,
    onAddTopic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row {
            // Accent strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Module $moduleNumber",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Module name field
                OutlinedTextField(
                    value = moduleName,
                    onValueChange = onModuleNameChange,
                    label = { Text("Module name") },
                    placeholder = { Text("e.g. Data Structures & Algorithms") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = accentColor,
                        cursorColor = accentColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Topics (${topics.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Topic list
                topics.forEachIndexed { index, topic ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = topic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemoveTopic(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add topic row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTopic,
                        onValueChange = onNewTopicChange,
                        placeholder = { Text("Add a topic...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = DarkBorder,
                            cursorColor = accentColor
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = onAddTopic,
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = accentColor.copy(alpha = 0.15f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add topic",
                            tint = accentColor
                        )
                    }
                }
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
            color = TextSecondary
        )
    }
}
