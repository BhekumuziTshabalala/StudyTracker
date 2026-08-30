package com.iu.studytracker.ui.screen.focus

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FlipClockTimer(timeRemainingSeconds: Int, modifier: Modifier = Modifier) {
    val hours = timeRemainingSeconds / 3600
    val minutes = (timeRemainingSeconds % 3600) / 60
    val seconds = timeRemainingSeconds % 60

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hours > 0) {
            FlipDigitUnit(value = hours, label = "HOURS")
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        FlipDigitUnit(value = minutes, label = "MINUTES")
        Spacer(modifier = Modifier.width(16.dp))
        FlipDigitUnit(value = seconds, label = "SECONDS")
    }
}

@Composable
fun FlipDigitUnit(value: Int, label: String) {
    val displayValue = String.format("%02d", value)
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = displayValue,
            transitionSpec = {
                (slideInVertically { -it } + fadeIn(tween(200))).togetherWith(
                    slideOutVertically { it } + fadeOut(tween(200))
                )
            },
            label = "flipAnimation"
        ) { targetCount ->
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111827)) // Dark slate clock face
            ) {
                Text(
                    text = targetCount,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // Horizontal split line
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black.copy(alpha = 0.8f))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827).copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
    }
}
