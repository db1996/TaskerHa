package com.github.db1996.taskerha.util

import android.content.Context
import com.github.db1996.taskerha.client.HomeAssistantClient
import com.github.db1996.taskerha.datamodels.HaInstance

/**
 * Single place where a [HomeAssistantClient] is built for an instance.
 *
 * Consolidates the three identical copies of createClientForInstance that used to
 * live in the CallService / GetState / OnTriggerState view models, which is what
 * lets the endpoint-selection bookkeeping live in exactly one place.
 */
object HaClientFactory {

    fun forInstance(context: Context, instance: HaInstance): HomeAssistantClient {
        val candidates = instance.resolveUrlCandidates()
        return HomeAssistantClient(
            baseUrl = candidates.first(),
            accessToken = instance.token,
            httpClient = HaHttpClientFactory.build(
                context,
                clientCertEnabled = instance.clientCertEnabled,
                clientCertAlias = instance.clientCertAlias
            ),
            urlCandidates = candidates,
            onEndpointSelected = { selected ->
                noteSelected(instance, selected, NetworkHelper.getCurrentSsid())
            }
        )
    }

    /**
     * Records which endpoint won. Settling on the local URL means the remote one was
     * unreachable, so we latch to skip its timeout next time; settling on the remote
     * one means connectivity is back, so any previous latch is stale.
     *
     * This is resilience bookkeeping, not a security decision — see
     * docs/superpowers/specs/2026-08-25-url-fallback-sicurezza-design.md
     */
    fun noteSelected(instance: HaInstance, selectedUrl: String, ssid: String?) {
        if (instance.localUrl.isNotBlank() && selectedUrl == instance.localUrl) {
            LanLatch.arm(ssid, selectedUrl)
        } else {
            LanLatch.disarm()
        }
    }
}
