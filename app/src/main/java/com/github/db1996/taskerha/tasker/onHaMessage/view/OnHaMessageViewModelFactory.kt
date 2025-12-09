package com.github.db1996.taskerha.tasker.onHaMessage.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class OnHaMessageViewModelFactory(
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OnHaMessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnHaMessageViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}