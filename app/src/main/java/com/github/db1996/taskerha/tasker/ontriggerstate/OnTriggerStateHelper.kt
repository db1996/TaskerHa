package com.github.db1996.taskerha.tasker.ontriggerstate

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery

class OnTriggerStateHelper(
    config: TaskerPluginConfig<OnTriggerStateInput>
) : TaskerPluginConfigHelper<OnTriggerStateInput, OnTriggerStateUpdate, OnTriggerStateRunner>(config) {

    override val runnerClass = OnTriggerStateRunner::class.java
    override val inputClass = OnTriggerStateInput::class.java
    override val outputClass = OnTriggerStateUpdate::class.java
}
fun Context.triggerOnTriggerStateEvent(rawJson: String) {
    ActivityConfigOnTriggerState::class.java.requestQuery(
        this,
        OnTriggerStateUpdate(
            rawJson = rawJson
        )
    )
}