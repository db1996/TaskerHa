package com.github.db1996.taskerha.activities.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.callservice.data.HaCallServiceBuiltForm
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnTriggerStateViewModel(
    private val client: HomeAssistantClient
) : ViewModel() {

    var entities: List<HaEntity> by mutableStateOf(emptyList())
        private set
    var form by mutableStateOf(OnTriggerStateForm())
        private set

    var currentDomainSearch: String by mutableStateOf("")
    var pendingRestore: HaCallServiceBuiltForm? = null
    var clientError: String by mutableStateOf("")

    fun loadEntities(force: Boolean = false) {
        viewModelScope.launch {
            try {
                val resultPing = withContext(Dispatchers.IO) {
                    client.ping()
                }
                val result = withContext(Dispatchers.IO) {
                    client.getEntities()
                }
                if(client.error != ""){
                    clientError = client.error
                    return@launch
                }
                entities = result
                Log.d("HA", "Loaded entities: ${entities.size}")
            } catch (e: Exception) {
                Log.e("HA", "Failed to load entities", e)
            }
        }
    }

    fun pickEntity(entityId: String) {
        form = form.copy(entityId = entityId)
    }

    fun setFrom(fromState: String) {
        form = form.copy(fromState = fromState)
    }

    fun setTo(toState: String) {
        form = form.copy(toState = toState)
    }

    fun buildForm(): OnTriggerStateBuiltForm {
        val entityId = form.entityId

        var msg = ""

        return OnTriggerStateBuiltForm(
            entityId = entityId,
            blurb = msg,
            fromState = form.fromState,
            toState = form.toState
        )
    }

    fun restoreForm(entity: String, fromState: String, toState: String) {
        Log.e("OnTriggerStateViewModel", "Restoring form: $entity, $fromState, $toState")
        form = OnTriggerStateForm().apply {
            this.entityId = entity
            this.fromState = fromState
            this.toState = toState
        }
    }
}