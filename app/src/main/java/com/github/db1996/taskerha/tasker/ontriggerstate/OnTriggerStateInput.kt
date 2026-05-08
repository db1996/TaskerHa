package com.github.db1996.taskerha.tasker.ontriggerstate

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class OnTriggerStateInput {

    @field:TaskerInputField("entityId")
    var entityId: String = ""

    @field:TaskerInputField("entityIds")
    var entityIds: String = ""

    @field:TaskerInputField("fromState")
    var fromState: String = ""

    @field:TaskerInputField("toState")
    var toState: String = ""

    @field:TaskerInputField("forDuration")
    var forDuration: String = ""

    @field:TaskerInputField("triggerId")
    var triggerId: String = ""

    @field:TaskerInputField("attributeMappingJson")
    var attributeMappingJson: String = "{}"
}
