package com.github.db1996.taskerha

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.db1996.taskerha.activities.screens.PluginConfigScreen
import com.github.db1996.taskerha.activities.viewmodels.PluginConfigViewModel
import com.github.db1996.taskerha.activities.viewmodels.PluginConfigViewModelFactory
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.tasker.HaConfigHelper
import com.github.db1996.taskerha.tasker.HaPluginInput
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class PluginConfigActivity :
    AppCompatActivity(),
    TaskerPluginConfig<HaPluginInput> {   // ✅ add type argument here

    // --- TaskerPluginConfig requirement ---
    override val context: Context
        get() = applicationContext

    private val client by lazy {
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)
        HomeAssistantClient(url, token)
    }

    private val viewModel: PluginConfigViewModel by viewModels {
        PluginConfigViewModelFactory(client)
    }

    // Helper that bridges this Activity <-> Tasker <-> Runner
    private val taskerHelper by lazy { HaConfigHelper(this) }

    // Values that will be sent back to Tasker
    private var selectedDomain: String = ""
    private var selectedService: String = ""
    private var selectedEntityId: String = ""
    private var selectedData: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Reads existing config from Tasker, then calls assignFromInput()
        taskerHelper.onCreate()

        setContent {
            TaskerHaTheme {
                PluginConfigScreen(viewModel) { domain, service, entityId, data ->
                    // 1. Save latest UI values
                    selectedDomain = domain
                    selectedService = service
                    selectedEntityId = entityId
                    selectedData = data

                    // 2. Let the helper validate + finish for Tasker
                    taskerHelper.finishForTasker()
                    // On success this will internally call setResult() + finish()
                }
            }
        }
    }

    /**
     * Called by the helper when editing an existing Tasker action.
     * Convert TaskerInput -> local fields -> restore the UI.
     */
    override fun assignFromInput(input: TaskerInput<HaPluginInput>) { // ✅ typed input
        val params = input.regular

        selectedDomain = params.domain
        selectedService = params.service
        selectedEntityId = params.entityId

        val dataMap: Map<String, String> = try {
            Json.Default.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                params.dataJson
            )
        } catch (_: Exception) {
            emptyMap()
        }

        selectedData = dataMap

        // Pre-fill your Compose form
        viewModel.restoreForm(
            selectedDomain,
            selectedService,
            selectedEntityId,
            dataMap
        )
    }

    /**
     * Called by the helper when it needs the final input to send back to Tasker.
     */
    override val inputForTasker: TaskerInput<HaPluginInput>   // ✅ typed TaskerInput
        get() {
            val jsonData = Json.Default.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                selectedData
            )

            val haInput = HaPluginInput().apply {
                domain = selectedDomain
                service = selectedService
                entityId = selectedEntityId
                dataJson = jsonData
            }

            return TaskerInput(haInput)
        }
}
