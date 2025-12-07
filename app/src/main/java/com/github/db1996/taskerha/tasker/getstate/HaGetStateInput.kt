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

    @get:TaskerOutputVariable("ha_state")
    val state: String,

    @get:TaskerOutputVariable("ha_attrs")
    val attributesJson: String,

    @get:TaskerOutputVariable("ha_raw")
    val rawJson: String
)