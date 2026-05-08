package com.github.db1996.taskerha.tasker.ontriggerstate

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigActivity
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
            fromState = builtForm.fromState
            toState = builtForm.toState
            forDuration = builtForm.forDuration
            triggerId = builtForm.triggerId ?: ""
            attributeMappingJson = mappingJson.encodeToString(mappingSerializer, builtForm.attributeMapping)
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
        return OnTriggerStateBuiltForm(
            entityId = input.entityId,
            entityIds = parsedEntityIds,
            fromState = input.fromState,
            toState = input.toState,
            forDuration = input.forDuration,
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

