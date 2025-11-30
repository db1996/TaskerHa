package com.github.db1996.taskerha.activities.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.db1996.taskerha.client.HomeAssistantClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PluginConfigViewModelFactory(
    private val client: HomeAssistantClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PluginConfigViewModel::class.java)) {
            CoroutineScope(Dispatchers.IO).launch {
                val successPing = client.ping()
                if(!successPing){
                    println("Ping failed")
                    println(client.error);
                    return@launch
                }
                println("Ping success")

            }
            @Suppress("UNCHECKED_CAST")
            return PluginConfigViewModel(client) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}