package com.github.db1996.taskerha.tasker.base

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Base factory for creating ViewModels with dependencies
 *
 * Usage example:
 * ```
 * class MyViewModelFactory(private val client: HomeAssistantClient)
 *     : BaseViewModelFactory<MyViewModel>() {
 *     override fun createViewModel(): MyViewModel {
 *         return MyViewModel(client)
 *     }
 * }
 * ```
 */
abstract class BaseViewModelFactory<VM : ViewModel> : ViewModelProvider.Factory {

    abstract fun createViewModel(): VM

    protected abstract val viewModelClass: Class<VM>

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(viewModelClass)) {
            return createViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * Simple factory for ViewModels with no dependencies
 *
 * Usage example:
 * ```
 * class MyViewModelFactory : SimpleViewModelFactory<MyViewModel>(MyViewModel::class.java) {
 *     override fun createViewModel() = MyViewModel()
 * }
 * ```
 */
abstract class SimpleViewModelFactory<VM : ViewModel>(
    override val viewModelClass: Class<VM>
) : BaseViewModelFactory<VM>()

/**
 * Factory for ViewModels that need HomeAssistantClient
 * Automatically creates and pings the client
 *
 * Usage example:
 * ```
 * class MyViewModelFactory(context: Context)
 *     : ClientViewModelFactory<MyViewModel>(context) {
 *     override val viewModelClass = MyViewModel::class.java
 *     override fun createViewModel(client: HomeAssistantClient) = MyViewModel(client)
 * }
 * ```
 */
abstract class ClientViewModelFactory<VM : ViewModel>(
    context: Context
) : BaseViewModelFactory<VM>() {

    protected val client: HomeAssistantClient by lazy {
        val url = HaSettings.loadUrl(context)
        val token = HaSettings.loadToken(context)
        HomeAssistantClient(url, token).also {
            CoroutineScope(Dispatchers.IO).launch {
                val success = it.ping()
                if (!success) {
                    println("ClientViewModelFactory: Ping failed - ${it.error}")
                } else {
                    println("ClientViewModelFactory: Ping success")
                }
            }
        }
    }

    protected abstract fun createViewModel(client: HomeAssistantClient): VM

    override fun createViewModel(): VM {
        return createViewModel(client)
    }
}
