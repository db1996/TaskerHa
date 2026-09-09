package com.github.db1996.taskerha.tasker.getstate.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.github.db1996.taskerha.activities.partials.InstanceConnectionStatus
import com.github.db1996.taskerha.activities.partials.InstanceSelector
import com.github.db1996.taskerha.activities.partials.TargetPickerRequest
import com.github.db1996.taskerha.activities.partials.TargetPickerScreen
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigScaffold
import com.github.db1996.taskerha.tasker.getstate.data.HaGetStateBuiltForm
import com.github.db1996.taskerha.tasker.getstate.view.HaGetStateViewModel

@Composable
fun HaGetStateScreen(
    viewModel: HaGetStateViewModel,
    onSave: (HaGetStateBuiltForm) -> Unit,
    isNewAction: Boolean = false
) {
    var targetPicker by remember { mutableStateOf<TargetPickerRequest?>(null) }
    val instances by HaInstanceRepository.instances.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadEntities()
    }

    val form = viewModel.form

    BaseTaskerConfigScaffold(
        title = "Get Home Assistant State",
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
                    onDismiss = { targetPicker = null }
                )
            }
        }
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

            InstanceConnectionStatus(
                isLoading = viewModel.isLoadingInstance,
                error = viewModel.clientError,
                onRetry = viewModel::retryLoad
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = form.entityId,
                        onValueChange = { viewModel.updateEntityId(it) },
                        label = { Text("Entity ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            targetPicker = TargetPickerRequest(
                                entityIds = listOfNotNull(form.entityId.trim().ifBlank { null }),
                                onCommit = { e, _, _, _ ->
                                    viewModel.pickEntity(e.firstOrNull().orEmpty())
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Select entity")
                    }
                }
            }
        }
    }
}
