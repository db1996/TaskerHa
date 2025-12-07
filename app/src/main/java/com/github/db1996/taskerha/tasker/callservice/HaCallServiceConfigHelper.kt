package com.github.db1996.taskerha.tasker.callservice

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class HaCallServiceConfigHelper(
    config: TaskerPluginConfig<HaCallServiceInput>
) : TaskerPluginConfigHelper<HaCallServiceInput, HaCallServiceOutput, HaCallServiceRunner>(config) {

    override val runnerClass = HaCallServiceRunner::class.java
    override val inputClass = HaCallServiceInput::class.java
    override val outputClass = HaCallServiceOutput::class.java
}
