package com.github.db1996.taskerha.activities.partials

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.db1996.taskerha.datamodels.HaInstance

/**
 * Instance selector dropdown for Tasker action/trigger configuration
 * 
 * @param instances List of all available instances
 * @param selectedInstanceId Currently selected instance ID
 * @param onInstanceSelected Callback when instance is selected
 * @param enabled Whether the dropdown is enabled (false for existing actions)
 * @param label Label for the dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstanceSelector(
    instances: List<HaInstance>,
    selectedInstanceId: String,
    onInstanceSelected: (String) -> Unit,
    enabled: Boolean = true,
    label: String = "Home Assistant Instance"
) {
    var expanded by remember { mutableStateOf(false) }
    
    val selectedInstance = instances.find { it.id == selectedInstanceId }
    val displayText = selectedInstance?.let {
        if (it.name.isNotBlank()) it.name else it.remoteUrl
    } ?: "No instance selected"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                if (enabled) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled
        )

        if (enabled) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                instances.forEach { instance ->
                    DropdownMenuItem(
                        text = {
                            val name = if (instance.name.isNotBlank()) {
                                instance.name
                            } else {
                                instance.remoteUrl
                            }
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        },
                        onClick = {
                            onInstanceSelected(instance.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
