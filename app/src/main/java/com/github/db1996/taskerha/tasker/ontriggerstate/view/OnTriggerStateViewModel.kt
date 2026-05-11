package com.github.db1996.taskerha.tasker.ontriggerstate.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.logging.LogChannel
import com.github.db1996.taskerha.tasker.base.BaseViewModel
import com.github.db1996.taskerha.tasker.base.ValidationResult
import com.github.db1996.taskerha.tasker.ontriggerstate.data.EntityTriggerConfig
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateForm
import java.util.UUID

class OnTriggerStateViewModel(
    client: HomeAssistantClient
) : BaseViewModel<OnTriggerStateForm, OnTriggerStateBuiltForm>(
    initialForm = OnTriggerStateForm(),
    client = client
) {

    var entities: List<HaEntity> by mutableStateOf(emptyList())
        private set

    var currentDomainSearch: String by mutableStateOf("")

    var availableAttributes: Map<String, List<String>> by mutableStateOf(emptyMap())
        private set

    var isLoadingAttributes: Boolean by mutableStateOf(false)
        private set


    override val logTag: String
        get() =  "OnTriggerStateViewModel"

    override val logChannel: LogChannel
        get() = LogChannel.WEBSOCKET

    // UI event handlers
//    fun pickEntity(entityId: String) {
//        form = form.copy(entityId = entityId)
//    }

    fun addEntity(entityId: String) {
        val trimmed = entityId.trim()
        if (trimmed.isNotBlank() && trimmed !in form.entityIds) {
            form = form.copy(
                entityIds = form.entityIds + trimmed,
                entityConfigs = form.entityConfigs + EntityTriggerConfig(entityId = trimmed)
            )
        }
    }

    fun removeEntity(index: Int) {
        form = form.copy(
            entityIds = form.entityIds.toMutableList().also { it.removeAt(index) },
            entityConfigs = form.entityConfigs.toMutableList().also {
                if (index < it.size) it.removeAt(index)
            }
        )
    }

    fun updateEntityAt(index: Int, value: String) {
        val updatedIds = form.entityIds.toMutableList()
        updatedIds[index] = value
        val updatedConfigs = form.entityConfigs.toMutableList()
        if (index < updatedConfigs.size) {
            updatedConfigs[index] = updatedConfigs[index].copy(entityId = value)
        }
        form = form.copy(entityIds = updatedIds, entityConfigs = updatedConfigs)
    }

    private fun updateConfig(index: Int, update: (EntityTriggerConfig) -> EntityTriggerConfig) {
        val updated = form.entityConfigs.toMutableList()
        if (index < updated.size) updated[index] = update(updated[index])
        form = form.copy(entityConfigs = updated)
    }

    fun setFrom(index: Int, fromState: String) = updateConfig(index) { it.copy(fromState = fromState) }
    fun setTo(index: Int, toState: String) = updateConfig(index) { it.copy(toState = toState) }
    fun setFor(index: Int, forDuration: String) = updateConfig(index) { it.copy(forDuration = forDuration) }
    fun setTargetAttribute(index: Int, value: String) = updateConfig(index) { it.copy(targetAttribute = value) }
    fun setIgnoreMainStateChanges(index: Int, value: Boolean) = updateConfig(index) { it.copy(ignoreMainStateChanges = value) }

    private fun updateSharedConfig(update: (EntityTriggerConfig) -> EntityTriggerConfig) {
        form = form.copy(sharedConfig = update(form.sharedConfig))
    }

    fun setSharedFrom(value: String) = updateSharedConfig { it.copy(fromState = value) }
    fun setSharedTo(value: String) = updateSharedConfig { it.copy(toState = value) }
    fun setSharedFor(value: String) = updateSharedConfig { it.copy(forDuration = value) }
    fun setSharedTargetAttribute(value: String) = updateSharedConfig { it.copy(targetAttribute = value) }
    fun setSharedIgnoreMainStateChanges(value: Boolean) = updateSharedConfig { it.copy(ignoreMainStateChanges = value) }

    fun setConfigPerEntity(value: Boolean) {
        if (value) {
            // Pre-fill entityConfigs from sharedConfig when switching to per-entity mode
            val shared = form.sharedConfig
            val newConfigs = form.entityIds.mapIndexed { i, id ->
                form.entityConfigs.getOrNull(i)?.copy(entityId = id)
                    ?: EntityTriggerConfig(
                        entityId = id,
                        fromState = shared.fromState,
                        toState = shared.toState,
                        forDuration = shared.forDuration,
                        targetAttribute = shared.targetAttribute,
                        ignoreMainStateChanges = shared.ignoreMainStateChanges
                    )
            }
            form = form.copy(configPerEntity = true, entityConfigs = newConfigs)
        } else {
            form = form.copy(configPerEntity = false)
        }
    }

    fun setAttributeSlot(attrKey: String, slot: Int?) {
        val updated = form.attributeMapping.toMutableMap()
        if (slot == null) updated.remove(attrKey) else updated[attrKey] = slot
        form = form.copy(attributeMapping = updated)
    }

    fun loadAttributesForAllEntities() {
        val entityIds = form.entityIds.filter { it.isNotBlank() }
            .ifEmpty { listOf(form.entityId).filter { it.isNotBlank() } }
        if (entityIds.isEmpty()) return
        isLoadingAttributes = true
        launchClientOperation { client ->
            val result = mutableMapOf<String, List<String>>()
            for (entityId in entityIds) {
                val keys = client.getEntityAttributeKeys(entityId)
                if (keys.isNotEmpty()) result[entityId] = keys
                logDebug("Loaded attributes for $entityId: ${keys.size}")
            }
            availableAttributes = result
            isLoadingAttributes = false
        }
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
            entityConfigs = form.entityConfigs,
            sharedConfig = form.sharedConfig,
            configPerEntity = form.configPerEntity,
            version = 1,
            blurb = blurb,
            fromState = "",
            toState = "",
            forDuration = "",
            triggerId = UUID.randomUUID().toString(),
            attributeMapping = form.attributeMapping
        )
    }

    override fun restoreForm(data: OnTriggerStateBuiltForm) {
        logVerbose("Restoring form: entityId=${data.entityId}, entityIds=${data.entityIds}, version=${data.version}, configPerEntity=${data.configPerEntity}")
        // Migrate legacy single entityId into the multi-entity list
        val migratedIds = if (data.entityIds.isEmpty() && data.entityId.isNotBlank()) {
            listOf(data.entityId.trim())
        } else {
            data.entityIds
        }

        // v0 migration: restore shared config from old top-level fields
        val restoredSharedConfig = if (data.version == 0) {
            EntityTriggerConfig(
                fromState = data.fromState,
                toState = data.toState,
                forDuration = data.forDuration
            )
        } else {
            data.sharedConfig
        }

        // Ensure entityConfigs is in sync with entityIds
        val restoredConfigs = if (data.configPerEntity) {
            migratedIds.mapIndexed { i, id ->
                data.entityConfigs.getOrNull(i)?.copy(entityId = id)
                    ?: EntityTriggerConfig(entityId = id)
            }
        } else {
            migratedIds.map { id ->
                data.entityConfigs.find { it.entityId == id }
                    ?: EntityTriggerConfig(entityId = id)
            }
        }

        form = OnTriggerStateForm(
            entityId = data.entityId,
            entityIds = migratedIds,
            entityConfigs = restoredConfigs,
            sharedConfig = restoredSharedConfig,
            configPerEntity = data.configPerEntity,
            attributeMapping = data.attributeMapping
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

