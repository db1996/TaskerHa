package com.github.db1996.taskerha.tasker.onHaMessage.view

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.db1996.taskerha.tasker.onHaMessage.data.OnHaMessageBuiltForm
import com.github.db1996.taskerha.tasker.onHaMessage.data.OnHaMessageForm

class OnHaMessageViewModel() : ViewModel() {
    var form by mutableStateOf(OnHaMessageForm())
        private set


    fun buildForm(): OnHaMessageBuiltForm {
        return OnHaMessageBuiltForm(
            blurb = "",
            type = form.type,
            message = form.message
        )
    }

    fun restoreForm(type: String, message: String) {
        Log.e("OnHaMessageViewModel", "Restoring form: $type, $message")
        form = OnHaMessageForm().apply {
            this.type = type
            this.message = message
        }
    }
}