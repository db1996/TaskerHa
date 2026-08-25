package com.github.db1996.taskerha.tasker.getstate.view

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.datamodels.HaInstance
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.tasker.base.BaseViewModel
import com.github.db1996.taskerha.tasker.base.ValidationResult
import com.github.db1996.taskerha.tasker.getstate.data.HaGetStateBuiltForm
import com.github.db1996.taskerha.tasker.getstate.data.HaGetStateForm
import com.github.db1996.taskerha.util.HaHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HaGetStateViewModel(
    private val context: Context,
    client: HomeAssistantClient
) : BaseViewModel<HaGetStateForm, HaGetStateBuiltForm>(
    initialForm = HaGetStateForm(
        instanceId = HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""
    ),
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
            instanceId = form.instanceId,
            blurb = "Get state: ${form.entityId}"
        )
    }

    override fun restoreForm(data: HaGetStateBuiltForm) {
        logDebug("Restoring form: entityId=${data.entityId}")
        form = HaGetStateForm(
            entityId = data.entityId,
            instanceId = data.instanceId
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

    /**
     * Change the target instance and reload entities.
     * Only for new actions - not for editing existing actions.
     */
    fun changeInstance(instanceId: String) {
        form = form.copy(instanceId = instanceId)
        entities = emptyList()
        clientError = ""
        isLoadingInstance = true

        viewModelScope.launch {
            try {
                val instance = HaInstanceRepository.getById(instanceId)
                if (instance != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val newClient = createClientForInstance(instance)
                            val success = newClient.ping()
                            if (success) {
                                entities = newClient.getEntities()
                                client = newClient
                                clientError = ""
                                logDebug("Loaded ${entities.size} entities from instance: ${instance.name}")
                            } else {
                                clientError = newClient.error
                                logError("Failed to ping instance: ${newClient.error}")
                            }
                        } catch (e: Exception) {
                            clientError = e.message ?: "Unknown error"
                            logError("Error loading entities: ${e.message}")
                        }
                    }
                } else {
                    clientError = "Instance not found"
                }
            } finally {
                isLoadingInstance = false
            }
        }
    }

    fun retryLoad() = changeInstance(form.instanceId)

    private fun createClientForInstance(instance: HaInstance): HomeAssistantClient {
        val url = instance.resolveUrl()
        val token = instance.token
        val httpClient = HaHttpClientFactory.build(
            context,
            clientCertEnabled = instance.clientCertEnabled,
            clientCertAlias = instance.clientCertAlias
        )
        return HomeAssistantClient(url, token, httpClient)
    }
}

