package com.github.db1996.taskerha.tasker.ontriggerstate

import android.content.Context
import android.util.Log
import com.github.db1996.taskerha.service.data.HaWsEnvelope
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
        Log.d("HaWebSocketService", "config: entity=${config.entityId}, from=${config.fromState}, to=${config.toState}")

        val raw = update.rawJson!!
        val envelope = try {
            json.decodeFromString<HaWsEnvelope>(raw)
        } catch (e: Exception) {
            Log.e("HaWebSocketService", "Invalid WS JSON: ${e.message}")
            return TaskerPluginResultConditionUnsatisfied()
        }

        update.entityId = envelope.event?.data?.entity_id
        update.fromState = envelope.event?.data?.old_state?.state
        update.toState = envelope.event?.data?.new_state?.state
        val attrsJson = envelope.event?.data?.new_state?.attributes?.toString()
        update.state = envelope.event?.data?.new_state?.state
        update.attributesJson = attrsJson


        fun matches(configVal: String, eventVal: String?): Boolean {
            if (configVal.isBlank()) return true
            if (eventVal == null) return false
            return configVal == eventVal
        }
        Log.d("HaWebSocketService", "update: entity=${update.entityId}, from=${update.fromState}, to=${update.toState}")

        if (!matches(config.entityId.trim(), update.entityId)) {
            return TaskerPluginResultConditionUnsatisfied()
        }
        if (!matches(config.fromState.trim(), update.fromState)) {
            return TaskerPluginResultConditionUnsatisfied()
        }
        if (!matches(config.toState.trim(), update.toState)) {
            return TaskerPluginResultConditionUnsatisfied()
        }

        return TaskerPluginResultConditionSatisfied(context, update)
    }
}
