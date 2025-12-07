package com.github.db1996.taskerha.tasker.getstate

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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class HaGetStateRunner : TaskerPluginRunnerAction<HaGetStateInput, HaGetStateOutput>() {
    override fun run(
        context: Context,
        input: TaskerInput<HaGetStateInput>
    ): TaskerPluginResult<HaGetStateOutput> {

        return runBlocking {
            val params = input.regular

            try {
                val client = HomeAssistantClient(
                    HaSettings.loadUrl(context),
                    HaSettings.loadToken(context)
                )

                if (!client.ping()) {
                    return@runBlocking TaskerPluginResultErrorWithOutput<HaGetStateOutput>(
                        1,
                        client.error
                    )
                }

                Log.d("HaGetStateRunner", "Pinged, getting state ${params.entityId}..")
                val ok = client.getState(params.entityId)
                if (!ok) {
                    return@runBlocking TaskerPluginResultErrorWithOutput<HaGetStateOutput>(
                        2,
                        client.error
                    )
                }

                val rawJson = client.result

                val jsonElement = Json.Default.parseToJsonElement(rawJson)
                val obj: JsonObject = jsonElement.jsonObject

                val state = obj["state"]?.jsonPrimitive?.content ?: ""

                val attrsAny = obj["attributes"]?.jsonObject ?: JsonObject(emptyMap())

                val attrsMap: Map<String, String> = attrsAny.mapValues { (_, value) ->
                    value.toString().trim('"')
                }

                val attrsJson = Json.Default.encodeToString(
                    MapSerializer(String.Companion.serializer(), String.serializer()),
                    attrsMap
                )

                Log.d("HaGetStateRunner", "Result, State: $state, Attributes: $attrsJson")

                TaskerPluginResultSucess(
                    HaGetStateOutput(
                        state = state,
                        attributesJson = attrsJson,
                        rawJson = rawJson
                    )
                )
            } catch (e: Exception) {
                return@runBlocking TaskerPluginResultErrorWithOutput<HaGetStateOutput>(
                    3,
                    e.message ?: "Unknown crash"
                )
            }
        }
    }
}