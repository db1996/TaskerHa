package com.github.db1996.taskerha.tasker.callservice.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class FieldState(
    val toggle: MutableState<Boolean> = mutableStateOf(false),
    val value: MutableState<String> = mutableStateOf("")
)