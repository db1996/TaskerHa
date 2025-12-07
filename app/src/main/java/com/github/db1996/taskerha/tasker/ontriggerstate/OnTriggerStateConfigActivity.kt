package com.github.db1996.taskerha.tasker.ontriggerstate

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.db1996.taskerha.activities.screens.OnTriggerStateScreen
import com.github.db1996.taskerha.activities.viewmodels.OnTriggerStateViewModel
import com.github.db1996.taskerha.activities.viewmodels.OnTriggerStateViewModelFactory
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class ActivityConfigOnTriggerState :
    AppCompatActivity(),
    TaskerPluginConfig<OnTriggerStateInput> {

    override val context: Context
        get() = applicationContext

    private val client by lazy {
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)
        HomeAssistantClient(url, token)
    }
    private val viewModel: OnTriggerStateViewModel by viewModels {
        OnTriggerStateViewModelFactory(client)
    }
    private val helper by lazy { OnTriggerStateHelper(this) }

    private var entityId: String = ""
    private var fromState: String = ""
    private var toState: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        helper.onCreate()
        Log.e("OnTriggerStateConfigActivity", "onCreate for Tasker config")

        viewModel.restoreForm(entityId, fromState, toState)
        setContent {
            TaskerHaTheme {
                OnTriggerStateScreen(viewModel) { entityId, fromState, toState ->
                    this.entityId = entityId
                    this.fromState = fromState
                    this.toState = toState
                    helper.finishForTasker()
                }
            }
        }
    }

    override fun assignFromInput(input: TaskerInput<OnTriggerStateInput>) {
        entityId = input.regular.entityId
        fromState = input.regular.fromState
        toState = input.regular.toState
        Log.d("OnTriggerStateConfigActivity", "Restoring data, entityId: $entityId, fromState: $fromState, toState: $toState")
        viewModel.restoreForm(entityId, fromState, toState)
    }

    override val inputForTasker: TaskerInput<OnTriggerStateInput>
        get() {
            Log.e("OnTriggerStateConfigActivity", "inputForTasker for Tasker config")
            val haInput = OnTriggerStateInput().apply {
                entityId = this@ActivityConfigOnTriggerState.entityId
                fromState = this@ActivityConfigOnTriggerState.fromState
                toState = this@ActivityConfigOnTriggerState.toState
            }
            return TaskerInput(haInput)
        }
}
