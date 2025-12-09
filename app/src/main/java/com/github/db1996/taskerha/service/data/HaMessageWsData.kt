package com.github.db1996.taskerha.service.data

import kotlinx.serialization.Serializable


@Serializable
data class HaMessageWsEnvelope(
    val type: String,
    val id: Int? = null,
    val event: HaMessageWsEvent? = null
)

@Serializable
data class HaMessageWsEvent(
    val event_type: String,
    val data: HaMessageWsData? = null
)

@Serializable
data class HaMessageWsData(
    val type: String? = null,
    val message: String? = null
)
