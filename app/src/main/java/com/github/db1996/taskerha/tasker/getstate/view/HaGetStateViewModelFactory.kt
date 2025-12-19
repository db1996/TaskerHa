package com.github.db1996.taskerha.tasker.getstate.view

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.tasker.base.ClientViewModelFactory

class HaGetStateViewModelFactory(
    context: Context
) : ClientViewModelFactory<HaGetStateViewModel>(context) {

    override val viewModelClass = HaGetStateViewModel::class.java

    override fun createViewModel(client: HomeAssistantClient): HaGetStateViewModel {
        return HaGetStateViewModel(client)
    }
}
