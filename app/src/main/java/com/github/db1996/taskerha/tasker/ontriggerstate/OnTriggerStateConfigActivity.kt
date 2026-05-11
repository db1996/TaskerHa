package com.github.db1996.taskerha.tasker.ontriggerstate

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigActivity
import com.github.db1996.taskerha.tasker.ontriggerstate.data.EntityTriggerConfig
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateForm
import com.github.db1996.taskerha.tasker.ontriggerstate.screens.OnTriggerStateScreen
import com.github.db1996.taskerha.tasker.ontriggerstate.view.OnTriggerStateViewModel
import com.github.db1996.taskerha.tasker.ontriggerstate.view.OnTriggerStateViewModelFactory
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class ActivityConfigOnTriggerState : BaseTaskerConfigActivity<
        OnTriggerStateInput,
        OnTriggerStateOutput,
    OnTriggerStateForm,
    OnTriggerStateBuiltForm,
    OnTriggerStateViewModel
>() {

    override val viewModel: OnTriggerStateViewModel by viewModels { createViewModelFactory() }

    override fun createViewModelFactory() = OnTriggerStateViewModelFactory(this)

    override fun createHelper() = OnTriggerStateConfigHelper(this)

    override fun createScreen(onSave: (OnTriggerStateBuiltForm) -> Unit): @Composable () -> Unit = {
        OnTriggerStateScreen(viewModel, onSave)
    }

    private val mappingJson = Json { ignoreUnknownKeys = true }
    private val mappingSerializer = MapSerializer(String.serializer(), Int.serializer())

    override fun convertBuiltFormToInput(builtForm: OnTriggerStateBuiltForm): OnTriggerStateInput {
        return OnTriggerStateInput().apply {
            entityId = builtForm.entityId
            entityIds = builtForm.entityIds.joinToString(",")
            configPerEntity = builtForm.configPerEntity.toString()
            version = "1"
            triggerId = builtForm.triggerId ?: ""
            attributeMappingJson = mappingJson.encodeToString(mappingSerializer, builtForm.attributeMapping)
            if (builtForm.configPerEntity) {
                val configs = builtForm.entityConfigs
                fromState = configs.joinToString("|;") { it.fromState }
                toState = configs.joinToString("|;") { it.toState }
                forDuration = configs.joinToString("|;") { it.forDuration }
                targetAttribute = configs.joinToString("|;") { it.targetAttribute }
                ignoreMainStateChanges = configs.joinToString("|;") { it.ignoreMainStateChanges.toString() }
            } else {
                val shared = builtForm.sharedConfig
                fromState = shared.fromState
                toState = shared.toState
                forDuration = shared.forDuration
                targetAttribute = shared.targetAttribute
                ignoreMainStateChanges = shared.ignoreMainStateChanges.toString()
            }
        }
    }

    override fun convertInputToBuiltForm(input: OnTriggerStateInput): OnTriggerStateBuiltForm {
        val parsedEntityIds = input.entityIds
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val parsedMapping = try {
            mappingJson.decodeFromString(mappingSerializer, input.attributeMappingJson)
        } catch (_: Exception) {
            emptyMap()
        }

        val inputConfigPerEntity = input.configPerEntity.trim().lowercase() == "true"

        val sharedConfig: EntityTriggerConfig
        val entityConfigs: List<EntityTriggerConfig>

        if (inputConfigPerEntity) {
            val entityCount = parsedEntityIds.size.coerceAtLeast(1)
            fun splitField(raw: String) = raw.split("|;").let { parts ->
                List(entityCount) { i -> parts.getOrElse(i) { "" } }
            }
            val fromList   = splitField(input.fromState)
            val toList     = splitField(input.toState)
            val forList    = splitField(input.forDuration)
            val attrList   = splitField(input.targetAttribute)
            val ignoreList = splitField(input.ignoreMainStateChanges)
            sharedConfig = EntityTriggerConfig()
            entityConfigs = parsedEntityIds.mapIndexed { i, id ->
                EntityTriggerConfig(
                    entityId = id,
                    targetAttribute = attrList[i],
                    fromState = fromList[i],
                    toState = toList[i],
                    forDuration = forList[i],
                    ignoreMainStateChanges = ignoreList[i].trim().lowercase() == "true"
                )
            }
        } else {
            // All-entities mode (default for v0 configs and explicit false)
            sharedConfig = EntityTriggerConfig(
                fromState = input.fromState,
                toState = input.toState,
                forDuration = input.forDuration,
                targetAttribute = input.targetAttribute,
                ignoreMainStateChanges = input.ignoreMainStateChanges.trim().lowercase() == "true"
            )
            entityConfigs = parsedEntityIds.map { EntityTriggerConfig(entityId = it) }
        }

        return OnTriggerStateBuiltForm(
            entityId = input.entityId,
            entityIds = parsedEntityIds,
            entityConfigs = entityConfigs,
            sharedConfig = sharedConfig,
            configPerEntity = inputConfigPerEntity,
            version = 1,
            fromState = "",
            toState = "",
            forDuration = "",
            triggerId = input.triggerId.takeIf { it.isNotBlank() },
            attributeMapping = parsedMapping,
            blurb = if (parsedEntityIds.isNotEmpty()) {
                "Get state: ${parsedEntityIds.joinToString(", ")}"
            } else if (input.entityId.isNotBlank()) {
                "Get state: ${input.entityId}"
            } else {
                "Get state: (any entity)"
            }
        )
    }

    override fun validateBeforeSave(builtForm: OnTriggerStateBuiltForm): String? {
        return null
    }

    override fun onAfterSave(builtForm: OnTriggerStateBuiltForm) {
        HaWebSocketService.resubscribeTriggers(this)
    }
}

