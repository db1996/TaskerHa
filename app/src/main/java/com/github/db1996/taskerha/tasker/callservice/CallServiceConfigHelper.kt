package com.github.db1996.taskerha.tasker.callservice

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class CallServiceConfigHelper(config: TaskerPluginConfig<CallServiceInput>) :
    TaskerPluginConfigHelper<CallServiceInput, CallServiceOutput, CallServiceRunner>(config) {

    override val inputClass = CallServiceInput::class.java
    override val outputClass = CallServiceOutput::class.java
    override val runnerClass = CallServiceRunner::class.java
}

