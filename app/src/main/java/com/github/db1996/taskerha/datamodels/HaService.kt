package com.github.db1996.taskerha.datamodels

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HaService(
    val name: String? = null,
    val description: String? = null,
    val target: Map<String, JsonElement>? = null,
    val fields: Map<String, Map<String, JsonElement>>? = null
)
