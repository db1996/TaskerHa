package com.github.db1996.taskerha.tasker.getstate

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.BaseTaskerRunner
import com.github.db1996.taskerha.tasker.base.ErrorCodes
import com.github.db1996.taskerha.tasker.base.RunnerResult
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class HaGetStateRunner : BaseTaskerRunner<HaGetStateInput, HaGetStateOutput>() {

    override val needsClient = true

    override val logTag: String
        get() = "HaGetStateRunner"

    override suspend fun executeWithClient(
        context: Context,
        input: HaGetStateInput,
        client: HomeAssistantClient
    ): RunnerResult<HaGetStateOutput> {

        return try {
            if(input.entityId.isBlank()){
                logError("Entity ID cannot be empty")
                return RunnerResult.Error(ErrorCodes.ERROR_CODE_INVALID_INPUT, "Entity ID cannot be empty")
            }

            logInfo("Getting state for entity: ${input.entityId}")
            // Get the state (client already pinged by base class)
            val success = client.getState(input.entityId)
            if (!success) {
                logError("Failed getting state from HA: ${client.error}")
                return RunnerResult.Error(ErrorCodes.ERROR_CODE_API_ERROR, client.error)
            }

            // Parse the response
            val rawJson = client.result
            val jsonElement = Json.parseToJsonElement(rawJson)
            val obj: JsonObject = jsonElement.jsonObject

            val state = obj["state"]?.jsonPrimitive?.content ?: ""

            val attrsAny = obj["attributes"]?.jsonObject ?: JsonObject(emptyMap())

            val attrsMap: Map<String, String> = attrsAny.mapValues { (_, value) ->
                value.toString().trim('"')
            }

            val attrsJson = Json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                attrsMap
            )

            logInfo("Successfully got state: $state, Attributes: $attrsJson")

            RunnerResult.Success(
                HaGetStateOutput(
                    state = state,
                    attributesJson = attrsJson,
                    rawJson = rawJson
                )
            )
        } catch (e: Exception) {
            logError("Unknown crash in HaGetStateRunner", e)
            RunnerResult.Error(ErrorCodes.ERROR_CODE_UNKNOWN, e.message ?: "Unknown crash")
        }
    }
}

