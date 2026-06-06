package com.github.db1996.taskerha.tasker.callservice

import com.github.db1996.taskerha.tasker.base.HasInstanceId
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
class CallServiceInput : HasInstanceId {
    @field:TaskerInputField("domain")
    var domain: String = ""

    @field:TaskerInputField("service")
    var service: String = ""

    @Deprecated("Migrated to dataJson with entity_id key", ReplaceWith("dataJson"),
        DeprecationLevel.WARNING)
    @field:TaskerInputField("entityId")
    var entityId: String = ""

    @field:TaskerInputField("dataJson")
    var dataJson: String = "{}"

    @field:TaskerInputField("instanceId")
    override var instanceId: String = ""
}

@TaskerOutputObject
class CallServiceOutput(
    @get:TaskerOutputVariable(
        name = "ha_data",
        labelResIdName = "ha_call_service_data_label"
    )
    val dataJson: String
)

