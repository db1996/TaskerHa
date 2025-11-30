package com.github.db1996.taskerha.datamodels

data class ActualService(
    val id: String,
    val name: String?,
    val description: String?,
    val type: String,
    val domain: String,
    val fields: MutableList<HaServiceField>,
    val targetEntity: Boolean
)
