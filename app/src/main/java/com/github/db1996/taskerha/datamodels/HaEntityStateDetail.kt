package com.github.db1996.taskerha.datamodels

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HaEntityStateDetail(
    val entity_id: String,
    val state: String = "",
    val attributes: Map<String, JsonElement> = emptyMap()
)
