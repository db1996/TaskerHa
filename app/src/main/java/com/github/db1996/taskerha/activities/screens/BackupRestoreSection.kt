package com.github.db1996.taskerha.activities.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BackupRestoreSection(incomingUri: Uri? = null) {
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    // If the app was opened via a file intent, pop the restore dialog immediately.
    // Use `key` so this only fires once per incoming URI.
    LaunchedEffect(incomingUri) {
        if (incomingUri != null) showRestoreDialog = true
    }

    Text(
        "Backup & Restore",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Create backup", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Export instances, triggers and settings to a file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showBackupDialog = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("Create") }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Restore from backup", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Import instances, triggers and settings from a file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showRestoreDialog = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("Restore") }
            }
        }
    }

    if (showBackupDialog) {
        BackupDialog(onDismiss = { showBackupDialog = false })
    }
    if (showRestoreDialog) {
        RestoreDialog(
            preloadedUri = incomingUri,
            onDismiss = { showRestoreDialog = false }
        )
    }
}

@Composable
private fun BackupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("Create backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    placeholder = { Text("Leave empty for no encryption") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Leave empty to create an unencrypted backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Client certificates are stored in Android's KeyChain and cannot be included in the backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isCreating = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val json = BackupManager.createBackupJson(context)
                            BackupManager.writeAndShareBackup(
                                context,
                                json,
                                password.ifBlank { null }
                            )
                        }
                        onDismiss()
                    }
                },
                enabled = !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create & Share")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text("Cancel") }
        }
    )
}

private sealed class RestoreStatus {
    object Idle : RestoreStatus()
    object Loading : RestoreStatus()
    object Success : RestoreStatus()
    data class Err(val msg: String) : RestoreStatus()
}

@Composable
private fun RestoreDialog(
    preloadedUri: Uri? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<RestoreStatus>(RestoreStatus.Idle) }

    // Resolve the display name of a preloaded URI for the user to see
    val preloadedFileName = remember(preloadedUri) {
        if (preloadedUri == null) return@remember null
        context.contentResolver.query(
            preloadedUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: preloadedUri.lastPathSegment
    }

    fun doRestore(uri: Uri) {
        scope.launch {
            status = RestoreStatus.Loading
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.readBytes() }.getOrNull()
            }
            if (bytes == null) {
                status = RestoreStatus.Err("Could not read the selected file.")
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                BackupManager.restoreBackup(context, bytes, password.ifBlank { null })
            }
            status = when (result) {
                is BackupManager.RestoreResult.Success -> RestoreStatus.Success
                is BackupManager.RestoreResult.NeedsPassword ->
                    RestoreStatus.Err("This backup is encrypted. Enter the password and try again.")
                is BackupManager.RestoreResult.WrongPassword ->
                    RestoreStatus.Err("Incorrect password.")
                is BackupManager.RestoreResult.InvalidFormat ->
                    RestoreStatus.Err("Invalid backup file.")
                is BackupManager.RestoreResult.Error ->
                    RestoreStatus.Err(result.message)
            }
        }
    }

    // File picker — only shown when there is no preloaded URI
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) doRestore(uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore from backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (preloadedFileName != null) {
                    Text(
                        "File: $preloadedFileName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (if encrypted)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (preloadedUri != null) {
                    // URI came in via intent — show a "Restore" button directly
                    Button(
                        onClick = { doRestore(preloadedUri) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = status != RestoreStatus.Loading
                    ) { Text("Restore") }
                } else {
                    // Manual flow — let the user pick a file
                    Button(
                        onClick = { fileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = status != RestoreStatus.Loading
                    ) { Text("Select backup file") }
                }
                when (val s = status) {
                    is RestoreStatus.Idle -> {}
                    is RestoreStatus.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Restoring…", style = MaterialTheme.typography.bodySmall)
                    }
                    is RestoreStatus.Success -> Text(
                        "Restored successfully. Restart the app to apply all changes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                    is RestoreStatus.Err -> Text(
                        s.msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
