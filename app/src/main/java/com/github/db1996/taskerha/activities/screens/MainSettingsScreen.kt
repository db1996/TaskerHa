package com.github.db1996.taskerha.activities.screens

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.tasker.ontriggerstate.triggerOnTriggerStateEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.content.edit

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(modifier: Modifier, setTopBar: (@Composable () -> Unit) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(HaSettings.loadUrl(context)) }
    var token by remember { mutableStateOf(HaSettings.loadToken(context)) }
    var wsEnabled by remember { mutableStateOf(HaSettings.loadWebSocketEnabled(context)) }

    var status by remember { mutableStateOf<Status>(Status.Idle) }
    var error by remember { mutableStateOf<String?>(null) }

    var testing by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var unsavedChanges by remember { mutableStateOf(false) }

    var showBatteryDialog by remember { mutableStateOf(!hasSeenBatteryDialog(context) && wsEnabled) }
    fun checkUnsavedChanges(){
        if(url != HaSettings.loadUrl(context) || token != HaSettings.loadToken(context)) {
            unsavedChanges = true
        }else{
            unsavedChanges = false
        }
    }

    fun setSaved(){
        saved = true
        checkUnsavedChanges()
        scope.launch {
            delay(1200)
            saved = false
        }
    }

    LaunchedEffect(Unit) {
        setTopBar {
            TopAppBar(
                title = { Text("HA Settings") },
                actions = {
                    Button(
                        enabled = unsavedChanges && !testing && url.isNotBlank() && token.isNotBlank(),
                        onClick = {
                            HaSettings.save(context, url.trim(), token.trim())
                            setSaved()
                        }) {

                        Text(if(saved)  "Saved!" else  "Save")
                    }
                }
            )
        }
    }

    val modifierFull = Modifier
        .fillMaxSize()
        .then(modifier)

    Column(
        modifier = modifierFull,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Input fields
        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it;
                checkUnsavedChanges()
            },
            label = { Text("Home Assistant URL") },
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

        Button(
            enabled = !testing && url.isNotBlank() && token.isNotBlank(),
            onClick = {
                testing = true
                status = Status.Testing
                val client = HomeAssistantClient(url, token)

                scope.launch {
                    val success = withContext(Dispatchers.IO) {
                        try {
                            client.ping()
                        } catch (e: Exception) {
                            false
                        }
                    }

                    status = if (success) Status.Success else Status.Failed
                    testing = false
                    error = client.error
                }
            }) {
            Text(if (testing) "Testing..." else "Test Connection")
        }

        StatusRow(status, error)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Enable background HA triggers")
            Switch(
                checked = wsEnabled,
                onCheckedChange = { enabled ->
                    wsEnabled = enabled
                    HaSettings.saveWebSocketEnabled(context, enabled)

                    if (enabled) {
                        if (!hasSeenBatteryDialog(context)) {
                            showBatteryDialog = true
                        }else{
                            HaWebSocketService.start(context)
                        }
                    } else {
                        HaWebSocketService.stop(context)
                    }
                }
            )
        }
        Text("This will enable a websocket server, only enable this if you intend to use profile events in tasker. This will listen to all state change triggers")
        Text("It will also request for battery optimization exclusion. Otherwise this could stop working unexpectedly")
        Text("Events will never be triggered if this is turned off. Regardless of profile activation in tasker")
    }

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
    Idle,
    Testing,
    Success,
    Failed
}
