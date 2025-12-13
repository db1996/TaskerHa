package com.github.db1996.taskerha.tasker.base

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.logging.LogChannel
import com.github.db1996.taskerha.tasker.base.ErrorCodes.ERROR_CODE_NETWORK
import com.github.db1996.taskerha.tasker.base.ErrorCodes.ERROR_CODE_UNKNOWN
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.runBlocking

/**
 * Base runner for Tasker plugin actions with optional client management
 *
 * @param I Input type - the Tasker input class
 * @param O Output type - the Tasker output class (use Unit if no output)
 *
 * Usage example with client:
 * ```
 * class MyRunner : BaseTaskerRunner<MyInput, MyOutput>() {
 *     override val needsClient = true
 *
 *     override suspend fun executeWithClient(
 *         context: Context,
 *         input: MyInput,
 *         client: HomeAssistantClient
 *     ): RunnerResult<MyOutput> {
 *         val result = client.doSomething()
 *         return RunnerResult.Success(MyOutput(result))
 *     }
 * }
 * ```
 *
 * Usage example without client:
 * ```
 * class MyRunner : BaseTaskerRunner<MyInput, MyOutput>() {
 *     override val needsClient = false
 *
 *     override suspend fun execute(context: Context, input: MyInput): RunnerResult<MyOutput> {
 *         // Do work without client
 *         return RunnerResult.Success(MyOutput())
 *     }
 * }
 * ```
 */
abstract class BaseTaskerRunner<I : Any, O : Any> : TaskerPluginRunnerAction<I, O>(), BaseLogger {
    protected open val needsClient: Boolean = true

    override val logTag: String
        get() = this::class.simpleName ?: "BaseTaskerRunner"

    override val logChannel: LogChannel
        get() = LogChannel.GENERAL

    override fun run(
        context: Context,
        input: TaskerInput<I>
    ): TaskerPluginResult<O> {
        return runBlocking {
            try {
                val result = if (needsClient) {
                    val client = createClient(context)

                    logInfo("Pinging HomeAssistant...")
                    if (!client.ping()) {
                        logError("Failed to ping HomeAssistant: ${client.error}")
                        return@runBlocking RunnerResult.Error<O>(
                            errorCode = ERROR_CODE_NETWORK,
                            message = client.error
                        ).toTaskerPluginResult()
                    }

                    logInfo("HomeAssistant ping successful")
                    executeWithClient(context, input.regular, client)
                } else {
                    execute(context, input.regular)
                }

                result.toTaskerPluginResult()
            } catch (e: Exception) {
                logError("Unexpected error in runner: ${e.message}", e)
                RunnerResult.Error<O>(
                    errorCode = ERROR_CODE_UNKNOWN,
                    message = e.message ?: "Unknown error"
                ).toTaskerPluginResult()
            }
        }
    }

    /**
     * Create a HomeAssistantClient from context settings
     */
    private fun createClient(context: Context): HomeAssistantClient {
        val url = HaSettings.loadUrl(context)
        val token = HaSettings.loadToken(context)
        return HomeAssistantClient(url, token)
    }

    /**
     * Execute the plugin action with HomeAssistantClient
     * Override this if needsClient = true
     *
     * @param context Android context
     * @param input The input configuration from Tasker
     * @param client The HomeAssistantClient (already pinged)
     * @return A RunnerResult indicating success or failure
     */
    protected open suspend fun executeWithClient(
        context: Context,
        input: I,
        client: HomeAssistantClient
    ): RunnerResult<O> {
        throw NotImplementedError("Override executeWithClient() when needsClient = true")
    }

    /**
     * Execute the plugin action without client
     * Override this if needsClient = false
     *
     * @param context Android context
     * @param input The input configuration from Tasker
     * @return A RunnerResult indicating success or failure
     */
    protected open suspend fun execute(context: Context, input: I): RunnerResult<O> {
        throw NotImplementedError("Override execute() when needsClient = false")
    }
}

/**
 * Result type for runner execution
 */
sealed class RunnerResult<O : Any> {
    data class Success<O : Any>(val output: O) : RunnerResult<O>()
    data class Error<O : Any>(val errorCode: Int, val message: String) : RunnerResult<O>()

    @Suppress("UNCHECKED_CAST")
    fun toTaskerPluginResult(): TaskerPluginResult<O> {
        return when (this) {
            is Success -> TaskerPluginResultSucess(output) as TaskerPluginResult<O>
            is Error -> TaskerPluginResultError(errorCode, message) as TaskerPluginResult<O>
        }
    }
}

