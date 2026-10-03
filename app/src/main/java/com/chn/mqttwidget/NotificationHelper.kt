package com.chn.mqttwidget

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Surfaces publish failures while the app is not on screen.
 *
 * A Toast is not good enough here: on Android 13+ the notification permission
 * also gates background toasts, and on several OEM skins toasts are dropped
 * whenever the app's notifications are blocked - the error then never reaches
 * the user. A notification is the supported channel, and the widget keeps a
 * persistent red error icon as a fallback that needs no permission at all.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "publish_errors"
    private const val NOTIFICATION_ID = 1
    private const val REQUEST_POST_NOTIFICATIONS = 41

    fun notifyPublishFailed(context: Context, text: String) {
        if (!canPost(context)) return
        createChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mqtt)
            .setContentTitle(context.getString(R.string.notification_error_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Asks for POST_NOTIFICATIONS on Android 13+. Safe to call on every start. */
    fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS,
        )
    }

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_errors),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}
