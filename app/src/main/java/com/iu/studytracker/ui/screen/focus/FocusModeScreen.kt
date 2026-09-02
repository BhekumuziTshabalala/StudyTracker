package com.iu.studytracker.ui.screen.focus

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iu.studytracker.service.FocusTimerService
import com.iu.studytracker.service.TimerState
import com.iu.studytracker.ui.theme.Module1Color
import com.iu.studytracker.ui.theme.Module2Color
import com.iu.studytracker.ui.theme.GradientStart
import com.iu.studytracker.ui.theme.GradientEnd

private val BREAK_QUOTES = listOf(
    "Great work! Rest is part of the process. 🌊",
    "Your brain is consolidating what you learned. ✨",
    "Take a breath. You're doing brilliantly. 🌿",
    "Step away, stretch, drink water. You've earned it. 💪",
    "Consistency beats intensity. Keep going! 🚀"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val window = remember { (context as? android.app.Activity)?.window }

    DisposableEffect(Unit) {
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val remainingMillis by TimerState.remainingMillis.collectAsState()
    val totalMillis by TimerState.totalMillis.collectAsState()
    val isRunning by TimerState.isRunning.collectAsState()
    val currentTaskTitle by TimerState.currentTaskTitle.collectAsState()
    val selectedTheme by TimerState.selectedTheme.collectAsState()
    val soundEnabled by TimerState.soundEnabled.collectAsState()

    // Session counter
    var sessionCount by remember { mutableIntStateOf(1) }
    var showStopConfirm by remember { mutableStateOf(false) }
    val breakQuote = remember { BREAK_QUOTES.random() }
    val view = androidx.compose.ui.platform.LocalView.current

    // Request POST_NOTIFICATIONS permission for Android 13+
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { _ -> }
        LaunchedEffect(Unit) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val soundEnabled by TimerState.soundEnabled.collectAsState()

                    IconButton(onClick = { TimerState.setSoundEnabled(context, !soundEnabled) }) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Toggle Sound"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val transition = rememberInfiniteTransition(label = "breathe")
        val glowRadius by transition.animateFloat(
            initialValue = 700f,
            targetValue = 950f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowRadius"
        )
        
        val currentRadius = if (isRunning) glowRadius else 800f
        val currentAlpha = if (isRunning) 0.12f else 0.06f

        // Subtle radial background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Module1Color.copy(alpha = currentAlpha),
                            Color.Transparent
                        ),
                        radius = currentRadius
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Task title
                if (currentTaskTitle != null) {
                    Text(
                        text = currentTaskTitle!!,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "No active task selected",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Session counter pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Module1Color.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Session #$sessionCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = Module1Color
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                LaunchedEffect(remainingMillis, isRunning) {
                    if (isRunning && soundEnabled && remainingMillis > 0L) {
                        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    }
                }
                
                // Timer Visuals
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTheme) {
                        com.iu.studytracker.service.TimerTheme.MODERN_RING -> {
                            val progress = if (totalMillis > 0) remainingMillis.toFloat() / totalMillis else 0f
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(1000, easing = LinearEasing),
                                label = "timerProgress"
                            )
                            val minutes = (remainingMillis / 1000) / 60
                            val seconds = (remainingMillis / 1000) % 60
                            val timeString = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
                            
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
                                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
                                Canvas(modifier = Modifier.size(260.dp)) {
                                    drawCircle(color = surfaceVariantColor, style = Stroke(width = 36.dp.toPx()))
                                    drawArc(
                                        brush = Brush.sweepGradient(listOf(GradientStart, GradientEnd, GradientStart)),
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedProgress,
                                        useCenter = false,
                                        style = Stroke(width = 36.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = timeString,
                                        fontSize = 60.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (!isRunning && remainingMillis < totalMillis && remainingMillis > 0) {
                                        Text("Paused", style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        com.iu.studytracker.service.TimerTheme.FLIP_CLOCK -> {
                            val timeRemainingSeconds = (remainingMillis / 1000).toInt()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FlipClockTimer(timeRemainingSeconds = timeRemainingSeconds)
                                Spacer(modifier = Modifier.height(24.dp))
                                if (!isRunning && remainingMillis < totalMillis && remainingMillis > 0) {
                                    Text("Paused", style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Break quote area
                if (!isRunning && remainingMillis > 0 && totalMillis > 0 && remainingMillis < totalMillis) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = breakQuote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Controls
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FloatingActionButton(
                        onClick = {
                            val action = if (isRunning) FocusTimerService.ACTION_PAUSE else FocusTimerService.ACTION_START
                            context.startService(Intent(context, FocusTimerService::class.java).apply { this.action = action })
                        },
                        containerColor = Module1Color,
                        contentColor = Color.White,
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    FloatingActionButton(
                        onClick = { showStopConfirm = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                // Theme Selector — always visible at bottom
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        com.iu.studytracker.service.TimerTheme.entries.forEach { theme ->
                            val isSelected = theme == selectedTheme
                            Surface(
                                onClick = { TimerState.setTheme(context, theme) },
                                shape = RoundedCornerShape(50.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            ) {
                                Text(
                                    text = theme.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stop confirmation dialog
        if (showStopConfirm) {
            AlertDialog(
                onDismissRequest = { showStopConfirm = false },
                title = { Text("End Session?", style = MaterialTheme.typography.titleMedium) },
                text = { Text("Your focus time will be saved, but the timer will stop.", style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    Button(
                        onClick = {
                            context.startService(Intent(context, FocusTimerService::class.java).apply {
                                action = FocusTimerService.ACTION_STOP
                            })
                            sessionCount++
                            showStopConfirm = false
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("End Session") }
                },
                dismissButton = {
                    TextButton(onClick = { showStopConfirm = false }) { Text("Keep Going") }
                }
            )
        }
    }
}

