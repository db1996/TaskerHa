package com.github.db1996.taskerha.tasker

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class HaConfigHelper(
    config: TaskerPluginConfig<HaPluginInput>
) : TaskerPluginConfigHelper<HaPluginInput, HaCallServiceOutput, HaActionRunner>(config) {

    override val runnerClass = HaActionRunner::class.java
    override val inputClass = HaPluginInput::class.java
    override val outputClass = HaCallServiceOutput::class.java

    override fun addToStringBlurb(
        input: TaskerInput<HaPluginInput>,
        blurbBuilder: StringBuilder
    ) {
        blurbBuilder.append("Service: ${input.regular.domain}.${input.regular.service}")
        if (input.regular.entityId.isNotEmpty()) {
            blurbBuilder.append("\nEntity: ${input.regular.entityId}")
        }
    }
}
