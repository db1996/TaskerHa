package com.github.db1996.taskerha.tasker.ontriggerstate

import android.content.Context
import android.util.Log
import com.github.db1996.taskerha.service.data.OnTriggerStateWsEnvelope
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied
import kotlinx.serialization.json.Json

class OnTriggerStateRunner :
    TaskerPluginRunnerConditionEvent<
            OnTriggerStateInput,
            OnTriggerStateUpdate,
            OnTriggerStateUpdate
            >() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<OnTriggerStateInput>,
        update: OnTriggerStateUpdate?
    ): TaskerPluginResultCondition<OnTriggerStateUpdate> {
        if (update?.rawJson.isNullOrBlank()) {
            return TaskerPluginResultConditionUnsatisfied()
        }

        val config = input.regular

        val raw = update.rawJson!!
        val envelope = try {
            json.decodeFromString<OnTriggerStateWsEnvelope>(raw)
        } catch (e: Exception) {
            Log.e("HaWebSocketService", "Invalid WS JSON1: ${e.message}")
            return TaskerPluginResultConditionUnsatisfied()
        }

        update.entityId = envelope.event?.data?.entity_id
        update.fromState = envelope.event?.data?.old_state?.state
        update.toState = envelope.event?.data?.new_state?.state
        val attrsJson = envelope.event?.data?.new_state?.attributes?.toString()
        update.attributesJson = attrsJson


        fun matches(configVal: String, eventVal: String?): Boolean {
            if (configVal.isBlank()) return true
            if (eventVal == null) return false
            return configVal == eventVal
        }

        if(update.entityId != null && config.entityId != ""){
            if (!matches(config.entityId.trim(), update.entityId)) {
                return TaskerPluginResultConditionUnsatisfied()
            }
            if (!matches(config.fromState.trim(), update.fromState)) {
                return TaskerPluginResultConditionUnsatisfied()
            }
            if (!matches(config.toState.trim(), update.toState)) {
                return TaskerPluginResultConditionUnsatisfied()
            }
        }

//        Log.d("HaWebSocketService", "update: entity=${update.entityId}, from=${update.fromState}, to=${update.toState}")
        return TaskerPluginResultConditionSatisfied(context, update)
    }
}
