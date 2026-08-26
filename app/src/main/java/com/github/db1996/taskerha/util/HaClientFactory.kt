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
        return HomeAssistantClient(
            baseUrl = instance.resolveUrlCandidates().first(),
            accessToken = instance.token,
            httpClient = HaHttpClientFactory.build(
                context,
                clientCertEnabled = instance.clientCertEnabled,
                clientCertAlias = instance.clientCertAlias
            ),
            // Re-resolved on every ping, never snapshotted: the client outlives the
            // network it was built on, and the SSID gate that decides whether the
            // local endpoint is eligible at all has to be evaluated against the
            // network in use now, not the one in use at construction time.
            candidateProvider = { instance.resolveUrlCandidates() },
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
     * This is resilience bookkeeping, not a security decision.
     */
    fun noteSelected(instance: HaInstance, selectedUrl: String, ssid: String?) {
        if (instance.localUrl.isNotBlank() && selectedUrl == instance.localUrl) {
            LanLatch.arm(ssid, selectedUrl)
        } else {
            LanLatch.disarm()
        }
    }
}
