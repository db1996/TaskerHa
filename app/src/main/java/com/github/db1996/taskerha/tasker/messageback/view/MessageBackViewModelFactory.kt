package com.github.db1996.taskerha.tasker.messageback.view

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.ClientViewModelFactory

class MessageBackViewModelFactory(
    context: Context
) : ClientViewModelFactory<MessageBackViewModel>(context) {

    override val viewModelClass = MessageBackViewModel::class.java

    override fun createViewModel(client: HomeAssistantClient): MessageBackViewModel {
        return MessageBackViewModel(client)
    }
}
