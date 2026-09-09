package com.github.db1996.taskerha.activities.partials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.datamodels.HaRegistryData

/**
 * Compact summary of the targets chosen for a service call: a chip per selected
 * entity / device / area / label (tap a chip to remove it) plus an "Edit targets"
 * chip that opens the full-screen [TargetPickerScreen] via [onEditTargets].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetSection(
    entityIds: List<String>,
    deviceIds: List<String>,
    areaIds: List<String>,
    labelIds: List<String>,
    registryData: HaRegistryData?,
    onEntityIdsChange: (List<String>) -> Unit,
    onDeviceIdsChange: (List<String>) -> Unit,
    onAreaIdsChange: (List<String>) -> Unit,
    onLabelIdsChange: (List<String>) -> Unit,
    onEditTargets: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Targets", style = MaterialTheme.typography.labelLarge)

        val hasAny = entityIds.isNotEmpty() || deviceIds.isNotEmpty() ||
            areaIds.isNotEmpty() || labelIds.isNotEmpty()

        if (hasAny) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entityIds.forEach { id ->
                    TargetChip(text = id, onRemove = { onEntityIdsChange(entityIds - id) })
                }
                deviceIds.forEach { id ->
                    val name = registryData?.service_response?.devices?.find { it.id == id }?.name ?: id
                    TargetChip(text = "Device: $name", onRemove = { onDeviceIdsChange(deviceIds - id) })
                }
                areaIds.forEach { id ->
                    val name = registryData?.service_response?.areas?.find { it.id == id }?.name ?: id
                    TargetChip(text = "Area: $name", onRemove = { onAreaIdsChange(areaIds - id) })
                }
                labelIds.forEach { id ->
                    val name = registryData?.service_response?.labels?.find { it.id == id }?.name ?: id
                    TargetChip(text = "Label: $name", onRemove = { onLabelIdsChange(labelIds - id) })
                }
            }
        }

        AssistChip(
            onClick = onEditTargets,
            label = { Text(if (hasAny) "Edit targets" else "Add target") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}

/**
 * Compact chip/tag summary of a plain list of entity ids (tap a chip to remove it)
 * plus an "Add / Edit entities" chip that opens the full-screen [TargetPickerScreen]
 * via [onEdit]. Used by event screens that previously showed one text field per id.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntityChipsSection(
    label: String,
    entityIds: List<String>,
    onRemove: (String) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    emptyHint: String = "No entities selected",
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)

        if (entityIds.isEmpty()) {
            Text(
                emptyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entityIds.forEach { id ->
                    TargetChip(text = id, onRemove = { onRemove(id) })
                }
            }
        }

        AssistChip(
            onClick = onEdit,
            label = { Text(if (entityIds.isEmpty()) "Add entities" else "Edit entities") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}

@Composable
private fun TargetChip(text: String, onRemove: () -> Unit) {
    AssistChip(
        onClick = onRemove,
        label = { Text(text, style = MaterialTheme.typography.bodySmall) },
        trailingIcon = {
            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
        },
        colors = AssistChipDefaults.assistChipColors()
    )
}
