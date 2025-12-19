package com.github.db1996.taskerha.tasker.onHaMessage

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigActivity
import com.github.db1996.taskerha.tasker.onHaMessage.data.OnHaMessageBuiltForm
import com.github.db1996.taskerha.tasker.onHaMessage.data.OnHaMessageForm
import com.github.db1996.taskerha.tasker.onHaMessage.screens.OnHaMessageScreen
import com.github.db1996.taskerha.tasker.onHaMessage.view.OnHaMessageViewModel
import com.github.db1996.taskerha.tasker.onHaMessage.view.OnHaMessageViewModelFactory

class OnHaMessageActivity : BaseTaskerConfigActivity<
        OnHaMessageInput,
        OnHaMessageOutput,
    OnHaMessageForm,
    OnHaMessageBuiltForm,
    OnHaMessageViewModel
>() {

    override val viewModel: OnHaMessageViewModel by viewModels { createViewModelFactory() }

    override fun createViewModelFactory() = OnHaMessageViewModelFactory(this)

    override fun createHelper() = OnHaMessageHelper(this)

    override fun createScreen(onSave: (OnHaMessageBuiltForm) -> Unit): @Composable () -> Unit = {
        OnHaMessageScreen (viewModel, onSave)
    }

    override fun convertBuiltFormToInput(builtForm: OnHaMessageBuiltForm): OnHaMessageInput {
        return OnHaMessageInput().apply {
            type = builtForm.type
            message = builtForm.message
        }
    }

    override fun convertInputToBuiltForm(input: OnHaMessageInput): OnHaMessageBuiltForm {
        return OnHaMessageBuiltForm(
            type = input.type,
            message = input.message,
            blurb = "Get state: ${input.type}"
        )
    }

    override fun validateBeforeSave(builtForm: OnHaMessageBuiltForm): String? {
        return null
    }
}

