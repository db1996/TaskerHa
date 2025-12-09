package com.github.db1996.taskerha.service.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class OnTriggerStateWsEnvelope(
    val type: String,
    val id: Int? = null,
    val event: OnTriggerStateWsEvent? = null
)

@Serializable
data class OnTriggerStateWsEvent(
    val event_type: String,
    val data: OnTriggerStateWsData? = null
)

@Serializable
data class OnTriggerStateWsData(
    val entity_id: String,
    val old_state: OnTriggerStateWsEntity? = null,
    val new_state: OnTriggerStateWsEntity? = null
)

@Serializable
data class OnTriggerStateWsEntity(
    val entity_id: String,
    val state: String,
    val last_changed: String,
    val last_updated: String,
    val last_reported: String,
    val attributes: JsonObject? = null   // <- changed here
)
