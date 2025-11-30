package com.github.db1996.taskerha.activities.partials

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

@Composable
fun HaSetupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(HaSettings.loadUrl(context)) }
    var token by remember { mutableStateOf(HaSettings.loadToken(context)) }

    var status by remember { mutableStateOf<Status>(Status.Idle) }
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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp, 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Home Assistant Setup", style = MaterialTheme.typography.headlineSmall)

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

        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                enabled = unsavedChanges && !testing && url.isNotBlank() && token.isNotBlank(),
                onClick = {
                    HaSettings.save(context, url.trim(), token.trim())
                    saved = true
                    checkUnsavedChanges()
                    // set saved back to false after 5 seconds
                    scope.launch {
                        delay(1200)
                        saved = false
                    }
                }) {

                Text(if(saved)  "Saved!" else  "Save")
            }

            Button(
                enabled = !testing && url.isNotBlank() && token.isNotBlank(),
                onClick = {
                    testing = true
                    status = Status.Testing

                    // Persist settings
                    HaSettings.save(context, url.trim(), token.trim())

                    scope.launch {
                        val success = withContext(Dispatchers.IO) {
                            try {
                                HomeAssistantClient(url, token).ping()
                            } catch (e: Exception) {
                                false
                            }
                        }

                        status = if (success) Status.Success else Status.Failed
                        testing = false
                    }
                }) {
                Text(if (testing) "Testing..." else "Test Connection")
            }
        }


        StatusRow(status)
    }
}

@Composable
private fun StatusRow(status: Status) {
    val text = when (status) {
        Status.Idle -> ""
        Status.Testing -> "Testing connection..."
        Status.Success -> "✅ Connected successfully!"
        Status.Failed -> "❌ Failed to connect"
    }

    Text(text)
}

enum class Status {
    Idle,
    Testing,
    Success,
    Failed
}
