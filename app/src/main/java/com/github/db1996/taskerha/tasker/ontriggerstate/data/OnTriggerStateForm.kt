package com.github.db1996.taskerha.tasker.ontriggerstate.data

import com.github.db1996.taskerha.util.SavePrefsJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EntityTriggerConfig(
    val entityId: String = "",
    val targetAttribute: String = "",
    val fromState: String = "",
    val toState: String = "",
    val forDuration: String = ""
)

data class OnTriggerStateForm(
    var entityId: String = "",
    var entityIds: List<String> = emptyList(),
    var entityConfigs: List<EntityTriggerConfig> = emptyList(),
    var sharedConfig: EntityTriggerConfig = EntityTriggerConfig(),
    var configPerEntity: Boolean = false,
    var attributeMapping: Map<String, Int> = emptyMap()
)

val json = Json {
    encodeDefaults = true
    prettyPrint = false
    ignoreUnknownKeys = true
}

@Serializable
data class OnTriggerStateBuiltForm(
    val entityId: String = "",
    val entityIds: List<String> = emptyList(),
    val entityConfigs: List<EntityTriggerConfig> = emptyList(),
    val sharedConfig: EntityTriggerConfig = EntityTriggerConfig(),
    val configPerEntity: Boolean = false,
    val blurb: String = "",
    // Kept for backward-compat deserialization of v0 SharedPrefs JSON
    val fromState: String = "",
    val toState: String = "",
    val forDuration: String = "",
    val version: Int = 0,
    val triggerId: String? = null,
    val attributeMapping: Map<String, Int> = emptyMap()
) : SavePrefsJson {
    override fun prefsName(): String = "TriggerStatePrefs"

    override fun jsonValue(): String = toJson()
    fun toJson(): String = json.encodeToString(this)
}