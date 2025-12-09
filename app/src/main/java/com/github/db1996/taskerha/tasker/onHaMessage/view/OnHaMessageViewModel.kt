package com.github.db1996.taskerha.tasker.onHaMessage.view

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.callservice.data.HaCallServiceBuiltForm
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.tasker.onHaMessage.data.OnHaMessageBuiltForm
import com.github.db1996.taskerha.tasker.onHaMessage.data.OnHaMessageForm
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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