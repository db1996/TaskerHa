package com.github.db1996.taskerha.tasker.ontriggerstate.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.logging.LogChannel
import com.github.db1996.taskerha.tasker.base.BaseViewModel
import com.github.db1996.taskerha.tasker.base.ValidationResult
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateForm

class OnTriggerStateViewModel(
    client: HomeAssistantClient
) : BaseViewModel<OnTriggerStateForm, OnTriggerStateBuiltForm>(
    initialForm = OnTriggerStateForm(),
    client = client
) {

    var entities: List<HaEntity> by mutableStateOf(emptyList())
        private set

    var currentDomainSearch: String by mutableStateOf("")


    override val logTag: String
        get() =  "OnTriggerStateViewModel"

    override val logChannel: LogChannel
        get() = LogChannel.WEBSOCKET

    // UI event handlers
    fun pickEntity(entityId: String) {
        form = form.copy(entityId = entityId)
    }

    fun setFrom(fromState: String) {
        form = form.copy(fromState = fromState)
    }

    fun setTo(toState: String) {
        form = form.copy(toState = toState)
    }


    override fun buildForm(): OnTriggerStateBuiltForm {
        return OnTriggerStateBuiltForm(
            entityId = form.entityId,
            fromState = form.fromState,
            toState = form.toState,
            blurb = "Get state: ${form.entityId}"
        )
    }

    override fun restoreForm(data: OnTriggerStateBuiltForm) {
        logVerbose("Restoring form: entityId=${data.entityId}, fromState=${data.fromState}, toState=${data.toState}")
        form = OnTriggerStateForm(
            entityId = data.entityId,
            fromState = data.fromState,
            toState = data.toState
        )
    }

    override fun createInitialForm(): OnTriggerStateForm {
        return OnTriggerStateForm()
    }

    override fun validateForm(): ValidationResult {
        if (form.entityId.isBlank()) {
            return ValidationResult.Invalid("Entity ID cannot be empty")
        }
        return ValidationResult.Valid
    }

    fun loadEntities() {
        launchClientOperation { client ->
            val result = client.getEntities()
            entities = result
            logDebug("Loaded entities: ${entities.size}")
        }
    }
}

