package com.github.db1996.taskerha.datamodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class HomeassistantForm(
    var domain: String = "",
    var service: String = "",
    var entityId: String = "",
    var dataContainer: MutableMap<String, FieldState> = mutableMapOf()
)

data class FieldState(
    val toggle: MutableState<Boolean> = mutableStateOf(false),
    val value: MutableState<String> = mutableStateOf("")
)

data class HaCallServiceBuiltForm(
    val domain: String,
    val service: String,
    val entityId: String,
    val data: Map<String, String>,
    val blurb: String
)