package com.github.db1996.taskerha.tasker.onHaMessage.view

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.ClientViewModelFactory

class OnHaMessageViewModelFactory(
    context: Context
) : ClientViewModelFactory<OnHaMessageViewModel>(context) {

    override val viewModelClass = OnHaMessageViewModel::class.java

    override fun createViewModel(client: HomeAssistantClient): OnHaMessageViewModel {
        return OnHaMessageViewModel(client)
    }
}
