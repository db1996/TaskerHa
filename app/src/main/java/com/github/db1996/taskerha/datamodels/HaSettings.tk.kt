package com.github.db1996.taskerha.datamodels

import android.content.Context

object HaSettings {
    private const val PREFS = "ha_settings"
    private const val KEY_URL = "ha_url"
    private const val KEY_TOKEN = "ha_token"
    private const val KEY_WS_ENABLED = "ws_enabled"

    fun save(context: Context, url: String, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URL, url)
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun loadUrl(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, "") ?: ""
    }

    fun loadToken(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, "") ?: ""
    }
    fun saveWebSocketEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WS_ENABLED, enabled)
            .apply()
    }

    fun loadWebSocketEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_WS_ENABLED, false)
    }
}
