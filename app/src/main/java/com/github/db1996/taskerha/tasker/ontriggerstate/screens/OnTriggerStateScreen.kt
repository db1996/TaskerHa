package com.github.db1996.taskerha.tasker.ontriggerstate.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
                .fillMaxWidth(),
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
            }
        }
    }
}
