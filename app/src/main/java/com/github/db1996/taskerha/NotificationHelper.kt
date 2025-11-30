package com.github.db1996.taskerha

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val ERROR_CHANNEL_ID = "tasker_ha_error_channel"
    private var errorNotificationId = 1 // Start ID for error notifications

    // Call this from your Application's onCreate()
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name =
                context.getString(R.string.notification_channel_error_name) // Add this to strings.xml
            val descriptionText =
                context.getString(R.string.notification_channel_error_description) // And this one
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(ERROR_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Call this from your BroadcastReceiver to show an error
    fun showErrorNotification(context: Context, title: String, content: String) {
        // First, check if you have permission to post notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // No permission, can't show notification. You could log this.
                return
            }
        }

        val builder = NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon) // You need to create this icon
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // Allows for longer text
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismisses the notification when the user taps it

        with(NotificationManagerCompat.from(context)) {
            // Use a unique ID for each notification to show multiple, or a static one to update the same notification.
            // Incrementing allows multiple distinct errors to be shown.
            notify(errorNotificationId++, builder.build())
        }
    }
}