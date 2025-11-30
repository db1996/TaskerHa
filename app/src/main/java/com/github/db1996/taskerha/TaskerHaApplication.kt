package com.github.db1996.taskerha

import android.app.Application

class TaskerHaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Create notification channels on app startup
        NotificationHelper.createNotificationChannels(this)
    }
}