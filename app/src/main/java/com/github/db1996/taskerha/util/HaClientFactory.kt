package com.github.db1996.taskerha.util

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaInstance

/**
 * Builds a [HomeAssistantClient] for an instance. Single place for this so the
 * endpoint-selection bookkeeping (see [LanLatch]) lives in exactly one spot instead
 * of being duplicated across the CallService / GetState / OnTriggerState view models.
 */
object HaClientFactory {

    fun forInstance(context: Context, instance: HaInstance): HomeAssistantClient {
        return HomeAssistantClient(
            baseUrl = instance.resolveUrlCandidates().first(),
            accessToken = instance.token,
            httpClient = HaHttpClientFactory.build(
                context,
                clientCertEnabled = instance.clientCertEnabled,
                clientCertAlias = instance.clientCertAlias
            ),
            candidateProvider = { instance.resolveUrlCandidates() },
            onEndpointSelected = { selected ->
                noteSelected(instance, selected, NetworkHelper.getCurrentSsid())
            }
        )
    }

    /**
     * Latches to the local URL when it wins (remote must have failed); disarms
     * otherwise. Resilience bookkeeping only, not a security decision.
     */
    fun noteSelected(instance: HaInstance, selectedUrl: String, ssid: String?) {
        if (instance.localUrl.isNotBlank() && selectedUrl == instance.localUrl) {
            LanLatch.arm(ssid, selectedUrl)
        } else {
            LanLatch.disarm()
        }
    }
}
