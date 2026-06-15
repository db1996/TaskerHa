package com.github.db1996.taskerha.tasker.setwebsocket

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.partials.InstanceSelector
import com.github.db1996.taskerha.datamodels.HaInstance
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class SetWebSocketActivity : AppCompatActivity(), TaskerPluginConfig<SetWebSocketInput> {

    override val context: Context
        get() = applicationContext

    private val helper by lazy { SetWebSocketHelper(this) }

    private val enabledState = mutableStateOf(true)
    private val selectedInstanceIdState = mutableStateOf("")

    override fun assignFromInput(input: TaskerInput<SetWebSocketInput>) {
        enabledState.value = input.regular.enabled != "false"
        val savedId = input.regular.instanceId
        selectedInstanceIdState.value = savedId.ifBlank {
            HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""
        }
    }

    override val inputForTasker: TaskerInput<SetWebSocketInput>
        get() = TaskerInput(SetWebSocketInput().apply {
            enabled = if (enabledState.value) "true" else "false"
            instanceId = selectedInstanceIdState.value.ifBlank {
                HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedInstanceIdState.value =
            HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""

        helper.onCreate()

        setContent {
            TaskerHaTheme {
                val enabled by enabledState
                val selectedInstanceId by selectedInstanceIdState
                val instances by HaInstanceRepository.instances.collectAsState()

                SetWebSocketScreen(
                    enabled = enabled,
                    instances = instances,
                    selectedInstanceId = selectedInstanceId,
                    onEnabledChanged = { enabledState.value = it },
                    onInstanceSelected = { selectedInstanceIdState.value = it },
                    onFinish = { helper.finishForTasker() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetWebSocketScreen(
    enabled: Boolean,
    instances: List<HaInstance>,
    selectedInstanceId: String,
    onEnabledChanged: (Boolean) -> Unit,
    onInstanceSelected: (String) -> Unit,
    onFinish: () -> Unit
) {
    val activeInstanceId = HaInstanceRepository.getActive()?.id
    val activeInstanceName = HaInstanceRepository.getActive()?.let {
        if (it.name.isNotBlank()) it.name else it.remoteUrl
    }
    val selectedInstance = instances.find { it.id == selectedInstanceId }
    val selectedInstanceName = selectedInstance?.let {
        if (it.name.isNotBlank()) it.name else it.remoteUrl
    }

    val showSwitchWarning = enabled
            && activeInstanceId != null
            && selectedInstanceId.isNotBlank()
            && selectedInstanceId != activeInstanceId

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Set WebSocket Connection",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Start or stop the WebSocket connection to Home Assistant. When starting, you can choose which instance to connect to.",
                style = MaterialTheme.typography.bodyMedium
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    onClick = { onEnabledChanged(true) },
                    selected = enabled
                ) {
                    Text("Start")
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    onClick = { onEnabledChanged(false) },
                    selected = !enabled
                ) {
                    Text("Stop")
                }
            }

            if (enabled && instances.isNotEmpty()) {
                InstanceSelector(
                    instances = instances,
                    selectedInstanceId = selectedInstanceId,
                    onInstanceSelected = onInstanceSelected,
                    enabled = true
                )
            }else if(!enabled && instances.isNotEmpty()) {
                Text(
                    text = "Any connected instance will be disconnected.",
                    style = MaterialTheme.typography.bodyMedium
                )

            }else {
                Text(
                    text = "No instances configured",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (showSwitchWarning) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "⚠",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "This will switch the WebSocket connection from \"$activeInstanceName\" to \"$selectedInstanceName\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onFinish,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Done")
            }
        }
    }
}
