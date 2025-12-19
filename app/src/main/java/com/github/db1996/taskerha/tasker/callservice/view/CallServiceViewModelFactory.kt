package com.github.db1996.taskerha.tasker.callservice.view

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.ClientViewModelFactory

class CallServiceViewModelFactory(
    context: Context
) : ClientViewModelFactory<CallServiceViewModel>(context) {

    override val viewModelClass = CallServiceViewModel::class.java

    override fun createViewModel(client: HomeAssistantClient): CallServiceViewModel {
        return CallServiceViewModel(client)
    }
}
