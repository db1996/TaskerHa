package com.github.db1996.taskerha.tasker.onHaMessage.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.partials.InstanceSelector
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigScaffold
import com.github.db1996.taskerha.tasker.onHaMessage.data.OnHaMessageBuiltForm
import com.github.db1996.taskerha.tasker.onHaMessage.view.OnHaMessageViewModel
import com.github.db1996.taskerha.util.copyToClipboard

@Composable
fun OnHaMessageScreen(
    viewModel: OnHaMessageViewModel,
    onSave: (OnHaMessageBuiltForm) -> Unit
) {
    val context = LocalContext.current
    val form = viewModel.form
    val instances by HaInstanceRepository.instances.collectAsState()

    BaseTaskerConfigScaffold(
        title = "Direct message to HA",
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
            if (instances.isNotEmpty()) {
                InstanceSelector(
                    instances = instances,
                    selectedInstanceId = form.instanceId,
                    onInstanceSelected = { viewModel.changeInstance(it) }
                )
            }

            Text("Type")
            TextField(
                value = form.type,
                onValueChange = { viewModel.setType(it) },
                label = { Text("Optional, can be used for filtering")}
            )
            Text("Message")
            TextField(
                value = form.message,
                onValueChange = { viewModel.setMessage(it) },
                label = { Text("Optional, can be used for filtering")}
            )

            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Home Assistant YAML example",
                            style = MaterialTheme.typography.titleSmall
                        )
                        IconButton(
                            onClick = {
                                copyToClipboard(
                                    context,
                                    viewModel.yamlExample,
                                    "Home Assistant event YAML copied!"
                                )
                            }
                        ) {
                            Icon(

                                Icons.Rounded.ContentCopy,
                                contentDescription = "Copy YAML",
                                modifier = Modifier.padding(all = 0.dp).size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Create an automation -> add action -> manual event -> edit yaml and paste this in",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    SelectionContainer {
                        Text(
                            text = viewModel.yamlExample,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}