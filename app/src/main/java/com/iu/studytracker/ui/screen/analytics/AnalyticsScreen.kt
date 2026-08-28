package com.iu.studytracker.ui.screen.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.data.database.entity.TaskPriority
import com.iu.studytracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Analytics", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Module1Color,
                    strokeWidth = 3.dp
                )
            } else {
                AnalyticsContent(uiState)
            }
        }
    }
}

@Composable
fun AnalyticsContent(uiState: AnalyticsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SummaryCard(
                title = "Focus Time",
                value = "${uiState.totalFocusTimeThisWeek}m",
                subtitle = "This week",
                icon = Icons.Default.Timer,
                accentColor = Module1Color,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Completion",
                value = "${(uiState.completionRate * 100).toInt()}%",
                subtitle = "${uiState.tasksCompletedThisWeek} / ${uiState.tasksScheduledThisWeek} tasks",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = Module2Color,
                modifier = Modifier.weight(1f)
            )
        }

        // Focus Time Bar Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Weekly Focus Time",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Minutes per day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                AnimatedBarChart(
                    data = uiState.focusTimePerDay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }

        // Eisenhower Distribution Donut
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Task Distribution",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "By Eisenhower priority",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                EisenhowerDonutChart(
                    distribution = uiState.eisenhowerDistribution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                DonutLegend(distribution = uiState.eisenhowerDistribution)
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null,
                        tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.headlineLarge,
                color = accentColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AnimatedBarChart(data: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val maxMinutes = data.maxOfOrNull { it.second }?.coerceAtLeast(30) ?: 30

    // Animate bars growing up from zero
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animationProgress.animateTo(1f, animationSpec = tween(900))
    }

    val barStartColor = Module1Color
    val barEndColor = Module2Color
    val labelColor = TextSecondary

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val totalWidth = size.width
            val totalHeight = size.height
            val barCount = data.size
            val barWidth = (totalWidth / barCount) * 0.5f
            val spacing = (totalWidth - barWidth * barCount) / (barCount + 1)

            data.forEachIndexed { index, (_, value) ->
                val targetBarH = (value.toFloat() / maxMinutes) * totalHeight * animationProgress.value
                val x = spacing + index * (barWidth + spacing)
                val y = totalHeight - targetBarH

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(barStartColor, barEndColor),
                        startY = y,
                        endY = totalHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, targetBarH.coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { (day, _) ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EisenhowerDonutChart(distribution: Map<TaskPriority, Int>, modifier: Modifier = Modifier) {
    val total = distribution.values.sum().coerceAtLeast(1)
    val priorityColors = mapOf(
        TaskPriority.HIGH   to Color(0xFFEF4444),
        TaskPriority.MEDIUM to Color(0xFFF59E0B),
        TaskPriority.LOW    to Color(0xFF3B82F6),
        TaskPriority.NONE   to Color(0xFF6B7280)
    )
    var startAngle = -90f

    Canvas(modifier = modifier) {
        val strokeWidth = 44f
        if (distribution.isEmpty()) {
            drawArc(color = Color.LightGray.copy(alpha = 0.25f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = strokeWidth))
            return@Canvas
        }
        distribution.forEach { (priority, count) ->
            val sweepAngle = (count.toFloat() / total) * 360f
            val actualSweepAngle = (sweepAngle - 2f).coerceAtLeast(0.5f)
            drawArc(
                color = priorityColors[priority] ?: Color.Gray,
                startAngle = startAngle, sweepAngle = actualSweepAngle,
                useCenter = false, style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun DonutLegend(distribution: Map<TaskPriority, Int>) {
    val labels = mapOf(
        TaskPriority.HIGH   to Pair("Do (Urgent & Important)", Color(0xFFEF4444)),
        TaskPriority.MEDIUM to Pair("Schedule (Important)", Color(0xFFF59E0B)),
        TaskPriority.LOW    to Pair("Delegate (Urgent)", Color(0xFF3B82F6)),
        TaskPriority.NONE   to Pair("Eliminate", Color(0xFF6B7280))
    )
    val total = distribution.values.sum().coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        distribution.entries
            .filter { it.value > 0 }
            .forEach { (priority, count) ->
                val (label, color) = labels[priority] ?: return@forEach
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(label, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f))
                    Text(
                        "$count (${(count * 100f / total).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
    }
}
