package com.github.db1996.taskerha.tasker.onHaMessage

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.db1996.taskerha.tasker.onHaMessage.view.OnHaMessageViewModel
import com.github.db1996.taskerha.tasker.onHaMessage.view.OnHaMessageViewModelFactory
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class OnHaMessageActivity :
    AppCompatActivity(),
    TaskerPluginConfig<OnHaMessageInput> {

    override val context: Context
        get() = applicationContext

    private val viewModel: OnHaMessageViewModel by viewModels {
        OnHaMessageViewModelFactory()
    }
    private val helper by lazy { OnHaMessageHelper(this) }

    private var type: String = ""
    private var message: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper.onCreate()
        Log.e("OnHaMessageActivity", "onCreate for Tasker config")

        viewModel.restoreForm(type, message)
        helper.finishForTasker()
//        setContent {
//            TaskerHaTheme {
//            }
//        }
    }

    override fun assignFromInput(input: TaskerInput<OnHaMessageInput>) {
        type = input.regular.type
        message = input.regular.message
        Log.d("OnHaMessageActivity", "Restoring data, type: $type, message: $message")
        viewModel.restoreForm(type, message)
    }

    override val inputForTasker: TaskerInput<OnHaMessageInput>
        get() {
            Log.e("OnHaMessageActivity", "inputForTasker for Tasker config")
            val haInput = OnHaMessageInput().apply {
                type = this@OnHaMessageActivity.type
                message = this@OnHaMessageActivity.message
            }
            return TaskerInput(haInput)
        }
}
