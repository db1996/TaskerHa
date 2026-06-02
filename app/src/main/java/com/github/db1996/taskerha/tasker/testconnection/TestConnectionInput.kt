package com.github.db1996.taskerha.tasker.testconnection

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class TestConnectionInput {
    @TaskerInputField("dummy")
    var dummy: String = ""
}
