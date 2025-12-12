package com.github.db1996.taskerha.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.util.hasNotificationPermission

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"
    
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CustomLogger.v(TAG, "BOOT_COMPLETED received")

        val wsEnabled = HaSettings.loadWebSocketEnabled(context)
        if (!wsEnabled) {
            CustomLogger.d(TAG, "Websocket not enabled in settings, not starting service")
            return
        }

        if (!hasNotificationPermission(context)) {
            CustomLogger.e(TAG, "Notification permission not granted, not starting service")
            return
        }

        val url = HaSettings.loadUrl(context)
        val token = HaSettings.loadToken(context)

        if (url.isBlank() || token.isBlank()) {
            CustomLogger.e(TAG, "HA URL/token missing, not starting service")
            return
        }

        CustomLogger.d(TAG, "Starting HaWebSocketService after boot")
        HaWebSocketService.start(context)
    }
}
