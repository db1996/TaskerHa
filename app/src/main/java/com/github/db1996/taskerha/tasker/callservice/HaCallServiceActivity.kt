package com.github.db1996.taskerha.tasker.callservice

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigActivity
import com.github.db1996.taskerha.tasker.callservice.data.CallServiceFormBuiltForm
import com.github.db1996.taskerha.tasker.callservice.data.CallServiceFormForm
import com.github.db1996.taskerha.tasker.callservice.screens.CallServiceScreen
import com.github.db1996.taskerha.tasker.callservice.view.CallServiceViewModel
import com.github.db1996.taskerha.tasker.callservice.view.CallServiceViewModelFactory
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class HaCallServiceActivity : BaseTaskerConfigActivity<
        CallServiceInput,
        CallServiceOutput,
    CallServiceFormForm,
    CallServiceFormBuiltForm,
    CallServiceViewModel
>() {

    override val viewModel: CallServiceViewModel by viewModels { createViewModelFactory() }
    
    // Track if this is a new action (no instanceId in input)
    private var isNewAction = false

    override fun createViewModelFactory() = CallServiceViewModelFactory(this)

    override fun createHelper() = CallServiceConfigHelper(this)

    override fun createScreen(onSave: (CallServiceFormBuiltForm) -> Unit): @Composable () -> Unit = {
        CallServiceScreen(viewModel, onSave, isNewAction)
    }

    override fun convertBuiltFormToInput(builtForm: CallServiceFormBuiltForm): CallServiceInput {
        val jsonData = Json.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            builtForm.data
        )

        return CallServiceInput().apply {
            entityId = builtForm.entityId
            domain = builtForm.domain
            service = builtForm.service
            dataJson = jsonData
            instanceId = builtForm.instanceId
        }
    }

    override fun convertInputToBuiltForm(input: CallServiceInput): CallServiceFormBuiltForm {
        // Track if this is a new action (no instanceId yet)
        isNewAction = input.instanceId.isBlank()
        
        val dataMap: Map<String, String> = try {
            Json.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                input.dataJson
            )
        } catch (_: Exception) {
            emptyMap()
        }

        return CallServiceFormBuiltForm(
            entityId = input.entityId,
            domain = input.domain,
            service = input.service,
            data = dataMap,
            instanceId = input.instanceId.ifBlank { 
                HaInstanceRepository.getActive()?.id ?: HaInstanceRepository.getDefault()?.id ?: "" 
            },
            blurb = ""
        )
    }

    override fun validateBeforeSave(builtForm: CallServiceFormBuiltForm): String? {
        return null
    }
}

