package com.github.db1996.taskerha

import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun HomeassistantConfigScreen(
    viewModel: HomeassistantFormViewModel,
    onSave: (domain: String, service: String, entityId: String, data: Map<String, String>) -> Unit
) {
    var entitySearching by remember { mutableStateOf(false) } // <--- lift here
    viewModel.loadServices()
    viewModel.loadEntities()

    val selectedService = viewModel.selectedService
    val form = viewModel.form


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 32.dp) // space around the whole list
    ) {
        Text("Home Assistant Form")


        // --- Search fields side by side ---
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

        if (selectedService == null && viewModel.services.isNotEmpty()) {
            ServiceSelectorBlocks(
                services = viewModel.services,
                onSelect = { service -> viewModel.pickService(service) },
                currentDomainSearch = viewModel.currentDomainSearch,
                currentServiceSearch = viewModel.currentServiceSearch,
                onDomainSearch = { viewModel.currentDomainSearch = it },
                onServiceSearch = { viewModel.currentServiceSearch = it }
            )
        }

        selectedService?.let { service ->
            Text("Domain: ${service.domain}")
            Text("Service: ${service.id}")

            Log.e("HA", "selectedService: ${selectedService.targetEntity}")

            if (selectedService.targetEntity) {
                EntitySelector(
                    entities = viewModel.entities,
                    serviceDomain = service.domain,
                    currentEntityId = form.entityId,
                    searching = entitySearching, // pass state down
                    onSearchChanged = { entitySearching = it }, // allow child to update
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
