package com.github.db1996.taskerha

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.datamodels.HaInstanceRepository
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.util.EntityRecents
import com.github.db1996.taskerha.util.NetworkHelper
import com.github.db1996.taskerha.util.PingManager
import com.github.db1996.taskerha.util.PrefsJsonStore
import com.github.db1996.taskerha.util.ServiceRecents
import com.github.db1996.taskerha.util.hasNotificationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskerHaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CustomLogger.init(this)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                CustomLogger.e("TaskerHaApplication", "Uncaught exception", t = throwable)
            } catch (_: Throwable) {
                // Avoid throwing from the crash handler
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
                    ?: kotlin.run { android.os.Process.killProcess(android.os.Process.myPid()) }
            }
        }

        EntityRecents.init(this)
        ServiceRecents.init(this)
        PrefsJsonStore.init(this)
        
        // Initialize instance repository and migrate from legacy settings if needed
        HaInstanceRepository.init(this)
        if (HaInstanceRepository.needsMigration()) {
            HaInstanceRepository.migrateFromLegacy()
            CustomLogger.i("TaskerHaApplication", "Migrated legacy settings to instance repository")
        }
        
        createForegroundChannel()

        // Start WiFi monitoring for SSID-based URL resolution.
        // This is a no-op if location permission has not been granted yet;
        // it will be retried from the Settings screen after the user grants it.
        NetworkHelper.startMonitoring(this)

        // Auto-start the WebSocket service if it was enabled and credentials are configured.
        // Covers: app update, process kill/restart, and any case the service isn't running.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            && HaSettings.loadWebSocketEnabled(this)
            && hasNotificationPermission(this)
        ) {
            val url = HaSettings.loadUrl(this)
            val localUrl = HaSettings.loadLocalUrl(this)
            val token = HaSettings.loadToken(this)
            if (token.isNotBlank() && (url.isNotBlank() || localUrl.isNotBlank())) {
                HaWebSocketService.start(this)
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            PingManager.ping(this@TaskerHaApplication)
        }
    }

    private fun createForegroundChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HaWebSocketService.CHANNEL_ID,
                "Home Assistant Events",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps TaskerHA connected to Home Assistant for triggers"
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
