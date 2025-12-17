package com.github.db1996.taskerha.tasker.ontriggerstate.data

import com.github.db1996.taskerha.util.SavePrefsJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


data class OnTriggerStateForm(
    var entityId: String = "",
    var fromState: String = "",
    var toState: String = "",
    var forDuration: String = ""
)

val json = Json {
    encodeDefaults = true
    prettyPrint = false
    ignoreUnknownKeys = true
}

@Serializable
data class OnTriggerStateBuiltForm(
    val entityId: String,
    val blurb: String,
    val fromState: String,
    val toState: String,
    val forDuration: String
) : SavePrefsJson {
    override fun prefsName(): String = "TriggerStatePrefs"

    override fun jsonValue(): String = toJson()
    fun toJson(): String = json.encodeToString(this)
}