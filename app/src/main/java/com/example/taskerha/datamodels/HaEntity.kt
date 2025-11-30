package com.github.db1996.taskerha.datamodels

import kotlinx.serialization.Serializable

@Serializable
data class HaEntity(
    val entity_id: String,
    val state: String? = null
)