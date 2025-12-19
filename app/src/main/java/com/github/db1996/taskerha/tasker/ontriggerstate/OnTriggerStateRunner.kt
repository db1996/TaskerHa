package com.github.db1996.taskerha.tasker.ontriggerstate

import android.content.Context
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.service.data.OnTriggerStateEnvelope
import com.github.db1996.taskerha.service.data.OnTriggerStateTriggerWsData
import com.github.db1996.taskerha.service.data.OnTriggerStateWsTriggerFor
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied
import kotlinx.serialization.json.Json

class OnTriggerStateRunner :
    TaskerPluginRunnerConditionEvent<
            OnTriggerStateInput,
            OnTriggerStateOutput,
            OnTriggerStateOutput
            >() {
    private val json = Json { ignoreUnknownKeys = true }

    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<OnTriggerStateInput>,
        update: OnTriggerStateOutput?
    ): TaskerPluginResultCondition<OnTriggerStateOutput> {
        if (update?.rawJson.isNullOrBlank()) {
            CustomLogger.e("OnTriggerStateRunner", "update is null or blank")
            return TaskerPluginResultConditionUnsatisfied()
        }

        val config = input.regular
        val raw = update.rawJson!!
        val envelope = try {
            json.decodeFromString<OnTriggerStateTriggerWsData>(raw)
        } catch (e: Exception) {
            CustomLogger.e("OnTriggerStateRunner", "Invalid WS JSON1: ${e.message}")
            return TaskerPluginResultConditionUnsatisfied()
        }

        update.entityId = envelope.entity_id
        update.fromState = envelope.from_state.state
        update.toState = envelope.to_state.state
        val attrsJson = envelope.to_state.attributes.toString()
        update.attributesJson = attrsJson
        val eventFor = wsForToDurationString(envelope.for_)
        update.forDuration = eventFor

        fun matches(configVal: String, eventVal: String?): Boolean {
            if (configVal.isBlank()) return true
            if (eventVal == null) return false
            return configVal == eventVal
        }
        fun matchesFor(configVal: String, eventVal: String): Boolean {
            // blank means "ignore"
            if (configVal.isBlank()) return true
            return configVal.trim() == eventVal
        }
        if(update.entityId != null && config.entityId != ""){
            if (!matches(config.entityId.trim(), update.entityId)) {
                return TaskerPluginResultConditionUnsatisfied()
            }

            if(update.fromState != null){
                if (!matches(config.fromState.trim(), update.fromState)) {
                    return TaskerPluginResultConditionUnsatisfied()
                }
            }else{
                if(config.toState.isNotBlank()) {
                    return TaskerPluginResultConditionUnsatisfied()
                }
            }

            if(update.toState != null) {
                if (!matches(config.toState.trim(), update.toState)) {
                    return TaskerPluginResultConditionUnsatisfied()
                }
            }else{
                if(config.toState.isNotBlank()) {
                    return TaskerPluginResultConditionUnsatisfied()
                }
            }

            if (!matchesFor(config.forDuration, eventFor)) {
                return TaskerPluginResultConditionUnsatisfied()
            }
        }
        CustomLogger.i("OnTriggerStateRunner", "update fired for event: entity=${update.entityId}, from=${update.fromState}, to=${update.toState}")
        return TaskerPluginResultConditionSatisfied(context, update)
    }

    private fun wsForToDurationString(forData: OnTriggerStateWsTriggerFor?): String {
        if (forData == null) return ""

        val totalSeconds = (forData.total_seconds ?: 0.0).toLong()
        if (totalSeconds <= 0L) return ""

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        // Match your UI output style: allow empty parts.
        // - If hours == 0 => ""
        // - If minutes == 0 => ""
        // - seconds always present when totalSeconds > 0
        val h = if (hours == 0L) "" else hours.toString()
        val m = if (minutes == 0L) "" else minutes.toString()
        val s = seconds.toString()

        return "$h:$m:$s"
    }

}
