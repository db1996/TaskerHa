package com.github.db1996.taskerha.activities.screens

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.partials.ServiceSelector
import com.github.db1996.taskerha.activities.partials.EntitySelector
import com.github.db1996.taskerha.activities.viewmodels.PluginConfigViewModel

@Composable
fun PluginConfigScreen(
    viewModel: PluginConfigViewModel,
    onSave: (domain: String, service: String, entityId: String, data: Map<String, String>) -> Unit
) {
    var entitySearching by remember { mutableStateOf(false) }
    viewModel.loadServices()
    viewModel.loadEntities()

    val selectedService = viewModel.selectedService
    val form = viewModel.form


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 32.dp)
    ) {
        Text("Create Home assistant service call", style = MaterialTheme.typography.headlineSmall)


        // --- Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                viewModel.unsetPickedService()

            }) {
                Text("Reset service/domain")
            }
            Button(onClick = { viewModel.testForm() }) {
                Text("Test action")
            }
        }

        val activity = LocalContext.current as? ComponentActivity
        if(activity != null){
            Button(onClick = { viewModel.saveFormToTasker(activity)  }) {
                Text("Save")
            }
        }

        // --- If no service selected show selector list with search fields
        if (selectedService == null && viewModel.services.isNotEmpty()) {
            ServiceSelector(
                services = viewModel.services,
                onSelect = { service -> viewModel.pickService(service) },
                currentDomainSearch = viewModel.currentDomainSearch,
                currentServiceSearch = viewModel.currentServiceSearch,
                onDomainSearch = { viewModel.currentDomainSearch = it },
                onServiceSearch = { viewModel.currentServiceSearch = it }
            )
        }

        // If service is selected, show data fields + entity selector
        selectedService?.let { service ->
            Text("Domain: ${service.domain}", style = MaterialTheme.typography.labelMedium)
            Text("Service: ${service.id}", style = MaterialTheme.typography.labelMedium)

            Log.e("HA", "selectedService: ${selectedService.targetEntity}")

            if (selectedService.targetEntity) {
                EntitySelector(
                    entities = viewModel.entities,
                    serviceDomain = service.domain,
                    currentEntityId = form.entityId,
                    searching = entitySearching,
                    onSearchChanged = { entitySearching = it },
                    onEntitySelected = { viewModel.pickEntity(it) }
                )
            }

            if (!entitySearching) {
                service.fields.forEach { field ->
                    form.dataContainer[field.id]?.let { state ->
                        Row {
                            Checkbox(
                                checked = state.toggle.value,
                                onCheckedChange = { viewModel.updateFieldToggle(field.id, it) }
                            )
                            TextField(
                                value = state.value.value,
                                onValueChange = { viewModel.updateFieldValue(field.id, it) },
                                label = { Text(field.name ?: field.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
