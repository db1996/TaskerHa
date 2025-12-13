package com.github.db1996.taskerha.tasker.base

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.logging.LogChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base ViewModel for Tasker plugin actions with generic form type support
 *
 * @param F Form type - the mutable state form that holds UI data
 * @param B Built form type - the immutable form that is saved to Tasker
 * @param initialForm The initial form state
 * @param client HomeAssistantClient instance (optional - pass null if not needed)
 */
abstract class BaseViewModel<F : Any, B : Any>(
    initialForm: F,
    protected val client: HomeAssistantClient? = null
) : ViewModel(), BaseLogger {

    var form by mutableStateOf(initialForm)
        protected set

    /**
     * Client error state - exposed to UI
     */
    var clientError: String by mutableStateOf("")
        protected set

    /**
     * Whether the client has been successfully pinged
     */
    private var clientPinged: Boolean = false

    /**
     * Build the form data to save to Tasker
     * Implement this to convert from mutable form (F) to built form (B)
     */
    abstract fun buildForm(): B

    /**
     * Restore form data from Tasker input
     * Implement this to convert saved data back to the mutable form
     */
    abstract fun restoreForm(data: B)

    /**
     * Reset form to initial state
     */
    open fun resetForm() {
        form = createInitialForm()
    }

    /**
     * Create a new instance of the initial form
     * Override if you need custom initialization logic
     */
    protected abstract fun createInitialForm(): F

    /**
     * Update the form state
     * Use this from UI event handlers
     */
    protected fun updateForm(update: F.() -> F) {
        form = form.update()
    }

    /**
     * Optional: Validate the form before saving
     * Override to add validation logic
     */
    open fun validateForm(): ValidationResult {
        return ValidationResult.Valid
    }

    /**
     * Optional: Log tag for debugging
     */
    override val logTag: String
        get() = this::class.simpleName ?: "BaseViewModel"

    override val logChannel: LogChannel
        get() = LogChannel.GENERAL

    /**
     * Ping the HomeAssistant client to verify connection.
     * Automatically called before executing client operations.
     *
     * @return true if ping succeeded, false otherwise
     */
    protected suspend fun ensureClientReady(): Boolean {
        if (client == null) {
            clientError = "No HomeAssistant client configured"
            logError(clientError)
            return false
        }

        if (clientPinged && client.error.isEmpty()) {
            return true // Already pinged successfully
        }

        val success = withContext(Dispatchers.IO) {
            client.ping()
        }

        if (!success) {
            clientError = client.error
            logError("Failed to ping HomeAssistant: ${client.error}")
            return false
        }

        clientPinged = true
        clientError = ""
        logDebug("Successfully pinged HomeAssistant")
        return true
    }

    /**
     * Execute a client operation with automatic ping and error handling.
     * Use this helper to safely call HomeAssistant client methods.
     *
     * @param operation The operation to execute
     * @return The result of the operation, or null if ping failed
     */
    protected suspend fun <T> executeClientOperation(operation: suspend (HomeAssistantClient) -> T): T? {
        if (!ensureClientReady()) {
            return null
        }

        return try {
            val result = withContext(Dispatchers.IO) {
                operation(client!!)
            }

            if (client!!.error.isNotEmpty()) {
                clientError = client!!.error
                logError("Client operation failed: ${client!!.error}")
                return null
            }

            clientError = ""
            result
        } catch (e: Exception) {
            clientError = e.message ?: "Unknown error"
            logError("Client operation exception: ${e.message}")
            null
        }
    }

    /**
     * Execute a client operation in a viewModelScope coroutine.
     * Convenience method for UI-triggered operations.
     */
    protected fun launchClientOperation(operation: suspend (HomeAssistantClient) -> Unit) {
        viewModelScope.launch {
            executeClientOperation(operation)
        }
    }

    /**
     * Clear client error state
     */
    protected fun clearClientError() {
        clientError = ""
    }

    /**
     * Check if client is available
     */
    protected val hasClient: Boolean
        get() = client != null
}

/**
 * Result of form validation
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

