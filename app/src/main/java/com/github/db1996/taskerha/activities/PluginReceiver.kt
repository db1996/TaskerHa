package com.github.db1996.taskerha.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.github.db1996.taskerha.NotificationHelper
import com.github.db1996.taskerha.TaskerConstants
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class PluginReceiver : BroadcastReceiver() {

    private companion object {
        const val TASKER_RESULT_CODE_FAILED = 1
    }

    override fun onReceive(context: Context, intent: Intent) {

        val pendingResult = goAsync()
        val bundle = intent.getBundleExtra(TaskerConstants.EXTRA_BUNDLE) ?: run {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val resultBundle = Bundle()

            try {
                val client = HomeAssistantClient(
                    HaSettings.loadUrl(context),
                    HaSettings.loadToken(context)
                )

                val ping = client.ping()

                if (!ping) {
                    val errorMessage = client.error ?: "Home Assistant Ping Failed (Unknown Error)"
                    resultBundle.putString(TaskerConstants.EXTRA_ERROR_MESSAGE, errorMessage)

                    Log.e("PluginReceiver", "Ping failed: $errorMessage")
                    NotificationHelper.showErrorNotification(context, "Onreceive can't ping HomeAssistant", errorMessage)

                    // Only set result if ordered
                    if (isOrderedBroadcast) {
                        Log.e("PluginReceiver", "Fatal crash: $errorMessage")
                        // CRITICAL: Set resultCode to signal failure
                        pendingResult.resultCode = TASKER_RESULT_CODE_FAILED
                        // Also set the error message as the Result Data String for maximum Tasker compatibility
                        pendingResult.setResultData(errorMessage)
                        pendingResult.setResultExtras(resultBundle)
                    }
                    return@launch
                }

                val ok = client.callService(
                    bundle.getString("DOMAIN")!!,
                    bundle.getString("SERVICE")!!,
                    bundle.getString("ENTITY_ID")!!,
                    decodeData(bundle)
                )

                if (!ok) {
                    val errorMessage = client.error
                    resultBundle.putString(TaskerConstants.EXTRA_ERROR_MESSAGE, errorMessage)
                    NotificationHelper.showErrorNotification(context, "TaskerHA service call error", errorMessage)

                    if (isOrderedBroadcast) {
                        Log.e("PluginReceiver", "Fatal crash: $errorMessage")
                        // Signal failure
                        pendingResult.resultCode = TASKER_RESULT_CODE_FAILED
                        // Also set the error message as the Result Data String for maximum Tasker compatibility
                        pendingResult.setResultData(errorMessage)
                        pendingResult.setResultExtras(resultBundle)
                    }
                }

            } catch (e: Exception) {
                // Failsafe for unexpected crashes in coroutine
                val errorMessage = e.message ?: "Plugin crashed during execution."
                Log.e("PluginReceiver", "Fatal crash: $errorMessage", e)
                NotificationHelper.showErrorNotification(context, "TaskerHA Plugin - CRASH", errorMessage)
                if (isOrderedBroadcast) {
                    Log.e("PluginReceiver", "Fatal crash: $errorMessage")
                    val errorBundle = Bundle()
                    errorBundle.putString(TaskerConstants.EXTRA_ERROR_MESSAGE, errorMessage)

                    // Signal failure on crash
                    pendingResult.resultCode = TASKER_RESULT_CODE_FAILED
                    pendingResult.setResultData(errorMessage)
                    pendingResult.setResultExtras(errorBundle)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun decodeData(bundle: Bundle): Map<String, Any> {
        val dataJson = bundle.getString("DATA") ?: return emptyMap()

        return try {
            Json.Default.decodeFromString(
                MapSerializer(String.Companion.serializer(), String.serializer()),
                dataJson
            ).mapValues { it.value as Any }
        } catch (e: Exception) {
            Log.e("PluginReceiver", "Error decoding JSON data: $dataJson", e)
            emptyMap()
        }
    }
}