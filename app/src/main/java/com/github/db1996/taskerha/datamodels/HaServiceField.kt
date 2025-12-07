package com.github.db1996.taskerha.datamodels

import com.github.db1996.taskerha.enums.HaServiceFieldType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HaService(
    val name: String? = null,
    val description: String? = null,
    val target: Map<String, JsonElement>? = null,
    val fields: Map<String, Map<String, JsonElement>>? = null
)

data class HaServiceField(
    val id: String,
    var name: String? = null,
    var description: String? = null,
    var required: Boolean? = null,
    var example: String? = null,
    var type: HaServiceFieldType? = null,

    var options: MutableList<Option>? = null,

    var min: Double? = null,
    var max: Double? = null,
    var unit_of_measurement: String? = null
)

data class Option(
    val label: String,
    val value: String
)
