package com.github.db1996.taskerha.tasker.onHaMessage

import android.content.Context
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery

class OnHaMessageHelper(
    config: TaskerPluginConfig<OnHaMessageInput>
) : TaskerPluginConfigHelper<OnHaMessageInput, OnHaMessageOutput, OnHaMessageRunner>(config) {

    override val runnerClass = OnHaMessageRunner::class.java
    override val inputClass = OnHaMessageInput::class.java
    override val outputClass = OnHaMessageOutput::class.java
}
fun Context.triggerOnHaMessageHelper2(type: String?, message: String?) {
    OnHaMessageActivity::class.java.requestQuery(
        this,
        OnHaMessageOutput(
            type = type,
            message = message
        )
    )
}