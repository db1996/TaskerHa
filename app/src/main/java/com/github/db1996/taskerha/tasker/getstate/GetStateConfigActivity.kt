package com.github.db1996.taskerha.tasker.getstate

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigActivity
import com.github.db1996.taskerha.tasker.getstate.data.HaGetStateBuiltForm
import com.github.db1996.taskerha.tasker.getstate.data.HaGetStateForm
import com.github.db1996.taskerha.tasker.getstate.screens.HaGetStateScreen
import com.github.db1996.taskerha.tasker.getstate.view.HaGetStateViewModel
import com.github.db1996.taskerha.tasker.getstate.view.HaGetStateViewModelFactory

class GetStateConfigActivity : BaseTaskerConfigActivity<
        HaGetStateInput,
        HaGetStateOutput,
    HaGetStateForm,
    HaGetStateBuiltForm,
    HaGetStateViewModel
>() {

    override val viewModel: HaGetStateViewModel by viewModels { createViewModelFactory() }

    override fun createViewModelFactory() = HaGetStateViewModelFactory(this)

    override fun createHelper() = HaGetStateConfigHelper(this)

    override fun createScreen(onSave: (HaGetStateBuiltForm) -> Unit): @Composable () -> Unit = {
        HaGetStateScreen(viewModel, onSave)
    }

    override fun convertBuiltFormToInput(builtForm: HaGetStateBuiltForm): HaGetStateInput {
        return HaGetStateInput().apply {
            entityId = builtForm.entityId
        }
    }

    override fun convertInputToBuiltForm(input: HaGetStateInput): HaGetStateBuiltForm {
        return HaGetStateBuiltForm(
            entityId = input.entityId,
            blurb = "Get state: ${input.entityId}"
        )
    }

    override fun validateBeforeSave(builtForm: HaGetStateBuiltForm): String? {
        if (builtForm.entityId.isBlank()) {
            return "Entity ID cannot be empty"
        }
        return null
    }
}

