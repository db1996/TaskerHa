package com.github.db1996.taskerha.datamodels

import android.content.Context
import androidx.core.content.edit
import com.github.db1996.taskerha.logging.LogChannel
import com.github.db1996.taskerha.logging.LogLevel

object HaSettings {
    private const val PREFS = "ha_settings"
    private const val KEY_URL = "ha_url"
    private const val KEY_TOKEN = "ha_token"
    private const val KEY_WS_ENABLED = "ws_enabled"
    private const val KEY_LOG_LEVEL_GENERAL = "log_level_general"
    private const val KEY_LOG_LEVEL_WS = "log_level_ws"

    // Local/home URL feature
    private const val KEY_LOCAL_URL_ENABLED = "local_url_enabled"
    private const val KEY_LOCAL_URL = "ha_local_url"
    private const val KEY_HOME_SSIDS = "home_ssids"

    // Client certificate (mTLS) feature
    private const val KEY_CLIENT_CERT_ENABLED = "client_cert_enabled"
    private const val KEY_CLIENT_CERT_ALIAS = "client_cert_alias"

    fun save(context: Context, url: String, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_URL, url)
                    .putString(KEY_TOKEN, token)
            }
    }

    fun loadUrl(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, "") ?: ""
    }

    // --- Local URL settings ---

    fun saveLocalUrlEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_LOCAL_URL_ENABLED, enabled) }
    }

    fun loadLocalUrlEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOCAL_URL_ENABLED, false)
    }

    fun saveLocalUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_LOCAL_URL, url) }
    }

    fun loadLocalUrl(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCAL_URL, "") ?: ""
    }

    fun saveHomeSsids(context: Context, ssids: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putStringSet(KEY_HOME_SSIDS, ssids) }
    }

    fun loadHomeSsids(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_HOME_SSIDS, emptySet()) ?: emptySet()
    }

    // --- Client certificate settings ---

    fun saveClientCertEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_CLIENT_CERT_ENABLED, enabled) }
    }

    fun loadClientCertEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CLIENT_CERT_ENABLED, false)
    }

    fun saveClientCertAlias(context: Context, alias: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_CLIENT_CERT_ALIAS, alias) }
    }

    fun loadClientCertAlias(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CLIENT_CERT_ALIAS, "") ?: ""
    }

    /**
     * Resolves the appropriate URL based on current network context.
     * If the "local URL" feature is enabled and the device is currently connected
     * to one of the configured home WiFi SSIDs, returns the local URL.
     * Otherwise returns the remote/internet URL.
     */
    fun resolveUrl(context: Context): String {
        if (!loadLocalUrlEnabled(context)) return loadUrl(context)

        val localUrl = loadLocalUrl(context)
        if (localUrl.isBlank()) return loadUrl(context)

        val homeSsids = loadHomeSsids(context)
        if (homeSsids.isEmpty()) return loadUrl(context)

        val currentSsid = com.github.db1996.taskerha.util.NetworkHelper.getCurrentSsid()
        if (currentSsid != null && homeSsids.contains(currentSsid)) {
            return localUrl
        }

        return loadUrl(context)
    }

    fun loadToken(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, "") ?: ""
    }
    fun saveWebSocketEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_WS_ENABLED, enabled)
            }
    }

    fun loadWebSocketEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_WS_ENABLED, false)
    }

    fun saveLogLevel(context: Context, channel: LogChannel, level: LogLevel) {
        val key = when (channel) {
            LogChannel.GENERAL -> KEY_LOG_LEVEL_GENERAL
            LogChannel.WEBSOCKET -> KEY_LOG_LEVEL_WS
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putString(key, level.name)
            }
    }

    fun loadLogLevel(context: Context, channel: LogChannel): LogLevel {
        val (key, def) = when (channel) {
            LogChannel.GENERAL -> KEY_LOG_LEVEL_GENERAL to LogLevel.INFO
            LogChannel.WEBSOCKET -> KEY_LOG_LEVEL_WS to LogLevel.INFO
        }

        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, null)

        return LogLevel.fromString(raw, def)
    }
}
