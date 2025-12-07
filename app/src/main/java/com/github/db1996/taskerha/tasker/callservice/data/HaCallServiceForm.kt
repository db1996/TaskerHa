package com.github.db1996.taskerha.tasker.callservice.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class HomeassistantForm(
    var domain: String = "",
    var service: String = "",
    var entityId: String = "",
    var dataContainer: MutableMap<String, FieldState> = mutableMapOf()
)


data class HaCallServiceBuiltForm(
    val domain: String,
    val service: String,
    val entityId: String,
    val data: Map<String, String>,
    val blurb: String
)