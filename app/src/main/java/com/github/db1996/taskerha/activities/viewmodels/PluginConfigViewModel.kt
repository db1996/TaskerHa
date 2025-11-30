package com.github.db1996.taskerha.activities.viewmodels

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.db1996.taskerha.TaskerConstants
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.ActualService
import com.github.db1996.taskerha.datamodels.BuiltForm
import com.github.db1996.taskerha.datamodels.FieldState
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.datamodels.HomeassistantForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class PluginConfigViewModel(
    private val client: HomeAssistantClient
) : ViewModel() {
    var services: List<ActualService> by mutableStateOf(value = emptyList())
        private set

    var entities: List<HaEntity> by mutableStateOf(emptyList())
        private set


    var selectedService: ActualService? by mutableStateOf(null)
    var form: HomeassistantForm by mutableStateOf(HomeassistantForm())

    var currentDomainSearch: String by mutableStateOf("")
    var currentServiceSearch: String by mutableStateOf("")
    var pendingRestore: BuiltForm? = null


    fun loadServices(force: Boolean = false) {
        viewModelScope.launch {
            try {
                val resultPing = withContext(Dispatchers.IO) {
                    client.ping()
                }

                val result = withContext(Dispatchers.IO) {
                    client.getServicesFront(force)
                }

                if(result.isEmpty()){
                    Log.e("HA", "No services found $result")
                }
                services = result
                Log.d("HA", "Loaded services: ${services.size}")

                services.forEach { service ->
                    if(service.id == "select_option" && service.domain == "input_select"){
                        Log.e("HA", "Found select_option service, $service")
                    }
                }
                pendingRestore?.let {
                    restoreForm(it.domain, it.service, it.entityId, it.data)
                    pendingRestore = null
                }
            } catch (e: Exception) {
                Log.e("HA", "Failed to load services", e)
            }
        }
    }

    fun loadEntities(force: Boolean = false) {
        viewModelScope.launch {
            try {
                val resultPing = withContext(Dispatchers.IO) {
                    client.ping()
                }
                val result = withContext(Dispatchers.IO) {
                    client.getEntities()
                }
                entities = result
                Log.d("HA", "Loaded entities: ${entities.size}")
            } catch (e: Exception) {
                Log.e("HA", "Failed to load entities", e)
            }
        }
    }

    fun pickService(pservice: ActualService) {
        selectedService = pservice
        form = HomeassistantForm()

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
        Log.e("HA", "Picked service: ${pservice.id}, fields: ${pservice.fields.size}, form: ${form.domain}, form: ${form.service}")
        Log.e("HA", "Form data: ${form.dataContainer}")

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

    fun testForm() {
        val domain = form.domain
        val service = form.service
        val entityId = form.entityId

        val data = form.dataContainer
            .filter { it.value.toggle.value }
            .mapValues { it.value.value.value }

        Log.d("HA", "Testing service call: $domain, $service, $entityId, $data")

        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    client.callService(domain, service, entityId, data)
                }
                Log.d("HA", "Service call ${if (success) "succeeded" else "failed"}")
            } catch (e: Exception) {
                Log.e("HA", "Error calling service", e)
            }
        }
    }

    fun buildForm(): BuiltForm {
        val domain = form.domain
        val service = form.service
        val entityId = form.entityId

        // Only selected fields
        val data = form.dataContainer
            .filter { it.value.toggle.value }
            .mapValues { it.value.value.value }

        // Build consistent blurb
        var msg = "Call Home Assistant: $domain.$service"
        if (entityId.isNotBlank()) {
            msg += " on $entityId"
        }

        return BuiltForm(
            domain = domain,
            service = service,
            entityId = entityId,
            data = data,
            blurb = msg
        )
    }

    fun restoreForm(domain: String, service: String, entity: String, dataMap: Map<String, String>) {
        val pservice = services.find { it.domain == domain && it.id == service }

        if (pservice == null) {
            // Not loaded yet → store for later
            pendingRestore = BuiltForm(domain, service, entity, dataMap, "")
            return
        }

        Log.e("HA", "Restoring form: $domain, $service, $entity, $dataMap")
        // Services loaded: perform the restoration
        selectedService = pservice

        form = HomeassistantForm().apply {
            this.domain = domain
            this.service = service
            this.entityId = entity

            dataContainer.clear()
            pservice.fields.forEach { field ->
                val state = FieldState()
                if (dataMap.containsKey(field.id)) {
                    state.value.value = dataMap[field.id].orEmpty()
                    state.toggle.value = true
                }
                if(field.required == true){
                    state.toggle.value = true
                }
                dataContainer[field.id] = state
            }
        }
    }
}