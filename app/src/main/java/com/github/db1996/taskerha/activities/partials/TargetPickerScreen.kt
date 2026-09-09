package com.github.db1996.taskerha.activities.partials

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.datamodels.HaRegistryData
import com.github.db1996.taskerha.util.EntityRecents

private enum class TargetTab(val label: String) {
    ENTITIES("Entities"),
    DEVICES("Devices"),
    AREAS("Areas"),
    LABELS("Labels"),
}

/**
 * Describes an entity/target picking session. Screens hold one of these in state and
 * hand it to [BaseTaskerConfigScaffold]'s `fullScreenOverlay`; [onCommit] receives the
 * chosen ids when the user presses Save.
 */
class TargetPickerRequest(
    val domainFilter: String? = null,
    val showRegistryTabs: Boolean = false,
    val entityIds: List<String> = emptyList(),
    val deviceIds: List<String> = emptyList(),
    val areaIds: List<String> = emptyList(),
    val labelIds: List<String> = emptyList(),
    val onCommit: (
        entityIds: List<String>,
        deviceIds: List<String>,
        areaIds: List<String>,
        labelIds: List<String>,
    ) -> Unit,
)

/**
 * Full-screen target picker. Replaces the old inline `EntitySelector` and the target
 * picker dialog. Multi-select, with a "Recent" section, manual id entry, and a
 * Cancel / Save action bar. The host screen must hide its own Save/Test actions while
 * this is shown (see [BaseTaskerConfigScaffold] `fullScreenOverlay`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetPickerScreen(
    request: TargetPickerRequest,
    entities: List<HaEntity>,
    onDismiss: () -> Unit,
    registryData: HaRegistryData? = null,
    hacsAvailable: Boolean = false,
    title: String = "Target picker",
) {
    var entityIds by remember { mutableStateOf(request.entityIds) }
    var deviceIds by remember { mutableStateOf(request.deviceIds) }
    var areaIds by remember { mutableStateOf(request.areaIds) }
    var labelIds by remember { mutableStateOf(request.labelIds) }

    val tabs = if (request.showRegistryTabs) TargetTab.entries.toList() else listOf(TargetTab.ENTITIES)
    var selectedTab by remember { mutableIntStateOf(0) }

    fun save() {
        EntityRecents.addAll(entityIds)
        request.onCommit(entityIds, deviceIds, areaIds, labelIds)
        onDismiss()
    }

    BackHandler(onBack = onDismiss)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = { save() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (request.showRegistryTabs) {
                ScrollableTabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                when (tabs[selectedTab]) {
                    TargetTab.ENTITIES -> EntityPickerPane(
                        entities = entities,
                        entityDomainFilter = request.domainFilter.orEmpty(),
                        selectedIds = entityIds,
                        onSelectedIdsChange = { entityIds = it }
                    )

                    TargetTab.DEVICES -> RegistryPickerPane(
                        hacsAvailable = hacsAvailable,
                        items = registryData?.service_response?.devices ?: emptyList(),
                        selectedIds = deviceIds,
                        displayName = { it.name },
                        itemId = { it.id },
                        onSelectedIdsChange = { deviceIds = it },
                        customButtonLabel = "Add device ID manually",
                        customPlaceholder = "Device ID",
                        unavailableMessage = "Install TaskerHA Companion to browse devices, or add device IDs manually below"
                    )

                    TargetTab.AREAS -> RegistryPickerPane(
                        hacsAvailable = hacsAvailable,
                        items = registryData?.service_response?.areas ?: emptyList(),
                        selectedIds = areaIds,
                        displayName = { it.name },
                        itemId = { it.id },
                        onSelectedIdsChange = { areaIds = it },
                        customButtonLabel = "Add area ID manually",
                        customPlaceholder = "Area ID",
                        unavailableMessage = "Install TaskerHA Companion to browse areas, or add area IDs manually below"
                    )

                    TargetTab.LABELS -> RegistryPickerPane(
                        hacsAvailable = hacsAvailable,
                        items = registryData?.service_response?.labels ?: emptyList(),
                        selectedIds = labelIds,
                        displayName = { it.name },
                        itemId = { it.id },
                        onSelectedIdsChange = { labelIds = it },
                        customButtonLabel = "Add label ID manually",
                        customPlaceholder = "Label ID",
                        unavailableMessage = "Install TaskerHA Companion to browse labels, or add label IDs manually below"
                    )
                }
            }
        }
    }
}

@Composable
private fun EntityPickerPane(
    entities: List<HaEntity>,
    entityDomainFilter: String,
    selectedIds: List<String>,
    onSelectedIdsChange: (List<String>) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val recentIds by EntityRecents.recents.collectAsState()

    val domainLower = entityDomainFilter.lowercase()
    fun domainOk(id: String) =
        domainLower.isBlank() || id.startsWith("$domainLower.", ignoreCase = true)

    val filtered = remember(entities, domainLower, searchQuery) {
        entities
            .filter { domainOk(it.entity_id) }
            .filter { it.entity_id.contains(searchQuery, ignoreCase = true) }
    }
    val recentEntities = remember(recentIds, domainLower, searchQuery) {
        if (searchQuery.isNotBlank()) emptyList()
        else recentIds.filter { domainOk(it) }
    }
    val customValues = remember(selectedIds, entities) {
        val known = entities.mapTo(HashSet()) { it.entity_id }
        selectedIds.filter { it !in known }
    }

    fun toggle(id: String) {
        onSelectedIdsChange(if (id in selectedIds) selectedIds - id else selectedIds + id)
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
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (recentEntities.isNotEmpty()) {
                item { SectionLabel("Recent") }
                items(recentEntities, key = { "recent_$it" }) { id ->
                    SelectableRow(
                        title = id,
                        selected = id in selectedIds,
                        recent = true,
                        onClick = { toggle(id) },
                        onRemoveRecent = { EntityRecents.remove(id) }
                    )
                }
                item { SectionLabel("All entities") }
            }

            items(filtered, key = { it.entity_id }) { entity ->
                SelectableRow(
                    title = entity.entity_id,
                    selected = entity.entity_id in selectedIds,
                    recent = false,
                    onClick = { toggle(entity.entity_id) },
                    onRemoveRecent = null
                )
            }

            if (filtered.isEmpty()) {
                item { EmptyText("No entities found") }
            }
        }

        CustomTargetSection(
            values = customValues,
            buttonLabel = "Add entity ID manually",
            placeholder = "Entity ID",
            onAdd = { value -> if (value !in selectedIds) onSelectedIdsChange(selectedIds + value) },
            onRemove = { value -> onSelectedIdsChange(selectedIds - value) }
        )
    }
}

@Composable
private fun <T> RegistryPickerPane(
    hacsAvailable: Boolean,
    items: List<T>,
    selectedIds: List<String>,
    displayName: (T) -> String,
    itemId: (T) -> String,
    onSelectedIdsChange: (List<String>) -> Unit,
    customButtonLabel: String,
    customPlaceholder: String,
    unavailableMessage: String,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(items, searchQuery) {
        items.filter { displayName(it).contains(searchQuery, ignoreCase = true) }
    }
    val customValues = remember(selectedIds, items) {
        val known = items.mapTo(HashSet()) { itemId(it) }
        selectedIds.filter { it !in known }
    }

    fun toggle(id: String) {
        onSelectedIdsChange(if (id in selectedIds) selectedIds - id else selectedIds + id)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!hacsAvailable) {
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
            Spacer(Modifier.weight(1f))
        } else {
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { itemId(it) }) { item ->
                    SelectableRow(
                        title = displayName(item),
                        selected = itemId(item) in selectedIds,
                        recent = false,
                        onClick = { toggle(itemId(item)) },
                        onRemoveRecent = null
                    )
                }
                if (filtered.isEmpty()) {
                    item { EmptyText("No items found") }
                }
            }
        }

        CustomTargetSection(
            values = customValues,
            buttonLabel = customButtonLabel,
            placeholder = customPlaceholder,
            onAdd = { value -> if (value !in selectedIds) onSelectedIdsChange(selectedIds + value) },
            onRemove = { value -> onSelectedIdsChange(selectedIds - value) }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(8.dp)
    )
}

@Composable
private fun SelectableRow(
    title: String,
    selected: Boolean,
    recent: Boolean,
    onClick: () -> Unit,
    onRemoveRecent: (() -> Unit)?,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (recent) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        "RECENT",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (onRemoveRecent != null) {
                IconButton(onClick = onRemoveRecent, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove from recents",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Free-text entry for a target id that isn't in the pickable lists (e.g. when the
 * TaskerHA Companion isn't installed, or for entity ids that don't exist yet).
 * Hidden behind a button until requested; added values show as removable chips so
 * pressing "Add" has visible feedback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomTargetSection(
    values: List<String>,
    buttonLabel: String,
    placeholder: String,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var showInput by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    fun commit() {
        val value = text.trim()
        if (value.isNotEmpty()) onAdd(value)
        text = ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider()

        if (values.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                values.forEach { value ->
                    AssistChip(
                        onClick = { onRemove(value) },
                        label = { Text(value, style = MaterialTheme.typography.bodySmall) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors()
                    )
                }
            }
        }

        if (showInput) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                trailingIcon = {
                    IconButton(onClick = { commit() }, enabled = text.isNotBlank()) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        } else {
            TextButton(onClick = { showInput = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(buttonLabel)
            }
        }
    }
}
