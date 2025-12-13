package com.github.db1996.taskerha.tasker.ontriggerstate

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
@TaskerOutputObject
class OnTriggerStateOutput @JvmOverloads constructor(

    @field:TaskerInputField("rawJson")
    @get:TaskerOutputVariable("ha_raw", labelResIdName = "ha_raw_label")
    var rawJson: String? = null,

    @field:TaskerInputField("entityId")
    @get:TaskerOutputVariable("ha_entity", labelResIdName = "ha_on_trigger_entity_label")
    var entityId: String? = null,

    @field:TaskerInputField("fromState")
    @get:TaskerOutputVariable("ha_from", labelResIdName = "ha_on_trigger_from_label")
    var fromState: String? = null,

    @field:TaskerInputField("toState")
    @get:TaskerOutputVariable("ha_to", labelResIdName = "ha_on_trigger_to_label")
    var toState: String? = null,

    @field:TaskerInputField("attributesJson")
    @get:TaskerOutputVariable("ha_attrs", labelResIdName = "ha_on_trigger_attrs_label")
    var attributesJson: String? = null
)
