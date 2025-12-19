package com.github.db1996.taskerha.tasker.base

import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.logging.LogChannel

/**
 * Interface for logging functionality that can be used by both ViewModels and Runners
 *
 * Provides default implementations for all logging methods.
 * Override logTag and logChannel to customize behavior.
 */
interface BaseLogger {

    val logTag: String
        get() = this::class.simpleName ?: "BaseLogger"

    val logChannel: LogChannel
        get() = LogChannel.GENERAL

    fun logVerbose(message: String) {
        CustomLogger.v(logTag, message, logChannel)
    }

    fun logDebug(message: String) {
        CustomLogger.d(logTag, message, logChannel)
    }

    fun logError(message: String, throwable: Throwable? = null) {
        CustomLogger.e(logTag, message, logChannel, throwable)
    }

    fun logInfo(message: String) {
        CustomLogger.i(logTag, message, logChannel)
    }
}

/**
 * Error codes used across the application
 */
object ErrorCodes {
    const val ERROR_CODE_UNKNOWN = 9999
    const val ERROR_CODE_NETWORK = 1
    const val ERROR_CODE_INVALID_INPUT = 2
    const val ERROR_CODE_API_ERROR = 3
}
