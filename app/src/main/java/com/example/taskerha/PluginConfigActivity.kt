package com.example.taskerha

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.example.taskerha.client.HomeAssistantClient
import com.example.taskerha.datamodels.ActualService
import com.example.taskerha.datamodels.FieldState
import com.example.taskerha.datamodels.HaSettings
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.collections.set

class PluginConfigActivity : ComponentActivity() {
    private val client by lazy {
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)

        HomeAssistantClient(url, token)
    }

    private val viewModel: HomeassistantFormViewModel by viewModels {
        HomeassistantFormViewModelFactory(client)
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
            ).mapValues { it.value as Any } // ensure it's Any

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
            MaterialTheme() {
                HomeassistantConfigScreen(viewModel) { domain, service, entityId, data ->
                    // This is called when the user presses save
                    val jsonData = Json.encodeToString(MapSerializer(String.serializer(), String.serializer()), data.mapValues { it.value.toString() })

                    val bundle = android.os.Bundle().apply {
                        putString("DOMAIN", domain)
                        putString("SERVICE", service)
                        putString("ENTITY_ID", entityId)
                        putSerializable("DATA", jsonData) // Bundle can't take Map directly
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
