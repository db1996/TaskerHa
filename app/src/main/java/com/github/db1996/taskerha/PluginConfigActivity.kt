package com.github.db1996.taskerha

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.db1996.taskerha.activities.screens.PluginConfigScreen
import com.github.db1996.taskerha.activities.viewmodels.PluginConfigViewModel
import com.github.db1996.taskerha.activities.viewmodels.PluginConfigViewModelFactory
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.tasker.TaskerPlugin
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class PluginConfigActivity : AppCompatActivity() {

    private val client by lazy {
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)
        HomeAssistantClient(url, token)
    }

    private val viewModel: PluginConfigViewModel by viewModels {
        PluginConfigViewModelFactory(client)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callingIntent = intent
        val existingBundle = intent.getBundleExtra(TaskerConstants.EXTRA_BUNDLE)

        if (existingBundle == null) {
            Log.d("PluginConfigActivity", "No existing bundle (new action)")
        } else {
            restoreStateIfNeeded(existingBundle)
        }
        if (TaskerPlugin.hostSupportsRelevantVariables(intent.extras)) {
            val passedNames = TaskerPlugin.getRelevantVariableList(intent.extras)

            for (name in passedNames) {
                Log.e("PluginConfigActivity", "Relevant variable: $name")
            }
        }

        setContent {
            TaskerHaTheme {
                PluginConfigScreen(viewModel) { domain, service, entityId, data ->
                    val jsonData = Json.Default.encodeToString(
                        MapSerializer(String.Companion.serializer(), String.serializer()),
                        data
                    )

                    val bundle = Bundle().apply {
                        putString("DOMAIN", domain)
                        putString("SERVICE", service)
                        putString("ENTITY_ID", entityId)
                        putString("DATA", jsonData)
                    }

                    val msg = buildString {
                        append("Domain: $domain\nService: $service")
                        if (entityId.isNotBlank()) append("\nEntity ID: $entityId")

                        if (data.isNotEmpty()) {
                            append("\nData:\n")
                            data.forEach { (key, value) ->
                                append("- $key: $value\n")
                            }
                        }
                    }

                    val resultIntent = Intent().apply {
                        // Put the bundle via Tasker’s expected key
                        putExtra(TaskerConstants.EXTRA_BUNDLE, bundle)
                        // Blurb
                        putExtra(TaskerConstants.EXTRA_BLURB, msg)
                    }

                    if (TaskerPlugin.Setting.hostSupportsSynchronousExecution(callingIntent.extras)) {
                        Log.e("PluginConfigActivity", "Running synchronously")
                        // e.g. 10 seconds, adjust as needed
                        TaskerPlugin.Setting.requestTimeoutMS(resultIntent, 10_000)
                    }

                    if (TaskerPlugin.hostSupportsRelevantVariables(callingIntent.extras)) {
                        Log.e("PluginConfigActivity", "Adding relevant variables")
                        TaskerPlugin.addRelevantVariableList(
                            resultIntent,
                            arrayOf(
                                "%err\nError Code\nError code returned by TaskerHA",
                                "%errmsg\nError Message\nHuman-readable error from Home Assistant"
                            )
                        )
                    }

                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }

    private fun restoreStateIfNeeded(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) return

        val domain = savedInstanceState.getString("DOMAIN") ?: return
        val service = savedInstanceState.getString("SERVICE") ?: return
        val entity = savedInstanceState.getString("ENTITY_ID") ?: return

        val dataJson = savedInstanceState.getString("DATA") ?: "{}"
        val dataMap: Map<String, String> = Json.Default.decodeFromString(
            MapSerializer(String.serializer(), String.serializer()),
            dataJson
        )

        viewModel.restoreForm(domain, service, entity, dataMap)
    }
}