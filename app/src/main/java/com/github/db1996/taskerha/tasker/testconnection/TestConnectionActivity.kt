package com.github.db1996.taskerha.tasker.testconnection

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
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class TestConnectionActivity : AppCompatActivity(), TaskerPluginConfig<TestConnectionInput> {

    override val context: Context
        get() = applicationContext

    private val helper by lazy { TestConnectionHelper(this) }

    private val selectedInstanceIdState = mutableStateOf("")

    override fun assignFromInput(input: TaskerInput<TestConnectionInput>) {
        val saved = input.regular.instanceId
        selectedInstanceIdState.value = saved.ifBlank {
            HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""
        }
    }

    override val inputForTasker: TaskerInput<TestConnectionInput>
        get() = TaskerInput(TestConnectionInput().apply {
            instanceId = selectedInstanceIdState.value.ifBlank {
                HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default to active instance; assignFromInput will override for existing configs
        selectedInstanceIdState.value =
            HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""

        helper.onCreate()

        setContent {
            TaskerHaTheme {
                val selectedInstanceId by selectedInstanceIdState
                val instances by HaInstanceRepository.instances.collectAsState()

                TestConnectionScreen(
                    instances = instances,
                    selectedInstanceId = selectedInstanceId,
                    onInstanceSelected = { selectedInstanceIdState.value = it },
                    onFinish = { helper.finishForTasker() }
                )
            }
        }
    }
}

@Composable
private fun TestConnectionScreen(
    instances: List<com.github.db1996.taskerha.datamodels.HaInstance>,
    selectedInstanceId: String,
    onInstanceSelected: (String) -> Unit,
    onFinish: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Test Home Assistant Connection",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "This action will test connectivity to your Home Assistant instance and return the results.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (instances.isNotEmpty()) {
                InstanceSelector(
                    instances = instances,
                    selectedInstanceId = selectedInstanceId,
                    onInstanceSelected = onInstanceSelected
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Output variables:",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "• %ha_remote - Remote connection status (true/false)",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "• %ha_local - Local connection status (only if local URL is configured)",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onFinish,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Done")
            }
        }
    }
}
