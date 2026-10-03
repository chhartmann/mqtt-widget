package com.chn.mqttwidget

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object WidgetConfigStore {

    private const val PREFS_NAME = "widget_configs"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, appWidgetId: Int, config: WidgetConfig) {
        prefs(context).edit().putString("cfg_$appWidgetId", config.toJson().toString()).apply()
    }

    fun load(context: Context, appWidgetId: Int): WidgetConfig? {
        val raw = prefs(context).getString("cfg_$appWidgetId", null) ?: return null
        return try {
            WidgetConfig.fromJson(JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }

    fun delete(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove("cfg_$appWidgetId").apply()
    }

    fun loadAll(context: Context): List<Pair<Int, WidgetConfig>> {
        return prefs(context).all.keys
            .filter { it.startsWith("cfg_") }
            .mapNotNull { key ->
                val id = key.removePrefix("cfg_").toIntOrNull() ?: return@mapNotNull null
                load(context, id)?.let { id to it }
            }
            .sortedBy { it.first }
    }
}
