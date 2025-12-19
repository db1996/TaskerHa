package com.github.db1996.taskerha.tasker.messageback

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigActivity
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackBuiltForm
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackForm
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackInput
import com.github.db1996.taskerha.tasker.messageback.data.MessageBackOutput
import com.github.db1996.taskerha.tasker.messageback.screens.MessageBackScreen
import com.github.db1996.taskerha.tasker.messageback.view.MessageBackViewModel
import com.github.db1996.taskerha.tasker.messageback.view.MessageBackViewModelFactory

class MessageBackConfigActivity : BaseTaskerConfigActivity<
        MessageBackInput,
        MessageBackOutput,
        MessageBackForm,
        MessageBackBuiltForm,
    MessageBackViewModel
>() {

    override val viewModel: MessageBackViewModel by viewModels { createViewModelFactory() }

    override fun createViewModelFactory() = MessageBackViewModelFactory(this)

    override fun createHelper() = MessageBackHelper(this)

    override fun createScreen(onSave: (MessageBackBuiltForm) -> Unit): @Composable () -> Unit = {
        MessageBackScreen (viewModel, onSave)
    }

    override fun convertBuiltFormToInput(builtForm: MessageBackBuiltForm): MessageBackInput {
        return MessageBackInput().apply {
            type = builtForm.type
            message = builtForm.message
        }
    }

    override fun convertInputToBuiltForm(input: MessageBackInput): MessageBackBuiltForm {
        return MessageBackBuiltForm(
            type = input.type,
            message = input.message,
            blurb = "Get state: ${input.type}"
        )
    }

    override fun validateBeforeSave(builtForm: MessageBackBuiltForm): String? {
        return null
    }
}

