package com.iu.studytracker.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
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
        val firebaseAppId by viewModel.firebaseAppId.collectAsState()
        val firebaseApiKey by viewModel.firebaseApiKey.collectAsState()
        val deviceId by viewModel.deviceId.collectAsState()
        val linkedDevices by viewModel.linkedDevices.collectAsState()

        var editProjectId by remember(firebaseProjectId) { mutableStateOf(firebaseProjectId ?: "") }
        var editAppId by remember(firebaseAppId) { mutableStateOf(firebaseAppId ?: "") }
        var editApiKey by remember(firebaseApiKey) { mutableStateOf(firebaseApiKey ?: "") }

        var showDegreeFields by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Appearance
            SettingsGroupLabel("Appearance")
            SettingsGroup {
                listOf(
                    Triple("System Default", ThemeMode.SYSTEM, Icons.Default.PhoneAndroid),
                    Triple("Light", ThemeMode.LIGHT, Icons.Default.LightMode),
                    Triple("Dark", ThemeMode.DARK, Icons.Default.DarkMode),
                ).forEachIndexed { index, (label, mode, icon) ->
                    if (index > 0) SettingsDivider()
                    SettingsRadioRow(
                        icon = icon,
                        title = label,
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Degree Progress
            SettingsGroupLabel("Degree Progress")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.Flag,
                    title = "Target Graduation",
                    subtitle = gradDate.ifBlank { "Not set" },
                    onClick = { showDegreeFields = !showDegreeFields },
                    trailing = {
                        Icon(
                            if (showDegreeFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
                if (showDegreeFields) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        OutlinedTextField(
                            value = gradDate,
                            onValueChange = {
                                gradDate = it
                                viewModel.updateGraduationDate(it)
                            },
                            label = { Text("Target Graduation") },
                            placeholder = { Text("e.g. July 2027") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Numbers,
                    title = "Total ECTS Required",
                    subtitle = "$totalEcts credits",
                    onClick = {}
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    OutlinedTextField(
                        value = totalEcts,
                        onValueChange = {
                            totalEcts = it
                            it.toIntOrNull()?.let { ects -> viewModel.updateTotalEcts(ects) }
                        },
                        label = { Text("Total ECTS") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cloud Sync
            SettingsGroupLabel("Cloud Sync")
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.Sync,
                    title = "Firebase Sync",
                    subtitle = if (isFirebaseSyncEnabled) "Syncing across devices" else "Sync disabled",
                    checked = isFirebaseSyncEnabled,
                    onCheckedChange = { viewModel.setFirebaseSyncEnabled(it) }
                )

                if (isFirebaseSyncEnabled) {
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Device ID",
                        value = deviceId.take(12) + "..."
                    )
                    SettingsDivider()

                    if (showCredentialsForm) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                "Firebase Credentials",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            OutlinedTextField(
                                value = editProjectId,
                                onValueChange = { editProjectId = it },
                                label = { Text("Project ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editAppId,
                                onValueChange = { editAppId = it },
                                label = { Text("App ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editApiKey,
                                onValueChange = { editApiKey = it },
                                label = { Text("Web API Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Connect your own Firebase project to sync data across devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (editProjectId.isNotEmpty() && editAppId.isNotEmpty() && editApiKey.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.linkFirebase(editProjectId, editAppId, editApiKey) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = linkStatus !is SettingsViewModel.LinkStatus.Loading,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (linkStatus is SettingsViewModel.LinkStatus.Loading) {
                                        CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Link Firebase")
                                    }
                                }
                            }
                        }
                    } else {
                        SettingsRow(
                            icon = Icons.Default.CheckCircle,
                            title = "Firebase Linked",
                            subtitle = "Project: $firebaseProjectId",
                            onClick = { showCredentialsForm = true },
                            trailing = {
                                Text(
                                    "Edit",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }

                    if (!firebaseProjectId.isNullOrEmpty() && !firebaseAppId.isNullOrEmpty() && !firebaseApiKey.isNullOrEmpty()) {
                        SettingsDivider()
                        SettingsRow(
                            icon = Icons.Default.Devices,
                            title = "Linked Devices",
                            subtitle = if (linkedDevices.isEmpty()) "No devices synced yet" else "${linkedDevices.size} device(s)",
                            onClick = { viewModel.refreshDevices() },
                            trailing = {
                                Icon(Icons.Default.Refresh, "Refresh",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp))
                            }
                        )
                        if (linkedDevices.isNotEmpty()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                                linkedDevices.forEach { deviceData ->
                                    val currentId = deviceData["deviceId"] as? String ?: ""
                                    val isCurrentDevice = currentId == deviceId
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        elevation = CardDefaults.cardElevation(0.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrentDevice) Icons.Default.PhoneAndroid else Icons.Default.Devices,
                                                contentDescription = null,
                                                tint = if (isCurrentDevice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    if (isCurrentDevice) "This Device" else "Device: ${currentId.take(8)}...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isCurrentDevice) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text("Last seen: ${deviceData["lastSeen"]}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (!isCurrentDevice) {
                                                TextButton(onClick = { viewModel.removeDevice(currentId) }) {
                                                    Text("Unlink", color = MaterialTheme.colorScheme.error,
                                                        style = MaterialTheme.typography.labelMedium)
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

            Spacer(modifier = Modifier.height(24.dp))

            // About
            SettingsGroupLabel("About")
            SettingsGroup {
                SettingsInfoRow(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    value = "1.0.0"
                )
                SettingsDivider()
                SettingsInfoRow(
                    icon = Icons.Default.School,
                    title = "StudyTracker",
                    value = "IU Student Companion"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Shared Settings Primitives

@Composable
fun SettingsGroupLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun SettingsInfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsRadioRow(icon: ImageVector, title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}
