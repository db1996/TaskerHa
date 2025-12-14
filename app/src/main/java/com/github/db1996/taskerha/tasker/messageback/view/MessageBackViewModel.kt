package com.github.db1996.taskerha.tasker.messageback.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.BaseViewModel
import com.github.db1996.taskerha.tasker.base.ValidationResult
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackForm
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackBuiltForm

class MessageBackViewModel(
    client: HomeAssistantClient
) : BaseViewModel<MessageBackForm, MessageBackBuiltForm>(
    initialForm = MessageBackForm(),
    client = client
) {
    override val logTag: String
        get() = "MessageBackViewModel"

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
        sb.appendLine("trigger: event")
        sb.appendLine("event_type: taskerha_message_back")
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

    override fun buildForm(): MessageBackBuiltForm {
        return MessageBackBuiltForm(
            blurb = "",
            type = form.type,
            message = form.message
        )
    }

    override fun restoreForm(data: MessageBackBuiltForm) {
        logVerbose("Restoring form: type=${data.type}, message=${data.message}")
        form = MessageBackForm(
            type = data.type,
            message = data.message
        )
    }

    override fun createInitialForm(): MessageBackForm {
        return MessageBackForm()
    }

    override fun validateForm(): ValidationResult {
        return ValidationResult.Valid
    }
}

