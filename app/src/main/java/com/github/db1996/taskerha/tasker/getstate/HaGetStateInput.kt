package com.github.db1996.taskerha.tasker.getstate

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
class HaGetStateInput {
    @field:TaskerInputField("entityId")
    var entityId: String = ""
}

@TaskerOutputObject
class HaGetStateOutput(
    @get:TaskerOutputVariable(
        name = "ha_state",
        labelResIdName = "ha_get_state_state_label",
    )
    val state: String,

    @get:TaskerOutputVariable(
        name = "ha_attrs",
        labelResIdName = "ha_get_state_attrs_label",
    )
    val attributesJson: String,

    @get:TaskerOutputVariable(
        name = "ha_raw",
        labelResIdName = "ha_raw_label",
    )
    val rawJson: String
)

