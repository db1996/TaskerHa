package com.github.db1996.taskerha.tasker.callservice

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
class HaCallServiceInput {
    @field:TaskerInputField("domain")
    var domain: String = ""

    @field:TaskerInputField("service")
    var service: String = ""

    @field:TaskerInputField("entityId")
    var entityId: String = ""
    @field:TaskerInputField("dataJson")
    var dataJson: String = "{}"
}

@TaskerOutputObject
class HaCallServiceOutput(

    @get:TaskerOutputVariable(
        name = "ha_data",
        labelResIdName = "ha_call_service_data_label"
    )
    val dataJson: String
)
