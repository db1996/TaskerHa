package com.github.db1996.taskerha.service.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscribeTriggerRequest(
    val id: Int,
    val type: String,
    val trigger: List<StateTrigger>
)

@Serializable
data class StateTrigger(
    val platform: String,
    val entity_id: String,

    // HA expects these keys on the trigger. Omit when blank.
    val from: String?,
    val to: String?,

    @SerialName("for")
    val for_: HaForDuration
)

@Serializable
data class HaForDuration(
    val hours: Long = 0,
    val minutes: Long = 0,
    val seconds: Long = 0
)
