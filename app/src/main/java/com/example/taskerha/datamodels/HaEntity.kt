package com.example.taskerha.datamodels

import kotlinx.serialization.Serializable

@Serializable
data class HaEntity(
    val entity_id: String,
    val state: String? = null
)