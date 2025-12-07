package com.github.db1996.taskerha.tasker.ontriggerstate

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class OnTriggerStateInput {

    @field:TaskerInputField("entityId")
    var entityId: String = ""

    @field:TaskerInputField("fromState")
    var fromState: String = ""

    @field:TaskerInputField("toState")
    var toState: String = ""
}
