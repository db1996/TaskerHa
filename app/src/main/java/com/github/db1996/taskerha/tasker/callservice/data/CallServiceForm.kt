package com.github.db1996.taskerha.tasker.callservice.data

import com.github.db1996.taskerha.util.HasServiceKeys

data class CallServiceFormForm(
    var domain: String = "",
    var service: String = "",
    var entityId: String = "",
    var dataContainer: MutableMap<String, FieldState> = mutableMapOf()
)

data class CallServiceFormBuiltForm(
    val domain: String,
    val service: String,
    val entityId: String,
    val data: Map<String, String>,
    val blurb: String
) : HasServiceKeys{
    override fun serviceKeys(): List<String> = listOf("$domain.$service")
}

