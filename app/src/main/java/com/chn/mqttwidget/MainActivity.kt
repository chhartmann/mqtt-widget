package com.chn.mqttwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private val entries = mutableListOf<Pair<Int, WidgetConfig>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.widget_list)
        emptyView = findViewById(R.id.empty_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = WidgetAdapter()

        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            showAddDialog()
        }

        // See WidgetConfigActivity.onCreate: without this permission publish
        // errors (notification + toast) never reach the user on Android 13+.
        NotificationHelper.requestPermission(this)
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        entries.clear()
        entries.addAll(WidgetConfigStore.loadAll(this))
        recyclerView.adapter?.notifyDataSetChanged()
        emptyView.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showAddDialog() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, MqttWidgetProvider::class.java))
        val unconfigured = ids.filter { WidgetConfigStore.load(this, it) == null }

        val message = if (unconfigured.isEmpty()) {
            getString(R.string.add_dialog_no_widgets)
        } else {
            getString(R.string.add_dialog_hint, unconfigured.size)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.add_dialog_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private inner class WidgetAdapter : RecyclerView.Adapter<WidgetAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.item_icon)
            val title: TextView = view.findViewById(R.id.item_title)
            val subtitle: TextView = view.findViewById(R.id.item_subtitle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_widget, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int = entries.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val (id, config) = entries[position]
            holder.icon.setImageResource(MqttWidgetProvider.iconResource(config.icon))
            holder.title.text = config.label.ifBlank {
                getString(R.string.widget_unnamed, id)
            }
            holder.subtitle.text = getString(
                R.string.item_subtitle_format,
                config.topic,
                config.serverUri,
            )

            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }
                startActivity(intent)
            }

            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.delete_dialog_title)
                    .setMessage(getString(R.string.delete_dialog_message, id))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        WidgetConfigStore.delete(this@MainActivity, id)
                        reload()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
        }
    }
}
