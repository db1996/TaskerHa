package com.github.db1996.taskerha.tasker.ontriggerstate.view

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.ClientViewModelFactory

class OnTriggerStateViewModelFactory(
    context: Context
) : ClientViewModelFactory<OnTriggerStateViewModel>(context) {

    override val viewModelClass = OnTriggerStateViewModel::class.java

    override fun createViewModel(client: HomeAssistantClient): OnTriggerStateViewModel {
        return OnTriggerStateViewModel(client)
    }
}
