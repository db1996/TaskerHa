package com.github.db1996.taskerha.activities.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.github.db1996.taskerha.tasker.callservice.data.FieldState
import com.github.db1996.taskerha.datamodels.HaServiceField
import com.github.db1996.taskerha.enums.HaServiceFieldType

@Composable
fun FieldInput(
    field: HaServiceField,
    state: FieldState,
    onValueChange: (String) -> Unit,
    onToggleChange: (Boolean) -> Unit,
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

