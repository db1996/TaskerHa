package com.github.db1996.taskerha.tasker.ontriggerstate

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.github.db1996.taskerha.tasker.ontriggerstate.screens.OnTriggerStateScreen
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigActivity
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateForm
import com.github.db1996.taskerha.tasker.ontriggerstate.view.OnTriggerStateViewModel
import com.github.db1996.taskerha.tasker.ontriggerstate.view.OnTriggerStateViewModelFactory

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

    override fun convertBuiltFormToInput(builtForm: OnTriggerStateBuiltForm): OnTriggerStateInput {
        return OnTriggerStateInput().apply {
            entityId = builtForm.entityId
            fromState = builtForm.fromState
            toState = builtForm.toState
        }
    }

    override fun convertInputToBuiltForm(input: OnTriggerStateInput): OnTriggerStateBuiltForm {
        return OnTriggerStateBuiltForm(
            entityId = input.entityId,
            fromState = input.fromState,
            toState = input.toState,
            blurb = "Get state: ${input.entityId}"
        )
    }

    override fun validateBeforeSave(builtForm: OnTriggerStateBuiltForm): String? {
        return null
    }
}

