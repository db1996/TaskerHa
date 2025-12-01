package com.github.db1996.taskerha.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.github.db1996.taskerha.NotificationHelper
import com.github.db1996.taskerha.TaskerConstants.EXTRA_BUNDLE
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import com.github.db1996.taskerha.tasker.TaskerPlugin

class PluginReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val pending = goAsync()
        val ordered = isOrderedBroadcast

        val bundle = intent.getBundleExtra(EXTRA_BUNDLE) ?: run {
            pending.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {

            try {
                val client = HomeAssistantClient(
                    HaSettings.loadUrl(context),
                    HaSettings.loadToken(context)
                )

                // 👉 PING
                if (!client.ping()) {
                    val msg = client.error
                    reportError(context, msg, intent.extras, pending, ordered)
                    return@launch
                }

                // 👉 CALL SERVICE
                val ok = client.callService(
                    bundle.getString("DOMAIN")!!,
                    bundle.getString("SERVICE")!!,
                    bundle.getString("ENTITY_ID")!!,
                    decodeData(bundle)
                )

                if (!ok) {
                    val msg = client.error
                    reportError(context, msg, intent.extras, pending, ordered)
                    return@launch
                }

                // All good
                if (ordered) {
                    pending.resultCode = TaskerPlugin.Setting.RESULT_CODE_OK
                }

            } catch (e: Exception) {
                val msg = e.message ?: "TaskerHA plugin crashed"
                reportError(context, msg, intent.extras, pending, ordered)
            } finally {
                pending.finish()
            }
        }
    }

    // ---------------------------------------------------------
    // ERROR HANDLING (Tasker-compatible)
    // ---------------------------------------------------------

    private fun reportError(
        context: Context,
        message: String,
        originalExtras: Bundle?,
        pending: PendingResult,
        ordered: Boolean
    ) {
        Log.e("PluginReceiver", message)
        NotificationHelper.showErrorNotification(context, "TaskerHA Error", message)

        if (!ordered) return  // Tasker only reads results from ORDERED broadcasts

        pending.resultCode = TaskerPlugin.Setting.RESULT_CODE_FAILED
        pending.setResultData(message)

        // add Tasker variables like %err and %errmsg
        if (TaskerPlugin.Setting.hostSupportsVariableReturn(originalExtras)) {
            Log.e("PluginReceiver", "Adding Tasker variables")
            val vars = Bundle().apply {
                putString("%err", "1")           // conventional "error flag"
                putString("%errmsg", message)     // your custom variable
                putString(                        // Tasker's built-in variable name
                    TaskerPlugin.Setting.VARNAME_ERROR_MESSAGE,
                    message
                )
            }

            val extras = pending.getResultExtras(true)
            TaskerPlugin.addVariableBundle(extras, vars)
        }
    }

    // ---------------------------------------------------------
    // Decode the JSON "DATA" field
    // ---------------------------------------------------------
    private fun decodeData(bundle: Bundle): Map<String, Any> {
        val json = bundle.getString("DATA") ?: return emptyMap()

        return try {
            Json.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                json
            ).mapValues { it.value as Any }
        } catch (e: Exception) {
            Log.e("PluginReceiver", "Error decoding DATA JSON", e)
            emptyMap()
        }
    }
}
