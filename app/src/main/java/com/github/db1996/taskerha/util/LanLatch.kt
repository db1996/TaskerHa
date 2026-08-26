package com.github.db1996.taskerha.util

/**
 * Remembers that the local endpoint answered while the remote one didn't, so
 * subsequent pings on the same WiFi network try local first instead of paying the
 * remote timeout again. In-memory only; cleared on network change or process death.
 *
 * Not a security control — the network itself could fabricate this signal.
 */
object LanLatch {

    @Volatile
    private var armedSsid: String? = null

    @Volatile
    private var armedUrl: String? = null

    fun arm(ssid: String?, url: String) {
        if (ssid.isNullOrBlank() || url.isBlank()) return
        armedSsid = ssid
        armedUrl = url
    }

    fun armedUrlFor(ssid: String?): String? =
        if (ssid != null && ssid == armedSsid) armedUrl else null

    fun disarm() {
        armedSsid = null
        armedUrl = null
    }
}
