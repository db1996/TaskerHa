package com.github.db1996.taskerha.tasker.testconnection

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.tasker.base.BaseTaskerRunner
import com.github.db1996.taskerha.tasker.base.RunnerResult
import com.github.db1996.taskerha.util.HaHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TestConnectionRunner : BaseTaskerRunner<TestConnectionInput, TestConnectionOutput>() {

    override val needsClient = false

    override val logTag: String
        get() = "TestConnectionRunner"

    override suspend fun execute(
        context: Context,
        input: TestConnectionInput
    ): RunnerResult<TestConnectionOutput> {
        return try {
            val instance = HaInstanceRepository.getById(input.instanceId)
                ?: HaInstanceRepository.getDefault()

            val token = instance?.token ?: HaSettings.loadToken(context)
            val httpClient = HaHttpClientFactory.build(
                context,
                clientCertEnabled = instance?.clientCertEnabled ?: HaSettings.loadClientCertEnabled(context),
                clientCertAlias = instance?.clientCertAlias ?: HaSettings.loadClientCertAlias(context)
            )

            // Test remote connection
            val remoteUrl = instance?.remoteUrl ?: HaSettings.loadUrl(context)
            val remoteSuccess = testConnection(remoteUrl, token, httpClient)
            logInfo("Remote connection test: $remoteSuccess (URL: $remoteUrl)")

            // Test local connection if a local URL is configured
            val localUrl = instance?.localUrl ?: HaSettings.loadLocalUrl(context).takeIf {
                HaSettings.loadLocalUrlEnabled(context)
            } ?: ""
            val localSuccess: Boolean? = if (localUrl.isNotBlank()) {
                val success = testConnection(localUrl, token, httpClient)
                logInfo("Local connection test: $success (URL: $localUrl)")
                success
            } else {
                logInfo("No local URL configured, skipping local test")
                null
            }

            // Build output
            val output = TestConnectionOutput(
                haRemote = if (remoteSuccess) "true" else "false",
                haLocal = localSuccess?.let { if (it) "true" else "false" }
            )

            logInfo("Test complete - Remote: ${output.haRemote}, Local: ${output.haLocal ?: "not tested"}")
            RunnerResult.Success(output)

        } catch (e: Exception) {
            logError("Error testing connections", e)
            RunnerResult.Success(
                TestConnectionOutput(
                    haRemote = "false",
                    haLocal = null
                )
            )
        }
    }

    private suspend fun testConnection(url: String, token: String, httpClient: okhttp3.OkHttpClient): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val client = HomeAssistantClient(url, token, httpClient)
                client.ping()
            } catch (e: Exception) {
                logError("Exception during ping: ${e.message}", e)
                false
            }
        }
}
