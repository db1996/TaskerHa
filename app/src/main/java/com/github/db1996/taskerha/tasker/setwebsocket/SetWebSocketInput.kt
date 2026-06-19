package com.github.db1996.taskerha.tasker.setwebsocket

import com.github.db1996.taskerha.tasker.base.HasInstanceId
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class SetWebSocketInput : HasInstanceId {
    @field:TaskerInputField("enabled")
    var enabled: String = "true"

    @field:TaskerInputField("instanceId")
    override var instanceId: String = ""
}
