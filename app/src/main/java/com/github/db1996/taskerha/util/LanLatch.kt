package com.github.db1996.taskerha.util

/**
 * Remembers that the remote endpoint was unreachable while the local one answered,
 * so subsequent pings on the same WiFi network try the local URL first instead of
 * paying the remote timeout over and over.
 *
 * NOT a security control, and must never be treated as one. The signal it latches
 * on — "remote failed, local answered" — is exactly what an attacker who controls
 * the network can fabricate: drop the remote, answer 200 locally. It exists only to
 * avoid repeating a timeout during a genuine connectivity outage.
 *
 * In-memory only. Process death clears it, which costs one extra timeout and is
 * preferable to letting a stale verdict survive a restart.
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
