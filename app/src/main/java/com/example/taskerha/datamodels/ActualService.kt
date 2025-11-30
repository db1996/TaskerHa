package com.example.taskerha.datamodels

data class ActualService(
    val id: String,
    val name: String?,
    val description: String?,
    val type: String,
    val domain: String,
    val fields: MutableList<HaServiceField>,
    val targetEntity: Boolean
)
