package com.github.db1996.taskerha.tasker.getstate.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.tasker.base.BaseViewModel
import com.github.db1996.taskerha.tasker.base.ValidationResult
import com.github.db1996.taskerha.tasker.getstate.data.HaGetStateBuiltForm
import com.github.db1996.taskerha.tasker.getstate.data.HaGetStateForm

class HaGetStateViewModel(
    client: HomeAssistantClient
) : BaseViewModel<HaGetStateForm, HaGetStateBuiltForm>(
    initialForm = HaGetStateForm(),
    client = client
) {

    var entities: List<HaEntity> by mutableStateOf(emptyList())
        private set

    var currentDomainSearch: String by mutableStateOf("")


    override val logTag: String
        get() =  "HaGetStateViewModel"

    // UI event handlers
    fun pickEntity(entityId: String) {
        form = form.copy(entityId = entityId)
    }

    fun updateEntityId(entityId: String){
        form = form.copy(entityId = entityId)
    }

    override fun buildForm(): HaGetStateBuiltForm {
        return HaGetStateBuiltForm(
            entityId = form.entityId,
            blurb = "Get state: ${form.entityId}"
        )
    }

    override fun restoreForm(data: HaGetStateBuiltForm) {
        logDebug("Restoring form: entityId=${data.entityId}")
        form = HaGetStateForm(
            entityId = data.entityId
        )
    }

    override fun createInitialForm(): HaGetStateForm {
        return HaGetStateForm()
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

    fun testForm() {
        val entityId = form.entityId
        logDebug("Testing get state call: $entityId")

        launchClientOperation { client ->
            val success = client.getState(entityId)
            logDebug("Get state ${if (success) "succeeded" else "failed"}, ${client.result}")
        }
    }
}

