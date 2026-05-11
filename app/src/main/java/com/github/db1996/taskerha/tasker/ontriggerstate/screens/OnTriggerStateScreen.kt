package com.github.db1996.taskerha.tasker.ontriggerstate.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.partials.EntitySelector
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigScaffold
import com.github.db1996.taskerha.tasker.ontriggerstate.data.EntityTriggerConfig
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.tasker.ontriggerstate.view.OnTriggerStateViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun OnTriggerStateScreen(
    viewModel: OnTriggerStateViewModel,
    onSave: (OnTriggerStateBuiltForm) -> Unit
) {
    var entityAdding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadEntities()
    }

    // Auto-load attributes whenever the entity list changes (debounced)
    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.form.entityIds }
            .distinctUntilChanged()
            .debounce(500)
            .collect { viewModel.loadAttributesForAllEntities() }
    }

    val form = viewModel.form

    BaseTaskerConfigScaffold(
        title = "On entity trigger state",
        onSave = { onSave(viewModel.buildForm()) },
        showTestButton = false
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .then(if (!entityAdding) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.clientError.isNotEmpty()) {
                Text(text = viewModel.clientError, color = MaterialTheme.colorScheme.error)
                Text("Please check your connection settings in the main app outside of tasker")
            }

            if (entityAdding) {
                TextField(
                    value = viewModel.currentDomainSearch,
                    onValueChange = { viewModel.currentDomainSearch = it },
                    label = { Text("Filter domain") },
                    modifier = Modifier.fillMaxWidth()
                )
                EntitySelector(
                    entities = viewModel.entities,
                    serviceDomain = viewModel.currentDomainSearch,
                    currentEntityId = "",
                    searching = true,
                    onSearchChanged = { if (!it) entityAdding = false },
                    onEntitySelected = { id ->
                        viewModel.addEntity(id)
                        entityAdding = false
                    },
                    onEntityIdChanged = {}
                )
            } else {
                // Entity ID list
                form.entityIds.forEachIndexed { index, entityId ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = entityId,
                            onValueChange = { viewModel.updateEntityAt(index, it) },
                            label = { Text("Entity ID") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { viewModel.removeEntity(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove entity")
                        }
                    }
                }

                OutlinedButton(
                    onClick = { entityAdding = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Add Entity")
                }

                // Config per entity toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setConfigPerEntity(!form.configPerEntity) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = form.configPerEntity,
                        onCheckedChange = { viewModel.setConfigPerEntity(it) }
                    )
                    Text("Config per entity", style = MaterialTheme.typography.bodyMedium)
                }

                if (!form.configPerEntity) {
                    // Single "All entities" config card
                    EntityConfigCard(
                        title = "All entities",
                        config = form.sharedConfig,
                        availableAttributes = viewModel.availableAttributes.values.flatten().toSet().toList(),
                        onTargetAttributeChange = { viewModel.setSharedTargetAttribute(it) },
                        onIgnoreMainStateChangesChange = { viewModel.setSharedIgnoreMainStateChanges(it) },
                        onFromChange = { viewModel.setSharedFrom(it) },
                        onToChange = { viewModel.setSharedTo(it) },
                        onForChange = { viewModel.setSharedFor(it) },
                        initiallyExpanded = true
                    )
                } else {
                    // Per-entity collapsible cards
                    if (form.entityIds.isNotEmpty()) {
                        Text(
                            text = "Entities (${form.entityIds.size})",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    form.entityIds.forEachIndexed { index, entityId ->
                        val config = form.entityConfigs.getOrElse(index) { EntityTriggerConfig(entityId = entityId) }
                        EntityConfigCard(
                            title = "${index + 1}. $entityId",
                            config = config,
                            availableAttributes = viewModel.availableAttributes[entityId] ?: emptyList(),
                            onTargetAttributeChange = { viewModel.setTargetAttribute(index, it) },
                            onIgnoreMainStateChangesChange = { viewModel.setIgnoreMainStateChanges(index, it) },
                            onFromChange = { viewModel.setFrom(index, it) },
                            onToChange = { viewModel.setTo(index, it) },
                            onForChange = { viewModel.setFor(index, it) },
                            initiallyExpanded = false
                        )
                    }
                }

                // Attribute Mappings section
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                AttributeMappingsSection(
                    availableAttributes = viewModel.availableAttributes,
                    attributeMapping = form.attributeMapping,
                    isLoading = viewModel.isLoadingAttributes,
                    onSlotSelected = { key, slot -> viewModel.setAttributeSlot(key, slot) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityConfigCard(
    title: String,
    config: EntityTriggerConfig,
    availableAttributes: List<String>,
    onTargetAttributeChange: (String) -> Unit,
    onIgnoreMainStateChangesChange: (Boolean) -> Unit,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onForChange: (String) -> Unit,
    initiallyExpanded: Boolean
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Header row — always visible
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand"
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Target attribute — dropdown if attributes loaded, else text field
                if (availableAttributes.isNotEmpty()) {
                    var attrExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = attrExpanded,
                        onExpandedChange = { attrExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = config.targetAttribute,
                            onValueChange = {
                                onTargetAttributeChange(it)
                            },
                            label = { Text("Target attribute (optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = attrExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                                .fillMaxWidth(),
                            singleLine = true
                        )
                        val filtered = availableAttributes.filter {
                            config.targetAttribute.isBlank() || it.contains(config.targetAttribute, ignoreCase = true)
                        }
                        if (filtered.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = attrExpanded,
                                onDismissRequest = { attrExpanded = false }
                            ) {
                                filtered.forEach { attr ->
                                    DropdownMenuItem(
                                        text = { Text(attr) },
                                        onClick = {
                                            onTargetAttributeChange(attr)
                                            attrExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = config.targetAttribute,
                        onValueChange = onTargetAttributeChange,
                        label = { Text("Target attribute (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Ignore main state changes checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = config.targetAttribute.isNotBlank()) {
                            onIgnoreMainStateChangesChange(!config.ignoreMainStateChanges)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = config.ignoreMainStateChanges,
                        onCheckedChange = onIgnoreMainStateChangesChange,
                        enabled = config.targetAttribute.isNotBlank()
                    )
                    Text(
                        text = "Ignore main state changes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (config.targetAttribute.isNotBlank())
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // From / To / For fields
                OutlinedTextField(
                    value = config.fromState,
                    onValueChange = onFromChange,
                    label = { Text("From (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = config.toState,
                    onValueChange = onToChange,
                    label = { Text("To (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                DurationHmsStringField(
                    value = config.forDuration,
                    onValueChange = onForChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AttributeMappingsSection(
    availableAttributes: Map<String, List<String>>,
    attributeMapping: Map<String, Int>,
    isLoading: Boolean,
    onSlotSelected: (String, Int?) -> Unit
) {
    Text(
        text = "Attribute Mappings",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 4.dp)
    )
    Text(
        text = "Assign entity attributes to %ha_attr_1 – %ha_attr_10 variables in Tasker.",
        style = MaterialTheme.typography.bodySmall
    )

    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        return
    }

    if (availableAttributes.isEmpty()) {
        // Show previously saved mappings even if no attributes loaded yet
        val savedKeys = attributeMapping.keys.toList()
        if (savedKeys.isNotEmpty()) {
            Text(
                text = "Saved mappings (add entities to refresh):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            savedKeys.forEach { attrKey ->
                AttributeSlotRow(
                    attrKey = attrKey,
                    entityTags = emptyList(),
                    currentSlot = attributeMapping[attrKey],
                    onSlotSelected = { onSlotSelected(attrKey, it) }
                )
            }
        }
        return
    }

    // Compute which keys appear in >=2 entities (shared)
    val allAttrKeys = availableAttributes.values.flatten()
    val keyEntityCount = allAttrKeys.groupBy { it }.mapValues { (_, v) -> v.size }
    val sharedKeys = keyEntityCount.filter { it.value >= 2 }.keys.sorted()
    val allLoadedKeys = availableAttributes.values.flatten().toSet()

    // Shared section
    if (sharedKeys.isNotEmpty()) {
        Text(
            text = "Shared (applies to all selected entities)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
        sharedKeys.forEach { attrKey ->
            val entityTags = availableAttributes
                .filter { (_, keys) -> attrKey in keys }
                .keys.toList()
            AttributeSlotRow(
                attrKey = attrKey,
                entityTags = entityTags,
                currentSlot = attributeMapping[attrKey],
                onSlotSelected = { onSlotSelected(attrKey, it) }
            )
        }
    }

    // Per-entity sections for keys unique to that entity
    availableAttributes.forEach { (entityId, attrKeys) ->
        val uniqueKeys = attrKeys.filter { keyEntityCount[it] == 1 }
        if (uniqueKeys.isNotEmpty()) {
            Text(
                text = entityId,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            uniqueKeys.forEach { attrKey ->
                AttributeSlotRow(
                    attrKey = attrKey,
                    entityTags = emptyList(),
                    currentSlot = attributeMapping[attrKey],
                    onSlotSelected = { onSlotSelected(attrKey, it) }
                )
            }
        }
    }

    // Show previously-saved mappings for keys no longer loaded
    val unmappedSavedKeys = attributeMapping.keys.filter { it !in allLoadedKeys }
    if (unmappedSavedKeys.isNotEmpty()) {
        Text(
            text = "Saved mappings (press Load to refresh):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        unmappedSavedKeys.forEach { attrKey ->
            AttributeSlotRow(
                attrKey = attrKey,
                entityTags = emptyList(),
                currentSlot = attributeMapping[attrKey],
                onSlotSelected = { onSlotSelected(attrKey, it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeSlotRow(
    attrKey: String,
    entityTags: List<String>,
    currentSlot: Int?,
    onSlotSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val slotOptions = listOf(null) + (1..10).toList()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = attrKey,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = currentSlot?.toString() ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(0.4f),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    slotOptions.forEach { slot ->
                        DropdownMenuItem(
                            text = { Text(slot?.toString() ?: "None") },
                            onClick = {
                                onSlotSelected(slot)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        // Entity tags for shared attributes
        if (entityTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entityTags.forEach { tag ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        }
    }
}
