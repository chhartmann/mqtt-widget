package com.chn.mqttwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MqttPublishWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appWidgetId = inputData.getInt(KEY_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "Work started without a valid widget id")
            return Result.failure()
        }

        val config = WidgetConfigStore.load(applicationContext, appWidgetId)
        if (config == null) {
            Log.e(TAG, "No configuration stored for widget #$appWidgetId")
            broadcastResult(
                appWidgetId,
                success = false,
                error = applicationContext.getString(R.string.error_no_config),
            )
            return Result.failure()
        }

        return try {
            withContext(Dispatchers.IO) {
                MqttPublisher.publish(config)
            }
            broadcastResult(appWidgetId, success = true)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Throwable, not Exception: an Error (e.g. NoClassDefFoundError from a
            // stripped or broken dependency) would otherwise escape doWork, fail the
            // work silently and never notify the widget.
            Log.e(TAG, "Publish failed for widget #$appWidgetId", e)
            broadcastResult(
                appWidgetId,
                success = false,
                error = PublishErrors.describe(e, applicationContext),
            )
            Result.failure()
        }
    }

    private fun broadcastResult(appWidgetId: Int, success: Boolean, error: String? = null) {
        val intent = Intent(applicationContext, MqttWidgetProvider::class.java).apply {
            action = MqttWidgetProvider.ACTION_RESULT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(MqttWidgetProvider.EXTRA_SUCCESS, success)
            error?.let { putExtra(MqttWidgetProvider.EXTRA_ERROR, it) }
        }
        applicationContext.sendBroadcast(intent)
    }

    companion object {
        private const val TAG = "MqttPublishWorker"
        private const val KEY_WIDGET_ID = "widget_id"

        fun enqueue(context: Context, appWidgetId: Int) {
            // No network constraint here on purpose: a pending work request is
            // invisible to the user. Failing fast reports the error right away
            // (toast + notification).
            val request = OneTimeWorkRequestBuilder<MqttPublishWorker>()
                .setInputData(
                    Data.Builder().putInt(KEY_WIDGET_ID, appWidgetId).build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
