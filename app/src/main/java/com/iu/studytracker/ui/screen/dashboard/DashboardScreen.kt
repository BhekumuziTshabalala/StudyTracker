package com.iu.studytracker.ui.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.iu.studytracker.data.model.DailyTaskWithDetails
import com.iu.studytracker.ui.theme.*
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (uiState.programmeName.isNotBlank()) uiState.programmeName else "Study Tracker",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
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
        floatingActionButton = {
            if (uiState.isSetupComplete) {
                SmallFloatingActionButton(
                    onClick = onNavigateToSetup,
                    containerColor = Module2Color,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Setup")
                }
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
                    color = Module1Color
                )
            } else if (!uiState.isSetupComplete && uiState.hasMonthPlan) {
                NoSetupState(onNavigateToSetup)
            } else {
                ActiveDashboardState(
                    uiState = uiState,
                    onToggleTask = viewModel::toggleTask,
                    onRebalance = viewModel::rebalanceSchedule
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
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Module1Color.copy(alpha = 0.2f),
                                Module2Color.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Module1Color,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Set up your month",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose your two modules and topics",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onNavigateToSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Module1Color),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Start Monthly Setup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ActiveDashboardState(
    uiState: DashboardUiState,
    onToggleTask: (Long, Boolean) -> Unit,
    onRebalance: () -> Unit
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
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Pane: Overview & Progress Card (40% width)
                Card(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.dayOfWeek.uppercase(),
                                    color = Module2Color,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                IconButton(onClick = onRebalance) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = "Rebalance Schedule", tint = Module1Color)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.todayFormatted,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = uiState.monthName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Rank Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Module1Color.copy(alpha = 0.2f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Rank", tint = Module1Color, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${uiState.rankTitle} (${uiState.xp} XP)",
                                    color = Module1Color,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Daily Progress
                            if (uiState.totalCount > 0) {
                                Text(
                                    text = "Daily Progress",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${uiState.completedCount} of ${uiState.totalCount} completed",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val progressPercentage = if (uiState.totalCount > 0) uiState.completedCount.toFloat() / uiState.totalCount else 0f
                                val animatedProgress by animateFloatAsState(
                                    targetValue = progressPercentage,
                                    animationSpec = tween(1000),
                                    label = "progress_animation"
                                )

                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = Module1Color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Curriculum Progress
                                if (uiState.totalEcts > 0) {
                                    Text(
                                        text = "Credit Progress",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${uiState.completedEcts} of ${uiState.totalEcts} ECTS",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val currProgressPercentage = uiState.completedEcts.toFloat() / uiState.totalEcts.toFloat()
                                    val animatedCurrProgress by animateFloatAsState(
                                        targetValue = currProgressPercentage,
                                        animationSpec = tween(1000),
                                        label = "curr_progress_animation"
                                    )

                                    LinearProgressIndicator(
                                        progress = { animatedCurrProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp)),
                                        color = Purple80,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            } else if (uiState.isSetupComplete) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Celebration,
                                        contentDescription = "Rest Day",
                                        tint = Module2Color,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Rest Day! 🎉",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "No tasks scheduled for today.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        // Module Legend
                        if (uiState.modules.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Active Modules",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                uiState.modules.forEachIndexed { index, module ->
                                    val color = if (index == 0) Module1Color else Module2Color
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = module.name,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Pane: Tasks List & Roadmap (58% width)
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = Module1Color,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = Module1Color
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { 
                                    Text(
                                        text = title, 
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTabIndex == index) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (selectedTabIndex == 0) {
                        // Tasks List
                        if (uiState.tasks.isEmpty() && uiState.isSetupComplete) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "All caught up! No tasks for today.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                itemsIndexed(uiState.tasks, key = { _, task -> task.taskId }) { _, task ->
                                    TaskCard(
                                        task = task,
                                        onToggle = { onToggleTask(task.taskId, task.isCompleted) }
                                    )
                                }
                            }
                        }
                    } else {
                        // Roadmap Timeline
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
            ) {
                // Header Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Module1Color.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 24.dp)
                ) {
                    Column {
                        Text(
                            text = uiState.dayOfWeek.uppercase(),
                            color = Module2Color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
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
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = uiState.monthName,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(onClick = onRebalance) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = "Rebalance Schedule", tint = Module1Color)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // Rank Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Module1Color.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Rank", tint = Module1Color, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${uiState.rankTitle} (${uiState.xp} XP)",
                                color = Module1Color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Progress Section
                if (uiState.totalCount > 0) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "Daily Progress",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.completedCount} of ${uiState.totalCount} completed",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val progressPercentage = if (uiState.totalCount > 0) uiState.completedCount.toFloat() / uiState.totalCount else 0f
                        val animatedProgress by animateFloatAsState(
                            targetValue = progressPercentage,
                            animationSpec = tween(1000),
                            label = "progress_animation"
                        )

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Module1Color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                
                if (uiState.totalEcts > 0) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "Credit Progress",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.completedEcts} of ${uiState.totalEcts} ECTS",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val currProgressPercentage = uiState.completedEcts.toFloat() / uiState.totalEcts.toFloat()
                        val animatedCurrProgress by animateFloatAsState(
                            targetValue = currProgressPercentage,
                            animationSpec = tween(1000),
                            label = "curr_progress_animation"
                        )

                        LinearProgressIndicator(
                            progress = { animatedCurrProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Purple80,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Module1Color,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Module1Color
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTabIndex == 0) {
                    // Task List or Empty Day State
                    if (uiState.totalCount == 0 && uiState.isSetupComplete) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Celebration,
                                    contentDescription = "Rest Day",
                                    tint = Module2Color,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Rest Day! 🎉",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Take a break, you've earned it.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentPadding = PaddingValues(bottom = 80.dp), // Space for FAB
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(uiState.tasks, key = { _, task -> task.taskId }) { _, task ->
                                TaskCard(
                                    task = task,
                                    onToggle = { onToggleTask(task.taskId, task.isCompleted) }
                                )
                            }
                        }
                    }
                } else {
                    // Roadmap Timeline
                    RoadmapTimeline(uiState)
                }
            }
        }
    }
}

@Composable
fun RoadmapTimeline(uiState: DashboardUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(uiState.monthPlans.size) { index ->
            val monthPlan = uiState.monthPlans[index]
            val monthName = java.time.Month.of(monthPlan.month).getDisplayName(TextStyle.FULL, Locale.getDefault())
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Module2Color)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "$monthName ${monthPlan.year}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (monthPlan.isSetupComplete) "Modules scheduled" else "Awaiting setup",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        if (uiState.targetGraduation.isNotBlank()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Flag, contentDescription = "Graduation", tint = Module1Color, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Graduation - ${uiState.targetGraduation}",
                        color = Module1Color,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: DailyTaskWithDetails,
    onToggle: () -> Unit
) {
    val moduleColor = if (task.moduleOrderIndex == 0) Module1Color else Module2Color
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = moduleColor,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = task.isCompleted,
                    label = "text_strikethrough"
                ) { isCompleted ->
                    Text(
                        text = task.topicTitle,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(moduleColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.moduleName,
                            color = moduleColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (task.actualMinutesSpent > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${task.actualMinutesSpent}m spent",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            // Action Buttons
            if (!task.isCompleted) {
                Row {
                    if (!task.resourceUri.isNullOrEmpty()) {
                        IconButton(onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(task.resourceUri))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Icon(Icons.Default.Link, contentDescription = "Resource Link", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
