package com.iu.studytracker.ui.screen.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.iu.studytracker.data.database.StudyTrackerDatabase
import com.iu.studytracker.data.repository.StudyRepository
import com.iu.studytracker.ui.theme.StudyTrackerTheme
import com.iu.studytracker.util.TaskParser
import kotlinx.coroutines.launch

class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (applicationContext as com.iu.studytracker.StudyTrackerApp).repository
        
        setContent {
            StudyTrackerTheme {
                // Transparent background wrapper
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QuickAddDialog(
                        onAdd = { inputTitle, recurrenceRule ->
                            val parsed = TaskParser.parse(inputTitle)
                            val task = com.iu.studytracker.data.database.entity.Task(
                                title = parsed.cleanTitle,
                                scheduledDate = parsed.dateString ?: repository.todayString(),
                                recurrenceRule = recurrenceRule
                            )
                            // We need a coroutine scope to launch repository methods
                            lifecycleScope.launch {
                                repository.insertTask(task)
                                finish() // Close activity when done
                            }
                        },
                        onDismiss = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddDialog(onAdd: (String, String?) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedRecurrence by remember { mutableStateOf<String?>(null) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Quick Add Task", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task (e.g. Study Math tomorrow)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Recurrence selection
            Text("Repeat", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedRecurrence == null,
                    onClick = { selectedRecurrence = null },
                    label = { Text("None") }
                )
                FilterChip(
                    selected = selectedRecurrence == "FREQ=DAILY",
                    onClick = { selectedRecurrence = "FREQ=DAILY" },
                    label = { Text("Daily") }
                )
                FilterChip(
                    selected = selectedRecurrence == "FREQ=WEEKLY",
                    onClick = { selectedRecurrence = "FREQ=WEEKLY" },
                    label = { Text("Weekly") }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onAdd(title, selectedRecurrence) },
                    enabled = title.isNotBlank()
                ) {
                    Text("Add Task")
                }
            }
        }
    }
}
