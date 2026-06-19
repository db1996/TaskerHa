package com.github.db1996.taskerha.tasker.testconnection

import com.github.db1996.taskerha.tasker.base.HasInstanceId
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class TestConnectionInput : HasInstanceId {
    @TaskerInputField("dummy")
    var dummy: String = ""

    @field:TaskerInputField("instanceId")
    override var instanceId: String = ""
}
