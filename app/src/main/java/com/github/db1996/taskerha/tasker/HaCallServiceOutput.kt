package com.github.db1996.taskerha.tasker

import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerOutputObject
class HaCallServiceOutput(

    // This becomes %ha_data in Tasker (Tasker adds the %)
    @get:TaskerOutputVariable("ha_data")
    val dataJson: String
)
