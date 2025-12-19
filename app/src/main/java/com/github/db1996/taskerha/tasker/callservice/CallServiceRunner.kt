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


            val dataMap = try {
                Json.Default.decodeFromString(
                    MapSerializer(String.serializer(), String.serializer()),
                    input.dataJson
                ).mapValues { it.value as Any }
            } catch (e: Exception) {
                logError("Invalid JSON Data: ${e.message}", e)
                return RunnerResult.Error(ErrorCodes.ERROR_CODE_API_ERROR, client.error)
            }

            logInfo("Calling service: ${input.domain}.${input.service} on entity: ${input.entityId}")
            val ok = client.callService(
                input.domain,
                input.service,
                input.entityId,
                dataMap
            )

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

