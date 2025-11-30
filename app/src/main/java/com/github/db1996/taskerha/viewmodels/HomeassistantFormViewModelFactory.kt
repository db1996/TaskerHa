package com.github.db1996.taskerha.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.db1996.taskerha.client.HomeAssistantClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeassistantFormViewModelFactory(
    private val client: HomeAssistantClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeassistantFormViewModel::class.java)) {
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
            return HomeassistantFormViewModel(client) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}