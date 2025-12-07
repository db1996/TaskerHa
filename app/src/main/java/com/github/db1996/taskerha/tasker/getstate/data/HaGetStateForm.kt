package com.github.db1996.taskerha.tasker.getstate.data

data class HaGetStateForm(
    var entityId: String = ""
)

data class HaGetStateBuiltForm(
    val entityId: String,
    val blurb: String
)
