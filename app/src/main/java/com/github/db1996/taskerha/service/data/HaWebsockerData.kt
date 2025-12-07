package com.github.db1996.taskerha.service.data
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class HaWsEnvelope(
    val type: String,
    val id: Int? = null,
    val event: HaWsEvent? = null
)

@Serializable
data class HaWsEvent(
    val event_type: String,
    val data: HaWsEventData? = null
)

@Serializable
data class HaWsEventData(
    val entity_id: String,
    val old_state: HaWsState? = null,
    val new_state: HaWsState? = null
)

@Serializable
data class HaWsState(
    val entity_id: String,
    val state: String,
    val last_changed: String,
    val last_updated: String,
    val last_reported: String,
    val attributes: JsonObject? = null   // <- changed here
)
