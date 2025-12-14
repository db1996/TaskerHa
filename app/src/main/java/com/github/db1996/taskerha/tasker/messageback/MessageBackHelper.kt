package com.github.db1996.taskerha.tasker.messageback

import android.content.Context
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackInput
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackOutput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery

class MessageBackHelper(
    config: TaskerPluginConfig<MessageBackInput>
) : TaskerPluginConfigHelper<MessageBackInput, MessageBackOutput, MessageBackRunner>(config) {

    override val runnerClass = MessageBackRunner::class.java
    override val inputClass = MessageBackInput::class.java
    override val outputClass = MessageBackOutput::class.java
}