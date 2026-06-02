package com.github.db1996.taskerha.tasker.testconnection

import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerOutputObject
class TestConnectionOutput @JvmOverloads constructor(
    @get:TaskerOutputVariable("ha_remote")
    var haRemote: String? = null,

    @get:TaskerOutputVariable("ha_local")
    var haLocal: String? = null
)
