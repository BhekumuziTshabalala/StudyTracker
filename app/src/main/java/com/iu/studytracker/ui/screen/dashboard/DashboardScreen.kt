package com.iu.studytracker.ui.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.iu.studytracker.data.model.TaskWithDetails
import kotlinx.coroutines.flow.Flow
import com.iu.studytracker.ui.theme.*
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToFocusMode: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.programmeName.isNotBlank()) uiState.programmeName else "Dolphin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    val transition = rememberInfiniteTransition(label = "syncRotation")
                    val angle by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "syncRotation"
                    )
                    val rotation = if (uiState.isSyncing) angle else 0f
                    IconButton(onClick = { viewModel.triggerManualSync() }) {
                        Icon(
                            Icons.Default.Sync, 
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                    IconButton(onClick = onNavigateToTemplates) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Templates",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (uiState.isSetupComplete) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToSetup,
                    containerColor = Module2Color,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    text = { Text("Edit Plan", fontWeight = FontWeight.SemiBold) }
                )
            }
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
            } else if (!uiState.isSetupComplete && uiState.hasMonthPlan) {
                NoSetupState(onNavigateToSetup)
            } else {
                ActiveDashboardState(
                    uiState = uiState,
                    onToggleTask = viewModel::toggleTaskCompletion,
                    onRebalance = viewModel::rebalanceSchedule,
                    onToggleSubTask = viewModel::toggleTaskCompletion,
                    onAddSubTask = viewModel::addSubTask,
                    observeSubTasks = viewModel::observeSubTasks,
                    onNavigateToFocusMode = onNavigateToFocusMode,
                    onRescheduleOverdue = viewModel::rescheduleOverdueTasks
                )
            }
        }
    }
}

@Composable
fun NoSetupState(onNavigateToSetup: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                GradientStart.copy(alpha = 0.12f),
                                GradientEnd.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Icon with gradient background circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Module1Color.copy(alpha = 0.2f),
                                        Module2Color.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Module1Color,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Set up your month",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose your modules and topics to get your personalised study schedule.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onNavigateToSetup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Module1Color),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Monthly Setup", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveDashboardState(
    uiState: DashboardUiState,
    onToggleTask: (String, Boolean) -> Unit,
    onRebalance: () -> Unit,
    onToggleSubTask: (String, Boolean) -> Unit,
    onAddSubTask: (String, String) -> Unit,
    observeSubTasks: (String) -> Flow<List<TaskWithDetails>>,
    onNavigateToFocusMode: () -> Unit,
    onRescheduleOverdue: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isTablet = maxWidth >= 720.dp
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf("Today's Tasks", "Degree Roadmap")

        if (isTablet) {
            // ── Tablet Dual-Pane Layout ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Pane
                Card(
                    modifier = Modifier
                        .weight(0.40f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            DashboardHeader(uiState = uiState, onRebalance = onRebalance)
                            Spacer(modifier = Modifier.height(28.dp))
                            DashboardProgressSection(uiState = uiState)
                        }
                        ModuleLegend(uiState = uiState)
                    }
                }

                // Right Pane
                Column(
                    modifier = Modifier
                        .weight(0.60f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    DashboardTabRow(tabs = tabs, selectedTabIndex = selectedTabIndex, onTabSelected = { selectedTabIndex = it })
                    Spacer(modifier = Modifier.height(16.dp))
                    if (selectedTabIndex == 0) {
                        DashboardTaskList(
                            uiState = uiState,
                            onRescheduleOverdue = onRescheduleOverdue,
                            onToggleTask = onToggleTask,
                            onToggleSubTask = onToggleSubTask,
                            onAddSubTask = onAddSubTask,
                            observeSubTasks = observeSubTasks,
                            onNavigateToFocusMode = onNavigateToFocusMode
                        )
                    } else {
                        RoadmapTimeline(uiState)
                    }
                }
            }
        } else {
            // ── Phone Single-Column Layout ───────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    GradientStart.copy(alpha = 0.14f),
                                    GradientEnd.copy(alpha = 0.06f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Column {
                        DashboardHeader(uiState = uiState, onRebalance = onRebalance)
                    }
                }

                // Progress cards
                if (uiState.totalCount > 0 || uiState.totalEcts > 0) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        DashboardProgressSection(uiState = uiState)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DashboardTabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTabIndex == 0) {
                    DashboardTaskList(
                        uiState = uiState,
                        onRescheduleOverdue = onRescheduleOverdue,
                        onToggleTask = onToggleTask,
                        onToggleSubTask = onToggleSubTask,
                        onAddSubTask = onAddSubTask,
                        observeSubTasks = observeSubTasks,
                        onNavigateToFocusMode = onNavigateToFocusMode,
                        horizontalPadding = true
                    )
                } else {
                    RoadmapTimeline(uiState)
                }
            }
        }
    }
}

// ── Extracted reusable composables ──────────────────────────────────

@Composable
fun DashboardHeader(uiState: DashboardUiState, onRebalance: () -> Unit) {
    Text(
        text = uiState.dayOfWeek.uppercase(),
        color = Module2Color,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 2.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = uiState.todayFormatted,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = uiState.monthName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        }
        IconButton(onClick = onRebalance) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = "Rebalance", tint = Module1Color)
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    // Rank Badge — pill shape
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Module1Color.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Module1Color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${uiState.rankTitle}  ·  ${uiState.xp} XP",
            color = Module1Color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun DashboardProgressSection(uiState: DashboardUiState) {
    if (uiState.totalCount > 0) {
        val progressPercentage = uiState.completedCount.toFloat() / uiState.totalCount
        val animatedProgress by animateFloatAsState(
            targetValue = progressPercentage,
            animationSpec = tween(1000),
            label = "progress"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Daily Progress", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground)
            Text(
                "${uiState.completedCount} / ${uiState.totalCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        )
                    )
            )
        }
    } else if (uiState.isSetupComplete) {
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Module2Color.copy(alpha = 0.1f))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Celebration, contentDescription = null,
                tint = Module2Color, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Rest Day! 🎉", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground)
            Text("No tasks scheduled for today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (uiState.totalEcts > 0) {
        val currProgress = uiState.completedEcts.toFloat() / uiState.totalEcts
        val animatedCurrProgress by animateFloatAsState(
            targetValue = currProgress,
            animationSpec = tween(1000),
            label = "ects_progress"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Credit Progress", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground)
            Text(
                "${uiState.completedEcts} / ${uiState.totalEcts} ECTS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedCurrProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(OceanBlueLight, SeafoamGreenLight)
                        )
                    )
            )
        }
    }
}

@Composable
fun ModuleLegend(uiState: DashboardUiState) {
    if (uiState.modules.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Active Modules",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
            uiState.modules.forEachIndexed { index, module ->
                val color = when (index % 3) {
                    0 -> Module1Color
                    1 -> Module2Color
                    else -> Module3Color
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = module.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTabRow(tabs: List<String>, selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    SecondaryTabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = Color.Transparent,
        contentColor = Module1Color
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTabIndex == index)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

@Composable
fun DashboardTaskList(
    uiState: DashboardUiState,
    onRescheduleOverdue: () -> Unit,
    onToggleTask: (String, Boolean) -> Unit,
    onToggleSubTask: (String, Boolean) -> Unit,
    onAddSubTask: (String, String) -> Unit,
    observeSubTasks: (String) -> Flow<List<TaskWithDetails>>,
    onNavigateToFocusMode: () -> Unit,
    horizontalPadding: Boolean = false
) {
    val hPad = if (horizontalPadding) 24.dp else 0.dp
    // Overdue banner
    if (uiState.overdueTasks.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPad, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "${uiState.overdueTasks.size} Overdue Task${if (uiState.overdueTasks.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "From past days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onRescheduleOverdue,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                        contentColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text("Reschedule", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }

    if (uiState.totalCount == 0 && uiState.manualTopics.isEmpty() && uiState.isSetupComplete && uiState.overdueTasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPad, vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("All caught up! ✅",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPad),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            uiState.tasks.forEach { task ->
                TaskCard(
                    task = task,
                    todayDateString = uiState.todayDateString,
                    onToggle = { onToggleTask(task.task.id, task.task.isCompleted) },
                    onToggleSubTask = onToggleSubTask,
                    onAddSubTask = { title -> onAddSubTask(task.task.id, title) },
                    observeSubTasks = observeSubTasks,
                    onNavigateToFocusMode = onNavigateToFocusMode
                )
            }
            
            uiState.manualTopics.forEach { topic ->
                ManualTopicCard(topic = topic)
            }
            Spacer(modifier = Modifier.height(if (uiState.isSetupComplete) 100.dp else 16.dp))
        }
    }
}

@Composable
fun ManualTopicCard(topic: com.iu.studytracker.data.database.entity.CurriculumTopic) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Scheduled for today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RoadmapTimeline(uiState: DashboardUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        if (uiState.semesterProgress.isNotEmpty()) {
            Text(
                text = "Semester Progress",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            uiState.semesterProgress.forEach { sem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Module1Color)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Semester ${sem.semester}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val semProgress = if (sem.totalCredits > 0) sem.completedCredits.toFloat() / sem.totalCredits else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(semProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${sem.completedCredits} / ${sem.totalCredits} ECTS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Timeline",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        uiState.monthPlans.forEach { monthPlan ->
            val monthName = java.time.Month.of(monthPlan.month)
                .getDisplayName(TextStyle.FULL, androidx.compose.ui.platform.LocalConfiguration.current.locales.get(0))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Module2Color)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "$monthName ${monthPlan.year}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (monthPlan.isSetupComplete) "Modules scheduled" else "Awaiting setup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.targetGraduation.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Module1Color.copy(alpha = 0.1f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Flag, contentDescription = null,
                    tint = Module1Color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Graduation Goal", style = MaterialTheme.typography.labelMedium,
                        color = Module1Color)
                    Text(uiState.targetGraduation, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun TaskCard(
    task: TaskWithDetails,
    todayDateString: String,
    onToggle: () -> Unit,
    onToggleSubTask: (String, Boolean) -> Unit,
    onAddSubTask: (String) -> Unit,
    observeSubTasks: (String) -> kotlinx.coroutines.flow.Flow<List<TaskWithDetails>>,
    onNavigateToFocusMode: () -> Unit
) {
    val moduleColor = when (task.moduleOrderIndex % 3) {
        0 -> Module1Color
        1 -> Module2Color
        else -> Module3Color
    }
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    val subtasks by remember(task.task.id) { observeSubTasks(task.task.id) }.collectAsState(initial = emptyList())
    val isOverdue = task.task.scheduledDate != null
            && task.task.scheduledDate < todayDateString
            && !task.task.isCompleted

    val cardBackground = when {
        task.task.isCompleted -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.task.isCompleted) 0.dp else 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left colour strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = if (task.task.isCompleted) moduleColor.copy(alpha = 0.3f) else moduleColor,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle() }
                        .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.task.isCompleted,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = moduleColor,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            checkmarkColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedContent(targetState = task.task.isCompleted, label = "strikethrough") { completed ->
                            Text(
                                text = task.topicTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.onBackground,
                                textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Module chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(moduleColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(task.moduleName, color = moduleColor,
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                            if (isOverdue) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Overdue", color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (task.task.actualMinutesSpent > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${task.task.actualMinutesSpent}m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                            if (task.task.recurrenceRule != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Repeat, contentDescription = "Recurring",
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    if (!task.task.isCompleted) {
                        Row {
                            IconButton(onClick = { showTimeDialog = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Focus",
                                    tint = moduleColor, modifier = Modifier.size(20.dp))
                            }
                            if (!task.resourceUri.isNullOrEmpty()) {
                                IconButton(onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(task.resourceUri)))
                                    } catch (e: Exception) { e.printStackTrace() }
                                }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Link, contentDescription = "Resource",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Subtasks",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Subtask expansion
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp, end = 12.dp, bottom = 12.dp)
                    ) {
                        subtasks.forEach { subTask ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 3.dp)
                                    .clickable { onToggleSubTask(subTask.task.id, subTask.task.isCompleted) }
                            ) {
                                Checkbox(
                                    checked = subTask.task.isCompleted,
                                    onCheckedChange = { onToggleSubTask(subTask.task.id, subTask.task.isCompleted) },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = subTask.task.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    textDecoration = if (subTask.task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (subTask.task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        var isAdding by remember { mutableStateOf(false) }
                        var newTitle by remember { mutableStateOf("") }
                        if (isAdding) {
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                placeholder = { Text("Subtask title…", style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (newTitle.isNotBlank()) {
                                            onAddSubTask(newTitle)
                                            newTitle = ""; isAdding = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Check, "Add")
                                    }
                                }
                            )
                        } else {
                            TextButton(onClick = { isAdding = true }) {
                                Icon(Icons.Default.Add, "Add Subtask", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add subtask", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTimeDialog) {
        var minutesText by remember { mutableStateOf("25") }
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            title = { Text("Set Focus Time", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) minutesText = it },
                    label = { Text("Minutes") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    val mins = minutesText.toIntOrNull() ?: 25
                    val intent = Intent(context, com.iu.studytracker.service.FocusTimerService::class.java).apply {
                        action = com.iu.studytracker.service.FocusTimerService.ACTION_START
                        putExtra(com.iu.studytracker.service.FocusTimerService.EXTRA_TASK_ID, task.task.id)
                        putExtra(com.iu.studytracker.service.FocusTimerService.EXTRA_TASK_TITLE, task.topicTitle)
                        putExtra(com.iu.studytracker.service.FocusTimerService.EXTRA_MINUTES, mins)
                    }
                    context.startService(intent)
                    showTimeDialog = false
                    onNavigateToFocusMode()
                }) { Text("Start") }
            },
            dismissButton = {
                TextButton(onClick = { showTimeDialog = false }) { Text("Cancel") }
            }
        )
    }
}
