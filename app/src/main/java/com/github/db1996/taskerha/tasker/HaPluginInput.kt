package com.github.db1996.taskerha.tasker

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class HaPluginInput {
    @field:TaskerInputField("domain")
    var domain: String = ""

    @field:TaskerInputField("service")
    var service: String = ""

    @field:TaskerInputField("entityId")
    var entityId: String = ""

    // We keep the data map as a JSON string to simplify migration
    // and because keys are dynamic
    @field:TaskerInputField("dataJson")
    var dataJson: String = "{}"
}