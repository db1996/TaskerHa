package com.github.db1996.taskerha.tasker.onHaMessage

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class OnHaMessageInput {

    @field:TaskerInputField("type")
    var type: String = ""

    @field:TaskerInputField("message")
    var message: String = ""
}