package com.github.db1996.taskerha.tasker.getstate.data

import com.github.db1996.taskerha.util.HasEntityIds

/**
 * Mutable form state used in the ViewModel and UI
 */
data class HaGetStateForm(
    var entityId: String = "",
    var instanceId: String = ""
)

/**
 * Immutable built form saved to Tasker
 */
data class HaGetStateBuiltForm(
    val entityId: String,
    val instanceId: String,
    val blurb: String
) : HasEntityIds {
    override fun entityIds(): List<String> = listOf(entityId)
}

