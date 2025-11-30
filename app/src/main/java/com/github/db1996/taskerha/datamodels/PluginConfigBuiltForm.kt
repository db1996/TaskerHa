package com.github.db1996.taskerha.datamodels

data class BuiltForm(
    val domain: String,
    val service: String,
    val entityId: String,
    val data: Map<String, String>,
    val blurb: String
)