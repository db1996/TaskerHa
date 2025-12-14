package com.github.db1996.taskerha.tasker.messageback

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.BaseTaskerRunner
import com.github.db1996.taskerha.tasker.base.ErrorCodes
import com.github.db1996.taskerha.tasker.base.RunnerResult
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackInput
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackOutput

class MessageBackRunner : BaseTaskerRunner<MessageBackInput, MessageBackOutput>() {

    override val needsClient = true

    override val logTag: String
        get() = "MessageBackRunner"

    override suspend fun executeWithClient(
        context: Context,
        input: MessageBackInput,
        client: HomeAssistantClient
    ): RunnerResult<MessageBackOutput> {

        return try {
            logInfo("Triggering event taskerha_message_back: ${input.type}, ${input.message}")
            val payload = mutableMapOf<String, Any>()

            if(input.type.isNotEmpty())
                payload["type"] = input.type
            if(input.message.isNotEmpty())
                payload["message"] = input.message

            logInfo("Payload: $payload")

            var actualPayload : Map<String, Any>? = null
            if(!payload.isEmpty()){
                actualPayload = payload;
            }
            val ok = client.fireEvent("taskerha_message_back", actualPayload)

            if(ok){
                RunnerResult.Success(
                    MessageBackOutput(
                        type = input.type,
                        message = input.message,
                        response = client.result
                    )
                )
            }else{
                logError("Error sending event", Exception(client.error))
                RunnerResult.Error(ErrorCodes.ERROR_CODE_UNKNOWN, client.error)
            }
        } catch (e: Exception) {
            logError("Unknown crash", e)
            RunnerResult.Error(ErrorCodes.ERROR_CODE_UNKNOWN, e.message ?: "Unknown crash")
        }
    }
}

