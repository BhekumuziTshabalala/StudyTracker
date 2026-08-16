package com.iu.studytracker.ui.screen.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
                title = { Text("Productivity Analytics") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Module1Color
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
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                title = "Focus Time",
                value = "${uiState.totalFocusTimeThisWeek}m",
                subtitle = "This week",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Completion Rate",
                value = "${(uiState.completionRate * 100).toInt()}%",
                subtitle = "${uiState.tasksCompletedThisWeek} / ${uiState.tasksScheduledThisWeek} tasks",
                modifier = Modifier.weight(1f)
            )
        }

        // Focus Time Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Weekly Focus Time",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                FocusTimeBarChart(
                    data = uiState.focusTimePerDay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        // Eisenhower Distribution
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Completed Task Distribution",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "By Eisenhower Priority",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                EisenhowerDonutChart(
                    distribution = uiState.eisenhowerDistribution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
fun FocusTimeBarChart(data: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    
    val maxMinutes = data.maxOfOrNull { it.second }?.coerceAtLeast(60) ?: 60
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationPlayed = true }

    val barColor = Module1Color

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val barWidth = width / (data.size * 2)
        val spacing = barWidth
        
        data.forEachIndexed { index, pair ->
            val (_, value) = pair
            val targetHeight = (value.toFloat() / maxMinutes) * height
            val animatedHeight = if (animationPlayed) targetHeight else 0f
            
            val x = (index * (barWidth + spacing)) + spacing / 2
            val y = height - animatedHeight
            
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, animatedHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}

@Composable
fun EisenhowerDonutChart(distribution: Map<TaskPriority, Int>, modifier: Modifier = Modifier) {
    val total = distribution.values.sum().coerceAtLeast(1)
    
    val colors = mapOf(
        TaskPriority.HIGH to Color(0xFFE57373),
        TaskPriority.MEDIUM to Color(0xFF81C784),
        TaskPriority.LOW to Color(0xFF64B5F6),
        TaskPriority.NONE to Color(0xFFE0E0E0)
    )

    var startAngle = -90f

    Canvas(modifier = modifier) {
        val strokeWidth = 40f
        
        if (total <= 1 && distribution.isEmpty()) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            return@Canvas
        }

        distribution.forEach { (priority, count) ->
            val sweepAngle = (count.toFloat() / total) * 360f
            val color = colors[priority] ?: Color.Gray
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}
