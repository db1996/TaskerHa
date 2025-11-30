package com.github.db1996.taskerha.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

        val bundle = intent.getBundleExtra(TaskerConstants.EXTRA_BUNDLE)

        if(bundle == null){
            Log.e("PluginConfigActivity", "onCreate, bundle is null")
        }else{
            restoreStateIfNeeded(bundle)
        }

        setContent {
            TaskerHaTheme {
                PluginConfigScreen(viewModel) { domain, service, entityId, data ->
                    val jsonData = Json.encodeToString(
                        MapSerializer(String.serializer(), String.serializer()),
                        data
                    )

                    val bundle = Bundle().apply {
                        putString("DOMAIN", domain)
                        putString("SERVICE", service)
                        putString("ENTITY_ID", entityId)
                        putString("DATA", jsonData)
                    }

                    val msg = buildString {
                        append("Call Home Assistant: $domain.$service")
                        if (entityId.isNotBlank()) append(" on $entityId")
                    }

                    val result = Intent().apply {
                        putExtra(TaskerConstants.EXTRA_BUNDLE, bundle)
                        putExtra(TaskerConstants.EXTRA_BLURB, msg)
                    }

                    setResult(RESULT_OK, result)
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
        val dataMap: Map<String, String> = Json.decodeFromString(
            MapSerializer(String.serializer(), String.serializer()),
            dataJson
        )

        viewModel.restoreForm(domain, service, entity, dataMap)
    }
}
