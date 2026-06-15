package com.github.db1996.taskerha.activities.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.security.KeyChain
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.github.db1996.taskerha.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.github.db1996.taskerha.service.WsConnectionState
import com.github.db1996.taskerha.util.HaHttpClientFactory
import com.github.db1996.taskerha.util.NetworkHelper
import com.github.db1996.taskerha.util.hasNotificationPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsTab(val label: String) {
    INSTANCES("Instances"),
    TRIGGERS("Triggers"),
    OPTIONS("Options"),
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
    // WS enable flow state
    var notificationGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
    var pendingWsInstance by remember { mutableStateOf<HaInstance?>(null) }
    var showWsPopup by remember { mutableStateOf<HaInstance?>(null) }
    // oldInstance to newInstance when switching WS between instances
    var showSwitchConfirm by remember { mutableStateOf<Pair<HaInstance, HaInstance>?>(null) }

    fun applyWsEnable(instance: HaInstance) {
        instances.filter { it.wsEnabled && it.id != instance.id }.forEach { old ->
            HaInstanceRepository.update(old.copy(wsEnabled = false))
        }
        HaInstanceRepository.update(instance.copy(wsEnabled = true))
        HaSettings.saveWebSocketEnabled(context, true)
        HaWebSocketService.start(context)
        HaInstanceRepository.setActive(instance.id)
    }

    fun disableWs(instance: HaInstance) {
        HaInstanceRepository.update(instance.copy(wsEnabled = false))
        HaSettings.saveWebSocketEnabled(context, false)
        HaWebSocketService.stop(context)
    }

    fun proceedEnableWs(instance: HaInstance) {
        val otherActive = instances.firstOrNull { it.wsEnabled && it.id != instance.id }
        if (otherActive != null) {
            showSwitchConfirm = otherActive to instance
        } else if (!hasWsPopupDismissed(context)) {
            showWsPopup = instance
        } else {
            applyWsEnable(instance)
        }
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            notificationGranted = granted
            pendingWsInstance?.let { inst ->
                if (granted) proceedEnableWs(inst)
                // if denied, showWsPopup stays null; popup is not shown
            }
            pendingWsInstance = null
        }

    // HACS companion check state for main screen cards
    var checkingHacsInstanceId by remember { mutableStateOf<String?>(null) }

    fun checkHacsForInstance(instance: HaInstance) {
        if (checkingHacsInstanceId != null) return
        checkingHacsInstanceId = instance.id
        val httpClient = HaHttpClientFactory.build(
            context,
            clientCertEnabled = instance.clientCertEnabled,
            clientCertAlias = instance.clientCertAlias
        )
        val client = HomeAssistantClient(instance.remoteUrl.trim(), instance.token.trim(), httpClient)
        scope.launch {
            val services = withContext(Dispatchers.IO) {
                try {
                    client.ping()
                    client.getServices()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val hacsAvailable = services.any { it.domain == "taskerha_companion" }
            HaInstanceRepository.update(instance.copy(hacsAvailable = hacsAvailable, hacsChecked = true))
            checkingHacsInstanceId = null
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
                    checkingHacsInstanceId = checkingHacsInstanceId,
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
                        if (instance.wsEnabled) {
                            HaSettings.saveWebSocketEnabled(context, false)
                            HaWebSocketService.stop(context)
                        }
                        HaInstanceRepository.delete(instance.id)
                    },
                    onSetDefault = { instance ->
                        HaInstanceRepository.setDefault(instance.id)
                    },
                    onToggleWs = { instance, enabled ->
                        if (enabled) {
                            if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                pendingWsInstance = instance
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                proceedEnableWs(instance)
                            }
                        } else {
                            disableWs(instance)
                        }
                    },
                    onCheckHacs = { instance -> checkHacsForInstance(instance) }
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

                SettingsTab.OPTIONS -> OptionsTab()

                SettingsTab.TRIGGERS -> ActiveTriggersTab()
            }
        }
    }

    // WS first-enable info popup
    showWsPopup?.let { instance ->
        WsInfoDialog(
            notificationGranted = notificationGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
            onRequestPermission = {
                pendingWsInstance = instance
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onEnable = { neverShowAgain ->
                if (neverShowAgain) setWsPopupDismissed(context)
                showWsPopup = null
                applyWsEnable(instance)
            },
            onDismiss = { showWsPopup = null }
        )
    }

    // Switch WS between instances confirmation
    showSwitchConfirm?.let { (oldInstance, newInstance) ->
        val oldName = oldInstance.name.takeIf { it.isNotBlank() } ?: oldInstance.remoteUrl
        val newName = newInstance.name.takeIf { it.isNotBlank() } ?: newInstance.remoteUrl
        AlertDialog(
            onDismissRequest = { showSwitchConfirm = null },
            title = { Text("Switch WebSocket instance?") },
            text = {
                Text(
                    "WebSocket triggers are currently active for \"$oldName\". " +
                        "Switch to \"$newName\"? The connection will reconnect to the new instance."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSwitchConfirm = null
                        applyWsEnable(newInstance)
                    }
                ) { Text("Switch") }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchConfirm = null }) { Text("Cancel") }
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

}

@Composable
private fun InstancesTab(
    instances: List<HaInstance>,
    activeInstanceId: String?,
    checkingHacsInstanceId: String?,
    onAddInstance: () -> Unit,
    onEditInstance: (HaInstance) -> Unit,
    onDeleteInstance: (HaInstance) -> Unit,
    onSetDefault: (HaInstance) -> Unit,
    onToggleWs: (HaInstance, Boolean) -> Unit,
    onCheckHacs: (HaInstance) -> Unit,
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
                    isCheckingHacs = checkingHacsInstanceId == instance.id,
                    onEdit = { onEditInstance(instance) },
                    onDelete = { onDeleteInstance(instance) },
                    onSetDefault = { onSetDefault(instance) },
                    onToggleWs = { enabled -> onToggleWs(instance, enabled) },
                    onCheckHacs = { onCheckHacs(instance) }
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
    isCheckingHacs: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    onToggleWs: (Boolean) -> Unit,
    onCheckHacs: () -> Unit,
) {
    val wsState by HaWebSocketService.connectionState.collectAsState()
    val wsStatus = when {
        !instance.wsEnabled || !isActive -> "Off"
        wsState == WsConnectionState.CONNECTED -> "Connected"
        wsState == WsConnectionState.CONNECTING -> "Connecting..."
        wsState == WsConnectionState.FAILED -> "Failed"
        else -> "Connecting..."
    }
    val isConnected = instance.wsEnabled && isActive &&
        wsState == WsConnectionState.CONNECTED
    val dotColor = when {
        !instance.wsEnabled || !isActive -> MaterialTheme.colorScheme.outline
        wsState == WsConnectionState.CONNECTED -> Color(0xFF4CAF50)
        wsState == WsConnectionState.CONNECTING -> Color(0xFFFFA000)
        wsState == WsConnectionState.FAILED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: dot + name + chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(color = dotColor)
                }
                if (instance.name.isNotBlank()) {
                    Text(instance.name, style = MaterialTheme.typography.titleMedium)
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

            // URLs
            Text(
                instance.remoteUrl.takeIf { it.isNotBlank() } ?: "(No URL)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (instance.localUrl.isNotBlank()) {
                Text(
                    "Local: ${instance.localUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // HACS companion badge
            when {
                instance.hacsAvailable -> Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            "TaskerHA Companion",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                else -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color(0xFFFFA000).copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFFFFA000)
                            )
                            Text(
                                if (!instance.hacsChecked) "Companion not checked"
                                else "Companion not found",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFA000)
                            )
                        }
                    }
                    if (isCheckingHacs) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFA000)
                        )
                    } else {
                        IconButton(
                            onClick = onCheckHacs,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "Check companion",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFFA000)
                            )
                        }
                    }
                }
            }

            // WebSocket row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "WebSocket triggers",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        wsStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isConnected -> MaterialTheme.colorScheme.primary
                            instance.wsEnabled && isActive &&
                                wsState == WsConnectionState.FAILED ->
                                    MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Switch(
                        checked = instance.wsEnabled,
                        onCheckedChange = onToggleWs
                    )
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onSetDefault,
                    enabled = !instance.isDefault,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Set Default")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete instance",
                        tint = MaterialTheme.colorScheme.error
                    )
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
    var localUrlEnabled by remember { mutableStateOf(instance.localUrl.isNotBlank() || instance.homeSsids.isNotEmpty()) }
    var clientCertEnabled by remember { mutableStateOf(instance.clientCertEnabled) }
    var clientCertAlias by remember { mutableStateOf(instance.clientCertAlias) }
    var hacsAvailable by remember { mutableStateOf(instance.hacsAvailable) }
    var hacsChecked by remember { mutableStateOf(instance.hacsChecked) }
    var hacsChecking by remember { mutableStateOf(false) }

    var status by remember { mutableStateOf(Status.Idle) }
    var error by remember { mutableStateOf<String?>(null) }
    var testingLabel by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun checkHacs() {
        hacsChecking = true
        val httpClient = HaHttpClientFactory.build(
            context,
            clientCertEnabled = clientCertEnabled,
            clientCertAlias = clientCertAlias
        )
        val client = HomeAssistantClient(remoteUrl.trim(), token.trim(), httpClient)
        scope.launch {
            val services = withContext(Dispatchers.IO) {
                try {
                    client.ping()
                    client.getServices()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            hacsAvailable = services.any { it.domain == "taskerha_companion" }
            hacsChecked = true
            hacsChecking = false
        }
    }

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
                    enabled = localUrlEnabled,
                    localUrl = localUrl,
                    homeSsids = homeSsids,
                    onEnabledChange = { enabled ->
                        localUrlEnabled = enabled
                        if (!enabled) {
                            localUrl = ""
                            homeSsids = emptySet()
                        }
                    },
                    onLocalUrlChange = { localUrl = it },
                    onHomeSsidsChange = { homeSsids = it }
                )

                val canTestLocal = testingLabel == null && localUrl.isNotBlank() && token.isNotBlank()
                if (localUrlEnabled && localUrl.isNotBlank()) {
                    Button(
                        enabled = canTestLocal,
                        onClick = { runTest("Local", localUrl) }
                    ) {
                        Text(if (testingLabel == "Local") "Testing..." else "Test Local")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TaskerHA Companion", style = MaterialTheme.typography.bodyMedium)
                        val hacsLabel = when {
                            hacsChecking -> "Checking..."
                            !hacsChecked -> "Not checked"
                            hacsAvailable -> "Integration found"
                            else -> "Integration not found"
                        }
                        val hacsColor = if (!hacsChecking && hacsChecked && hacsAvailable) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        Text(
                            hacsLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = hacsColor
                        )
                    }
                    IconButton(
                        onClick = { checkHacs() },
                        enabled = !hacsChecking && remoteUrl.isNotBlank() && token.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh HACS check")
                    }
                    IconButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                "https://github.com/db1996/taskerha-hacs".toUri()
                            )
                            context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = "About TaskerHA Companion")
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
                            clientCertAlias = clientCertAlias,
                            hacsAvailable = hacsAvailable,
                            hacsChecked = hacsChecked
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
private fun WsInfoDialog(
    notificationGranted: Boolean,
    onRequestPermission: () -> Unit,
    onEnable: (neverShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var neverShow by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Background HA triggers") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Keeps a persistent WebSocket connection to Home Assistant so Tasker " +
                        "profiles can fire from HA state changes and custom events."
                )
                Text(
                    "For more details, see the TaskerHA documentation on GitHub.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Text("Notification access", style = MaterialTheme.typography.titleSmall)
                if (notificationGranted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Notification access granted",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Required to show the background service notification",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedButton(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant notification access")
                    }
                }

                HorizontalDivider()

                Text("Battery optimization", style = MaterialTheme.typography.titleSmall)
                Text(
                    "For reliable triggers, set Battery usage to Unrestricted in Android settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { openAppBatterySettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open battery settings")
                }

                HorizontalDivider()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = neverShow, onCheckedChange = { neverShow = it })
                    Text("Don't show this again", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!notificationGranted) {
                        Toast.makeText(context, "Grant notification access first", Toast.LENGTH_SHORT).show()
                    } else {
                        onEnable(neverShow)
                    }
                }
            ) { Text("Enable") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun OptionsTab() {
    val context = LocalContext.current

    // Notification permission
    var hasNotifPermission by remember { mutableStateOf(hasNotificationPermission(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifPermission = granted
    }
    var showDisableNotifDialog by remember { mutableStateOf(false) }

    val instances by HaInstanceRepository.instances.collectAsState()

    // Battery optimization — refresh status on every resume so the card updates after returning from settings
    val pm = context.getSystemService(PowerManager::class.java)
    var ignoringBattery by remember {
        mutableStateOf(pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ignoringBattery = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
                hasNotifPermission = hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var showBatteryDialog by remember { mutableStateOf(false) }


    // Request timeout — track raw text so the field is editable mid-type
    var timeoutText by remember { mutableStateOf(HaSettings.loadRequestTimeout(context).toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "General",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Notification permission card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notifications", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (hasNotifPermission) "Permission granted" else "Permission not granted",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasNotifPermission) Color(0xFF4CAF50) else Color(0xFFFFA000)
                    )
                }
                Switch(
                    checked = hasNotifPermission,
                    onCheckedChange = { on ->
                        if (on) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            showDisableNotifDialog = true
                        }
                    }
                )
            }
        }

        // Battery optimization card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Battery optimization", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (ignoringBattery) "Unrestricted" else "Restricted",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ignoringBattery) Color(0xFF4CAF50) else Color(0xFFFFA000)
                        )
                    }
                    IconButton(onClick = { showBatteryDialog = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "Info")
                    }
                }
                OutlinedButton(
                    onClick = { openAppBatterySettings(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open settings")
                }
            }
        }

        // Request timeout card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Request timeout", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = timeoutText,
                    onValueChange = { raw ->
                        // Allow only digits, cap at 4 chars to prevent absurd values
                        val filtered = raw.filter { it.isDigit() }.take(4)
                        timeoutText = filtered
                        val v = filtered.toIntOrNull()
                        if (v != null && v >= 1) {
                            HaSettings.saveRequestTimeout(context, v)
                        }
                    },
                    label = { Text("Seconds") },
                    suffix = { Text("s") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Time a single request has to reach Home Assistant, set this higher if you have a slower connection. You can set it lower if you have fast connections but this could result in more reconnections of the websocket",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Applies to all Home Assistant connections. Default: 5s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // About section
        Text(
            "About",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AboutLinkRow(
                    label = "Repository",
                    url = "https://github.com/db1996/TaskerHa"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                AboutLinkRow(
                    label = "Documentation",
                    url = "https://taskerha.db1996-gh.com/"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                AboutLinkRow(
                    label = "Report bug or feature request",
                    url = "https://github.com/db1996/TaskerHa/issues"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                AboutLinkRow(
                    label = "Install companion HACS integration",
                    url = "https://github.com/db1996/taskerha-hacs"
                )
            }
        }

        // Buy Me a Coffee button
        Button(
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, "https://buymeacoffee.com/db1996".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDD00)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "☕  Buy me a coffee",
                color = Color(0xFF000000),
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showDisableNotifDialog) {
        AlertDialog(
            onDismissRequest = { showDisableNotifDialog = false },
            title = { Text("Disable notifications?") },
            text = {
                Text(
                    "Notification permission is required for the WebSocket background service. " +
                    "Disabling it will stop WebSocket triggers.\n\n" +
                    "You will be taken to app settings to revoke the permission."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisableNotifDialog = false
                    // Disable WS for all instances
                    instances.filter { it.wsEnabled }.forEach { inst ->
                        HaInstanceRepository.update(inst.copy(wsEnabled = false))
                    }
                    HaSettings.saveWebSocketEnabled(context, false)
                    HaWebSocketService.stop(context)
                    // Open notification settings so user can revoke the permission
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableNotifDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = { Text("Battery optimization") },
            text = {
                Text(
                    "Android may restrict background services to save battery. " +
                    "For reliable WebSocket triggers, set TaskerHA to Unrestricted in battery settings.\n\n" +
                    "Current status: ${if (ignoringBattery) "Unrestricted ✓" else "Restricted"}"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    openAppBatterySettings(context)
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) { Text("Close") }
            }
        )
    }
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
private const val KEY_WS_POPUP_DISMISSED = "ws_popup_dismissed"

fun hasWsPopupDismissed(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_WS_POPUP_DISMISSED, false)
}

fun setWsPopupDismissed(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putBoolean(KEY_WS_POPUP_DISMISSED, true) }
}

@Composable
private fun AboutLinkRow(label: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, url.toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_github),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Rounded.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun openAppBatterySettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
