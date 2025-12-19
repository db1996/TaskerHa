package com.github.db1996.taskerha.tasker.callservice.view

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.ActualService
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.tasker.base.BaseViewModel
import com.github.db1996.taskerha.tasker.base.ValidationResult
import com.github.db1996.taskerha.tasker.callservice.data.CallServiceFormBuiltForm
import com.github.db1996.taskerha.tasker.callservice.data.CallServiceFormForm
import com.github.db1996.taskerha.tasker.callservice.data.FieldState
import kotlin.text.orEmpty

class CallServiceViewModel(
    client: HomeAssistantClient
) : BaseViewModel<CallServiceFormForm, CallServiceFormBuiltForm>(
    initialForm = CallServiceFormForm(),
    client = client
) {

    var services: List<ActualService> by mutableStateOf(value = emptyList())
        private set

    var entities: List<HaEntity> by mutableStateOf(emptyList())
        private set

    var selectedService: ActualService? by mutableStateOf(null)

    var currentDomainSearch: String by mutableStateOf("")
    var currentServiceSearch: String by mutableStateOf("")
    var pendingRestore: CallServiceFormBuiltForm? = null

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
        form = CallServiceFormForm()

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
        Log.d("HA", "Picked service: ${pservice.id}, fields: ${pservice.fields.size}, form: ${form.domain}, form: ${form.service}")
        Log.d("HA", "Form data: ${form.dataContainer}")

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
        val entityId = form.entityId

        val data = form.dataContainer
            .filter { it.value.toggle.value }
            .mapValues { it.value.value.value }

        return CallServiceFormBuiltForm(
            domain = domain,
            service = service,
            entityId = entityId,
            data = data,
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
        form.dataContainer.clear()

        pservice.fields.forEach { field ->
            val state = FieldState()
            if (data.data.containsKey(field.id)) {
                state.value.value = data.data[field.id].orEmpty()
                state.toggle.value = true
            }
            if(field.required == true){
                state.toggle.value = true
            }
            form.dataContainer[field.id] = state
        }
        logDebug("Restoring form: ${data.domain}, ${data.service}, ${data.entityId},")
        selectedService = pservice

        form = CallServiceFormForm(
            entityId = data.entityId,
            domain = data.domain,
            service = data.service,
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
            val success = client.callService(domain, service, entityId, data)
            logDebug("Service call ${if (success) "succeeded" else "failed"}")
        }
    }
}

