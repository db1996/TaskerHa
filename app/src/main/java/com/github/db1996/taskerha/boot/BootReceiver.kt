package com.github.db1996.taskerha.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.service.HaWebSocketService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        Log.d("BootReceiver", "BOOT_COMPLETED received")

        // 1) Respect your own “feature enabled” setting
        val wsEnabled = HaSettings.loadWebSocketEnabled(context)

        if (!wsEnabled) {
            Log.d("BootReceiver", "Websocket not enabled in settings, not starting service")
            return
        }

        // 2) Only start if we have URL + token
        val url = HaSettings.loadUrl(context)
        val token = HaSettings.loadToken(context)

        if (url.isNullOrBlank() || token.isNullOrBlank()) {
            Log.d("BootReceiver", "HA URL/token missing, not starting service")
            return
        }

        HaWebSocketService.start(context)
    }
}
