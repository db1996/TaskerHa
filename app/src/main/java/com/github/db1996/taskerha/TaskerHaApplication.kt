package com.github.db1996.taskerha

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.service.HaWebSocketService
import com.github.db1996.taskerha.util.EntityRecents
import com.github.db1996.taskerha.util.PrefsJsonStore
import com.github.db1996.taskerha.util.ServiceRecents

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
        createForegroundChannel()
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
