package com.github.db1996.taskerha.tasker.callservice.data

/**
 * Mutable form state used in the ViewModel and UI
 */
data class CallServiceFormForm(
    var domain: String = "",
    var service: String = "",
    var entityId: String = "",
    var dataContainer: MutableMap<String, FieldState> = mutableMapOf()
)

/**
 * Immutable built form saved to Tasker
 */
data class CallServiceFormBuiltForm(
    val domain: String,
    val service: String,
    val entityId: String,
    val data: Map<String, String>,
    val blurb: String
)

