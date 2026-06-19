package com.github.db1996.taskerha.tasker.ontriggerstate

import com.github.db1996.taskerha.tasker.base.HasInstanceId
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class OnTriggerStateInput : HasInstanceId {

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

    @field:TaskerInputField("targetAttribute")
    var targetAttribute: String = ""

    @field:TaskerInputField("version")
    var version: String = ""

    @field:TaskerInputField("configPerEntity")
    var configPerEntity: String = ""

    @field:TaskerInputField("instanceId")
    override var instanceId: String = ""
}
