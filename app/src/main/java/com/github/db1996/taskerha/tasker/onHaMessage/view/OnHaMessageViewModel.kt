package com.github.db1996.taskerha.tasker.onHaMessage.view

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.BaseViewModel
import com.github.db1996.taskerha.tasker.base.ValidationResult
import com.github.db1996.taskerha.tasker.onHaMessage.data.OnHaMessageBuiltForm
import com.github.db1996.taskerha.tasker.onHaMessage.data.OnHaMessageForm

class OnHaMessageViewModel(
    client: HomeAssistantClient
) : BaseViewModel<OnHaMessageForm, OnHaMessageBuiltForm>(
    initialForm = OnHaMessageForm(),
    client = client
) {

    fun setType(value: String) {
        form = form.copy(type = value)
        yamlExample = buildHaMessageYaml(form.type, form.message)
    }

    fun setMessage(value: String) {
        form = form.copy(message = value)
        yamlExample = buildHaMessageYaml(form.type, form.message)
    }

    var yamlExample by mutableStateOf(buildHaMessageYaml(form.type, form.message))
        private set

    private fun buildHaMessageYaml(
        type: String,
        message: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("event: taskerha_message")
        sb.appendLine("event_data:")

        if (type.isNotBlank()) {
            sb.appendLine("  type: $type")
        } else {
            sb.appendLine("  # type: some_type  # optional")
        }

        if (message.isNotBlank()) {
            sb.appendLine("  message: $message")
        } else {
            sb.appendLine("  # message: \"some message\"  # optional")
        }

        return sb.toString().trimEnd()
    }

    fun restoreForm(type: String, message: String) {
        Log.e("OnHaMessageViewModel", "Restoring form: $type, $message")
        form = OnHaMessageForm().apply {
            this.type = type
            this.message = message
        }

        yamlExample = buildHaMessageYaml(form.type, form.message)
    }

    override fun buildForm(): OnHaMessageBuiltForm {
        return OnHaMessageBuiltForm(
            blurb = "",
            type = form.type,
            message = form.message
        )
    }

    override fun restoreForm(data: OnHaMessageBuiltForm) {
        logVerbose("Restoring form: type=${data.type}, message=${data.message}")
        form = OnHaMessageForm(
            type = data.type,
            message = data.message
        )
    }

    override fun createInitialForm(): OnHaMessageForm {
        return OnHaMessageForm()
    }

    override fun validateForm(): ValidationResult {
        return ValidationResult.Valid
    }
}

