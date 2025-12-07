package com.github.db1996.taskerha.tasker.ontriggerstate

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperEventNoOutputOrInputOrUpdate
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoInput
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class OnTriggerStateHelper(
    config: TaskerPluginConfig<Unit>
) : TaskerPluginConfigHelperEventNoOutputOrInputOrUpdate(config) {

    override fun addToStringBlurb(
        input: TaskerInput<Unit>,
        blurbBuilder: StringBuilder
    ) {
        blurbBuilder.append("Home Assistant OnTriggerState (no filters yet)")
    }
}

class ActivityConfigOnTriggerState :
    Activity(),
    TaskerPluginConfigNoInput {

    override val context: Context
        get() = applicationContext

    private val helper by lazy { OnTriggerStateHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper.finishForTasker()
    }
}

fun Context.triggerOnTriggerState() {
    ActivityConfigOnTriggerState::class.java.requestQuery(this)
}
