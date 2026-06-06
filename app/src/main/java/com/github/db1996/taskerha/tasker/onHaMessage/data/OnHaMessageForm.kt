package com.github.db1996.taskerha.tasker.onHaMessage.data


data class OnHaMessageForm(
    var type: String = "",
    var message: String = "",
    var instanceId: String = ""
)

data class OnHaMessageBuiltForm(
    val blurb: String,
    val type: String,
    val message: String,
    val instanceId: String
)
