package com.github.db1996.taskerha.tasker.callservice.view

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.ActualService
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.datamodels.HaInstance
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.datamodels.HaRegistryData
import com.github.db1996.taskerha.tasker.base.BaseViewModel
import com.github.db1996.taskerha.tasker.base.ValidationResult
import com.github.db1996.taskerha.tasker.callservice.data.CallServiceFormBuiltForm
import com.github.db1996.taskerha.tasker.callservice.data.CallServiceFormForm
import com.github.db1996.taskerha.tasker.callservice.data.FieldState
import com.github.db1996.taskerha.util.HaHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.orEmpty

class CallServiceViewModel(
    private val context: Context,
    client: HomeAssistantClient
) : BaseViewModel<CallServiceFormForm, CallServiceFormBuiltForm>(
    initialForm = CallServiceFormForm(
        instanceId = HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: ""
    ),
    client = client
) {

    var services: List<ActualService> by mutableStateOf(value = emptyList())
        private set

    var entities: List<HaEntity> by mutableStateOf(emptyList())
        private set

    var selectedService: ActualService? by mutableStateOf(null)
    var registryData: HaRegistryData? by mutableStateOf(null)
        private set
    var registryLoading: Boolean by mutableStateOf(false)
        private set

    var currentDomainSearch: String by mutableStateOf("")
    var currentServiceSearch: String by mutableStateOf("")
    var pendingRestore: CallServiceFormBuiltForm? = null

    val hacsAvailable: Boolean
        get() = HaInstanceRepository.getById(form.instanceId)?.hacsAvailable ?: false

    override val logTag: String
        get() =  "CallServiceViewModel"
    fun loadServices() {
        launchClientOperation { client ->
            val result = client.getServicesFront()
            services = result
            logDebug("Loaded services: ${services.size}")
            if(pendingRestore != null){
                restoreForm(pendingRestore!!)
            }
        }
    }

    fun loadEntities() {
        launchClientOperation { client ->
            val result = client.getEntities()
            entities = result
            logDebug("Loaded entities: ${entities.size}")

            if(pendingRestore != null){
                restoreForm(pendingRestore!!)
            }
        }
    }

    fun pickService(pservice: ActualService) {
        selectedService = pservice
        form = CallServiceFormForm(instanceId = form.instanceId)

        form.domain = pservice.domain
        form.service = pservice.id
        form.entityId = ""

        form.dataContainer.clear()
        pservice.fields.forEach { field ->
            form.dataContainer[field.id] = FieldState()

            if(field.required == true){
                form.dataContainer[field.id]?.toggle?.value = true
            }
        }

        if (pservice.hasTargetDefinition) {
            ensureTargetKeys()
            launchClientOperation { client ->
                registryLoading = true
                registryData = client.getRegistryData()
                registryLoading = false
            }
        }

        Log.d("HA", "Picked service: ${pservice.id}, fields: ${pservice.fields.size}, form: ${form.domain}, form: ${form.service}")
        Log.d("HA", "Form data: ${form.dataContainer}")

    }

    private fun ensureTargetKeys() {
        for (key in listOf("device_id", "area_id", "label_id")) {
            if (!form.dataContainer.containsKey(key)) {
                form.dataContainer[key] = FieldState()
            }
        }
    }

    fun unsetPickedService() {
        selectedService = null
        form.domain = ""
        form.service = ""
        form.entityId = ""
        form.dataContainer.clear()

        currentDomainSearch = ""
        currentServiceSearch = ""
    }

    fun pickEntity(entityId: String) {
        form = form.copy(entityId = entityId)
    }

    fun updateFieldValue(fieldId: String, value: String) {
        form.dataContainer[fieldId]?.value?.value = value
    }

    fun updateFieldToggle(fieldId: String, toggle: Boolean) {
        form.dataContainer[fieldId]?.toggle?.value = toggle
    }

    override fun buildForm(): CallServiceFormBuiltForm {
        val domain = form.domain
        val service = form.service

        val targetKeys = setOf("entity_id", "device_id", "area_id", "label_id")
        val data = form.dataContainer
            .filter { (key, state) ->
                state.toggle.value || (key in targetKeys && state.value.value.isNotBlank())
            }
            .mapValues { it.value.value.value }

        // For backward compatibility, populate legacy entityId field from dataContainer["entity_id"]
        val entityIdValue = data.getOrDefault("entity_id", "")

        return CallServiceFormBuiltForm(
            domain = domain,
            service = service,
            entityId = entityIdValue,  // @Deprecated - for old Tasker versions
            data = data,
            instanceId = form.instanceId,
            blurb = ""
        )
    }

    override fun restoreForm(data: CallServiceFormBuiltForm) {
        logDebug("Restoring form: entityId=${data.entityId}")
        pendingRestore = null
        val pservice = services.find { it.domain == data.domain && it.id == data.service }

        if(pservice == null){
            logDebug("Service not found: ${data.domain}, ${data.service}")
            pendingRestore = data
            return
        }

        logError("Restoring form: ${data}")
        logInfo("Restoring form: ${data.domain}, ${data.service}, ${data.entityId},")

        val restoredDataContainer = mutableMapOf<String, FieldState>()
        pservice.fields.forEach { field ->
            val state = FieldState()
            if (data.data.containsKey(field.id)) {
                state.value.value = data.data[field.id].orEmpty()
                state.toggle.value = true
            }
            if(field.required == true){
                state.toggle.value = true
            }
            restoredDataContainer[field.id] = state
        }
        
        // Backward compatibility: Migrate legacy entityId to dataContainer["entity_id"]
        // This handles old Tasker configs that stored entity in the legacy field
        if (data.entityId.isNotBlank() && !data.data.containsKey("entity_id")) {
            // Move to dataContainer if entity_id field exists in service
            if (pservice.fields.any { it.id == "entity_id" }) {
                val entityState = restoredDataContainer.getOrPut("entity_id") { FieldState() }
                entityState.value.value = data.entityId
                entityState.toggle.value = true
            }
        }

        // Restore synthetic target keys for services with target definition
        if (pservice.hasTargetDefinition) {
            for (key in listOf("entity_id", "device_id", "area_id", "label_id")) {
                val state = restoredDataContainer.getOrPut(key) { FieldState() }
                if (data.data.containsKey(key)) {
                    state.value.value = data.data[key].orEmpty()
                    state.toggle.value = true
                }
            }
        }

        logDebug("Restoring form: ${data.domain}, ${data.service}, ${data.entityId},")
        selectedService = pservice

        if (pservice.hasTargetDefinition) {
            launchClientOperation { client ->
                registryLoading = true
                registryData = client.getRegistryData()
                registryLoading = false
            }
        }

        form = CallServiceFormForm(
            entityId = data.entityId,  // Keep for display purposes during migration
            domain = data.domain,
            service = data.service,
            dataContainer = restoredDataContainer,
            instanceId = data.instanceId
        )
    }

    override fun createInitialForm(): CallServiceFormForm {
        return CallServiceFormForm()
    }

    override fun validateForm(): ValidationResult {
        return ValidationResult.Valid
    }

    fun testForm() {
        val domain = form.domain
        val service = form.service
        val entityId = form.entityId
        logDebug("Testing call service: $domain, $service, $entityId")
        val data = form.dataContainer
            .filter { it.value.toggle.value }
            .mapValues { it.value.value.value }

        launchClientOperation { client ->
            val targetKeys = setOf("entity_id", "device_id", "area_id", "label_id")
            val hasTargetKeys = targetKeys.any { data.getOrDefault(it, "").isNotBlank() }
            val success = if (hasTargetKeys) {
                fun csv(key: String) = data.getOrDefault(key, "").split(",").filter { it.isNotBlank() }
                val target = buildMap {
                    csv("entity_id").takeIf { it.isNotEmpty() }?.let { put("entity_id", it) }
                    csv("device_id").takeIf { it.isNotEmpty() }?.let { put("device_id", it) }
                    csv("area_id").takeIf { it.isNotEmpty() }?.let { put("area_id", it) }
                    csv("label_id").takeIf { it.isNotEmpty() }?.let { put("label_id", it) }
                }
                val cleanData = data.filterKeys { it !in targetKeys }
                client.callService(domain, service, target = target, data = cleanData)
            } else {
                client.callService(domain, service, entityId, data)
            }
            logDebug("Service call ${if (success) "succeeded" else "failed"}")
        }
    }

    /**
     * Change the target instance and reload services and entities.
     * Only for new actions - not for editing existing actions.
     */
    fun changeInstance(instanceId: String) {
        form = form.copy(instanceId = instanceId)
        selectedService = null
        services = emptyList()
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
                                services = newClient.getServicesFront()
                                entities = newClient.getEntities()
                                clientError = ""
                                logDebug("Loaded ${services.size} services and ${entities.size} entities from instance: ${instance.name}")
                            } else {
                                clientError = newClient.error
                                logError("Failed to ping instance: ${newClient.error}")
                            }
                        } catch (e: Exception) {
                            clientError = e.message ?: "Unknown error"
                            logError("Error loading data: ${e.message}")
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

