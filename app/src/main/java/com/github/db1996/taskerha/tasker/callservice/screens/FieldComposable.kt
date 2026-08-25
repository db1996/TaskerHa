package com.github.db1996.taskerha.tasker.callservice.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.datamodels.HaServiceField
import com.github.db1996.taskerha.enums.HaServiceFieldType
import com.github.db1996.taskerha.tasker.callservice.data.FieldState

@Composable
fun FieldInput(
    field: HaServiceField,
    state: FieldState,
    onValueChange: (String) -> Unit,
    onToggleChange: (Boolean) -> Unit,
    onEntitySearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Checkbox(
            enabled = field.required != true,
            checked = state.toggle.value || field.required == true,
            onCheckedChange = onToggleChange
        )
        when (field.type) {
            HaServiceFieldType.SELECT -> {
                FieldSelectInput(
                    field = field,
                    state = state,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f)
                )
            }

            HaServiceFieldType.NUMBER -> {
                FieldTextInput(
                    field = field,
                    state = state,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f)
                )
            }

            HaServiceFieldType.BOOLEAN -> {
                FieldBooleanInput(
                    field = field,
                    state = state,
                    onValueChange = onValueChange
                )
            }

            HaServiceFieldType.DATE,
            HaServiceFieldType.TIME,
            HaServiceFieldType.DATETIME -> {
                FieldDateTimeInput(
                    field = field,
                    state = state,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f)
                )
            }

            HaServiceFieldType.STATE -> {
                // Entity ID field - show with helpful label
                FieldEntityInput(
                    field = field,
                    state = state,
                    onValueChange = onValueChange,
                    onEntitySearch = onEntitySearch,
                    modifier = Modifier.weight(1f)
                )
            }

            HaServiceFieldType.OBJECT -> {
                FieldObjectInput(
                    field = field,
                    state = state,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f)
                )
            }

            else -> {
                FieldTextInput(
                    field = field,
                    state = state,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FieldTextInput(
    field: HaServiceField,
    state: FieldState,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = state.value.value,
        onValueChange = onValueChange,
        label = { Text(field.name ?: field.id) },
        modifier = modifier
    )
}


@Composable
fun FieldObjectInput(
    field: HaServiceField,
    state: FieldState,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = state.value.value,
        onValueChange = onValueChange,
        label = { Text("${field.name ?: field.id} (YAML)") },
        placeholder = { Text("key: value\nother_key: true") },
        minLines = 3,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldSelectInput(
    field: HaServiceField,
    state: FieldState,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = field.options ?: emptyList()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            value = state.value.value,
            onValueChange = {},
            readOnly = true,
            label = { Text(field.name ?: field.id) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onValueChange(option.value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FieldNumberInput(
    field: HaServiceField,
    state: FieldState,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = state.value.value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() || it == '.' }) },
        label = { Text(field.name ?: field.id) },
        modifier = modifier
    )
}

@Composable
fun FieldBooleanInput(
    field: HaServiceField,
    state: FieldState,
    onValueChange: (String) -> Unit
) {
    Switch(
        checked = state.value.value == "true",
        onCheckedChange = { onValueChange(it.toString()) }
    )
}

@Composable
fun FieldDateTimeInput(
    field: HaServiceField,
    state: FieldState,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = state.value.value,
        onValueChange = {},
        modifier = modifier,
        label = { Text(field.name ?: field.id) },
        readOnly = true
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FieldEntityInput(
    field: HaServiceField,
    state: FieldState,
    onValueChange: (String) -> Unit,
    onEntitySearch: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (field.multipleEntities) {
        // Multiple entity mode - show chips and add button
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = field.name ?: field.id,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Parse comma-separated entities
            val entities = state.value.value.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            
            if (entities.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entities.forEach { entityId ->
                        AssistChip(
                            label = { Text(entityId) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(),
                            onClick = {
                                // Remove this entity from the list
                                val newList = entities.filter { it != entityId }
                                onValueChange(newList.joinToString(","))
                            }
                        )
                    }
                }
            }
            
            onEntitySearch?.let { searchCallback ->
                Button(onClick = searchCallback) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Text(" Add Entity")
                }
            }
        }
    } else {
        // Single entity mode - text field with search button
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = state.value.value,
                onValueChange = onValueChange,
                label = { Text("${field.name ?: field.id} (Entity ID)") },
                placeholder = { Text("e.g., light.living_room") },
                modifier = Modifier.weight(1f)
            )
            
            onEntitySearch?.let { searchCallback ->
                IconButton(onClick = searchCallback) {
                    Icon(Icons.Default.Search, contentDescription = "Search entities")
                }
            }
        }
    }
}

