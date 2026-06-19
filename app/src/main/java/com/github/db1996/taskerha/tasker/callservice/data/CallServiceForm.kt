package com.github.db1996.taskerha.tasker.callservice.data

import com.github.db1996.taskerha.util.HasServiceKeys

data class CallServiceFormForm(
    var domain: String = "",
    var service: String = "",
    @Deprecated("Migrated to dataContainer with entity_id key", ReplaceWith("dataJson"),
        DeprecationLevel.WARNING)
    var entityId: String = "",
    var dataContainer: MutableMap<String, FieldState> = mutableMapOf(),
    var instanceId: String = ""
)

data class CallServiceFormBuiltForm(
    val domain: String,
    val service: String,
    @Deprecated("Migrated to data with entity_id key", ReplaceWith("dataJson"),
        DeprecationLevel.WARNING)
    val entityId: String,
    val data: Map<String, String>,
    val instanceId: String,
    val blurb: String
) : HasServiceKeys{
    override fun serviceKeys(): List<String> = listOf("$domain.$service")
}

