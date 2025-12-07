package com.github.db1996.taskerha.tasker

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class HaCallServiceConfigHelper(
    config: TaskerPluginConfig<HaCallServiceInput>
) : TaskerPluginConfigHelper<HaCallServiceInput, HaCallServiceOutput, HaCallServiceRunner>(config) {

    override val runnerClass = HaCallServiceRunner::class.java
    override val inputClass = HaCallServiceInput::class.java
    override val outputClass = HaCallServiceOutput::class.java

    override fun addToStringBlurb(
        input: TaskerInput<HaCallServiceInput>,
        blurbBuilder: StringBuilder
    ) {
//        blurbBuilder.append("Service: ${input.regular.domain}.${input.regular.service}")
//        if (input.regular.entityId.isNotEmpty()) {
//            blurbBuilder.append("\nEntity: ${input.regular.entityId}")
//        }
    }
}
