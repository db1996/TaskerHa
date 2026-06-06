package com.github.db1996.taskerha.activities.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.security.KeyChain
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.datamodels.HaInstance
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.logging.LogChannel
import com.github.db1996.taskerha.logging.LogLevel
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.util.HaHttpClientFactory
import com.github.db1996.taskerha.util.NetworkHelper
import com.github.db1996.taskerha.util.hasNotificationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsTab(val label: String) {
    INSTANCES("Instances"),
    WEBSOCKET("WebSocket"),
    TRIGGERS("Triggers"),
    LOGGING("Logging"),
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

    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.INSTANCES) }

    // Observe instances from repository
    val instances by HaInstanceRepository.instances.collectAsState()
    val activeInstanceId by HaInstanceRepository.activeInstanceId.collectAsState()

    // Instance editor state
    var editingInstance by remember { mutableStateOf<HaInstance?>(null) }
    var showActivateConfirmation by remember { mutableStateOf<HaInstance?>(null) }

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

    // Simplified top bar - no save button needed for instances tab
    LaunchedEffect(selectedTab) {
        setTopBar {
            TopAppBar(
                title = { Text("TaskerHA Settings") }
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
    ) { innerPadding ->
        // INSTANCES tab uses LazyColumn which handles its own scrolling
        // Other tabs need parent scroll for their static content
        val needsScroll = selectedTab != SettingsTab.INSTANCES
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(
                    if (needsScroll) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                SettingsTab.INSTANCES -> InstancesTab(
                    instances = instances,
                    activeInstanceId = activeInstanceId,
                    onAddInstance = {
                        editingInstance = HaInstance(
                            id = HaInstance.generateShortId(),
                            name = "",
                            remoteUrl = "",
                            token = "",
                            isDefault = instances.isEmpty()
                        )
                    },
                    onEditInstance = { instance ->
                        editingInstance = instance
                    },
                    onDeleteInstance = { instance ->
                        HaInstanceRepository.delete(instance.id)
                    },
                    onSetDefault = { instance ->
                        HaInstanceRepository.setDefault(instance.id)
                    },
                    onActivateInstance = { instance ->
                        if (wsEnabled && activeInstanceId != instance.id) {
                            showActivateConfirmation = instance
                        } else {
                            HaInstanceRepository.setActive(instance.id)
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

                SettingsTab.TRIGGERS -> ActiveTriggersTab()
            }
        }
    }

    // Battery dialog stays global because it can be triggered from the websocket tab
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { },
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
                        wsEnabled = false
                        HaSettings.saveWebSocketEnabled(context, false)
                        HaWebSocketService.stop(context)
                    }
                ) { Text("Not now") }
            }
        )
    }

    // Instance editor dialog
    editingInstance?.let { instance ->
        InstanceEditorDialog(
            instance = instance,
            onDismiss = { editingInstance = null },
            onSave = { updatedInstance ->
                if (instances.any { it.id == updatedInstance.id }) {
                    HaInstanceRepository.update(updatedInstance)
                } else {
                    HaInstanceRepository.add(updatedInstance)
                }
                editingInstance = null
            }
        )
    }

    // Activation confirmation dialog
    showActivateConfirmation?.let { instance ->
        AlertDialog(
            onDismissRequest = { showActivateConfirmation = null },
            title = { Text("Switch Active Instance?") },
            text = {
                Text(
                    "Switching the active instance will change which Home Assistant triggers are active. " +
                            "The websocket connection will reconnect to:\n\n" +
                            (instance.name.takeIf { it.isNotBlank() } ?: instance.remoteUrl)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HaInstanceRepository.setActive(instance.id)
                        HaWebSocketService.stop(context)
                        HaWebSocketService.start(context)
                        showActivateConfirmation = null
                    }
                ) { Text("Switch") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showActivateConfirmation = null }
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun InstancesTab(
    instances: List<HaInstance>,
    activeInstanceId: String?,
    onAddInstance: () -> Unit,
    onEditInstance: (HaInstance) -> Unit,
    onDeleteInstance: (HaInstance) -> Unit,
    onSetDefault: (HaInstance) -> Unit,
    onActivateInstance: (HaInstance) -> Unit
) {
    val sortedInstances = HaInstanceRepository.getAllSorted()
    
    if (instances.isEmpty()) {
        // Empty state
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "No instances configured",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Add your first Home Assistant instance to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddInstance) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Instance")
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sortedInstances, key = { it.id }) { instance ->
                InstanceCard(
                    instance = instance,
                    isActive = instance.id == activeInstanceId,
                    onEdit = { onEditInstance(instance) },
                    onDelete = { onDeleteInstance(instance) },
                    onSetDefault = { onSetDefault(instance) },
                    onActivate = { onActivateInstance(instance) }
                )
            }
        }

        // FAB for adding instances
        FloatingActionButton(
            onClick = onAddInstance,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add instance")
        }
    }
}

@Composable
private fun InstanceCard(
    instance: HaInstance,
    isActive: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onActivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and badges
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (instance.name.isNotBlank()) {
                        Text(
                            instance.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (instance.isDefault) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Default",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (isActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Active",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Text(
                    instance.remoteUrl.takeIf { it.isNotBlank() } ?: "(No URL)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (instance.localUrl.isNotBlank()) {
                    Text(
                        "Local: ${instance.localUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // First row of buttons: Edit and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete")
                }
            }

            // Second row of buttons: Set Default and Activate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSetDefault,
                    enabled = !instance.isDefault,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Set Default")
                }
                OutlinedButton(
                    onClick = onActivate,
                    enabled = !isActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Activate")
                }
            }
        }
    }
}

@Composable
private fun InstanceEditorDialog(
    instance: HaInstance,
    onDismiss: () -> Unit,
    onSave: (HaInstance) -> Unit
) {
    var name by remember { mutableStateOf(instance.name) }
    var remoteUrl by remember { mutableStateOf(instance.remoteUrl) }
    var localUrl by remember { mutableStateOf(instance.localUrl) }
    var token by remember { mutableStateOf(instance.token) }
    var homeSsids by remember { mutableStateOf(instance.homeSsids) }
    var clientCertEnabled by remember { mutableStateOf(instance.clientCertEnabled) }
    var clientCertAlias by remember { mutableStateOf(instance.clientCertAlias) }
    
    var status by remember { mutableStateOf(Status.Idle) }
    var error by remember { mutableStateOf<String?>(null) }
    var testingLabel by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    fun runTest(label: String, targetUrl: String) {
        testingLabel = label
        status = Status.Testing
        val httpClient = HaHttpClientFactory.build(
            context,
            clientCertEnabled = clientCertEnabled,
            clientCertAlias = clientCertAlias
        )
        val client = HomeAssistantClient(targetUrl.trim(), token.trim(), httpClient)

        scope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    client.ping()
                } catch (_: Exception) {
                    false
                }
            }
            status = if (success) Status.Success else Status.Failed
            testingLabel = null
            error = client.error
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (instance.remoteUrl.isBlank()) "Add Instance" else "Edit Instance")
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    placeholder = { Text("Home, Office, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = remoteUrl,
                    onValueChange = { remoteUrl = it },
                    label = { Text("Remote URL") },
                    placeholder = { Text("https://ha.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Access Token") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )

                ClientCertSection(
                    enabled = clientCertEnabled,
                    alias = clientCertAlias,
                    onEnabledChange = { clientCertEnabled = it },
                    onAliasChange = { clientCertAlias = it }
                )

                val canTestRemote = testingLabel == null && remoteUrl.isNotBlank() && token.isNotBlank()
                Button(
                    enabled = canTestRemote,
                    onClick = { runTest("Remote", remoteUrl) }
                ) {
                    Text(if (testingLabel == "Remote") "Testing..." else "Test Connection")
                }
                StatusRow(status, error)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LocalUrlSection(
                    enabled = localUrl.isNotBlank() || homeSsids.isNotEmpty(),
                    localUrl = localUrl,
                    homeSsids = homeSsids,
                    onEnabledChange = { enabled ->
                        if (!enabled) {
                            localUrl = ""
                            homeSsids = emptySet()
                        }
                    },
                    onLocalUrlChange = { localUrl = it },
                    onHomeSsidsChange = { homeSsids = it }
                )

                val canTestLocal = testingLabel == null && localUrl.isNotBlank() && token.isNotBlank()
                if (localUrl.isNotBlank()) {
                    Button(
                        enabled = canTestLocal,
                        onClick = { runTest("Local", localUrl) }
                    ) {
                        Text(if (testingLabel == "Local") "Testing..." else "Test Local")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = remoteUrl.isNotBlank() && token.isNotBlank(),
                onClick = {
                    onSave(
                        instance.copy(
                            name = name.trim(),
                            remoteUrl = remoteUrl.trim(),
                            localUrl = localUrl.trim(),
                            token = token.trim(),
                            homeSsids = homeSsids,
                            clientCertEnabled = clientCertEnabled,
                            clientCertAlias = clientCertAlias
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ConnectionTab(
    url: String,
    token: String,
    testing: Boolean,
    testingLabel: String?,
    lastTestedLabel: String?,
    status: Status,
    error: String?,
    localUrlEnabled: Boolean,
    localUrl: String,
    homeSsids: Set<String>,
    clientCertEnabled: Boolean,
    clientCertAlias: String,
    onUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onLocalUrlEnabledChange: (Boolean) -> Unit,
    onLocalUrlChange: (String) -> Unit,
    onHomeSsidsChange: (Set<String>) -> Unit,
    onClientCertEnabledChange: (Boolean) -> Unit,
    onClientCertAliasChange: (String) -> Unit,
    onTestRemote: () -> Unit,
    onTestLocal: () -> Unit
) {
    OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        label = { Text("Remote Home Assistant URL") },
        placeholder = { Text("https://ha.example.com") },
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

    // --- Client certificate section (applies to the remote URL) ---
    ClientCertSection(
        enabled = clientCertEnabled,
        alias = clientCertAlias,
        onEnabledChange = onClientCertEnabledChange,
        onAliasChange = onClientCertAliasChange
    )

    val canTestRemote = !testing && url.isNotBlank() && token.isNotBlank()
    val canTestLocal = !testing && localUrlEnabled && localUrl.isNotBlank() && token.isNotBlank()

    Button(
        enabled = canTestRemote,
        onClick = onTestRemote
    ) {
        Text(
            when {
                testingLabel == "Remote" -> "Testing..."
                localUrlEnabled -> "Test remote"
                else -> "Test Connection"
            }
        )
    }

    // Show the last test outcome only for the remote URL here.
    if (lastTestedLabel == "Remote") StatusRow(status, error)

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    // --- Local URL section ---
    LocalUrlSection(
        enabled = localUrlEnabled,
        localUrl = localUrl,
        homeSsids = homeSsids,
        onEnabledChange = onLocalUrlEnabledChange,
        onLocalUrlChange = onLocalUrlChange,
        onHomeSsidsChange = onHomeSsidsChange
    )

    if (localUrlEnabled) {
        Button(
            enabled = canTestLocal,
            onClick = onTestLocal
        ) {
            Text(if (testingLabel == "Local") "Testing..." else "Test local")
        }
        if (lastTestedLabel == "Local") StatusRow(status, error)
    }
}

@Composable
private fun LocalUrlSection(
    enabled: Boolean,
    localUrl: String,
    homeSsids: Set<String>,
    onEnabledChange: (Boolean) -> Unit,
    onLocalUrlChange: (String) -> Unit,
    onHomeSsidsChange: (Set<String>) -> Unit
) {
    val context = LocalContext.current

    // Location permission launcher for reading SSID
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission just granted: start monitoring so the SSID is
            // populated by the time the pending action runs.
            NetworkHelper.startMonitoring(context)
            pendingAction?.invoke()
        } else {
            Toast.makeText(
                context,
                "Precise location permission is required to detect the home WiFi network name. " +
                "Please grant 'Precise' (not 'Approximate') location.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun withLocationPermission(action: () -> Unit) {
        if (NetworkHelper.hasLocationPermission(context)) {
            // Make sure the long-lived monitoring is running so the cached
            // SSID is available when the user presses "Add current WiFi".
            NetworkHelper.startMonitoring(context)
            action()
        } else {
            pendingAction = action
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("URL when at home")
        Switch(
            checked = enabled,
            onCheckedChange = { newValue ->
                if (newValue) {
                    withLocationPermission { onEnabledChange(true) }
                } else {
                    onEnabledChange(false)
                }
            }
        )
    }

    if (!enabled) {
        Text(
            "Enable to use a different URL when connected to your home WiFi.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    OutlinedTextField(
        value = localUrl,
        onValueChange = onLocalUrlChange,
        label = { Text("Local Home Assistant URL") },
        placeholder = { Text("http://192.168.1.x:8123") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(Modifier.height(8.dp))

    Text("Home WiFi networks", style = MaterialTheme.typography.titleSmall)
    Text(
        "When connected to one of these networks, the local URL will be used.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(4.dp))

    // List of configured SSIDs
    homeSsids.forEach { ssid ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(ssid)
            }
            IconButton(onClick = { onHomeSsidsChange(homeSsids - ssid) }) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove $ssid",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // "Add current WiFi" button
    val scope = rememberCoroutineScope()
    var detecting by remember { mutableStateOf(false) }
    OutlinedButton(
        enabled = !detecting,
        onClick = {
            withLocationPermission {
                scope.launch {
                    // The long-lived NetworkCallback may not have fired yet
                    // (especially if monitoring just started). Poll the cache
                    // for up to ~2 seconds before giving up.
                    var currentSsid = NetworkHelper.getCurrentSsid()
                    var attempts = 0
                    while (currentSsid == null && attempts < 20) {
                        delay(100)
                        currentSsid = NetworkHelper.getCurrentSsid()
                        attempts++
                    }
                    if (currentSsid != null) {
                        onHomeSsidsChange(homeSsids + currentSsid)
                    } else {
                        Toast.makeText(
                            context,
                            "Could not detect current WiFi network. Make sure you are connected to WiFi and that location services are enabled.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (detecting) "Detecting..." else "Add current WiFi")
    }
}

@Composable
private fun ClientCertSection(
    enabled: Boolean,
    alias: String,
    onEnabledChange: (Boolean) -> Unit,
    onAliasChange: (String) -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Use client certificate")
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
    }

    if (!enabled) {
        Text(
            "Enable to authenticate to your remote URL with a client certificate (mTLS) " +
                    "stored in the Android keystore.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("Selected alias", style = MaterialTheme.typography.titleSmall)
            Text(
                alias.ifBlank { "Not selected" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (alias.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
            )
        }
        if (alias.isNotBlank()) {
            IconButton(onClick = { onAliasChange("") }) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear alias",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    OutlinedButton(
        onClick = {
            val activity = findActivity(context)
            if (activity == null) {
                Toast.makeText(
                    context,
                    "Cannot open certificate picker — host activity not found",
                    Toast.LENGTH_LONG
                ).show()
                return@OutlinedButton
            }
            // Permission to read the chosen private key is granted to this app once
            // the user picks. Subsequent KeyChain.getPrivateKey() calls will not
            // prompt — important for background tasks while the device is locked.
            KeyChain.choosePrivateKeyAlias(
                activity,
                { chosen ->
                    Handler(Looper.getMainLooper()).post {
                        if (chosen != null) onAliasChange(chosen)
                    }
                },
                arrayOf("RSA", "EC"),
                null,
                null,
                -1,
                alias.ifBlank { null }
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (alias.isBlank()) "Choose certificate" else "Change certificate")
    }

    Text(
        "The certificate must be installed in Android (Settings → Security → Encryption " +
                "& credentials → Install a certificate → VPN & app user certificate). " +
                "Don't forget to save after picking.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun findActivity(context: Context): Activity? {
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
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
        onExpandedChange = { !expanded }
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
            onDismissRequest = { }
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
        Status.Failed -> "❌ Failed to connect: " +
                (error?.takeIf { it.isNotBlank() } ?: "Unknown error")
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
