package com.github.db1996.taskerha.tasker.ontriggerstate

import android.content.Context
import android.util.Log
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
fun Context.triggerOnTriggerStateTestEvent(rawJson: String) {
    Log.e("OnTriggerState", "Sending test event to Tasker")
    ActivityConfigOnTriggerState::class.java.requestQuery(
        this,
        OnTriggerStateUpdate(
            rawJson = rawJson
        )
    )
}