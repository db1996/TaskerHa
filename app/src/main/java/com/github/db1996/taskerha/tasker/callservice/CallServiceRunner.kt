package com.github.db1996.taskerha.tasker.callservice

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.BaseTaskerRunner
import com.github.db1996.taskerha.tasker.base.ErrorCodes
import com.github.db1996.taskerha.tasker.base.RunnerResult
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class CallServiceRunner : BaseTaskerRunner<CallServiceInput, CallServiceOutput>() {

    override val needsClient = true

    override val logTag: String
        get() = "CallServiceRunner"

    override suspend fun executeWithClient(
        context: Context,
        input: CallServiceInput,
        client: HomeAssistantClient
    ): RunnerResult<CallServiceOutput> {

        return try {
            if(input.domain.isBlank() && input.service.isBlank()){
                logError("Domain and service cannot be empty")
                return RunnerResult.Error(ErrorCodes.ERROR_CODE_INVALID_INPUT, "Domain and service cannot be empty")
            }


            val dataMap: Map<String, String> = try {
                Json.Default.decodeFromString(
                    MapSerializer(String.serializer(), String.serializer()),
                    input.dataJson
                )
            } catch (e: Exception) {
                logError("Invalid JSON Data: ${e.message}", e)
                return RunnerResult.Error(ErrorCodes.ERROR_CODE_API_ERROR, client.error)
            }

            val targetKeys = setOf("entity_id", "device_id", "area_id", "label_id")
            val hasTargetKeys = targetKeys.any { dataMap.getOrDefault(it, "").isNotBlank() }

            logInfo("Calling service: ${input.domain}.${input.service} on entity: ${input.entityId}")
            val ok = if (hasTargetKeys) {
                fun csv(key: String): List<String> =
                    dataMap.getOrDefault(key, "").split(",").filter { it.isNotBlank() }
                val target = mutableMapOf<String, List<String>>()
                csv("entity_id").takeIf { it.isNotEmpty() }?.let { target["entity_id"] = it }
                csv("device_id").takeIf { it.isNotEmpty() }?.let { target["device_id"] = it }
                csv("area_id").takeIf { it.isNotEmpty() }?.let { target["area_id"] = it }
                csv("label_id").takeIf { it.isNotEmpty() }?.let { target["label_id"] = it }
                val cleanData: Map<String, Any> = dataMap.filterKeys { it !in targetKeys }
                client.callService(input.domain, input.service, target = target, data = cleanData)
            } else {
                val entityId = input.entityId.ifEmpty { dataMap.getOrDefault("entity_id", "") }
                val cleanData: Map<String, Any> = dataMap.filterKeys { it != "entity_id" }
                client.callService(input.domain, input.service, entityId, cleanData)
            }

            if (!ok) {
                logError("Failed calling service: ${client.error}")
                return RunnerResult.Error(ErrorCodes.ERROR_CODE_API_ERROR, client.error)
            }
            logInfo("Service called successfully ${ok}, ${client.result}")
            RunnerResult.Success(
                CallServiceOutput(
                    dataJson = client.result
                )
            )
        } catch (e: Exception) {
            logError("Unknown crash", e)
            RunnerResult.Error(ErrorCodes.ERROR_CODE_UNKNOWN, e.message ?: "Unknown crash")
        }
    }
}

