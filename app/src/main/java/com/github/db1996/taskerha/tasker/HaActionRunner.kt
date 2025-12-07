package com.github.db1996.taskerha.tasker

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class HaActionRunner : TaskerPluginRunnerAction<HaPluginInput, Unit>() {

    override fun run(
        context: Context,
        input: TaskerInput<HaPluginInput>
    ): TaskerPluginResult<Unit> {

        return runBlocking {
            val params = input.regular

            try {
                val client = HomeAssistantClient(
                    HaSettings.loadUrl(context),
                    HaSettings.loadToken(context)
                )

                // 1. Ping
                if (!client.ping()) {
                    return@runBlocking TaskerPluginResultError(
                        1,
                        client.error ?: "Failed to ping Home Assistant"
                    )
                }

                // 2. Parse Data JSON
                val dataMap = try {
                    Json.Default.decodeFromString(
                        MapSerializer(String.serializer(), String.serializer()),
                        params.dataJson
                    ).mapValues { it.value as Any }
                } catch (e: Exception) {
                    return@runBlocking TaskerPluginResultError(2, "Invalid JSON Data: ${e.message}")
                }

                // 3. Call Service
                val ok = client.callService(
                    params.domain,
                    params.service,
                    params.entityId,
                    dataMap
                )

                if (!ok) {
                    return@runBlocking TaskerPluginResultError(3, client.error ?: "Service call failed")
                }

            } catch (e: Exception) {
                return@runBlocking TaskerPluginResultError(4, e.message ?: "Unknown crash")
            }

            // Success (Note the spelling 'Sucess' is correct for this library)
            return@runBlocking TaskerPluginResultSucess(Unit)
        }
    }
}