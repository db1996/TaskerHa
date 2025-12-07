package com.github.db1996.taskerha.tasker

import android.content.Context
import android.util.Log
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultErrorWithOutput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class HaCallServiceRunner : TaskerPluginRunnerAction<HaCallServiceInput, HaCallServiceOutput>() {

    override fun run(
        context: Context,
        input: TaskerInput<HaCallServiceInput>
    ): TaskerPluginResult<HaCallServiceOutput> {

        return runBlocking {
            val params = input.regular
            var result: String

            try {
                val client = HomeAssistantClient(
                    HaSettings.loadUrl(context),
                    HaSettings.loadToken(context)
                )

                // 1. Ping
                if (!client.ping()) {
                    return@runBlocking TaskerPluginResultErrorWithOutput<HaCallServiceOutput>(
                        1,
                        client.error
                    )
                }

                // 2. Parse Data JSON
                val dataMap = try {
                    Json.Default.decodeFromString(
                        MapSerializer(String.serializer(), String.serializer()),
                        params.dataJson
                    ).mapValues { it.value as Any }
                } catch (e: Exception) {
                    return@runBlocking TaskerPluginResultErrorWithOutput<HaCallServiceOutput>(
                        2,
                        "Invalid JSON Data: ${e.message}"
                    )
                }

                // 3. Call Service
                val ok = client.callService(
                    params.domain,
                    params.service,
                    params.entityId,
                    dataMap
                )

                if (!ok) {
                    return@runBlocking TaskerPluginResultErrorWithOutput<HaCallServiceOutput>(
                        3,
                        client.error
                    )
                }
                Log.e("HA Runner", "Result: ${client.result}")
                result = client.result
            } catch (e: Exception) {
                return@runBlocking TaskerPluginResultErrorWithOutput<HaCallServiceOutput>(
                    4,
                    e.message ?: "Unknown crash"
                )
            }

            // ✅ Success: Tasker gets %ha_data from this object
            TaskerPluginResultSucess(
                HaCallServiceOutput(
                    dataJson = result
                )
            )
        }
    }
}
