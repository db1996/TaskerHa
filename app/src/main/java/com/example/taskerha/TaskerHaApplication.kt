package com.github.db1996.taskerha

// In a new file: TaskerHaApplication.kt

import android.app.Application

class TaskerHaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Create notification channels on app startup
        NotificationHelper.createNotificationChannels(this)
    }
}