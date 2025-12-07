package com.github.db1996.taskerha.tasker.callservice

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.db1996.taskerha.activities.screens.HaCallServiceScreen
import com.github.db1996.taskerha.activities.viewmodels.HaCallServiceViewModel
import com.github.db1996.taskerha.activities.viewmodels.HaCallServiceViewModelFactory
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class HaCallServiceActivity :
    AppCompatActivity(),
    TaskerPluginConfig<HaCallServiceInput> {

    override val context: Context
        get() = applicationContext
    private val client by lazy {
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)
        HomeAssistantClient(url, token)
    }

    private val viewModel: HaCallServiceViewModel by viewModels {
        HaCallServiceViewModelFactory(client)
    }
    private val taskerHelper by lazy { HaCallServiceConfigHelper(this) }
    private var selectedDomain: String = ""
    private var selectedService: String = ""
    private var selectedEntityId: String = ""
    private var selectedData: Map<String, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        taskerHelper.onCreate()

        setContent {
            TaskerHaTheme {
                HaCallServiceScreen(viewModel) { domain, service, entityId, data ->
                    selectedDomain = domain
                    selectedService = service
                    selectedEntityId = entityId
                    selectedData = data

                    taskerHelper.finishForTasker()
                }
            }
        }
    }

    /**
     * Called by the helper when editing an existing Tasker action.
     * Convert TaskerInput -> local fields -> restore the UI.
     */
    override fun assignFromInput(input: TaskerInput<HaCallServiceInput>) {
        val params = input.regular

        selectedDomain = params.domain
        selectedService = params.service
        selectedEntityId = params.entityId

        val dataMap: Map<String, String> = try {
            Json.Default.decodeFromString(
                MapSerializer(String.Companion.serializer(), String.serializer()),
                params.dataJson
            )
        } catch (_: Exception) {
            emptyMap()
        }

        selectedData = dataMap

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
    override val inputForTasker: TaskerInput<HaCallServiceInput>
        get() {
            val jsonData = Json.Default.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                selectedData
            )

            val haInput = HaCallServiceInput().apply {
                domain = selectedDomain
                service = selectedService
                entityId = selectedEntityId
                dataJson = jsonData
            }

            return TaskerInput(haInput)
        }
}