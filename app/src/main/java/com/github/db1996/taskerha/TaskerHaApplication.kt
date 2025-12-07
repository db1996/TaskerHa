package com.github.db1996.taskerha

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.github.db1996.taskerha.service.HaWebSocketService

class TaskerHaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
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
