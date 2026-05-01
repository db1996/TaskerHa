package com.github.db1996.taskerha.activities.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.util.PrefsJsonStore
import kotlinx.serialization.json.Json

@Composable
internal fun ActiveTriggersTab() {
    val context = LocalContext.current
    val rawItems by PrefsJsonStore.observe("TriggerStatePrefs").collectAsState()

    val jsonParser = remember { Json { ignoreUnknownKeys = true } }

    val triggers = remember(rawItems) {
        rawItems.mapNotNull { raw ->
            runCatching { jsonParser.decodeFromString<OnTriggerStateBuiltForm>(raw) to raw }
                .getOrNull()
        }
    }

    // Warning banner
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "If a Tasker profile is deleted, its trigger remains active here. Remove it manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "If a trigger is removed here while the Tasker profile still exists, it won't fire until the profile is resaved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }

    if (triggers.isEmpty()) {
        Text(
            "No active triggers",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp)
        )
        return
    }

    triggers.forEach { (form, rawJson) ->
        TriggerCard(
            form = form,
            onDelete = {
                PrefsJsonStore.remove("TriggerStatePrefs", rawJson)
                HaWebSocketService.resubscribeTriggers(context)
            }
        )
    }
}

@Composable
private fun TriggerCard(
    form: OnTriggerStateBuiltForm,
    onDelete: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    val hasId = form.triggerId != null
    val entityLabel = when {
        form.entityIds.isNotEmpty() -> form.entityIds.joinToString(", ")
        form.entityId.isNotBlank() -> form.entityId
        else -> "(any entity)"
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(entityLabel, style = MaterialTheme.typography.titleSmall)
                    if (form.fromState.isNotBlank()) DataRow("From", form.fromState)
                    if (form.toState.isNotBlank()) DataRow("To", form.toState)
                    if (form.forDuration.isNotBlank()) DataRow("For", form.forDuration)
                }
                IconButton(onClick = { showConfirm = true }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete trigger",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hasId) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "Old version — resave profile in Tasker, then delete this entry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Text(
                    form.triggerId ?: "no id",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Remove trigger?") },
            text = {
                Text(
                    "\"$entityLabel\" will be removed from active triggers. " +
                        "If the Tasker profile still exists, it won't fire until resaved."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showConfirm = false; onDelete() }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
