package com.github.db1996.taskerha.tasker.callservice.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.partials.EntitySelector
import com.github.db1996.taskerha.activities.partials.InstanceSelector
import com.github.db1996.taskerha.activities.partials.ServiceSelector
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigScaffold
import com.github.db1996.taskerha.tasker.callservice.data.CallServiceFormBuiltForm
import com.github.db1996.taskerha.tasker.callservice.view.CallServiceViewModel

@Composable
fun CallServiceScreen(
    viewModel: CallServiceViewModel,
    onSave: (CallServiceFormBuiltForm) -> Unit,
    isNewAction: Boolean = false
) {
    var fieldEntitySearching by remember { mutableStateOf<String?>(null) }
    val instances by HaInstanceRepository.instances.collectAsState()

    // Load entities on first composition
    LaunchedEffect(Unit) {
        viewModel.loadEntities()
        viewModel.loadServices()
    }

    val form = viewModel.form

    BaseTaskerConfigScaffold(
        title = "Call HA service",
        onSave = {
            val built = viewModel.buildForm()
            onSave(built)
        },
        onTest = { viewModel.testForm() },
        showTestButton = true
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Instance selector (only for new actions)
            if (instances.isNotEmpty()) {
                InstanceSelector(
                    instances = instances,
                    selectedInstanceId = form.instanceId,
                    onInstanceSelected = { instanceId ->
                        if (isNewAction) {
                            viewModel.changeInstance(instanceId)
                        }
                    },
                    enabled = isNewAction
                )
            }

            if (viewModel.clientError != "") {
                Text(viewModel.clientError, color = MaterialTheme.colorScheme.error)

                Text("Please check your connection settings in the main app outside of tasker")
            }

            Button(onClick = { viewModel.unsetPickedService() }) {
                Text("Reset domain/service")
            }
            // --- If no service selected → show selector
            if (viewModel.selectedService == null && viewModel.services.isNotEmpty()) {
                ServiceSelector(
                    services = viewModel.services,
                    onSelect = { service -> viewModel.pickService(service) },
                    currentDomainSearch = viewModel.currentDomainSearch,
                    currentServiceSearch = viewModel.currentServiceSearch,
                    onDomainSearch = { viewModel.currentDomainSearch = it },
                    onServiceSearch = { viewModel.currentServiceSearch = it }
                )
            }

            // --- If a service is selected → show details
            viewModel.selectedService?.let { service ->
                Text("Domain: ${service.domain}", style = MaterialTheme.typography.labelMedium)
                Text("Service: ${service.id}", style = MaterialTheme.typography.labelMedium)

                // Show EntitySelector for STATE-type fields (entity_id parameters)
                fieldEntitySearching?.let { fieldId ->
                    service.fields.find { it.id == fieldId }?.let { field ->
                        form.dataContainer[fieldId]?.let { state ->
                            if (state.value.value.isNotBlank()) {
                                Text(
                                    "${field.name ?: field.id}: ${state.value.value}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            EntitySelector(
                                entities = viewModel.entities,
                                serviceDomain = field.domain ?: "",
                                currentEntityId = state.value.value,
                                searching = true,
                                onSearchChanged = { searching ->
                                    if (!searching) fieldEntitySearching = null
                                },
                                onEntitySelected = { entityId ->
                                    if (field.multipleEntities) {
                                        // Append to comma-separated list
                                        val current = state.value.value
                                        val newValue = if (current.isBlank()) entityId 
                                                      else "$current,$entityId"
                                        viewModel.updateFieldValue(fieldId, newValue)
                                    } else {
                                        // Replace single entity
                                        viewModel.updateFieldValue(fieldId, entityId)
                                    }
                                    fieldEntitySearching = null
                                },
                                onEntityIdChanged = { entityId ->
                                    viewModel.updateFieldValue(fieldId, entityId)
                                }
                            )
                        }
                    }
                }

                if (fieldEntitySearching == null) {
                    service.fields.forEach { field ->
                        form.dataContainer[field.id]?.let { state ->
                            FieldInput(
                                field = field,
                                state = state,
                                onValueChange = { viewModel.updateFieldValue(field.id, it) },
                                onToggleChange = { viewModel.updateFieldToggle(field.id, it) },
                                onEntitySearch = { fieldEntitySearching = field.id }
                            )
                        }
                    }
                }

            }
        }
    }
}

