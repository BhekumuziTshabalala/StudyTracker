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
import androidx.compose.ui.graphics.asImageBitmap
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
    onSignOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
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
        val deviceId by viewModel.deviceId.collectAsState()
        val linkedDevices by viewModel.linkedDevices.collectAsState()

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



            val reminderEnabled by viewModel.reminderEnabled.collectAsState()
            val reminderHour by viewModel.reminderHour.collectAsState()
            val reminderMinute by viewModel.reminderMinute.collectAsState()
            var showTimePicker by remember { mutableStateOf(false) }

            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = reminderHour,
                    initialMinute = reminderMinute
                )
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Set Reminder Time") },
                    text = {
                        TimePicker(state = timePickerState)
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.setReminderTime(reminderEnabled, timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            SettingsGroupLabel("Reminders")
            SettingsGroup {
                SettingsSwitchRow(
                    icon = Icons.Default.Notifications,
                    title = "Daily Study Reminder",
                    subtitle = "Get notified about incomplete tasks",
                    checked = reminderEnabled,
                    onCheckedChange = { viewModel.setReminderTime(it, reminderHour, reminderMinute) }
                )
                if (reminderEnabled) {
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Schedule,
                        title = "Reminder Time",
                        subtitle = String.format(java.util.Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute),
                        onClick = { showTimePicker = true }
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
                
                SettingsDivider()
                
                val gradingSystem by viewModel.gradingSystem.collectAsState()
                var showGradingDropdown by remember { mutableStateOf(false) }
                val gradingOptions = listOf("GERMAN" to "German System (1.0 - 5.0)", "PERCENTAGE" to "Percentage (0% - 100%)", "LETTER" to "Letter (A - F)")
                val selectedGradingText = gradingOptions.find { it.first == gradingSystem }?.second ?: "Unknown"

                SettingsRow(
                    icon = Icons.Default.Grade,
                    title = "Grading System",
                    subtitle = selectedGradingText,
                    onClick = { showGradingDropdown = true }
                )
                
                if (showGradingDropdown) {
                    AlertDialog(
                        onDismissRequest = { showGradingDropdown = false },
                        title = { Text("Select Grading System") },
                        text = {
                            Column {
                                gradingOptions.forEach { (value, text) ->
                                    SettingsRadioRow(
                                        icon = Icons.Default.Grade,
                                        title = text,
                                        selected = gradingSystem == value,
                                        onClick = {
                                            viewModel.setGradingSystem(value)
                                            showGradingDropdown = false
                                        }
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showGradingDropdown = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            SettingsGroupLabel("Account")
            SettingsGroup {
                if (currentUser != null) {
                    SettingsInfoRow(
                        icon = Icons.Default.AccountCircle,
                        title = "Signed In As",
                        value = currentUser.email ?: "Unknown"
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Default.Logout,
                        title = "Sign Out",
                        subtitle = "Sign out of your account",
                        onClick = {
                            viewModel.signOutAndClearData {
                                onSignOut()
                            }
                        }
                    )
                } else {
                    SettingsInfoRow(
                        icon = Icons.Default.AccountCircle,
                        title = "Not Signed In",
                        value = ""
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

            Spacer(modifier = Modifier.height(24.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"

            // About
            SettingsGroupLabel("About")
            SettingsGroup {
                SettingsInfoRow(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    value = versionName
                )
                SettingsDivider()
                SettingsInfoRow(
                    icon = Icons.Default.School,
                    title = "Dolphin",
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

fun generateQrCode(content: String, size: Int = 512): androidx.compose.ui.graphics.ImageBitmap? {
    if (content.isEmpty()) return null
    return try {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
