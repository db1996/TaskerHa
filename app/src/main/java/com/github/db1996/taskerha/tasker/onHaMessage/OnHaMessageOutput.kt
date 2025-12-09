package com.github.db1996.taskerha.tasker.onHaMessage

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
@TaskerOutputObject
class OnHaMessageOutput @JvmOverloads constructor(

    @field:TaskerInputField("type")
    @get:TaskerOutputVariable("ha_type", labelResIdName = "on_ha_message_type_label")
    var type: String? = null,

    @field:TaskerInputField("message")
    @get:TaskerOutputVariable("ha_message", labelResIdName = "on_ha_message_message_label")
    var message: String? = null,
)
