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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.partials.InstanceConnectionStatus
import com.github.db1996.taskerha.activities.partials.InstanceSelector
import com.github.db1996.taskerha.activities.partials.ServiceSelector
import com.github.db1996.taskerha.activities.partials.TargetPickerRequest
import com.github.db1996.taskerha.activities.partials.TargetPickerScreen
import com.github.db1996.taskerha.activities.partials.TargetSection
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigScaffold
import com.github.db1996.taskerha.tasker.callservice.data.CallServiceFormBuiltForm
import com.github.db1996.taskerha.tasker.callservice.view.CallServiceViewModel

private fun csvToList(value: String?): List<String> =
    value?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

private fun listToCsv(list: List<String>): String = list.joinToString(",")

@Composable
fun CallServiceScreen(
    viewModel: CallServiceViewModel,
    onSave: (CallServiceFormBuiltForm) -> Unit,
    isNewAction: Boolean = false
) {
    var targetPicker by remember { mutableStateOf<TargetPickerRequest?>(null) }
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
        showTestButton = true,
        fullScreenOverlay = targetPicker?.let { req ->
            {
                TargetPickerScreen(
                    request = req,
                    entities = viewModel.entities,
                    registryData = viewModel.registryData,
                    hacsAvailable = viewModel.hacsAvailable,
                    onDismiss = { targetPicker = null }
                )
            }
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        val serviceSelected = viewModel.selectedService != null
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .let { if (serviceSelected) it.verticalScroll(scrollState) else it },
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

            InstanceConnectionStatus(
                isLoading = viewModel.isLoadingInstance,
                error = viewModel.clientError,
                onRetry = viewModel::retryLoad
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                        if (service.hasTargetDefinition) {
                            // Target picker UI: Entities + Devices + Areas + Labels
                            val entityField = service.fields.find { it.id == "entity_id" }

                            TargetSection(
                                entityIds = csvToList(form.dataContainer["entity_id"]?.value?.value),
                                deviceIds = csvToList(form.dataContainer["device_id"]?.value?.value),
                                areaIds = csvToList(form.dataContainer["area_id"]?.value?.value),
                                labelIds = csvToList(form.dataContainer["label_id"]?.value?.value),
                                registryData = viewModel.registryData,
                                onEntityIdsChange = { viewModel.updateFieldValue("entity_id", listToCsv(it)) },
                                onDeviceIdsChange = { viewModel.updateFieldValue("device_id", listToCsv(it)) },
                                onAreaIdsChange = { viewModel.updateFieldValue("area_id", listToCsv(it)) },
                                onLabelIdsChange = { viewModel.updateFieldValue("label_id", listToCsv(it)) },
                                onEditTargets = {
                                    targetPicker = TargetPickerRequest(
                                        domainFilter = entityField?.domain,
                                        showRegistryTabs = true,
                                        entityIds = csvToList(form.dataContainer["entity_id"]?.value?.value),
                                        deviceIds = csvToList(form.dataContainer["device_id"]?.value?.value),
                                        areaIds = csvToList(form.dataContainer["area_id"]?.value?.value),
                                        labelIds = csvToList(form.dataContainer["label_id"]?.value?.value),
                                        onCommit = { e, d, a, l ->
                                            viewModel.updateFieldValue("entity_id", listToCsv(e))
                                            viewModel.updateFieldValue("device_id", listToCsv(d))
                                            viewModel.updateFieldValue("area_id", listToCsv(a))
                                            viewModel.updateFieldValue("label_id", listToCsv(l))
                                        }
                                    )
                                }
                            )

                            // Render remaining non-entity fields
                            service.fields.forEach { field ->
                                if (field.id == "entity_id") return@forEach
                                form.dataContainer[field.id]?.let { state ->
                                    FieldInput(
                                        field = field,
                                        state = state,
                                        onValueChange = { viewModel.updateFieldValue(field.id, it) },
                                        onToggleChange = { viewModel.updateFieldToggle(field.id, it) }
                                    )
                                }
                            }
                        } else {
                            // Services without a target definition: each entity field opens
                            // the full-screen picker for that field.
                            service.fields.forEach { field ->
                                form.dataContainer[field.id]?.let { state ->
                                    FieldInput(
                                        field = field,
                                        state = state,
                                        onValueChange = { viewModel.updateFieldValue(field.id, it) },
                                        onToggleChange = { viewModel.updateFieldToggle(field.id, it) },
                                        onEntitySearch = {
                                            val fieldId = field.id
                                            targetPicker = TargetPickerRequest(
                                                domainFilter = field.domain,
                                                showRegistryTabs = false,
                                                entityIds = csvToList(state.value.value),
                                                onCommit = { e, _, _, _ ->
                                                    viewModel.updateFieldValue(fieldId, listToCsv(e))
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
