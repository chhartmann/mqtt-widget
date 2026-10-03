package com.chn.mqttwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.content.ContextCompat

class MqttWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WidgetConfigStore.delete(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive action=${intent.action}")
        when (intent.action) {
            ACTION_PUBLISH -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // Reset a possibly stale error display right away, so the
                    // tap itself gives feedback even before the result arrives.
                    updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                    MqttPublishWorker.enqueue(context, appWidgetId)
                }
            }

            ACTION_RESULT -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
                val success = intent.getBooleanExtra(EXTRA_SUCCESS, false)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    showResult(
                        context,
                        appWidgetId,
                        success,
                        intent.getStringExtra(EXTRA_ERROR),
                    )
                }
            }
        }
    }

    private fun showResult(
        context: Context,
        appWidgetId: Int,
        success: Boolean,
        error: String?,
    ) {
        if (!success) {
            val message = error ?: context.getString(R.string.unknown_error)
            val description = context.getString(R.string.publish_failed, message)
            Toast.makeText(context, description, Toast.LENGTH_LONG).show()
            NotificationHelper.notifyPublishFailed(context, description)
        }

        val manager = AppWidgetManager.getInstance(context)
        val config = WidgetConfigStore.load(context, appWidgetId) ?: return

        if (!success) {
            // Deliberately no auto-reset here: a Toast is dropped on Android 13+
            // (and on several OEM skins) whenever the app's notifications are
            // blocked, so the red icon is the only feedback guaranteed to reach
            // the user. The error text itself is deliberately not rendered inside
            // the widget (it looked like a mini-toast there) - it goes to the
            // Toast and the notification only. Cleared on the next tap or when
            // the widget is reconfigured.
            manager.updateAppWidget(
                appWidgetId,
                buildViews(context, appWidgetId, config, STATUS_ERROR),
            )
            return
        }

        manager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId, config, STATUS_OK))

        Handler(Looper.getMainLooper()).postDelayed({
            val cfg = WidgetConfigStore.load(context, appWidgetId) ?: return@postDelayed
            val mgr = AppWidgetManager.getInstance(context)
            mgr.updateAppWidget(appWidgetId, buildViews(context, appWidgetId, cfg, STATUS_IDLE))
        }, 1500)
    }

    companion object {
        private const val TAG = "MqttWidgetProvider"
        const val ACTION_PUBLISH = "com.chn.mqttwidget.ACTION_PUBLISH"
        const val ACTION_RESULT = "com.chn.mqttwidget.ACTION_RESULT"
        const val EXTRA_SUCCESS = "extra_success"
        const val EXTRA_ERROR = "extra_error"

        private const val STATUS_IDLE = 0
        private const val STATUS_OK = 1
        private const val STATUS_ERROR = 2

        fun updateWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val config = WidgetConfigStore.load(context, appWidgetId)
            manager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId, config, STATUS_IDLE))
        }

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, MqttWidgetProvider::class.java)
            )
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun buildViews(
            context: Context,
            appWidgetId: Int,
            config: WidgetConfig?,
            status: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            val tint = when (status) {
                STATUS_OK -> ContextCompat.getColor(context, R.color.status_ok)
                STATUS_ERROR -> ContextCompat.getColor(context, R.color.status_error)
                else -> ContextCompat.getColor(context, R.color.widget_icon)
            }
            val iconSize = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
            views.setImageViewBitmap(
                R.id.widget_icon,
                MaterialIconFont.bitmap(
                    context,
                    config?.icon ?: IconCatalog.DEFAULT_ICON,
                    tint,
                    iconSize,
                ),
            )
            views.setTextViewText(
                R.id.widget_label,
                config?.label?.ifBlank { context.getString(R.string.app_name) }
                    ?: context.getString(R.string.app_name),
            )

            val publishIntent = Intent(context, MqttWidgetProvider::class.java).apply {
                action = ACTION_PUBLISH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                publishIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
        }
    }
}
