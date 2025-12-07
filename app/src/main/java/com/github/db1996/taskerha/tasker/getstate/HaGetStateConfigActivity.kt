package com.github.db1996.taskerha.tasker.getstate

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.db1996.taskerha.activities.screens.HaGetStateScreen
import com.github.db1996.taskerha.activities.viewmodels.HaGetStateViewModel
import com.github.db1996.taskerha.activities.viewmodels.HaGetStateViewModelFactory
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.ui.theme.TaskerHaTheme
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import kotlin.getValue

class GetStateConfigActivity :
    AppCompatActivity(),
    TaskerPluginConfig<HaGetStateInput> {

    override val context: Context
        get() = applicationContext

    private val client by lazy {
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)
        HomeAssistantClient(url, token)
    }

    private val viewModel: HaGetStateViewModel by viewModels {
        HaGetStateViewModelFactory(client)
    }
    private val helper by lazy { HaGetStateConfigHelper(this) }

    private var entityId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        helper.onCreate()

        setContent {
            TaskerHaTheme {
                HaGetStateScreen(viewModel) { entityId ->
                    this.entityId = entityId

                    helper.finishForTasker()
                }
            }
        }
    }

    override fun assignFromInput(input: TaskerInput<HaGetStateInput>) {
        entityId = input.regular.entityId
        Log.e("GetStateConfigActivity", "entityId: $entityId")
        viewModel.restoreForm(entityId)
    }

    override val inputForTasker: TaskerInput<HaGetStateInput>
        get() {
            val haInput = HaGetStateInput().apply {
                entityId = this@GetStateConfigActivity.entityId
            }
            Log.e("GetStateConfigActivity", "inputForTasker: ${haInput.entityId}")
            return TaskerInput(haInput)
        }
}
