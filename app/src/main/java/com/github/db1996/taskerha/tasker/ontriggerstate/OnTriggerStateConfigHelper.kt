package com.github.db1996.taskerha.tasker.ontriggerstate

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery

class OnTriggerStateConfigHelper(
    config: TaskerPluginConfig<OnTriggerStateInput>
) : TaskerPluginConfigHelper<OnTriggerStateInput, OnTriggerStateOutput, OnTriggerStateRunner>(config) {

    override val runnerClass = OnTriggerStateRunner::class.java
    override val inputClass = OnTriggerStateInput::class.java
    override val outputClass = OnTriggerStateOutput::class.java
}
fun Context.triggerOnTriggerStateEvent2(rawJson: String) {
    ActivityConfigOnTriggerState::class.java.requestQuery(
        this,
        OnTriggerStateOutput(
            rawJson = rawJson
        )
    )
}