package com.github.db1996.taskerha.activities.partials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.datamodels.HaArea
import com.github.db1996.taskerha.datamodels.HaDevice
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.datamodels.HaLabel
import com.github.db1996.taskerha.datamodels.HaRegistryData

private enum class TargetTab(val label: String) {
    ENTITIES("Entities"),
    DEVICES("Devices"),
    AREAS("Areas"),
    LABELS("Labels"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TargetSection(
    entityIds: List<String>,
    deviceIds: List<String>,
    areaIds: List<String>,
    labelIds: List<String>,
    entities: List<HaEntity>,
    entityDomainFilter: String?,
    registryData: HaRegistryData?,
    hacsAvailable: Boolean,
    onEntityIdsChange: (List<String>) -> Unit,
    onDeviceIdsChange: (List<String>) -> Unit,
    onAreaIdsChange: (List<String>) -> Unit,
    onLabelIdsChange: (List<String>) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

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
                    AssistChip(
                        label = { Text(id, style = MaterialTheme.typography.bodySmall) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        },
                        onClick = { onEntityIdsChange(entityIds - id) },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                }
                deviceIds.forEach { id ->
                    val name = registryData?.service_response?.devices?.find { it.id == id }?.name ?: id
                    AssistChip(
                        label = { Text("Device: $name", style = MaterialTheme.typography.bodySmall) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        },
                        onClick = { onDeviceIdsChange(deviceIds - id) },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                }
                areaIds.forEach { id ->
                    val name = registryData?.service_response?.areas?.find { it.id == id }?.name ?: id
                    AssistChip(
                        label = { Text("Area: $name", style = MaterialTheme.typography.bodySmall) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        },
                        onClick = { onAreaIdsChange(areaIds - id) },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                }
                labelIds.forEach { id ->
                    val name = registryData?.service_response?.labels?.find { it.id == id }?.name ?: id
                    AssistChip(
                        label = { Text("Label: $name", style = MaterialTheme.typography.bodySmall) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        },
                        onClick = { onLabelIdsChange(labelIds - id) },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                }
            }
        }

        AssistChip(
            onClick = { showPicker = true },
            label = { Text("Add target") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }

    if (showPicker) {
        TargetPickerDialog(
            entityIds = entityIds,
            deviceIds = deviceIds,
            areaIds = areaIds,
            labelIds = labelIds,
            entities = entities,
            entityDomainFilter = entityDomainFilter,
            registryData = registryData,
            hacsAvailable = hacsAvailable,
            onEntityIdsChange = onEntityIdsChange,
            onDeviceIdsChange = onDeviceIdsChange,
            onAreaIdsChange = onAreaIdsChange,
            onLabelIdsChange = onLabelIdsChange,
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun TargetPickerDialog(
    entityIds: List<String>,
    deviceIds: List<String>,
    areaIds: List<String>,
    labelIds: List<String>,
    entities: List<HaEntity>,
    entityDomainFilter: String?,
    registryData: HaRegistryData?,
    hacsAvailable: Boolean,
    onEntityIdsChange: (List<String>) -> Unit,
    onDeviceIdsChange: (List<String>) -> Unit,
    onAreaIdsChange: (List<String>) -> Unit,
    onLabelIdsChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = TargetTab.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add target") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tab.label) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    when (tabs[selectedTab]) {
                        TargetTab.ENTITIES -> EntityPickerTab(
                            entities = entities,
                            entityDomainFilter = entityDomainFilter ?: "",
                            selectedIds = entityIds,
                            onToggle = { id ->
                                onEntityIdsChange(
                                    if (id in entityIds) entityIds - id else entityIds + id
                                )
                            }
                        )

                        TargetTab.DEVICES -> RegistryPickerTab(
                            hacsAvailable = hacsAvailable,
                            items = registryData?.service_response?.devices ?: emptyList(),
                            selectedIds = deviceIds,
                            displayName = { it.name },
                            itemId = { it.id },
                            onToggle = { id ->
                                onDeviceIdsChange(
                                    if (id in deviceIds) deviceIds - id else deviceIds + id
                                )
                            },
                            unavailableMessage = "Install TaskerHA Companion to select devices"
                        )

                        TargetTab.AREAS -> RegistryPickerTab(
                            hacsAvailable = hacsAvailable,
                            items = registryData?.service_response?.areas ?: emptyList(),
                            selectedIds = areaIds,
                            displayName = { it.name },
                            itemId = { it.id },
                            onToggle = { id ->
                                onAreaIdsChange(
                                    if (id in areaIds) areaIds - id else areaIds + id
                                )
                            },
                            unavailableMessage = "Install TaskerHA Companion to select areas"
                        )

                        TargetTab.LABELS -> RegistryPickerTab(
                            hacsAvailable = hacsAvailable,
                            items = registryData?.service_response?.labels ?: emptyList(),
                            selectedIds = labelIds,
                            displayName = { it.name },
                            itemId = { it.id },
                            onToggle = { id ->
                                onLabelIdsChange(
                                    if (id in labelIds) labelIds - id else labelIds + id
                                )
                            },
                            unavailableMessage = "Install TaskerHA Companion to select labels"
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun EntityPickerTab(
    entities: List<HaEntity>,
    entityDomainFilter: String,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val domainLower = entityDomainFilter.lowercase()
    val filtered = remember(entities, domainLower, searchQuery) {
        entities
            .filter { e ->
                if (domainLower.isBlank()) true
                else e.entity_id.startsWith("$domainLower.", ignoreCase = true)
            }
            .filter { e -> e.entity_id.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search entities") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.entity_id }) { entity ->
                TargetItemRow(
                    name = entity.entity_id,
                    subtitle = null,
                    selected = entity.entity_id in selectedIds,
                    onClick = { onToggle(entity.entity_id) }
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No entities found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> RegistryPickerTab(
    hacsAvailable: Boolean,
    items: List<T>,
    selectedIds: List<String>,
    displayName: (T) -> String,
    itemId: (T) -> String,
    onToggle: (String) -> Unit,
    unavailableMessage: String,
) {
    if (!hacsAvailable) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    unavailableMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(items, searchQuery) {
        items.filter { displayName(it).contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { itemId(it) }) { item ->
                TargetItemRow(
                    name = displayName(item),
                    subtitle = null,
                    selected = itemId(item) in selectedIds,
                    onClick = { onToggle(itemId(item)) }
                )
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        "No items found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetItemRow(
    name: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
