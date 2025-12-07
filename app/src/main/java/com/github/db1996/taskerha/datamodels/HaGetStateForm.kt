package com.github.db1996.taskerha.datamodels

data class HaGetStateForm(
    var entityId: String = ""
)

data class HaGetStateBuiltForm(
    val entityId: String,
    val blurb: String
)
