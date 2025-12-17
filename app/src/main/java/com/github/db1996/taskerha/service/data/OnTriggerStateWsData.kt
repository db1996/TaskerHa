package com.github.db1996.taskerha.service.data
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject


@Serializable
data class OnSmallMessageEvent(
    val type: String,
    val id: Int? = null,
)

@Serializable
data class OnTriggerStateEnvelope(
    val type: String,
    val id: Int? = null,
    val event: OnTriggerStateEvent? = null
)

@Serializable
data class OnTriggerStateEvent(
    val variables: Map<String, OnTriggerStateTriggerWsData> = emptyMap(),
    val context: OnTriggerStateStateContext
)

@Serializable
data class OnTriggerStateTriggerWsData(
    val alias: String?,
    val platform: String?,
    val entity_id: String?,
    val from_state: OnTriggerStateState,
    val to_state: OnTriggerStateState,

    @SerialName("for")
    val for_: OnTriggerStateWsTriggerFor?
)

@Serializable
data class OnTriggerStateWsTriggerFor(
    val __type: String? = null,
    val total_seconds: Double? = null,
    val total_hours: Double? = null,
    val total_minutes: Double? = null,
)

@Serializable
data class OnTriggerStateState(
    val entity_id: String,
    val state: String,
    val attributes: Map<String, JsonElement> = emptyMap(),
    val last_changed: String,
    val last_updated: String,
    val last_reported: String,
    val context: JsonObject
)

@Serializable
data class OnTriggerStateStateContext(
    val id: String,
    val parent_id: String? = null,
    val user_id: String? = null,
)
