package com.github.db1996.taskerha.activities.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Surface
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
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
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
    var expanded by remember { mutableStateOf(false) }

    val hasId = form.triggerId != null
    val entityLabel = when {
        form.entityIds.isNotEmpty() -> form.entityIds.first()
        form.entityId.isNotBlank() -> form.entityId
        else -> "(any entity)"
    }
    val instanceName = remember(form.instanceId) {
        HaInstanceRepository.getById(form.instanceId)
            ?.name?.takeIf { it.isNotBlank() }
            ?: form.instanceId.takeIf { it.isNotBlank() }
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header row: entity label + instance chip + delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entityLabel, style = MaterialTheme.typography.titleSmall)
                    if (instanceName != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                instanceName,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
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

            // Footer: old-version warning / trigger ID + view details link
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
                    Text(
                        form.triggerId ?: "no id",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 4.dp, vertical = 0.dp
                    )
                ) {
                    Text(
                        if (expanded) "Hide details" else "View details →",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Expandable detail section
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider()
                    if (form.entityIds.size > 1) {
                        Text(
                            form.entityIds.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (form.configPerEntity && form.entityConfigs.isNotEmpty()) {
                        form.entityConfigs.forEach { cfg ->
                            Column(
                                modifier = Modifier.padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(cfg.entityId, style = MaterialTheme.typography.labelSmall)
                                if (cfg.targetAttribute.isNotBlank()) DataRow("Attribute", cfg.targetAttribute)
                                if (cfg.fromState.isNotBlank()) DataRow("From", cfg.fromState)
                                if (cfg.toState.isNotBlank()) DataRow("To", cfg.toState)
                                if (cfg.forDuration.isNotBlank()) DataRow("For", cfg.forDuration)
                            }
                        }
                    } else {
                        // v0 legacy: top-level fromState/toState/forDuration
                        // v1+: sharedConfig holds the data (top-level fields are empty)
                        val effectiveFrom = form.sharedConfig.fromState.ifBlank { form.fromState }
                        val effectiveTo = form.sharedConfig.toState.ifBlank { form.toState }
                        val effectiveFor = form.sharedConfig.forDuration.ifBlank { form.forDuration }
                        val effectiveAttr = form.sharedConfig.targetAttribute
                        if (effectiveAttr.isNotBlank()) DataRow("Attribute", effectiveAttr)
                        if (effectiveFrom.isNotBlank()) DataRow("From", effectiveFrom)
                        if (effectiveTo.isNotBlank()) DataRow("To", effectiveTo)
                        if (effectiveFor.isNotBlank()) DataRow("For", effectiveFor)
                        if (effectiveAttr.isBlank() && effectiveFrom.isBlank() &&
                            effectiveTo.isBlank() && effectiveFor.isBlank()) {
                            Text(
                                "No conditions — triggers on any state change",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
