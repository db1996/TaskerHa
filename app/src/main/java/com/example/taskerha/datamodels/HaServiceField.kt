package com.github.db1996.taskerha.datamodels

import com.github.db1996.taskerha.enums.HaServiceFieldType

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
