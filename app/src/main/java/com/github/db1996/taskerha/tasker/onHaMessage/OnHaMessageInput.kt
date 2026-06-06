package com.github.db1996.taskerha.tasker.onHaMessage

import com.github.db1996.taskerha.tasker.base.HasInstanceId
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class OnHaMessageInput : HasInstanceId {

    @field:TaskerInputField("type")
    var type: String = ""

    @field:TaskerInputField("message")
    var message: String = ""

    @field:TaskerInputField("instanceId")
    override var instanceId: String = ""
}