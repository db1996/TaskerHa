package com.github.db1996.taskerha.activities.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.logging.LogChannel
import com.github.db1996.taskerha.logging.LogLevel
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.util.hasNotificationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsTab(val label: String) {
    CONNECTION("Connection"),
    WEBSOCKET("WebSocket"),
    LOGGING("Logging")
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    modifier: Modifier,
    setTopBar: (@Composable () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.CONNECTION) }

    // --- Connection state
    var url by remember { mutableStateOf(HaSettings.loadUrl(context)) }
    var token by remember { mutableStateOf(HaSettings.loadToken(context)) }

    var status by remember { mutableStateOf(Status.Idle) }
    var error by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    val unsavedChanges by remember(url, token) {
        mutableStateOf(url != HaSettings.loadUrl(context) || token != HaSettings.loadToken(context))
    }

    fun setSaved() {
        saved = true
        scope.launch {
            delay(1200)
            saved = false
        }
    }

    // --- Websocket state
    var wsEnabled by remember { mutableStateOf(HaSettings.loadWebSocketEnabled(context)) }
    var showBatteryDialog by remember { mutableStateOf(!hasSeenBatteryDialog(context) && wsEnabled) }

    fun enableWebSocket() {

        if (!hasSeenBatteryDialog(context)) {
            showBatteryDialog = true
        } else {
            wsEnabled = true
            HaSettings.saveWebSocketEnabled(context, true)
            HaWebSocketService.start(context)
        }
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                enableWebSocket()
            } else {
                wsEnabled = false
                HaSettings.saveWebSocketEnabled(context, false)
            }
        }

    // --- Logging state
    var generalLevel by remember { mutableStateOf(HaSettings.loadLogLevel(context, LogChannel.GENERAL)) }
    var wsLevel by remember { mutableStateOf(HaSettings.loadLogLevel(context, LogChannel.WEBSOCKET)) }

    // Top bar depends on selected tab
    LaunchedEffect(selectedTab, unsavedChanges, testing, saved, url, token) {
        setTopBar {
            TopAppBar(
                title = { Text("TaskerHA Settings") },
                actions = {
                    if (selectedTab == SettingsTab.CONNECTION) {
                        FilledIconButton(
                            enabled = unsavedChanges && !testing && url.isNotBlank() && token.isNotBlank(),
                            onClick = {
                                HaSettings.save(context, url.trim(), token.trim())
                                setSaved()
                            }
                        ) {
                            if (saved) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Saved")
                            } else {
                                Icon(Icons.Rounded.Save, contentDescription = "Save settings")
                            }
                        }
                    }
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().then(modifier),
        bottomBar = {
            NavigationBar {
                SettingsTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab.label) },
                        icon = {}
                    )
                }
            }
        }
    ) { _ ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                SettingsTab.CONNECTION -> ConnectionTab(
                    url = url,
                    token = token,
                    testing = testing,
                    status = status,
                    error = error,
                    onUrlChange = { url = it },
                    onTokenChange = { token = it },
                    onTest = {
                        testing = true
                        status = Status.Testing
                        val client = HomeAssistantClient(url, token)

                        scope.launch {
                            val success = withContext(Dispatchers.IO) {
                                try {
                                    client.ping()
                                } catch (_: Exception) {
                                    false
                                }
                            }
                            status = if (success) Status.Success else Status.Failed
                            testing = false
                            error = client.error
                        }
                    }
                )

                SettingsTab.WEBSOCKET -> WebSocketTab(
                    wsEnabled = wsEnabled,
                    onToggle = { enabled ->
                        if (enabled) {
                            if (hasNotificationPermission(context)) {
                                enableWebSocket()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                wsEnabled = false
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                enableWebSocket()
                            }
                        } else {
                            wsEnabled = false
                            HaSettings.saveWebSocketEnabled(context, false)
                            HaWebSocketService.stop(context)
                        }
                    }
                )

                SettingsTab.LOGGING -> LoggingTab(
                    generalLevel = generalLevel,
                    wsLevel = wsLevel,
                    onGeneralLevelChange = { new ->
                        generalLevel = new
                        HaSettings.saveLogLevel(context, LogChannel.GENERAL, new)
                        CustomLogger.setMinLevel(LogChannel.GENERAL, new)
                    },
                    onWsLevelChange = { new ->
                        wsLevel = new
                        HaSettings.saveLogLevel(context, LogChannel.WEBSOCKET, new)
                        CustomLogger.setMinLevel(LogChannel.WEBSOCKET, new)
                    },
                    onShare = { channel ->
                        val base = CustomLogger.buildShareLogsIntent(context, channel)
                        if (base == null) {
                            Toast.makeText(context, "No logs to share", Toast.LENGTH_SHORT).show()
                            return@LoggingTab
                        }
                        val chooser = Intent.createChooser(base, "Share TaskerHA logs").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(chooser)
                    }
                )
            }
        }
    }

    // Battery dialog stays global because it can be triggered from the websocket tab
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text("Allow background activity") },
            text = {
                Text(
                    "To reliably receive Home Assistant triggers, Android must allow this app " +
                            "to run in the background.\n\n" +
                            "On the next screen, open Battery and set it to allow background activity " +
                            "or Unrestricted (wording may differ per device)."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryDialog = false
                        setBatteryDialogShown(context)
                        openAppBatterySettings(context)
                        wsEnabled = true
                        HaSettings.saveWebSocketEnabled(context, true)
                        HaWebSocketService.start(context)
                    }
                ) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBatteryDialog = false
                        wsEnabled = false
                        HaSettings.saveWebSocketEnabled(context, false)
                        HaWebSocketService.stop(context)
                    }
                ) { Text("Not now") }
            }
        )
    }
}

@Composable
private fun ConnectionTab(
    url: String,
    token: String,
    testing: Boolean,
    status: Status,
    error: String?,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onTest: () -> Unit
) {
    OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        label = { Text("Home Assistant URL") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    OutlinedTextField(
        value = token,
        onValueChange = onTokenChange,
        label = { Text("Access Token") },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true
    )

    Button(
        enabled = !testing && url.isNotBlank() && token.isNotBlank(),
        onClick = onTest
    ) {
        Text(if (testing) "Testing..." else "Test Connection")
    }

    StatusRow(status, error)
}

@Composable
private fun WebSocketTab(
    wsEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Enable background HA triggers")
        Switch(
            checked = wsEnabled,
            onCheckedChange = onToggle
        )
    }

    Spacer(Modifier.height(8.dp))

    Text(
        "Keeps a persistent WebSocket connection to Home Assistant so Tasker profile events can trigger from HA events."
    )
    Text("It listens to state changes and your custom event channel.")
    Text("For reliability, you may need to set Battery usage to Unrestricted / allow background activity.")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoggingTab(
    generalLevel: LogLevel,
    wsLevel: LogLevel,
    onGeneralLevelChange: (LogLevel) -> Unit,
    onWsLevelChange: (LogLevel) -> Unit,
    onShare: (LogChannel?) -> Unit
) {
    Text("Log levels")
    LogLevelDropdown(
        label = "General log level",
        value = generalLevel,
        onChange = onGeneralLevelChange
    )
    LogLevelDropdown(
        label = "WebSocket log level",
        value = wsLevel,
        onChange = onWsLevelChange
    )

    Spacer(Modifier.height(12.dp))
    Text("Download / share logs")

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(onClick = { onShare(LogChannel.GENERAL) }) { Text("General") }
        Button(onClick = { onShare(LogChannel.WEBSOCKET) }) { Text("WebSocket") }
        Button(onClick = { onShare(null) }) { Text("All") }
    }
    Spacer(Modifier.height(12.dp))
    Text("Clear logs")

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(onClick = { CustomLogger.clear(LogChannel.GENERAL) }) { Text("General") }
        Button(onClick = { CustomLogger.clear(LogChannel.WEBSOCKET) }) { Text("Websocket") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogLevelDropdown(
    label: String,
    value: LogLevel,
    onChange: (LogLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { LogLevel.entries }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable, true),
            value = value.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { lvl ->
                DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)){
                            Text(lvl.name, modifier = Modifier.width(80.dp))
                            Text(lvl.label, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    onClick = {
                        expanded = false
                        onChange(lvl)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusRow(status: Status, error: String? = null) {
    val text = when (status) {
        Status.Idle -> ""
        Status.Testing -> "Testing connection..."
        Status.Success -> "✅ Connected successfully!"
        Status.Failed -> "❌ Failed to connect: " + (error ?: "Unknown error")
    }
    Text(text)
}

enum class Status {
    Idle, Testing, Success, Failed
}

private const val PREFS_NAME = "settings"
private const val KEY_BATTERY_DIALOG_SHOWN = "battery_dialog_shown"

fun hasSeenBatteryDialog(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_BATTERY_DIALOG_SHOWN, false)
}

fun setBatteryDialogShown(context: Context, value: Boolean = true) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putBoolean(KEY_BATTERY_DIALOG_SHOWN, value) }
}

fun openAppBatterySettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
