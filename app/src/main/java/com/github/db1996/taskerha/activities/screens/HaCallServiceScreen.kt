package com.github.db1996.taskerha.activities.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.composables.FieldInput
import com.github.db1996.taskerha.activities.partials.ServiceSelector
import com.github.db1996.taskerha.activities.partials.EntitySelector
import com.github.db1996.taskerha.activities.viewmodels.HaCallServiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaCallServiceScreen(
    viewModel: HaCallServiceViewModel,
    onSave: (domain: String, service: String, entityId: String, data: Map<String, String>) -> Unit
) {
    var entitySearching by remember { mutableStateOf(false) }

    // Load on first composition
    LaunchedEffect(Unit) {
        viewModel.loadServices()
        viewModel.loadEntities()
    }

    val selectedService = viewModel.selectedService
    val form = viewModel.form

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Action") },
                actions = {
                    IconButton(onClick = {
                        viewModel.testForm()
                    }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Test action")
                    }
                    FilledIconButton(
                        onClick = {
                            val built = viewModel.buildForm()
                            onSave(built.domain, built.service, built.entityId, built.data)
                        }
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Save action")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { viewModel.unsetPickedService() }) {
                Text("Reset domain/service")
            }
            // --- If no service selected → show selector
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

            // --- If a service is selected → show details
            selectedService?.let { service ->
                Text("Domain: ${service.domain}", style = MaterialTheme.typography.labelMedium)
                Text("Service: ${service.id}", style = MaterialTheme.typography.labelMedium)

                if (service.targetEntity) {
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
                            FieldInput(
                                field = field,
                                state = state,
                                onValueChange = { viewModel.updateFieldValue(field.id, it) },
                                onToggleChange = { viewModel.updateFieldToggle(field.id, it) },
                            )
                        }
                    }
                }

            }
        }
    }
}
