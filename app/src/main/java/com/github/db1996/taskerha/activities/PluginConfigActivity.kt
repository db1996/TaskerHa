package com.github.db1996.taskerha.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.db1996.taskerha.activities.screens.PluginConfigScreen
import com.github.db1996.taskerha.TaskerConstants
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.FieldState
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import com.github.db1996.taskerha.activities.viewmodels.PluginConfigViewModel
import com.github.db1996.taskerha.activities.viewmodels.PluginConfigViewModelFactory
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

        if(savedInstanceState != null) {
            val domain = savedInstanceState.getString("DOMAIN") ?: return
            val service = savedInstanceState.getString("SERVICE") ?: return
            val entity = savedInstanceState.getString("ENTITY_ID") ?: return
            val dataJson = savedInstanceState.getString("DATA") ?: "{}"
            val dataMap: Map<String, Any> = Json.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                dataJson
            ).mapValues { it.value as Any }

            println("Domain: $domain")
            println("Service: $service")
            println("Entity: $entity")
            println("Data: $dataMap")

            viewModel.form.domain = domain
            viewModel.form.service = service
            viewModel.form.entityId = entity
            viewModel.form.dataContainer.clear()

            val pservice = viewModel.services.find({ it.domain == domain && it.id == service })

            pservice?.fields?.forEach { field ->
                val fieldState = FieldState();
                if(dataMap.containsKey(field.id)) {
                    fieldState.value.value = dataMap[field.id].toString()
                }
                viewModel.form.dataContainer[field.id] = fieldState

            }

        }


        setContent {
            TaskerHaTheme() {
                PluginConfigScreen(viewModel) { domain, service, entityId, data ->
                    // --- User press save action
                    val jsonData = Json.encodeToString(
                        MapSerializer(
                            String.serializer(),
                            String.serializer()
                        ), data.mapValues { it.value })

                    val bundle = Bundle().apply {
                        putString("DOMAIN", domain)
                        putString("SERVICE", service)
                        putString("ENTITY_ID", entityId)
                        putSerializable("DATA", jsonData)
                    }

                    val result = Intent().apply {
                        putExtra(TaskerConstants.EXTRA_BUNDLE, bundle)
                        putExtra(TaskerConstants.EXTRA_BLURB, "Call Home Assistant service")
                    }

                    setResult(RESULT_OK, result)
                    finish()
                }
            }
        }
    }
}