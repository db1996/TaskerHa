package com.github.db1996.taskerha.tasker.getstate.data

/**
 * Mutable form state used in the ViewModel and UI
 */
data class HaGetStateForm(
    var entityId: String = ""
)

/**
 * Immutable built form saved to Tasker
 */
data class HaGetStateBuiltForm(
    val entityId: String,
    val blurb: String
)

