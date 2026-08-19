package com.iu.studytracker.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iu.studytracker.data.repository.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val linkStatus by viewModel.linkStatus.collectAsState()
    val firebaseProjectId by viewModel.firebaseProjectId.collectAsState()
    var showCredentialsForm by remember { mutableStateOf(firebaseProjectId.isNullOrEmpty()) }

    LaunchedEffect(linkStatus) {
        when (val status = linkStatus) {
            is SettingsViewModel.LinkStatus.Success -> {
                snackbarHostState.showSnackbar("Firebase linked successfully!")
                showCredentialsForm = false
                viewModel.clearLinkStatus()
            }
            is SettingsViewModel.LinkStatus.Error -> {
                snackbarHostState.showSnackbar("Link failed: ${status.message}")
                viewModel.clearLinkStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val degreePlan by viewModel.degreePlan.collectAsState()
        
        var gradDate by remember(degreePlan) { mutableStateOf(degreePlan?.targetGraduation ?: "July 2027") }
        var totalEcts by remember(degreePlan) { mutableStateOf(degreePlan?.totalCreditsRequired?.toString() ?: "180") }

        val isFirebaseSyncEnabled by viewModel.isFirebaseSyncEnabled.collectAsState()
        val firebaseProjectId by viewModel.firebaseProjectId.collectAsState()
        val firebaseAppId by viewModel.firebaseAppId.collectAsState()
        val firebaseApiKey by viewModel.firebaseApiKey.collectAsState()
        val deviceId by viewModel.deviceId.collectAsState()
        val linkedDevices by viewModel.linkedDevices.collectAsState()

        var editProjectId by remember(firebaseProjectId) { mutableStateOf(firebaseProjectId ?: "") }
        var editAppId by remember(firebaseAppId) { mutableStateOf(firebaseAppId ?: "") }
        var editApiKey by remember(firebaseApiKey) { mutableStateOf(firebaseApiKey ?: "") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ThemeSelectionRow(
                title = "System Default",
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
            )
            ThemeSelectionRow(
                title = "Light",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
            )
            ThemeSelectionRow(
                title = "Dark",
                selected = themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Degree Progress",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = gradDate,
                onValueChange = { 
                    gradDate = it
                    viewModel.updateGraduationDate(it)
                },
                label = { Text("Target Graduation (e.g. July 2027)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = totalEcts,
                onValueChange = { 
                    totalEcts = it
                    it.toIntOrNull()?.let { ects ->
                        viewModel.updateTotalEcts(ects)
                    }
                },
                label = { Text("Total ECTS Required") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Cloud Sync (Firebase)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Enable Firebase Sync",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isFirebaseSyncEnabled,
                    onCheckedChange = { viewModel.setFirebaseSyncEnabled(it) }
                )
            }

            if (isFirebaseSyncEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Device ID: $deviceId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (showCredentialsForm) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editProjectId,
                        onValueChange = { editProjectId = it },
                        label = { Text("Project ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editAppId,
                        onValueChange = { editAppId = it },
                        label = { Text("App ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editApiKey,
                        onValueChange = { editApiKey = it },
                        label = { Text("Web API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Provide your Firebase project credentials above. This will allow the app to initialize Firebase locally and sync your data to your own Firestore database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (editProjectId.isNotEmpty() && editAppId.isNotEmpty() && editApiKey.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.linkFirebase(editProjectId, editAppId, editApiKey) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = linkStatus !is SettingsViewModel.LinkStatus.Loading
                        ) {
                            if (linkStatus is SettingsViewModel.LinkStatus.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Start Link")
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Firebase is Linked", style = MaterialTheme.typography.titleMedium)
                            Text("Project ID: $firebaseProjectId", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { showCredentialsForm = true }) {
                                Text("Edit Credentials")
                            }
                        }
                    }
                }

                if (!firebaseProjectId.isNullOrEmpty() && !firebaseAppId.isNullOrEmpty() && !firebaseApiKey.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "Linked Devices",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.refreshDevices() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Devices")
                        }
                    }

                    if (linkedDevices.isEmpty()) {
                        Text(
                            text = "No devices synced yet or connecting...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        linkedDevices.forEach { deviceData ->
                            val currentId = deviceData["deviceId"] as? String ?: ""
                            val isCurrentDevice = currentId == deviceId

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isCurrentDevice) "This Device" else "Device: ${currentId.take(8)}...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isCurrentDevice) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                        Text(
                                            text = "Last seen: ${deviceData["lastSeen"]}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!isCurrentDevice) {
                                        TextButton(onClick = { viewModel.removeDevice(currentId) }) {
                                            Text("Unlink", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
