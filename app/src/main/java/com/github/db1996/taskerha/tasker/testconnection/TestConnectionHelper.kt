package com.github.db1996.taskerha.tasker.testconnection

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class TestConnectionHelper(
    config: TaskerPluginConfig<TestConnectionInput>
) : TaskerPluginConfigHelper<TestConnectionInput, TestConnectionOutput, TestConnectionRunner>(config) {

    override val runnerClass = TestConnectionRunner::class.java
    override val inputClass = TestConnectionInput::class.java
    override val outputClass = TestConnectionOutput::class.java
}
