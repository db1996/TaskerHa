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
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class TestConnectionActivity : AppCompatActivity(), TaskerPluginConfig<TestConnectionInput> {

    override val context: Context
        get() = applicationContext

    private val helper by lazy { TestConnectionHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        helper.onCreate()

        setContent {
            TaskerHaTheme {
                TestConnectionScreen(
                    onFinish = {
                        helper.finishForTasker()
                    }
                )
            }
        }
    }

    override fun assignFromInput(input: TaskerInput<TestConnectionInput>) {
        // No configuration to restore
    }

    override val inputForTasker: TaskerInput<TestConnectionInput>
        get() = TaskerInput(TestConnectionInput().apply {
            instanceId = HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""
        })
}

@Composable
private fun TestConnectionScreen(
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
