package com.github.db1996.taskerha.tasker.ontriggerstate.data


data class OnTriggerStateForm(
    var entityId: String = "",
    var fromState: String = "",
    var toState: String = ""
)

data class OnTriggerStateBuiltForm(
    val entityId: String,
    val blurb: String,
    val fromState: String,
    val toState: String
)
