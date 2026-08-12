package com.iu.studytracker.ui.screen.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.ui.theme.*

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Purple80)
        }
        return
    }

    if (!uiState.hasSetup) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Please complete the setup first to view progress.", color = TextPrimary)
        }
        return
    }

    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    val animatedPercentage by animateFloatAsState(
        targetValue = if (animationPlayed) uiState.overallPercentage else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            // ── Tablet Dual-Pane Layout ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Pane: Overall Progress (45% width)
                Card(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Gradient Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(Purple60, Cyan60)))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${uiState.monthName} Progress",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Large circular progress
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(190.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = DarkSurfaceVariant,
                                strokeWidth = 14.dp
                            )
                            CircularProgressIndicator(
                                progress = { animatedPercentage },
                                modifier = Modifier.fillMaxSize(),
                                color = Cyan80,
                                strokeWidth = 14.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(animatedPercentage * 100).toInt()}%",
                                    color = TextPrimary,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Completed",
                                    color = TextSecondary,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Study Streak Card
                        StatCard(
                            icon = Icons.Filled.LocalFireDepartment,
                            value = "${uiState.studyStreak} days",
                            label = "Current Study Streak",
                            iconTint = StatusOrange,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Right Pane: Breakdown & Details (55% width)
                Card(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Performance Breakdown",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Stats Cards Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                icon = Icons.Filled.CalendarToday,
                                value = "${uiState.daysRemaining}",
                                label = "Days Left",
                                iconTint = Cyan80,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = Icons.Filled.Checklist,
                                value = "${uiState.overallCompleted}/${uiState.overallTotal}",
                                label = "Topics Done",
                                iconTint = StatusGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "Module Progress",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ModuleProgressSection(
                                    moduleName = uiState.module1Name,
                                    completed = uiState.module1Completed,
                                    total = uiState.module1Total,
                                    color = Module1Color
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ModuleProgressSection(
                                    moduleName = uiState.module2Name,
                                    completed = uiState.module2Completed,
                                    total = uiState.module2Total,
                                    color = Module2Color
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ── Phone Single-Column Layout ───────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gradient Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.horizontalGradient(listOf(Purple60, Cyan60)))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${uiState.monthName} Progress",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Large circular progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = DarkSurfaceVariant,
                        strokeWidth = 12.dp
                    )
                    CircularProgressIndicator(
                        progress = { animatedPercentage },
                        modifier = Modifier.fillMaxSize(),
                        color = Cyan80,
                        strokeWidth = 12.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animatedPercentage * 100).toInt()}%",
                            color = TextPrimary,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completed",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCard(
                        icon = Icons.Filled.LocalFireDepartment,
                        value = "${uiState.studyStreak} days",
                        label = "Study Streak",
                        iconTint = StatusOrange,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatCard(
                        icon = Icons.Filled.CalendarToday,
                        value = "${uiState.daysRemaining}",
                        label = "Days Left",
                        iconTint = Cyan80,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatCard(
                        icon = Icons.Filled.Checklist,
                        value = "${uiState.overallCompleted}/${uiState.overallTotal}",
                        label = "Topics Done",
                        iconTint = StatusGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Module Progress Bars
                ModuleProgressSection(
                    moduleName = uiState.module1Name,
                    completed = uiState.module1Completed,
                    total = uiState.module1Total,
                    color = Module1Color
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModuleProgressSection(
                    moduleName = uiState.module2Name,
                    completed = uiState.module2Completed,
                    total = uiState.module2Total,
                    color = Module2Color
                )
            }
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(text = label, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ModuleProgressSection(
    moduleName: String,
    completed: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = moduleName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "$completed of $total topics", color = TextSecondary, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = DarkSurfaceVariant
        )
    }
}
