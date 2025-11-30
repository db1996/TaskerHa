package com.github.db1996.taskerha.activities.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(modifier: Modifier, setTopBar: (@Composable () -> Unit) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(HaSettings.loadUrl(context)) }
    var token by remember { mutableStateOf(HaSettings.loadToken(context)) }

    var status by remember { mutableStateOf<Status>(Status.Idle) }
    var error by remember { mutableStateOf<String?>(null) }

    var testing by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var unsavedChanges by remember { mutableStateOf(false) }

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
        // set saved back to false after 1.2s
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
    Idle,
    Testing,
    Success,
    Failed
}
