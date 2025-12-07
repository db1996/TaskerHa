package com.github.db1996.taskerha.tasker.ontriggerstate

import android.content.Context
import android.util.Log
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OnTriggerStateRunner :
    TaskerPluginRunnerConditionEvent<
            OnTriggerStateInput,
            OnTriggerStateUpdate,
            OnTriggerStateUpdate
            >() {

    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<OnTriggerStateInput>,
        update: OnTriggerStateUpdate?
    ): TaskerPluginResultCondition<OnTriggerStateUpdate> {
        if (update?.rawJson.isNullOrBlank()) {
            return TaskerPluginResultConditionUnsatisfied()
        }


        val config = input.regular
        Log.d("OnTriggerStateRunner", "config: entity=${config.entityId}, from=${config.fromState}, to=${config.toState}")

        val raw = update.rawJson!!
        val jsonElement = try {
            Json.parseToJsonElement(raw)
        } catch (e: Exception) {
            Log.e("OnTriggerStateRunner", "JSON parse failed: ${e.message}")
            return TaskerPluginResultConditionUnsatisfied()
        }

        val root = jsonElement.jsonObject
        val trigger = root["event"]
            ?.jsonObject
            ?.get("variables")
            ?.jsonObject
            ?.get("trigger")
            ?.jsonObject ?: return TaskerPluginResultConditionUnsatisfied()

        val entityId = trigger["entity_id"]?.jsonPrimitive?.content ?: ""
        val fromStateObj = trigger["from_state"]?.jsonObject
        val toStateObj = trigger["to_state"]?.jsonObject

        val fromState = fromStateObj?.get("state")?.jsonPrimitive?.content
        val toState = toStateObj?.get("state")?.jsonPrimitive?.content

        val attributesMap: Map<String, String> = toStateObj
            ?.get("attributes")
            ?.jsonObject
            ?.mapValues { (_, v) -> v.toString().trim('"') }
            ?: emptyMap()

        val attrsJson = Json.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            attributesMap
        )

        update.entityId = entityId
        update.fromState = fromState
        update.toState = toState
        update.state = toState
        update.attributesJson = attrsJson

        fun matches(configVal: String, eventVal: String?): Boolean {
            if (configVal.isBlank()) return true
            if (eventVal == null) return false
            return configVal == eventVal
        }

        Log.d("OnTriggerStateRunner", "update: entity=${update.entityId}, from=${update.fromState}, to=${update.toState}")
        if (!matches(config.entityId.trim(), entityId)) {
            return TaskerPluginResultConditionUnsatisfied()
        }
        if (!matches(config.fromState.trim(), fromState)) {
            return TaskerPluginResultConditionUnsatisfied()
        }
        if (!matches(config.toState.trim(), toState)) {
            return TaskerPluginResultConditionUnsatisfied()
        }

        return TaskerPluginResultConditionSatisfied(context, update)
    }
}
