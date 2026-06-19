package com.github.db1996.taskerha.tasker.messageback.data


data class MessageBackForm(
    var type: String = "",
    var message: String = "",
    var instanceId: String = ""
)

data class MessageBackBuiltForm(
    val blurb: String,
    val type: String,
    val message: String,
    val instanceId: String
)
