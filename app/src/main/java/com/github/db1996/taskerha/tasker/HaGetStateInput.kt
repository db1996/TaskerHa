// HaGetStateInput.kt
package com.github.db1996.taskerha.tasker

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

    // Becomes %ha_state
    @get:TaskerOutputVariable("ha_state")
    val state: String,

    // Becomes %ha_attrs
    @get:TaskerOutputVariable("ha_attrs")
    val attributesJson: String,

    // Becomes %ha_raw
    @get:TaskerOutputVariable("ha_raw")
    val rawJson: String
)