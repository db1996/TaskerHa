package com.github.db1996.taskerha.datamodels

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HaArea(
    val id: String,
    val name: String,
    val entity_ids: List<String> = emptyList()
)

@Serializable
data class HaDevice(
    val id: String,
    val name: String,
    val entity_ids: List<String> = emptyList()
)

@Serializable
data class HaLabel(
    val id: String,
    val name: String,
    val entities: List<String> = emptyList(),
    val devices: List<String> = emptyList()
)

@Serializable
data class HaRegistryServiceResponse(
    val labels: List<HaLabel> = emptyList(),
    val devices: List<HaDevice> = emptyList(),
    val areas: List<HaArea> = emptyList()
)

@Serializable
data class HaRegistryData(
    val changed_states: List<JsonElement> = emptyList(),
    val service_response: HaRegistryServiceResponse
)
