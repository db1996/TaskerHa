package com.github.db1996.taskerha.activities.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaCallServiceBuiltForm
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.datamodels.HaGetStateBuiltForm
import com.github.db1996.taskerha.datamodels.HaGetStateForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HaGetStateViewModel(
    private val client: HomeAssistantClient
) : ViewModel() {

    var entities: List<HaEntity> by mutableStateOf(emptyList())
        private set
    var form: HaGetStateForm by mutableStateOf(HaGetStateForm())

    var currentDomainSearch: String by mutableStateOf("")
    var pendingRestore: HaCallServiceBuiltForm? = null

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

    fun unsetPickedService() {
        form.entityId = ""

        currentDomainSearch = ""
    }

    fun pickEntity(entityId: String) {
        form = form.copy(entityId = entityId)
    }

    fun testForm() {
        val entityId = form.entityId

        Log.d("HA", "Testing get state call:  $entityId")

        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    client.getState(entityId)
                }
                Log.d("HA", "Get state ${if (success) "succeeded" else "failed"}, ${client.result}")
            } catch (e: Exception) {
                Log.e("HA", "Error get state", e)
            }
        }
    }

    fun buildForm(): HaGetStateBuiltForm {
        val entityId = form.entityId

        // Build consistent blurb
        var msg = ""

        return HaGetStateBuiltForm(
            entityId = entityId,
            blurb = msg
        )
    }

    fun restoreForm(entity: String) {
        form = HaGetStateForm().apply {
            this.entityId = entity
        }
    }
}