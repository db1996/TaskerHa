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
