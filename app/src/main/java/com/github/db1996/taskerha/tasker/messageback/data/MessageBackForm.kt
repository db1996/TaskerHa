package com.github.db1996.taskerha.tasker.messageback.data


data class MessageBackForm(
    var type: String = "",
    var message: String = "",
)

data class MessageBackBuiltForm(
    val blurb: String,
    val type: String,
    val message: String
)
