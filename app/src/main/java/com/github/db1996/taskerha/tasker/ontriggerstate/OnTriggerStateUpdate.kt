package com.github.db1996.taskerha.tasker.ontriggerstate

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
@TaskerOutputObject
class OnTriggerStateUpdate @JvmOverloads constructor(

    @field:TaskerInputField("rawJson")
    @get:TaskerOutputVariable("ha_raw")
    var rawJson: String? = null,

    @field:TaskerInputField("entityId")
    @get:TaskerOutputVariable("ha_entity")
    var entityId: String? = null,

    @field:TaskerInputField("state")
    @get:TaskerOutputVariable("ha_state")
    var state: String? = null,

    @field:TaskerInputField("fromState")
    @get:TaskerOutputVariable("ha_from")
    var fromState: String? = null,

    @field:TaskerInputField("toState")
    @get:TaskerOutputVariable("ha_to")
    var toState: String? = null,

    @field:TaskerInputField("attributesJson")
    @get:TaskerOutputVariable("ha_attrs")
    var attributesJson: String? = null
)
