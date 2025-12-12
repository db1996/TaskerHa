package com.github.db1996.taskerha.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.util.hasNotificationPermission

class BootReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "BOOT_COMPLETED received")

        val wsEnabled = HaSettings.loadWebSocketEnabled(context)
        if (!wsEnabled) {
            Log.d("BootReceiver", "Websocket not enabled in settings, not starting service")
            return
        }

        if (!hasNotificationPermission(context)) {
            Log.d("BootReceiver", "Notification permission not granted, not starting service")
            return
        }

        val url = HaSettings.loadUrl(context)
        val token = HaSettings.loadToken(context)

        if (url.isBlank() || token.isBlank()) {
            Log.d("BootReceiver", "HA URL/token missing, not starting service")
            return
        }

        Log.d("BootReceiver", "Starting HaWebSocketService after boot")
        HaWebSocketService.start(context)
    }
}
