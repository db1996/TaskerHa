package com.github.db1996.taskerha.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.logging.LogChannel

/**
 * Reads the SSID of the currently connected WiFi network.
 *
 * Approach:
 *  - On API < 31 we use the legacy WifiManager.getConnectionInfo() path.
 *  - On API >= 31 we MUST use a NetworkCallback registered with
 *    FLAG_INCLUDE_LOCATION_INFO. NetworkCapabilities.transportInfo
 *    obtained from getNetworkCapabilities() returns a redacted WifiInfo
 *    on Android 12+, so we keep a long-lived callback that maintains
 *    a cached value of the current SSID.
 *
 *  - In all cases ACCESS_FINE_LOCATION must be granted.
 *  - On API >= 33 the manifest also declares NEARBY_WIFI_DEVICES with
 *    "neverForLocation" usage flag.
 *
 *  Synchronous getCurrentSsid() reads the cached value, so callers
 *  outside of coroutine scopes (e.g. resolveUrl in HaSettings) keep
 *  working unchanged.
 */
object NetworkHelper {

    private const val TAG = "NetworkHelper"

    @Volatile
    private var cachedSsid: String? = null

    private var monitoringCallback: ConnectivityManager.NetworkCallback? = null
    private var monitoringCm: ConnectivityManager? = null

    /**
     * Listener notified whenever the WiFi network or SSID changes.
     * Used by HaWebSocketService to reconnect when the device switches network.
     */
    private val changeListeners = mutableSetOf<() -> Unit>()

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Synchronously returns the last known SSID. May return null if:
     *  - no WiFi connection
     *  - monitoring not started
     *  - location permission not granted
     */
    fun getCurrentSsid(): String? = cachedSsid

    /**
     * Registers a long-lived NetworkCallback that keeps the cached SSID
     * up to date. Safe to call multiple times — it's a no-op if already running.
     *
     * Should be called:
     *  - From Application.onCreate() (will silently no-op if permission missing)
     *  - Right after the user grants ACCESS_FINE_LOCATION
     *
     * On API < 31 it uses the legacy path. On API >= 31 it uses the
     * NetworkCallback with FLAG_INCLUDE_LOCATION_INFO so the WifiInfo
     * received in onCapabilitiesChanged() is NOT redacted.
     */
    @SuppressLint("MissingPermission")
    fun startMonitoring(context: Context) {
        if (monitoringCallback != null) return
        if (!hasLocationPermission(context)) {
            CustomLogger.d(TAG, "Skipping startMonitoring: location permission not granted", LogChannel.GENERAL)
            return
        }

        try {
            val cm = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                buildModernCallback()
            } else {
                buildLegacyCallback(context)
            }

            cm.registerNetworkCallback(request, callback)
            monitoringCm = cm
            monitoringCallback = callback
            CustomLogger.i(TAG, "WiFi monitoring started", LogChannel.GENERAL)
        } catch (t: Throwable) {
            CustomLogger.e(TAG, "Failed to start WiFi monitoring", LogChannel.GENERAL, t)
        }
    }

    fun stopMonitoring() {
        val cm = monitoringCm
        val cb = monitoringCallback
        if (cm != null && cb != null) {
            try {
                cm.unregisterNetworkCallback(cb)
            } catch (_: Throwable) {
            }
        }
        monitoringCm = null
        monitoringCallback = null
        cachedSsid = null
    }

    /**
     * Register a listener notified when WiFi connectivity or SSID changes.
     * Returns a handle that must be used to unregister the listener.
     */
    fun addChangeListener(listener: () -> Unit): ChangeListenerRegistration {
        synchronized(changeListeners) { changeListeners.add(listener) }
        return ChangeListenerRegistration(listener)
    }

    class ChangeListenerRegistration(private val listener: () -> Unit) {
        fun unregister() {
            synchronized(changeListeners) { changeListeners.remove(listener) }
        }
    }

    private fun notifyChange() {
        val snapshot = synchronized(changeListeners) { changeListeners.toList() }
        snapshot.forEach {
            try {
                it()
            } catch (t: Throwable) {
                CustomLogger.e(TAG, "Change listener threw", LogChannel.GENERAL, t)
            }
        }
    }

    /**
     * API 31+ path: receive an unredacted WifiInfo via callback flag.
     */
    @android.annotation.TargetApi(Build.VERSION_CODES.S)
    private fun buildModernCallback(): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback(
            FLAG_INCLUDE_LOCATION_INFO
        ) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                try {
                    if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        updateSsid(null)
                        return
                    }
                    val info = networkCapabilities.transportInfo as? WifiInfo
                    updateSsid(cleanSsid(info?.ssid))
                } catch (t: Throwable) {
                    CustomLogger.e(TAG, "onCapabilitiesChanged crashed", LogChannel.GENERAL, t)
                }
            }

            override fun onLost(network: Network) {
                updateSsid(null)
            }
        }
    }

    /**
     * API < 31 path: rely on WifiManager.getConnectionInfo() which is
     * still allowed (and required) on these versions.
     */
    private fun buildLegacyCallback(context: Context): ConnectivityManager.NetworkCallback {
        val appContext = context.applicationContext
        return object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateSsid(getSsidFromWifiManagerLegacy(appContext))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    updateSsid(getSsidFromWifiManagerLegacy(appContext))
                } else {
                    updateSsid(null)
                }
            }

            override fun onLost(network: Network) {
                updateSsid(null)
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun getSsidFromWifiManagerLegacy(context: Context): String? {
        return try {
            val wm = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
            val info = wm.connectionInfo ?: return null
            cleanSsid(info.ssid)
        } catch (t: Throwable) {
            CustomLogger.e(TAG, "Legacy SSID read failed", LogChannel.GENERAL, t)
            null
        }
    }

    private fun updateSsid(newSsid: String?) {
        val old = cachedSsid
        cachedSsid = newSsid
        if (old != newSsid) {
            CustomLogger.d(TAG, "SSID changed: $old -> $newSsid", LogChannel.GENERAL)
            notifyChange()
        }
    }

    /**
     * Strip surrounding quotes and filter out unknown SSID placeholders.
     */
    private fun cleanSsid(raw: String?): String? {
        if (raw == null) return null
        val cleaned = raw.trim().removeSurrounding("\"")
        if (cleaned.isBlank() ||
            cleaned == WifiManager.UNKNOWN_SSID ||
            cleaned == "<unknown ssid>" ||
            cleaned == "0x"
        ) return null
        return cleaned
    }
}
