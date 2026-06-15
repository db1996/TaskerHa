package com.github.db1996.taskerha.tasker.setwebsocket

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class SetWebSocketHelper(
    config: TaskerPluginConfig<SetWebSocketInput>
) : TaskerPluginConfigHelper<SetWebSocketInput, SetWebSocketOutput, SetWebSocketRunner>(config) {
    override val runnerClass = SetWebSocketRunner::class.java
    override val inputClass = SetWebSocketInput::class.java
    override val outputClass = SetWebSocketOutput::class.java
}
