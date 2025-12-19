package com.github.db1996.taskerha.tasker.messageback.data

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class MessageBackInput {
    @field:TaskerInputField("type")
    var type: String = ""

    @field:TaskerInputField("message")
    var message: String = ""
}