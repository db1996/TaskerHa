package com.example.taskerha.datamodels
import kotlinx.serialization.Serializable

@Serializable
data class HaDomainService(
    val domain: String,
    val services: Map<String, HaService>
)
