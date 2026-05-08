package com.github.db1996.taskerha.tasker.ontriggerstate.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.partials.EntitySelector
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigScaffold
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.tasker.ontriggerstate.view.OnTriggerStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnTriggerStateScreen(
    viewModel: OnTriggerStateViewModel,
    onSave: (OnTriggerStateBuiltForm) -> Unit
) {
    var entityAdding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadEntities()
    }

    val form = viewModel.form

    BaseTaskerConfigScaffold(
        title = "On entity trigger state",
        onSave = {
            val built = viewModel.buildForm()
            onSave(built)
        },
        showTestButton = false
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .then(if (!entityAdding) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Error message
            if (viewModel.clientError.isNotEmpty()) {
                Text(
                    text = viewModel.clientError,
                    color = MaterialTheme.colorScheme.error
                )
                Text("Please check your connection settings in the main app outside of tasker")
            }

            if (entityAdding) {
                // Domain search filter
                TextField(
                    value = viewModel.currentDomainSearch,
                    onValueChange = { viewModel.currentDomainSearch = it },
                    label = { Text("Filter domain") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Entity selector (adds entity on pick)
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
                // Editable text field per entity, with a delete button
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
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove entity"
                            )
                        }
                    }
                }

                // Add Entity button
                OutlinedButton(
                    onClick = { entityAdding = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Add Entity")
                }

                // From / To / For filters
                TextField(
                    value = form.fromState,
                    onValueChange = { viewModel.setFrom(it) },
                    label = { Text("From") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = form.toState,
                    onValueChange = { viewModel.setTo(it) },
                    label = { Text("To") },
                    modifier = Modifier.fillMaxWidth()
                )

                DurationHmsStringField(
                    value = form.forDuration,
                    onValueChange = { viewModel.setFor(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Attribute Mappings section
                Text(
                    text = "Attribute Mappings",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "Assign entity attributes to %ha_attr_1 – %ha_attr_10 variables in Tasker.",
                    style = MaterialTheme.typography.bodySmall
                )

                OutlinedButton(
                    onClick = { viewModel.loadAttributesForAllEntities() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load attributes from entities")
                }

                if (viewModel.isLoadingAttributes) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                // Per-entity attribute rows with a header for each entity
                val allLoadedKeys = viewModel.availableAttributes.values.flatten().toSet()
                viewModel.availableAttributes.forEach { (entityId, attrKeys) ->
                    Text(
                        text = entityId,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    attrKeys.forEach { attrKey ->
                        AttributeSlotRow(
                            attrKey = attrKey,
                            currentSlot = form.attributeMapping[attrKey],
                            onSlotSelected = { slot -> viewModel.setAttributeSlot(attrKey, slot) }
                        )
                    }
                }

                // Show already-mapped keys not yet loaded (restored from prefs)
                val unmappedSavedKeys = form.attributeMapping.keys
                    .filter { it !in allLoadedKeys }
                if (unmappedSavedKeys.isNotEmpty()) {
                    Text(
                        text = "Saved mappings (press Load to refresh):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    unmappedSavedKeys.forEach { attrKey ->
                        AttributeSlotRow(
                            attrKey = attrKey,
                            currentSlot = form.attributeMapping[attrKey],
                            onSlotSelected = { slot -> viewModel.setAttributeSlot(attrKey, slot) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeSlotRow(
    attrKey: String,
    currentSlot: Int?,
    onSlotSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val slotOptions = listOf(null) + (1..10).toList()
    val slotLabel = currentSlot?.toString() ?: "None"

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
                value = slotLabel,
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
}
