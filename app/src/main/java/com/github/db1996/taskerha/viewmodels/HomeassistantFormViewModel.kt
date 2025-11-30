package com.github.db1996.taskerha.viewmodels

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
import com.github.db1996.taskerha.datamodels.FieldState
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.datamodels.HomeassistantForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class HomeassistantFormViewModel(
    private val client: HomeAssistantClient
) : ViewModel() {

    // Compose-friendly state
    var services: List<ActualService> by mutableStateOf(value = emptyList())
        private set

    var entities: List<HaEntity> by mutableStateOf(emptyList())
        private set


    var selectedService: ActualService? by mutableStateOf(null)
    var form: HomeassistantForm by mutableStateOf(HomeassistantForm())

    var currentDomainSearch: String by mutableStateOf("")
    var currentServiceSearch: String by mutableStateOf("")



    // ------------------ LOAD SERVICES ------------------
    fun loadServices(force: Boolean = false) {
        viewModelScope.launch {
            try {
                val resultPing = withContext(Dispatchers.IO) {
                    client.ping()
                }

                val result = withContext(Dispatchers.IO) {
                    client.getServicesFront(force)
                }
                services = result
                Log.d("HA", "Loaded services: ${services.size}")
            } catch (e: Exception) {
                Log.e("HA", "Failed to load services", e)
            }
        }
    }

    // ------------------ LOAD ENTITIES ------------------
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

    // ------------------ PICK SERVICE ------------------
    fun pickService(pservice: ActualService) {
        selectedService = pservice
        form = HomeassistantForm()

        form.domain = pservice.domain
        form.service = pservice.id
        form.entityId = ""

        form.dataContainer.clear()
        pservice.fields.forEach { field ->
            form.dataContainer[field.id] = FieldState()
        }
        Log.e("HA", "Picked service: ${pservice.id}, fields: ${pservice.fields.size}, form: ${form.domain}, form: ${form.service}")

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

    // ------------------ PICK ENTITY ------------------
    fun pickEntity(entityId: String) {
        form = form.copy(entityId = entityId)
    }

    // ------------------ UPDATE FIELDS ------------------
    fun updateFieldValue(fieldId: String, value: String) {
        form.dataContainer[fieldId]?.value?.value = value
    }

    fun updateFieldToggle(fieldId: String, toggle: Boolean) {
        form.dataContainer[fieldId]?.toggle?.value = toggle
    }

    // ------------------ SAVE FORM ------------------
    fun testForm() {
        val domain = form.domain
        val service = form.service
        val entityId = form.entityId

        // Extract actual values from MutableState
        val data = form.dataContainer
            .filter { it.value.toggle.value } // only include toggled fields
            .mapValues { it.value.value.value } // extract string value from MutableState


        Log.d("HA", "Saving form: $domain, $service, $entityId, $data")



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

    fun saveFormToTasker(activity: ComponentActivity) {
        val domain = form.domain
        val service = form.service
        val entityId = form.entityId

        // Extract actual values from MutableState
        val data = form.dataContainer
            .filter { it.value.toggle.value } // only include toggled fields
            .mapValues { it.value.value.value } // extract string value from MutableState

        Log.d("HA", "Saving form: $domain, $service, $entityId, $data")

        viewModelScope.launch {
            try {

                val jsonData = Json.Default.encodeToString(
                    MapSerializer(
                        String.Companion.serializer(),
                        String.serializer()
                    ), data.mapValues { it.value.toString() })

                // --- RETURN TO TASKER ---
                val bundle = Bundle().apply {
                    putString("DOMAIN", domain)
                    putString("SERVICE", service)
                    putString("ENTITY_ID", entityId)
                    putSerializable("DATA", jsonData) // Bundle can't take Map directly
                }

                val resultIntent = Intent().apply {
                    putExtra(TaskerConstants.EXTRA_BUNDLE, bundle)
                    var msg = "Call Home Assistant: $domain.$service"
                    if(entityId.isNotBlank()) {
                        msg += " on $entityId"
                    }

                    putExtra(
                        TaskerConstants.EXTRA_BLURB,
                        msg
                    )
                }

                // If the service call succeeded, return RESULT_OK, otherwise RESULT_CANCELED
                activity.setResult(
                    Activity.RESULT_OK,
                    resultIntent
                )
                activity.finish()

            } catch (e: Exception) {
                Log.e("HA", "Error calling service", e)
                // Return failure to Tasker
                activity.setResult(Activity.RESULT_CANCELED)
                activity.finish()
            }
        }
    }

}