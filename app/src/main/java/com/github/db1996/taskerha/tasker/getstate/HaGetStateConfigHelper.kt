package com.github.db1996.taskerha.tasker.getstate

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class HaGetStateConfigHelper(
    config: TaskerPluginConfig<HaGetStateInput>
) : TaskerPluginConfigHelper<HaGetStateInput, HaGetStateOutput, HaGetStateRunner>(config) {

    override val runnerClass = HaGetStateRunner::class.java
    override val inputClass = HaGetStateInput::class.java
    override val outputClass = HaGetStateOutput::class.java
}
