package com.iu.studytracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.iu.studytracker.MainActivity
import com.iu.studytracker.StudyTrackerApp
import kotlinx.coroutines.flow.first

class ModuleProgressWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as StudyTrackerApp
        val repository = app.repository
        
        // Find active modules
        val activeModules = repository.getAllCurriculumModulesSync().filter { !it.isCompleted }
        
        val progressData = activeModules.map { module ->
            val total = repository.observeTaskCountForModule(module.id).first()
            val completed = repository.observeCompletedTaskCountForModule(module.id).first()
            ModuleProgress(module.name, completed, total)
        }.filter { it.total > 0 }

        provideContent {
            GlanceTheme {
                WidgetContent(progressData)
            }
        }
    }

    companion object {
        suspend fun updateAllWidgets(context: Context) {
            ModuleProgressWidget().updateAll(context)
        }
    }
}

data class ModuleProgress(val name: String, val completed: Int, val total: Int)

@Composable
fun WidgetContent(progressData: List<ModuleProgress>) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = "Module Progress",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(12.dp))
            
            if (progressData.isEmpty()) {
                Text(
                    text = "No active modules found.",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                )
            } else {
                progressData.take(3).forEach { progress ->
                    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            Text(
                                text = progress.name,
                                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Text(
                                text = "${progress.completed}/${progress.total}",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                            )
                        }
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        val pct = if (progress.total > 0) progress.completed.toFloat() / progress.total else 0f
                        androidx.glance.appwidget.LinearProgressIndicator(
                            progress = pct,
                            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                            color = androidx.glance.color.ColorProvider(day = Color(0xFF00796B), night = Color(0xFF00796B)),
                            backgroundColor = androidx.glance.color.ColorProvider(day = Color(0xFFE0E0E0), night = Color(0xFFE0E0E0))
                        )
                    }
                }
            }
        }
    }
}
