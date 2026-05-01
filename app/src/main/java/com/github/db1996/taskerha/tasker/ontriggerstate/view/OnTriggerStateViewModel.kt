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

    fun addEntity(entityId: String) {
        val trimmed = entityId.trim()
        if (trimmed.isNotBlank() && trimmed !in form.entityIds) {
            form = form.copy(entityIds = form.entityIds + trimmed)
        }
    }

    fun removeEntity(index: Int) {
        form = form.copy(entityIds = form.entityIds.toMutableList().also { it.removeAt(index) })
    }

    fun updateEntityAt(index: Int, value: String) {
        val updated = form.entityIds.toMutableList()
        updated[index] = value
        form = form.copy(entityIds = updated)
    }

    fun setFrom(fromState: String) {
        form = form.copy(fromState = fromState)
    }

    fun setTo(toState: String) {
        form = form.copy(toState = toState)
    }

    fun setFor(forDuration: String){
        form = form.copy(forDuration = forDuration)
    }

    override fun buildForm(): OnTriggerStateBuiltForm {
        val blurb = when {
            form.entityIds.isNotEmpty() -> "Get state: ${form.entityIds.joinToString(", ")}"
            form.entityId.isNotBlank() -> "Get state: ${form.entityId}"
            else -> "Get state: (any entity)"
        }
        return OnTriggerStateBuiltForm(
            entityId = "",
            entityIds = form.entityIds,
            fromState = form.fromState,
            toState = form.toState,
            blurb = blurb,
            forDuration = form.forDuration
        )
    }

    override fun restoreForm(data: OnTriggerStateBuiltForm) {
        logVerbose("Restoring form: entityId=${data.entityId}, entityIds=${data.entityIds}, fromState=${data.fromState}, toState=${data.toState}")
        // Migrate legacy single entityId into the multi-entity list
        val migratedIds = if (data.entityIds.isEmpty() && data.entityId.isNotBlank()) {
            listOf(data.entityId.trim())
        } else {
            data.entityIds
        }
        form = OnTriggerStateForm(
            entityId = data.entityId,
            entityIds = migratedIds,
            fromState = data.fromState,
            toState = data.toState,
            forDuration = data.forDuration
        )
    }

    override fun createInitialForm(): OnTriggerStateForm {
        return OnTriggerStateForm()
    }

    override fun validateForm(): ValidationResult {
        // Both entityId (legacy) and entityIds (multi) are optional; no entity filter = wildcard
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

